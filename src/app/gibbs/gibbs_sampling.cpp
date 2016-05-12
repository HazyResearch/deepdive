#include "app/gibbs/gibbs_sampling.h"
#include "app/gibbs/single_node_sampler.h"
#include "io/binary_format.h"
#include "common.h"
#include "timer.h"
#include <fstream>
#include <memory>
#include <unistd.h>

dd::GibbsSampling::GibbsSampling(const CmdParser *const _p_cmd_parser,
                                 bool sample_evidence, int burn_in,
                                 bool learn_non_evidence)
    : p_fg(NULL),
      p_cmd_parser(_p_cmd_parser),
      sample_evidence(sample_evidence),
      burn_in(burn_in),
      learn_non_evidence(learn_non_evidence) {}

void dd::GibbsSampling::init(CompiledFactorGraph *const p_cfg, int n_datacopy) {
  // the highest node number available
  n_numa_nodes = numa_max_node();

  // if n_datacopy is valid, use it, otherwise, use numa_max_node
  if (n_datacopy >= 1 && n_datacopy <= n_numa_nodes + 1) {
    n_numa_nodes = n_datacopy - 1;
  }

  // max possible threads per NUMA node
  n_thread_per_numa = (sysconf(_SC_NPROCESSORS_CONF)) / (n_numa_nodes + 1);
  // n_thread_per_numa = 1;

  this->factorgraphs.push_back(*p_cfg);

  // copy factor graphs
  for (int i = 1; i <= n_numa_nodes; i++) {
    numa_run_on_node(i);
    numa_set_localalloc();

    std::cout << "CREATE CFG ON NODE ..." << i << std::endl;
    dd::CompiledFactorGraph cfg(p_cfg->n_var, p_cfg->n_factor, p_cfg->n_weight,
                                p_cfg->n_edge);

    cfg.copy_from(p_cfg);

    this->factorgraphs.push_back(cfg);
  }
};

void dd::GibbsSampling::do_resume(bool is_quiet, int n_datacopy, long n_var,
                                  long n_factor, long n_weight, long n_edge) {
  n_numa_nodes = numa_max_node();

  // if n_datacopy is valid, use it, otherwise, use numa_max_node
  if (n_datacopy >= 1 && n_datacopy <= n_numa_nodes + 1) {
    n_numa_nodes = n_datacopy - 1;
  }

  // max possible threads per NUMA node
  n_thread_per_numa = (sysconf(_SC_NPROCESSORS_CONF)) / (n_numa_nodes + 1);

  // Resume from checkpoint file while still maintaining NUMA awareness.
  for (int i = 1; i <= n_numa_nodes; i++) {
    numa_run_on_node(i);
    numa_set_localalloc();

    std::cout << "RESUMING CFG ON NODE " << i << "..." << std::endl;
    dd::CompiledFactorGraph cfg(n_var, n_factor, n_weight, n_edge);

    resume(p_cmd_parser->graph_snapshot_file, i, cfg);

    this->factorgraphs.push_back(cfg);
  }

  this->load_weights_snapshot(is_quiet);
}

void dd::GibbsSampling::do_checkpoint(bool is_quiet) {
  // TODO: Implement me!
  return;
}

void dd::GibbsSampling::save_weights_snapshot(const bool is_quiet) {
  // Filename is p_cmd_parser->saved_weights_filename;
  std::ofstream file;
  file.open(p_cmd_parser->weights_snapshot_file,
            std::ios::out | std::ios::binary);

  /*
   * Dump weights from only the first factor graph, since we have aggregated
   * the results in the first factor graph.
   *
   * TODO: This means we could as easily move this function to the FactorGraph
   * class, except it'll be awkward if load_weights_snapshot is still in
   * GibbsSampling.
   */
  const CompiledFactorGraph &cfg = factorgraphs[0];
  long n_weights = cfg.n_weight;
  for (long i = 0; i < n_weights; i++) {
    long wid = i;
    file.write((char *)&wid, sizeof(long));
    file.write((char *)&cfg.infrs->weight_values[wid], sizeof(double));
  }

  file.close();
}

void dd::GibbsSampling::load_weights_snapshot(const bool is_quiet) {
  // Filename is p_cmd_parser->saved_weights_filename;
  std::ifstream file;
  file.open(p_cmd_parser->weights_snapshot_file,
            std::ios::in | std::ios::binary);

  /*
   * The number of weights in the file should match the one stored in the main
   * copy of the factor graph.
   */
  long n_weight = p_fg->n_weight;
  for (long i = 0; i < n_weight; i++) {
    long wid;
    double value;

    file.read((char *)&wid, sizeof(long));
    file.read((char *)&value, sizeof(double));

    /*
     * In addition, we assert that wid is stored in increasing order from 0
     * to n_weights in the binary_file.
     */
    assert(wid == i);

    for (int j = 0; j < n_numa_nodes; j++) {
      factorgraphs[j].infrs->weight_values[wid] = value;
    }
  }

  file.close();
}

