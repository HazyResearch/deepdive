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
  n_numa_nodes = numa_max_node() + 1;
  if (opts.n_datacopy > 0 && opts.n_datacopy < n_numa_nodes) {
    n_numa_nodes = opts.n_datacopy - 1;
  }

  // max possible threads per NUMA node
  n_thread_per_numa = (sysconf(_SC_NPROCESSORS_CONF)) / n_numa_nodes;
  // n_thread_per_numa = 1;

  // copy factor graphs
  for (int i = 0; i < n_numa_nodes; ++i) {
    numa_run_on_node(i);
    numa_set_localalloc();

    std::cout << "CREATE CFG ON NODE ..." << i << std::endl;
    sampler.push_back(SingleNodeSampler(
        std::unique_ptr<CompiledFactorGraph>(
            i == 0 ? p_cfg.release() : new CompiledFactorGraph(*p_cfg)),
        weights, n_thread_per_numa /* TODO fold this into opts */, i, opts));
  }
}

void GibbsSampling::inference(int n_epoch, bool is_quiet) {
  n_epoch = compute_n_epochs(n_epoch);
  int nvar = sampler[0].fg.size.num_variables;
  Timer t_total, t;

  for (auto &s : sampler) s.infrs.clear_variabletally();

  // inference epochs
  for (int i_epoch = 0; i_epoch < n_epoch; ++i_epoch) {
    if (!is_quiet) {
      std::cout << std::setprecision(2) << "INFERENCE EPOCH "
                << i_epoch * n_numa_nodes << "~"
                << ((i_epoch + 1) * n_numa_nodes) << "...." << std::flush;
    }

    // restart timer
    t.restart();

    // sample
    for (auto &s : sampler) s.sample(i_epoch);

    // wait for samplers to finish
    for (auto &s : sampler) s.wait();

    double elapsed = t.elapsed();
    if (!is_quiet) {
      std::cout << "" << elapsed << " sec.";
      std::cout << "," << (nvar * n_numa_nodes) / elapsed << " vars/sec"
                << std::endl;
    }
  }

  double elapsed = t_total.elapsed();
  std::cout << "TOTAL INFERENCE TIME: " << elapsed << " sec." << std::endl;
}

void GibbsSampling::learn(int n_epoch, int n_sample_per_epoch, double stepsize,
                          double decay, double reg_param, bool is_quiet,
                          regularization reg) {
  n_epoch = compute_n_epochs(n_epoch);

  InferenceResult &infrs = sampler[0].infrs;
  int nvar = infrs.nvars;
  int nweight = infrs.nweights;

  Timer t_total, t;

  double current_stepsize = stepsize;
  std::unique_ptr<double[]> ori_weights(new double[nweight]);
  memcpy(ori_weights.get(), infrs.weight_values.get(),
         sizeof(*ori_weights.get()) * nweight);

  // learning epochs
  for (int i_epoch = 0; i_epoch < n_epoch; ++i_epoch) {
    if (!is_quiet) {
      std::cout << std::setprecision(2) << "LEARNING EPOCH "
                << i_epoch * n_numa_nodes << "~"
                << ((i_epoch + 1) * n_numa_nodes) << "...." << std::flush;
    }

    t.restart();

    // performs stochastic gradient descent with sampling
    for (auto &s : sampler) s.sample_sgd(current_stepsize);

    // wait the samplers to finish
    for (auto &s : sampler) s.wait_sgd();

    // sum the weights and store in the first factor graph
    // the average weights will be calculated and assigned to all factor graphs
    for (int i = 1; i < n_numa_nodes; ++i) {
      auto &sampler_other = sampler[i];
      for (int j = 0; j < nweight; ++j) {
        infrs.weight_values[j] += sampler_other.infrs.weight_values[j];
      }
    }

    // calculate average weights and regularize weights
    for (int j = 0; j < nweight; ++j) {
      infrs.weight_values[j] /= n_numa_nodes;
      if (!infrs.weights_isfixed[j]) {
        if (reg == REG_L2)
          infrs.weight_values[j] *=
              (1.0 / (1.0 + reg_param * current_stepsize));
        else  // l1
          infrs.weight_values[j] += reg_param * (infrs.weight_values[j] < 0);
      }
    }

    // set weights for other factor graph to be the same as the first factor
    // graph
    for (int i = 1; i < n_numa_nodes; ++i) {
      auto &sampler_other = sampler[i];
      for (int j = 0; j < nweight; ++j) {
        if (!infrs.weights_isfixed[j]) {
          sampler_other.infrs.weight_values[j] = infrs.weight_values[j];
        }
      }
    }

    // calculate the norms of the difference of weights from the current epoch
    // and last epoch
    double lmax = -1000000;
    double l2 = 0.0;
    for (int j = 0; j < nweight; ++j) {
      double diff = fabs(ori_weights[j] - infrs.weight_values[j]);
      ori_weights[j] = infrs.weight_values[j];
      l2 += diff * diff;
      if (lmax < diff) {
        lmax = diff;
      }
    }
    lmax = lmax / current_stepsize;

    double elapsed = t.elapsed();
    if (!is_quiet) {
      std::cout << "" << elapsed << " sec.";
      std::cout << "," << (nvar * n_numa_nodes) / elapsed << " vars/sec."
                << ",stepsize=" << current_stepsize << ",lmax=" << lmax
                << ",l2=" << sqrt(l2) / current_stepsize << std::endl;
    }

    current_stepsize = current_stepsize * decay;
  }

  double elapsed = t_total.elapsed();
  std::cout << "TOTAL LEARNING TIME: " << elapsed << " sec." << std::endl;
}

