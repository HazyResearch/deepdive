#include "dimmwitted.h"
#include "assert.h"
#include "bin2text.h"
#include "binary_format.h"
#include "numa_nodes.h"
#include "common.h"
#include "factor_graph.h"
#include "gibbs_sampler.h"
#include "text2bin.h"

#include <fstream>
#include <iomanip>
#include <map>
#include <unistd.h>
#include <algorithm>

namespace dd {

// the command-line entry point
int dw(int argc, const char *const argv[]) {
  // available modes
  const std::map<std::string, int (*)(const CmdParser &)> MODES = {
      {"gibbs", gibbs},  // to do the learning and inference with Gibbs sampling
      {"text2bin", text2bin},  // to generate binary factor graphs from TSV
      {"bin2text", bin2text},  // to dump TSV of binary factor graphs
  };

  // parse command-line arguments
  CmdParser cmd_parser(argc, argv, MODES);
  if (cmd_parser.num_errors() > 0) return cmd_parser.num_errors();

  // dispatch to the correct function
  const auto &mode = MODES.find(cmd_parser.app_name);
  return (mode != MODES.end()) ? mode->second(cmd_parser) : 1;
}

int gibbs(const CmdParser &args) {
  // number of NUMA nodes
  size_t n_numa_node = NumaNodes::num_configured();
  // number of max threads per NUMA node
  size_t n_thread_per_numa = (sysconf(_SC_NPROCESSORS_CONF)) / (n_numa_node);

  if (!args.should_be_quiet) {
    std::cout << std::endl;
    std::cout << "#################MACHINE CONFIG#################"
              << std::endl;
    std::cout << "# # NUMA Node        : " << n_numa_node << std::endl;
    std::cout << "# # Thread/NUMA Node : " << n_thread_per_numa << std::endl;
    std::cout << "################################################"
              << std::endl;
    std::cout << std::endl;
    std::cout << args << std::endl;
  }

  FactorGraphDescriptor meta = read_meta(args.fg_file);
  std::cout << "Factor graph to load:\t" << meta << std::endl;

  // Allocate the input on the first group of NUMA nodes
  NumaNodes::partition(0, args.n_datacopy).bind();

  // Load factor graph
  dprintf("Initializing factor graph...\n");
  std::unique_ptr<FactorGraph> fg(new FactorGraph(meta));

  fg->load_variables(args.variable_file);
  fg->load_weights(args.weight_file);
  fg->load_domains(args.domain_file);
  fg->load_factors(args.factor_file);
  std::cout << "Factor graph loaded:\t" << fg->size << std::endl;
  fg->safety_check();

  if (!args.should_be_quiet) {
    std::cout << "Printing FactorGraph statistics:" << std::endl;
    std::cout << *fg << std::endl;
  }

  // Initialize Gibbs sampling application.
  DimmWitted dw(
      std::unique_ptr<CompactFactorGraph>(new CompactFactorGraph(*fg)),
      fg->weights.get(), args);

  // Explicitly drop the raw factor graph used only during loading
  fg.release();

  // learning
  dw.learn();

  // dump weights
  dw.dump_weights();

  // inference
  dw.inference();
  dw.aggregate_results_and_dump();

  return 0;
}

DimmWitted::DimmWitted(std::unique_ptr<CompactFactorGraph> p_cfg,
                       const Weight weights[], const CmdParser &opts)
    : n_samplers_(opts.n_datacopy), weights(weights), opts(opts) {
  size_t n_thread_per_numa =
      std::max(size_t(1), opts.n_threads / opts.n_datacopy);

  // copy factor graphs and create samplers
  size_t i = 0;
  for (auto &numa_nodes : NumaNodes::partition(opts.n_datacopy)) {
    numa_nodes.bind();
    std::cout << "CREATE CFG ON NODE ... " << numa_nodes << std::endl;
    samplers.push_back(GibbsSampler(
        std::unique_ptr<CompactFactorGraph>(
            i == 0 ?
                   // use the given factor graph for the first sampler
                p_cfg.release()
                   :
                   // then, make a copy for the rest
                new CompactFactorGraph(samplers[0].fg)),
        weights, numa_nodes, n_thread_per_numa, i, opts));
    ++i;
  }
}

void DimmWitted::inference() {
  const num_epochs_t n_epoch = compute_n_epochs(opts.n_inference_epoch);
  const variable_id_t nvar = samplers[0].fg.size.num_variables;
  const bool should_show_progress = !opts.should_be_quiet;
  Timer t_total, t;

  for (auto &sampler : samplers) sampler.infrs.clear_variabletally();

  // inference epochs
  for (num_epochs_t i_epoch = 0; i_epoch < n_epoch; ++i_epoch) {
    if (should_show_progress) {
      std::cout << std::setprecision(2) << "INFERENCE EPOCH "
                << i_epoch * n_samplers_ << "~" << ((i_epoch + 1) * n_samplers_)
                << "...." << std::flush;
    }

    // restart timer
    t.restart();

    // sample
    for (auto &sampler : samplers) sampler.sample(i_epoch);

    // wait for samplers to finish
    for (auto &sampler : samplers) sampler.wait();

    double elapsed = t.elapsed();
    if (should_show_progress) {
      std::cout << "" << elapsed << " sec.";
      std::cout << "," << (nvar * n_samplers_) / elapsed << " vars/sec"
                << std::endl;
    }
  }

  double elapsed = t_total.elapsed();
  std::cout << "TOTAL INFERENCE TIME: " << elapsed << " sec." << std::endl;
}

void DimmWitted::learn() {
  InferenceResult &infrs = samplers[0].infrs;

  const num_epochs_t n_epoch = compute_n_epochs(opts.n_learning_epoch);
  const variable_id_t nvar = infrs.nvars;
  const weight_id_t nweight = infrs.nweights;
  const double decay = opts.decay;
  const bool should_show_progress = !opts.should_be_quiet;
  Timer t_total, t;

  double current_stepsize = opts.stepsize;
  const std::unique_ptr<double[]> prev_weights(new double[nweight]);
  COPY_ARRAY(infrs.weight_values.get(), nweight, prev_weights.get());

  // learning epochs
  for (num_epochs_t i_epoch = 0; i_epoch < n_epoch; ++i_epoch) {
    if (should_show_progress) {
      std::cout << std::setprecision(2) << "LEARNING EPOCH "
                << i_epoch * n_samplers_ << "~" << ((i_epoch + 1) * n_samplers_)
                << "...." << std::flush;
    }

    t.restart();

    // performs stochastic gradient descent with sampling
    for (auto &sampler : samplers) sampler.sample_sgd(current_stepsize);

    // wait the samplers to finish
    for (auto &sampler : samplers) sampler.wait();

    // sum the weights and store in the first factor graph
    // the average weights will be calculated and assigned to all factor graphs
    for (size_t i = 1; i < n_samplers_; ++i)
      infrs.merge_weights_from(samplers[i].infrs);
    if (n_samplers_ > 1) {
      infrs.average_weights(n_samplers_);
    }
    for (size_t i = 1; i < n_samplers_; ++i)
      infrs.copy_weights_to(samplers[i].infrs);

    // calculate the norms of the difference of weights from the current epoch
    // and last epoch
    double lmax = -INFINITY;
    double l2 = 0.0;
    for (weight_id_t j = 0; j < nweight; ++j) {
      double diff = fabs(prev_weights[j] - infrs.weight_values[j]);
      prev_weights[j] = infrs.weight_values[j];
      l2 += diff * diff;
      if (lmax < diff) lmax = diff;
    }
    lmax /= current_stepsize;

    double elapsed = t.elapsed();
    if (should_show_progress) {
      std::cout << "" << elapsed << " sec.";
      std::cout << "," << (nvar * n_samplers_) / elapsed << " vars/sec."
                << ",stepsize=" << current_stepsize << ",lmax=" << lmax
                << ",l2=" << sqrt(l2) / current_stepsize << std::endl;
    }

    current_stepsize *= decay;
  }

  double elapsed = t_total.elapsed();
  std::cout << "TOTAL LEARNING TIME: " << elapsed << " sec." << std::endl;
}

void DimmWitted::dump_weights() {
  // learning weights snippets
  const InferenceResult &infrs = samplers[0].infrs;

  if (!opts.should_be_quiet) infrs.show_weights_snippet(std::cout);

  // dump learned weights
  std::string filename_text(opts.output_folder +
                            "/inference_result.out.weights.text");
  std::cout << "DUMPING... TEXT    : " << filename_text << std::endl;
  std::ofstream fout_text(filename_text);
  infrs.dump_weights_in_text(fout_text);
  fout_text.close();
}

void DimmWitted::aggregate_results_and_dump() {
  InferenceResult &infrs = samplers[0].infrs;

  // aggregate assignments across all possible worlds
  for (size_t i = 1; i < n_samplers_; ++i)
    infrs.aggregate_marginals_from(samplers[i].infrs);

  if (!opts.should_be_quiet) infrs.show_marginal_snippet(std::cout);

  // dump inference results
  std::string filename_text(opts.output_folder + "/inference_result.out.text");
  std::cout << "DUMPING... TEXT    : " << filename_text << std::endl;
  std::ofstream fout_text(filename_text);
  infrs.dump_marginals_in_text(fout_text);
  fout_text.close();

  if (!opts.should_be_quiet) infrs.show_marginal_histogram(std::cout);
}

// compute number of NUMA-aware epochs for learning or inference
num_epochs_t DimmWitted::compute_n_epochs(num_epochs_t n_epoch) {
  return std::ceil((double)n_epoch / n_samplers_);
}

}  // namespace dd
