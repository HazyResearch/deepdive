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
                             const Weight weights[],
                             const CmdParser *const p_cmd_parser,
                             bool sample_evidence, int burn_in,
                             bool learn_non_evidence, int n_datacopy)
    : weights(weights),
      p_cmd_parser(p_cmd_parser),
      cfg(*p_cfg),
      sample_evidence(sample_evidence),
      burn_in(burn_in),
      learn_non_evidence(learn_non_evidence) {
  // the highest node number available
  n_numa_nodes = numa_max_node();

  // if n_datacopy is valid, use it, otherwise, use numa_max_node
  if (n_datacopy >= 1 && n_datacopy <= n_numa_nodes + 1) {
    n_numa_nodes = n_datacopy - 1;
  }

  // max possible threads per NUMA node
  n_thread_per_numa = (sysconf(_SC_NPROCESSORS_CONF)) / (n_numa_nodes + 1);
  // n_thread_per_numa = 1;

  factorgraphs[0] = std::move(p_cfg);

  // copy factor graphs
  for (int i = 1; i <= n_numa_nodes; i++) {
    numa_run_on_node(i);
    numa_set_localalloc();

    std::cout << "CREATE CFG ON NODE ..." << i << std::endl;
    factorgraphs[i].reset(new CompiledFactorGraph(*p_cfg));
  }
};

