
#include "app/gibbs/gibbs_sampling.h"
#include "app/gibbs/single_node_sampler.h"
#include "common.h"
#include <unistd.h>
#include <fstream>
#include <memory>
#include "timer.h"

dd::GibbsSampling::GibbsSampling(FactorGraph *const _p_fg,
                                 CmdParser *const _p_cmd_parser, int n_datacopy,
                                 bool sample_evidence, int burn_in,
                                 bool learn_non_evidence)
    : p_fg(_p_fg),
      p_cmd_parser(_p_cmd_parser),
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

  this->factorgraphs.push_back(*p_fg);

  // copy factor graphs
  for (int i = 1; i <= n_numa_nodes; i++) {
    numa_run_on_node(i);
    numa_set_localalloc();

    std::cout << "CREATE FG ON NODE ..." << i << std::endl;
    dd::FactorGraph fg(p_fg->n_var, p_fg->n_factor, p_fg->n_weight,
                       p_fg->n_edge);

    fg.copy_from(p_fg);

    this->factorgraphs.push_back(fg);
  }
};

void dd::GibbsSampling::inference(const int &n_epoch, const bool is_quiet,
                                  const bool is_mat, const bool is_inc) {
  Timer t_total;

  Timer t;
  int nvar = this->factorgraphs[0].n_var;
  int nnode = n_numa_nodes + 1;

  // single node samplers
  std::vector<SingleNodeSampler> single_node_samplers;
  for (int i = 0; i <= n_numa_nodes; i++) {
    factorgraphs[i].is_inc = is_inc;
    single_node_samplers.push_back(SingleNodeSampler(&this->factorgraphs[i],
                                                     n_thread_per_numa, i,
                                                     sample_evidence, burn_in));
  }

  for (int i = 0; i <= n_numa_nodes; i++) {
    single_node_samplers[i].clear_variabletally();
  }

  // inference epochs
  for (int i_epoch = 0; i_epoch < n_epoch; i_epoch++) {
    if (is_inc) {
      std::string a = p_cmd_parser->original_folder + "/mat_samples.epoch_" +
                      std::to_string(i_epoch) + "_numa_" + std::to_string(0) +
                      ".text";
      std::cout << "Loading samples... " << a << std::endl;
      std::ifstream fin(a);
      long long ct = 0;
      while (fin >> factorgraphs[0].variables[ct].next_sample) {
        // std::cout << factorgraphs[0].variables[ct].next_sample << std::endl;
        ct = ct + 1;
      }
    }

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

    if (is_mat) {
      std::cout << "    | Dumping samples for materailization..." << std::endl;
      for (int i = 0; i <= n_numa_nodes; i++) {
        std::string filename_text =
            p_cmd_parser->original_folder + "/mat_samples.epoch_" +
            std::to_string(i_epoch) + "_numa_" + std::to_string(i) + ".text";
        std::ofstream fout(filename_text.c_str());
        for (long i = 0; i < factorgraphs[0].n_var; i++) {
          const Variable &variable = factorgraphs[0].variables[i];
          if (variable.isactive) {
            fout << factorgraphs[0].infrs->assignments_evid[i] << std::endl;
          }
        }
        fout.close();
      }
    }
  }

  double elapsed = t_total.elapsed();
  std::cout << "TOTAL INFERENCE TIME: " << elapsed << " sec." << std::endl;
}