void dd::GibbsSampling::inference(const int &n_epoch, const bool is_quiet) {
  Timer t_total;

  Timer t;
  int nvar = this->factorgraphs[0].n_var;
  int nnode = n_numa_nodes + 1;

  // single node samplers
  std::vector<SingleNodeSampler> single_node_samplers;
  for (int i = 0; i <= n_numa_nodes; i++) {
    single_node_samplers.push_back(SingleNodeSampler(&this->factorgraphs[i],
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

void dd::GibbsSampling::learn(const int &n_epoch, const int &n_sample_per_epoch,
                              const double &stepsize, const double &decay,
                              const double reg_param, const bool is_quiet,
                              const regularization reg) {
  Timer t_total;

  double current_stepsize = stepsize;

  Timer t;
  int nvar = this->factorgraphs[0].n_var;
  int nnode = n_numa_nodes + 1;
  int nweight = this->factorgraphs[0].n_weight;

  // single node samplers
  std::vector<SingleNodeSampler> single_node_samplers;
  for (int i = 0; i <= n_numa_nodes; i++) {
    single_node_samplers.push_back(
        SingleNodeSampler(&this->factorgraphs[i], n_thread_per_numa, i, false,
                          0, learn_non_evidence));
  }

  std::unique_ptr<double[]> ori_weights(new double[nweight]);
  memcpy(ori_weights.get(), this->factorgraphs[0].infrs->weight_values,
         sizeof(double) * nweight);

  // learning epochs
  for (int i_epoch = 0; i_epoch < n_epoch; i_epoch++) {
    if (!is_quiet) {
      std::cout << std::setprecision(2) << "LEARNING EPOCH " << i_epoch * nnode
                << "~" << ((i_epoch + 1) * nnode) << "...." << std::flush;
    }

    t.restart();

    // set stepsize
    for (int i = 0; i < nnode; i++) {
      single_node_samplers[i].p_fg->stepsize = current_stepsize;
    }

    // performs stochastic gradient descent with sampling
    for (int i = 0; i < nnode; i++) {
      single_node_samplers[i].sample_sgd();
    }

    // wait the samplers to finish
    for (int i = 0; i < nnode; i++) {
      single_node_samplers[i].wait_sgd();
    }

    CompiledFactorGraph &cfg = this->factorgraphs[0];

    // sum the weights and store in the first factor graph
    // the average weights will be calculated and assigned to all factor graphs
    for (int i = 1; i <= n_numa_nodes; i++) {
      CompiledFactorGraph &cfg_other = this->factorgraphs[i];
      for (int j = 0; j < nweight; j++) {
        cfg.infrs->weight_values[j] += cfg_other.infrs->weight_values[j];
      }
    }

    // calculate average weights and regularize weights
    for (int j = 0; j < nweight; j++) {
      cfg.infrs->weight_values[j] /= nnode;
      if (cfg.infrs->weights_isfixed[j] == false) {
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
      CompiledFactorGraph &cfg_other = this->factorgraphs[i];
      for (int j = 0; j < nweight; j++) {
        if (cfg.infrs->weights_isfixed[j] == false) {
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

void dd::GibbsSampling::dump_weights(const bool is_quiet) {
  // learning weights snippets
  CompiledFactorGraph const &cfg = this->factorgraphs[0];
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

void dd::GibbsSampling::aggregate_results_and_dump(const bool is_quiet) {
  // sum of variable assignments
  std::unique_ptr<double[]> agg_means(new double[factorgraphs[0].n_var]);
  // number of samples
  std::unique_ptr<double[]> agg_nsamples(new double[factorgraphs[0].n_var]);
  std::unique_ptr<int[]> multinomial_tallies(
      new int[factorgraphs[0].infrs->ntallies]);

  for (long i = 0; i < factorgraphs[0].n_var; i++) {
    agg_means[i] = 0;
    agg_nsamples[i] = 0;
  }

  for (long i = 0; i < factorgraphs[0].infrs->ntallies; i++) {
    multinomial_tallies[i] = 0;
  }

  // sum variable assignments over all NUMA nodes
  for (int i = 0; i <= n_numa_nodes; i++) {
    const CompiledFactorGraph &cfg = factorgraphs[i];
    for (long i = 0; i < factorgraphs[0].n_var; i++) {
      const Variable &variable = factorgraphs[0].variables[i];
      agg_means[variable.id] += cfg.infrs->agg_means[variable.id];
      agg_nsamples[variable.id] += cfg.infrs->agg_nsamples[variable.id];
    }
    for (long i = 0; i < factorgraphs[0].infrs->ntallies; i++) {
      multinomial_tallies[i] += cfg.infrs->multinomial_tallies[i];
    }
  }

  // inference snippets
  if (!is_quiet) {
    std::cout << "INFERENCE SNIPPETS (QUERY VARIABLES):" << std::endl;
    int ct = 0;
    for (long i = 0; i < factorgraphs[0].n_var; i++) {
      const Variable &variable = factorgraphs[0].variables[i];
      if (variable.is_evid == false || sample_evidence) {
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
  for (long i = 0; i < factorgraphs[0].n_var; i++) {
    const Variable &variable = factorgraphs[0].variables[i];
    if (variable.is_evid == true && !sample_evidence) {
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
    for (long i = 0; i < factorgraphs[0].n_var; i++) {
      const Variable &variable = factorgraphs[0].variables[i];
      if (variable.is_evid == true && !sample_evidence) {
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