void GibbsSampling::inference(const int &n_epoch, const bool is_quiet) {
  Timer t_total;

  Timer t;
  int nvar = cfg.size.num_variables;
  int nnode = n_numa_nodes + 1;

  // single node samplers
  std::vector<SingleNodeSampler> single_node_samplers;
  for (int i = 0; i <= n_numa_nodes; i++) {
    single_node_samplers.push_back(SingleNodeSampler(*factorgraphs[i], weights,
                                                     n_thread_per_numa, i,
                                                     sample_evidence, burn_in));
  }

  for (int i = 0; i <= n_numa_nodes; i++) {
    single_node_samplers[i].clear_variabletally();
  }

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
      single_node_samplers[i].sample(i_epoch);
    }

    // wait for samplers to finish
    for (int i = 0; i < nnode; i++) {
      single_node_samplers[i].wait();
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
  std::cerr << cfg.size << std::endl;

  Timer t;
  int nvar = cfg.size.num_variables;
  int nnode = n_numa_nodes + 1;
  int nweight = cfg.size.num_weights;

  // single node samplers
  std::vector<SingleNodeSampler> single_node_samplers;
  for (int i = 0; i <= n_numa_nodes; i++) {
    single_node_samplers.push_back(
        SingleNodeSampler(*factorgraphs[i], weights, n_thread_per_numa, i,
                          false, 0, learn_non_evidence));
  }

  std::cerr << cfg.size << std::endl;

  dprintf("%p %d\n", (cfg.infrs->weight_values.get()), nweight);
  std::unique_ptr<double[]> ori_weights(new double[nweight]);
  memcpy(ori_weights.get(), cfg.infrs->weight_values.get(),
         sizeof(*ori_weights.get()) * nweight);
  std::cerr << cfg.size << std::endl;

  // learning epochs
  for (int i_epoch = 0; i_epoch < n_epoch; i_epoch++) {
    if (!is_quiet) {
      std::cout << std::setprecision(2) << "LEARNING EPOCH " << i_epoch * nnode
                << "~" << ((i_epoch + 1) * nnode) << "...." << std::flush;
    }

    t.restart();

    // performs stochastic gradient descent with sampling
    for (int i = 0; i < nnode; i++) {
      single_node_samplers[i].sample_sgd(current_stepsize);
    }

    // wait the samplers to finish
    for (int i = 0; i < nnode; i++) {
      single_node_samplers[i].wait_sgd();
    }

    // sum the weights and store in the first factor graph
    // the average weights will be calculated and assigned to all factor graphs
    for (int i = 1; i <= n_numa_nodes; i++) {
      auto &cfg_other = *factorgraphs[i];
      for (int j = 0; j < nweight; j++) {
        cfg.infrs->weight_values[j] += cfg_other.infrs->weight_values[j];
      }
    }

    // calculate average weights and regularize weights
    for (int j = 0; j < nweight; j++) {
      cfg.infrs->weight_values[j] /= nnode;
      if (!cfg.infrs->weights_isfixed[j]) {
        if (reg == REG_L2)
          cfg.infrs->weight_values[j] *=
              (1.0 / (1.0 + reg_param * current_stepsize));
        else  // l1
          cfg.infrs->weight_values[j] +=
              reg_param * (cfg.infrs->weight_values[j] < 0);
      }
    }

    // set weights for other factor graph to be the same as the first factor
    // graph
    for (int i = 1; i <= n_numa_nodes; i++) {
      auto &cfg_other = *factorgraphs[i];
      for (int j = 0; j < nweight; j++) {
        if (!cfg.infrs->weights_isfixed[j]) {
          cfg_other.infrs->weight_values[j] = cfg.infrs->weight_values[j];
        }
      }
    }

    // calculate the norms of the difference of weights from the current epoch
    // and last epoch
    double lmax = -1000000;
    double l2 = 0.0;
    for (int i = 0; i < nweight; i++) {
      double diff = fabs(ori_weights[i] - cfg.infrs->weight_values[i]);
      ori_weights[i] = cfg.infrs->weight_values[i];
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
    for (long i = 0; i < cfg.infrs->nweights; i++) {
      ct++;
      std::cout << "   " << i << " " << cfg.infrs->weight_values[i]
                << std::endl;
      if (ct % 10 == 0) {
        break;
      }
    }
    std::cout << "   ..." << std::endl;
  }

  // dump learned weights
  std::string filename_text;
  filename_text =
      p_cmd_parser->output_folder + "/inference_result.out.weights.text";

  std::cout << "DUMPING... TEXT    : " << filename_text << std::endl;

  std::ofstream fout_text(filename_text.c_str());
  for (long i = 0; i < cfg.infrs->nweights; i++) {
    fout_text << i << " " << cfg.infrs->weight_values[i] << std::endl;
  }
  fout_text.close();
}

void GibbsSampling::aggregate_results_and_dump(const bool is_quiet) {
  // sum of variable assignments
  std::unique_ptr<double[]> agg_means(
      new double[cfg.size.num_variables]);
  // number of samples
  std::unique_ptr<double[]> agg_nsamples(
      new double[cfg.size.num_variables]);
  std::unique_ptr<int[]> multinomial_tallies(
      new int[cfg.infrs->ntallies]);

  for (long i = 0; i < cfg.size.num_variables; i++) {
    agg_means[i] = 0;
    agg_nsamples[i] = 0;
  }

  for (long i = 0; i < cfg.infrs->ntallies; i++) {
    multinomial_tallies[i] = 0;
  }

  // sum variable assignments over all NUMA nodes
  for (int i = 0; i <= n_numa_nodes; i++) {
    const CompiledFactorGraph &cfg = *factorgraphs[i];
    for (long i = 0; i < cfg.size.num_variables; i++) {
      const Variable &variable = cfg.variables[i];
      agg_means[variable.id] += cfg.infrs->agg_means[variable.id];
      agg_nsamples[variable.id] += cfg.infrs->agg_nsamples[variable.id];
    }
    for (long i = 0; i < cfg.infrs->ntallies; i++) {
      multinomial_tallies[i] += cfg.infrs->multinomial_tallies[i];
    }
  }

  // inference snippets
  if (!is_quiet) {
    std::cout << "INFERENCE SNIPPETS (QUERY VARIABLES):" << std::endl;
    int ct = 0;
    for (long i = 0; i < cfg.size.num_variables; i++) {
      const Variable &variable = cfg.variables[i];
      if (!variable.is_evid || sample_evidence) {
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
  filename_text = p_cmd_parser->output_folder + "/inference_result.out.text";

  std::cout << "DUMPING... TEXT    : " << filename_text << std::endl;
  std::ofstream fout_text(filename_text.c_str());
  for (long i = 0; i < cfg.size.num_variables; i++) {
    const Variable &variable = cfg.variables[i];
    if (variable.is_evid && !sample_evidence) {
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
    for (long i = 0; i < cfg.size.num_variables; i++) {
      const Variable &variable = cfg.variables[i];
      if (variable.is_evid && !sample_evidence) {
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