void dd::GibbsSampling::learn(const int &n_epoch, const int &n_sample_per_epoch,
                              const double &stepsize, const double &decay,
                              const double reg_param, const bool is_quiet,
                              const bool is_inc, const regularization reg) {
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

    FactorGraph &cfg = this->factorgraphs[0];

    // sum the weights and store in the first factor graph
    // the average weights will be calculated and assigned to all factor graphs
    for (int i = 1; i <= n_numa_nodes; i++) {
      FactorGraph &cfg_other = this->factorgraphs[i];
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
      FactorGraph &cfg_other = this->factorgraphs[i];
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

  if (is_inc) {
    std::cout << "CALCULATING DELTA WEIGHTS..." << std::endl;
    long nnot_change = 0;
    long nnot_change2 = 0;
    for (long wid = 0; wid < nweight; wid++) {
      float f1 = factorgraphs[0].infrs->weight_values[wid];
      float f2 = factorgraphs[0].old_weight_values[wid];

      if (f1 == f2) {
        nnot_change++;
      }

      factorgraphs[0].old_weight_values[wid] = f1 - f2;
      if (fabs(f1 - f2) < 0.1) {
        factorgraphs[0].old_weight_values[wid] = 0.0;
        nnot_change2++;
      }
    }

    std::cout << nnot_change << "/" << nweight << std::endl;
    std::cout << nnot_change2 << "/" << nweight << std::endl;

    std::cout << "INACTIVATE FACTORS THAT DOES NOT CHANGE." << std::endl;
    for (long long fid = 0; fid < factorgraphs[0].n_edge; fid++) {
      long long wid = factorgraphs[0].compact_factors_weightids[fid];
      if (factorgraphs[0].old_weight_values[wid] == 0) {
        factorgraphs[0].compact_factors_weightids[fid] = -1;
      }
      // std::cout << factorgraphs[0].compact_factors_weightids[fid] <<
      // std::endl;
    }
  }
}

void dd::GibbsSampling::dump_weights(const bool is_quiet, int inc) {
  // learning weights snippets
  FactorGraph const &cfg = this->factorgraphs[0];
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
  if (inc == 0) {  // original
    filename_text = p_cmd_parser->output_folder;
  } else if (inc == 1) {  // materialization
    filename_text = p_cmd_parser->original_folder;
  } else {  // incremental
    filename_text = p_cmd_parser->delta_folder;
  }
  filename_text += "/inference_result.out.weights.text";
  std::cout << "DUMPING... TEXT    : " << filename_text << std::endl;

  std::ofstream fout_text(filename_text.c_str());
  for (long i = 0; i < cfg.infrs->nweights; i++) {
    fout_text << i << " " << cfg.infrs->weight_values[i] << std::endl;
  }
  fout_text.close();
}

void dd::GibbsSampling::aggregate_results_and_dump(const bool is_quiet,
                                                   int inc) {
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
    const FactorGraph &cfg = factorgraphs[i];
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
        std::cout << "   " << variable.id << " EXP="
                  << agg_means[variable.id] / agg_nsamples[variable.id]
                  << "  NSAMPLE=" << agg_nsamples[variable.id] << std::endl;

        if (variable.domain_type != DTYPE_BOOLEAN) {
          if (variable.domain_type == DTYPE_MULTINOMIAL) {
            for (size_t j = 0; j < variable.domain.size(); j++) {
              std::cout
                  << "        @ " << variable.domain[j] << " -> "
                  << 1.0 * multinomial_tallies[variable.n_start_i_tally + j] /
                         agg_nsamples[variable.id]
                  << std::endl;
            }
          } else {
            std::cerr << "ERROR: Only support boolean and multinomial "
                         "variables for now!"
                      << std::endl;
            assert(false);
          }
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
  if (inc == 0) {  // original
    filename_text = p_cmd_parser->output_folder;
  } else if (inc == 1) {  // materialization
    filename_text = p_cmd_parser->original_folder;
  } else {  // incremental
    filename_text = p_cmd_parser->delta_folder;
  }

  filename_text += "/inference_result.out.text";
  std::cout << "DUMPING... TEXT    : " << filename_text << std::endl;
  std::ofstream fout_text(filename_text.c_str());
  for (long i = 0; i < factorgraphs[0].n_var; i++) {
    const Variable &variable = factorgraphs[0].variables[i];
    if (variable.is_evid == true && !sample_evidence) {
      continue;
    }

    if (variable.domain_type != DTYPE_BOOLEAN) {
      if (variable.domain_type == DTYPE_MULTINOMIAL) {
        for (size_t j = 0; j < variable.domain.size(); j++) {
          fout_text << variable.id << " " << variable.domain[j] << " "
                    << (1.0 *
                        multinomial_tallies[variable.n_start_i_tally + j] /
                        agg_nsamples[variable.id])
                    << std::endl;
        }
      } else {
        std::cerr
            << "ERROR: Only support boolean and multinomial variables for now!"
            << std::endl;
        assert(false);
      }
    } else {
      fout_text << variable.id << " " << 1 << " "
                << (agg_means[variable.id] / agg_nsamples[variable.id])
                << std::endl;
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
