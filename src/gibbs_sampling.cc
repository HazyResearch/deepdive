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

void GibbsSampling::inference() {
  const int n_epoch = compute_n_epochs(opts.n_inference_epoch);
  const int nvar = sampler[0].fg.size.num_variables;
  const bool should_show_progress = !opts.should_be_quiet;
  Timer t_total, t;

  for (auto &s : sampler) s.infrs.clear_variabletally();

  // inference epochs
  for (int i_epoch = 0; i_epoch < n_epoch; ++i_epoch) {
    if (should_show_progress) {
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
    if (should_show_progress) {
      std::cout << "" << elapsed << " sec.";
      std::cout << "," << (nvar * n_numa_nodes) / elapsed << " vars/sec"
                << std::endl;
    }
  }

  double elapsed = t_total.elapsed();
  std::cout << "TOTAL INFERENCE TIME: " << elapsed << " sec." << std::endl;
}

void GibbsSampling::learn() {
  InferenceResult &infrs = sampler[0].infrs;

  const int n_epoch = compute_n_epochs(opts.n_learning_epoch);
  const int nvar = infrs.nvars;
  const int nweight = infrs.nweights;
  const double decay = opts.decay;
  const bool should_show_progress = !opts.should_be_quiet;
  Timer t_total, t;

  double current_stepsize = opts.stepsize;
  const std::unique_ptr<double[]> prev_weights(new double[nweight]);
  memcpy(prev_weights.get(), infrs.weight_values.get(),
         sizeof(*prev_weights.get()) * nweight);

  // learning epochs
  for (int i_epoch = 0; i_epoch < n_epoch; ++i_epoch) {
    if (should_show_progress) {
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
    for (int i = 1; i < n_numa_nodes; ++i)
      infrs.merge_weights_from(sampler[i].infrs);
    infrs.average_regularize_weights(current_stepsize);
    for (int i = 1; i < n_numa_nodes; ++i)
      infrs.copy_weights_to(sampler[i].infrs);

    // calculate the norms of the difference of weights from the current epoch
    // and last epoch
    double lmax = -INFINITY;
    double l2 = 0.0;
    for (int j = 0; j < nweight; ++j) {
      double diff = fabs(prev_weights[j] - infrs.weight_values[j]);
      prev_weights[j] = infrs.weight_values[j];
      l2 += diff * diff;
      if (lmax < diff) lmax = diff;
    }
    lmax /= current_stepsize;

    double elapsed = t.elapsed();
    if (should_show_progress) {
      std::cout << "" << elapsed << " sec.";
      std::cout << "," << (nvar * n_numa_nodes) / elapsed << " vars/sec."
                << ",stepsize=" << current_stepsize << ",lmax=" << lmax
                << ",l2=" << sqrt(l2) / current_stepsize << std::endl;
    }

    current_stepsize *= decay;
  }

  double elapsed = t_total.elapsed();
  std::cout << "TOTAL LEARNING TIME: " << elapsed << " sec." << std::endl;
}

void GibbsSampling::dump_weights() {
  // learning weights snippets
  const InferenceResult &infrs = sampler[0].infrs;

  if (!opts.should_be_quiet) {
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

void GibbsSampling::aggregate_results_and_dump() {
  InferenceResult &infrs = sampler[0].infrs;

  // aggregate assignments across all possible worlds
  for (int i = 1; i < n_numa_nodes; ++i)
    infrs.aggregate_marginals_from(sampler[i].infrs);

  if (!opts.should_be_quiet) infrs.show_marginal_snippet(std::cout);

  // dump inference results
  std::string filename_text(opts.output_folder + "/inference_result.out.text");
  std::cout << "DUMPING... TEXT    : " << filename_text << std::endl;
  std::ofstream fout_text(filename_text);
  infrs.dump_marginals(fout_text);
  fout_text.close();

  if (!opts.should_be_quiet) infrs.show_marginal_histogram(std::cout);
}

// compute number of NUMA-aware epochs for learning or inference
int GibbsSampling::compute_n_epochs(int n_epoch) {
  return std::ceil((double)n_epoch / n_numa_nodes);
}

}  // namespace dd