void GibbsSampling::dump_weights(bool is_quiet) {
  // learning weights snippets
  InferenceResult &infrs = sampler[0].infrs;

  if (!is_quiet) {
    std::cout << "LEARNING SNIPPETS (QUERY WEIGHTS):" << std::endl;
    int ct = 0;
    for (long j = 0; j < infrs.nweights; ++j) {
      ++ct;
      std::cout << "   " << j << " " << infrs.weight_values[j] << std::endl;
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
  for (long j = 0; j < infrs.nweights; ++j) {
    fout_text << j << " " << infrs.weight_values[j] << std::endl;
  }
  fout_text.close();
}

void GibbsSampling::aggregate_results_and_dump(bool is_quiet) {
  InferenceResult &infrs = sampler[0].infrs;
  // sum of variable assignments
  std::unique_ptr<double[]> agg_means(new double[infrs.nvars]);
  // number of samples
  std::unique_ptr<double[]> agg_nsamples(new double[infrs.nvars]);
  std::unique_ptr<int[]> multinomial_tallies(new int[infrs.ntallies]);

  for (long j = 0; j < infrs.nvars; ++j) {
    agg_means[j] = 0;
    agg_nsamples[j] = 0;
  }

  for (long j = 0; j < infrs.ntallies; ++j) {
    multinomial_tallies[j] = 0;
  }

  // sum variable assignments over all NUMA nodes
  for (int i = 0; i < n_numa_nodes; ++i) {
    const InferenceResult &infrs_i = sampler[i].infrs;
    for (long j = 0; j < infrs_i.nvars; ++j) {
      const Variable &variable = sampler[i].fg.variables[j];
      agg_means[variable.id] += infrs_i.agg_means[variable.id];
      agg_nsamples[variable.id] += infrs_i.agg_nsamples[variable.id];
    }
    for (long j = 0; j < infrs_i.ntallies; ++j) {
      multinomial_tallies[j] += infrs_i.multinomial_tallies[j];
    }
  }

  // inference snippets
  if (!is_quiet) {
    std::cout << "INFERENCE SNIPPETS (QUERY VARIABLES):" << std::endl;
    int ct = 0;
    for (long j = 0; j < sampler[0].fg.size.num_variables; ++j) {
      const Variable &variable = sampler[0].fg.variables[j];
      if (!variable.is_evid || opts.should_sample_evidence) {
        ++ct;
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
              for (size_t j = 0; j < variable.cardinality; ++j)
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
  std::string filename_text(opts.output_folder + "/inference_result.out.text");
  std::cout << "DUMPING... TEXT    : " << filename_text << std::endl;
  std::ofstream fout_text(filename_text);
  infrs.dump_marginals(fout_text);
  fout_text.close();

  if (!is_quiet) infrs.show_histogram(std::cout);
}

// compute number of NUMA-aware epochs for learning or inference
int GibbsSampling::compute_n_epochs(int n_epoch) {
  return std::ceil((double)n_epoch / n_numa_nodes);
}

}  // namespace dd
