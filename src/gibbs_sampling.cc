#include "gibbs_sampling.h"
#include "single_node_sampler.h"
#include "binary_format.h"
#include "common.h"
#include "timer.h"
#include <fstream>
#include <memory>
#include <unistd.h>

namespace dd {

GibbsSampling::GibbsSampling(std::unique_ptr<CompiledFactorGraph> p_cfg,
                             const Weight weights[], const CmdParser &opts)
    : weights(weights), opts(opts) {
  // the highest node number available
  n_numa_nodes = numa_max_node();

  // if n_datacopy is valid, use it, otherwise, use numa_max_node
  if (opts.n_datacopy >= 1 && opts.n_datacopy <= n_numa_nodes + 1) {
    n_numa_nodes = opts.n_datacopy - 1;
  }

  // max possible threads per NUMA node
  n_thread_per_numa = (sysconf(_SC_NPROCESSORS_CONF)) / (n_numa_nodes + 1);
  // n_thread_per_numa = 1;

  // copy factor graphs
  for (int i = 0; i <= n_numa_nodes; ++i) {
    numa_run_on_node(i);
    numa_set_localalloc();

    std::cout << "CREATE CFG ON NODE ..." << i << std::endl;
    sampler.push_back(SingleNodeSampler(
        std::unique_ptr<CompiledFactorGraph>(
            i == 0 ? p_cfg.release() : new CompiledFactorGraph(*p_cfg)),
        weights, n_thread_per_numa /* TODO fold this into opts */, i, opts));
  }
};

void GibbsSampling::inference(const int &n_epoch, const bool is_quiet) {
  Timer t_total;

  Timer t;
  int nvar = sampler[0].fg.size.num_variables;
  int nnode = n_numa_nodes + 1;

  // inference epochs
  for (int i_epoch = 0; i_epoch < n_epoch; i_epoch++) {
    if (!is_quiet) {
      std::cout << std::setprecision(2) << "INFERENCE EPOCH " << i_epoch * nnode
                << "~" << ((i_epoch + 1) * nnode) << "...." << std::flush;
    }

    // restart timer
    t.restart();

    // sample
    for (int i = 0; i < nnode; i++) {
      sampler[i].sample(i_epoch);
    }

    // wait for samplers to finish
    for (int i = 0; i < nnode; i++) {
      sampler[i].wait();
    }

    double elapsed = t.elapsed();
    if (!is_quiet) {
      std::cout << "" << elapsed << " sec.";
      std::cout << "," << (nvar * nnode) / elapsed << " vars/sec" << std::endl;
    }
  }

  double elapsed = t_total.elapsed();
  std::cout << "TOTAL INFERENCE TIME: " << elapsed << " sec." << std::endl;
}

void GibbsSampling::learn(const int &n_epoch, const int &n_sample_per_epoch,
                          const double &stepsize, const double &decay,
                          const double reg_param, const bool is_quiet,
                          const regularization reg) {
  Timer t_total;

  double current_stepsize = stepsize;
  std::cerr << sampler[0].fg.size << std::endl;

  Timer t;
  int nvar = sampler[0].fg.size.num_variables;
  int nnode = n_numa_nodes + 1;
  int nweight = sampler[0].fg.size.num_weights;

  std::cerr << sampler[0].fg.size << std::endl;

  std::unique_ptr<double[]> ori_weights(new double[nweight]);
  memcpy(ori_weights.get(), sampler[0].infrs.weight_values.get(),
         sizeof(*ori_weights.get()) * nweight);
  std::cerr << sampler[0].fg.size << std::endl;

  // learning epochs
  for (int i_epoch = 0; i_epoch < n_epoch; i_epoch++) {
    if (!is_quiet) {
      std::cout << std::setprecision(2) << "LEARNING EPOCH " << i_epoch * nnode
                << "~" << ((i_epoch + 1) * nnode) << "...." << std::flush;
    }

    t.restart();

    // performs stochastic gradient descent with sampling
    for (int i = 0; i < nnode; i++) {
      sampler[i].sample_sgd(current_stepsize);
    }

    // wait the samplers to finish
    for (int i = 0; i < nnode; i++) {
      sampler[i].wait_sgd();
    }

    // sum the weights and store in the first factor graph
    // the average weights will be calculated and assigned to all factor graphs
    for (int i = 1; i <= n_numa_nodes; i++) {
      auto &sampler_other = sampler[i];
      for (int j = 0; j < nweight; j++) {
        sampler[0].infrs.weight_values[j] +=
            sampler_other.infrs.weight_values[j];
      }
    }

    // calculate average weights and regularize weights
    for (int j = 0; j < nweight; j++) {
      sampler[0].infrs.weight_values[j] /= nnode;
      if (!sampler[0].infrs.weights_isfixed[j]) {
        if (reg == REG_L2)
          sampler[0].infrs.weight_values[j] *=
              (1.0 / (1.0 + reg_param * current_stepsize));
        else  // l1
          sampler[0].infrs.weight_values[j] +=
              reg_param * (sampler[0].infrs.weight_values[j] < 0);
      }
    }

    // set weights for other factor graph to be the same as the first factor
    // graph
    for (int i = 1; i <= n_numa_nodes; i++) {
      auto &sampler_other = sampler[i];
      for (int j = 0; j < nweight; j++) {
        if (!sampler[0].infrs.weights_isfixed[j]) {
          sampler_other.infrs.weight_values[j] =
              sampler[0].infrs.weight_values[j];
        }
      }
    }

    // calculate the norms of the difference of weights from the current epoch
    // and last epoch
    double lmax = -1000000;
    double l2 = 0.0;
    for (int i = 0; i < nweight; i++) {
      double diff = fabs(ori_weights[i] - sampler[0].infrs.weight_values[i]);
      ori_weights[i] = sampler[0].infrs.weight_values[i];
      l2 += diff * diff;
      if (lmax < diff) {
        lmax = diff;
      }
    }
    lmax = lmax / current_stepsize;

    double elapsed = t.elapsed();
    if (!is_quiet) {
      std::cout << "" << elapsed << " sec.";
      std::cout << "," << (nvar * nnode) / elapsed << " vars/sec."
                << ",stepsize=" << current_stepsize << ",lmax=" << lmax
                << ",l2=" << sqrt(l2) / current_stepsize << std::endl;
    }

    current_stepsize = current_stepsize * decay;
  }

  double elapsed = t_total.elapsed();
  std::cout << "TOTAL LEARNING TIME: " << elapsed << " sec." << std::endl;
}

void GibbsSampling::dump_weights(const bool is_quiet) {
  // learning weights snippets
  if (!is_quiet) {
    std::cout << "LEARNING SNIPPETS (QUERY WEIGHTS):" << std::endl;
    int ct = 0;
    for (long i = 0; i < sampler[0].infrs.nweights; i++) {
      ct++;
      std::cout << "   " << i << " " << sampler[0].infrs.weight_values[i]
                << std::endl;
      if (ct % 10 == 0) {
        break;
      }
    }
    std::cout << "   ..." << std::endl;
  }

  // dump learned weights
  std::string filename_text;
  filename_text = opts.output_folder + "/inference_result.out.weights.text";

  std::cout << "DUMPING... TEXT    : " << filename_text << std::endl;

  std::ofstream fout_text(filename_text.c_str());
  for (long i = 0; i < sampler[0].infrs.nweights; i++) {
    fout_text << i << " " << sampler[0].infrs.weight_values[i] << std::endl;
  }
  fout_text.close();
}

void GibbsSampling::aggregate_results_and_dump(const bool is_quiet) {
  // sum of variable assignments
  std::unique_ptr<double[]> agg_means(
      new double[sampler[0].fg.size.num_variables]);
  // number of samples
  std::unique_ptr<double[]> agg_nsamples(
      new double[sampler[0].fg.size.num_variables]);
  std::unique_ptr<int[]> multinomial_tallies(
      new int[sampler[0].infrs.ntallies]);

  for (long i = 0; i < sampler[0].fg.size.num_variables; i++) {
    agg_means[i] = 0;
    agg_nsamples[i] = 0;
  }

  for (long i = 0; i < sampler[0].infrs.ntallies; i++) {
    multinomial_tallies[i] = 0;
  }

  // sum variable assignments over all NUMA nodes
  for (int i = 0; i <= n_numa_nodes; i++) {
    const InferenceResult &infrs = sampler[i].infrs;
    for (long j = 0; j < sampler[i].fg.size.num_variables; ++j) {
      const Variable &variable = sampler[i].fg.variables[j];
      agg_means[variable.id] += infrs.agg_means[variable.id];
      agg_nsamples[variable.id] += infrs.agg_nsamples[variable.id];
    }
    for (long j = 0; j < infrs.ntallies; ++j) {
      multinomial_tallies[j] += infrs.multinomial_tallies[j];
    }
  }

  // inference snippets
  if (!is_quiet) {
    std::cout << "INFERENCE SNIPPETS (QUERY VARIABLES):" << std::endl;
    int ct = 0;
    for (long i = 0; i < sampler[0].fg.size.num_variables; i++) {
      const Variable &variable = sampler[0].fg.variables[i];
      if (!variable.is_evid || opts.should_sample_evidence) {
        ct++;
        std::cout << "   " << variable.id
                  << "  NSAMPLE=" << agg_nsamples[variable.id] << std::endl;
        switch (variable.domain_type) {
          case DTYPE_BOOLEAN:
            std::cout << "        @ 1 -> EXP="
                      << agg_means[variable.id] / agg_nsamples[variable.id]
                      << std::endl;
            break;

          case DTYPE_MULTINOMIAL: {
            const auto &print_snippet = [&multinomial_tallies, &agg_nsamples,
                                         variable](int domain_value,
                                                   int domain_index) {
              std::cout << "        @ " << domain_value << " -> EXP="
                        << 1.0 * multinomial_tallies[variable.n_start_i_tally +
                                                     domain_index] /
                               agg_nsamples[variable.id]
                        << std::endl;
            };
            if (variable.domain_map) {  // sparse
              for (const auto &entry : *variable.domain_map)
                print_snippet(entry.first, entry.second);
            } else {  // dense case
              for (size_t j = 0; j < variable.cardinality; j++)
                print_snippet(j, j);
            }
            break;
          }

          default:
            abort();
        }

        if (ct % 10 == 0) {
          break;
        }
      }
    }
    std::cout << "   ..." << std::endl;
  }

  // dump inference results
  std::string filename_text;
  filename_text = opts.output_folder + "/inference_result.out.text";

  std::cout << "DUMPING... TEXT    : " << filename_text << std::endl;
  std::ofstream fout_text(filename_text.c_str());
  for (long i = 0; i < sampler[0].fg.size.num_variables; i++) {
    const Variable &variable = sampler[0].fg.variables[i];
    if (variable.is_evid && !opts.should_sample_evidence) {
      continue;
    }

    switch (variable.domain_type) {
      case DTYPE_BOOLEAN: {
        fout_text << variable.id << " " << 1 << " "
                  << (agg_means[variable.id] / agg_nsamples[variable.id])
                  << std::endl;
        break;
      }

      case DTYPE_MULTINOMIAL: {
        const auto &print_result = [&fout_text, &multinomial_tallies,
                                    &agg_nsamples, variable](int domain_value,
                                                             int domain_index) {
          fout_text
              << variable.id << " " << domain_value << " "
              << (1.0 *
                  multinomial_tallies[variable.n_start_i_tally + domain_index] /
                  agg_nsamples[variable.id])
              << std::endl;
        };
        if (variable.domain_map) {  // sparse
          for (const auto &entry : *variable.domain_map)
            print_result(entry.first, entry.second);
        } else {  // dense
          for (size_t j = 0; j < variable.cardinality; j++) print_result(j, j);
        }
        break;
      }

      default:
        abort();
    }
  }
  fout_text.close();

  if (!is_quiet) {
    // show a histogram of inference results
    std::cout << "INFERENCE CALIBRATION (QUERY BINS):" << std::endl;
    std::vector<int> abc;
    for (int i = 0; i <= 10; i++) {
      abc.push_back(0);
    }
    int bad = 0;
    for (long i = 0; i < sampler[0].fg.size.num_variables; i++) {
      const Variable &variable = sampler[0].fg.variables[i];
      if (variable.is_evid && !opts.should_sample_evidence) {
        continue;
      }
      int bin = (int)(agg_means[variable.id] / agg_nsamples[variable.id] * 10);
      if (bin >= 0 && bin <= 10) {
        abc[bin]++;
      } else {
        bad++;
      }
    }
    abc[9] += abc[10];
    for (int i = 0; i < 10; i++) {
      std::cout << "PROB BIN 0." << i << "~0." << (i + 1) << "  -->  # "
                << abc[i] << std::endl;
    }
  }
}

}  // namespace dd
