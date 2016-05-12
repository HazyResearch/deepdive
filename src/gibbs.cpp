#include "gibbs.h"
#include "io/binary_format.h"
#include "app/gibbs/gibbs_sampling.h"
#include "io/bin2text.h"
#include "dstruct/factor_graph/factor_graph.h"
#include <cmath>
#include <map>
#include "assert.h"
#include "common.h"
#include "unistd.h"

// the command-line entry point
int dw(int argc, const char *const argv[]) {
  // available modes
  const std::map<std::string, int (*)(const dd::CmdParser &)> MODES = {
      {"gibbs", gibbs},  // to do the learning and inference with Gibbs sampling
      {"bin2text", bin2text},  // to dump TSV of binary factor graphs
  };

  // parse command-line arguments
  dd::CmdParser cmd_parser(argc, argv);

  // dispatch to the correct function
  const auto &mode = MODES.find(cmd_parser.app_name);
  return (mode != MODES.end()) ? mode->second(cmd_parser) : 1;
}

// compute number of NUMA-aware epochs for learning or inference
int compute_n_epochs(int n_epoch, int n_numa_node, int n_datacopy) {
  int n_valid_node = n_numa_node;
  if (n_datacopy >= 1 && n_datacopy <= n_numa_node + 1) {
    n_valid_node = n_datacopy;
  }
  return std::ceil((double)n_epoch / n_valid_node);
}

void compact(dd::CmdParser &cmd_parser) {
  // TODO: Implement me!
  return;
}

void init_assignments(dd::CmdParser &cmd_parser) {
  // TODO: Implement me!
  return;
}

void init_weights(dd::CmdParser &cmd_parser) {
  // TODO: Implement me!
  return;
}

int gibbs(const dd::CmdParser &cmd_parser) {
  // number of NUMA nodes
  int n_numa_node = numa_max_node() + 1;
  // number of max threads per NUMA node
  int n_thread_per_numa = (sysconf(_SC_NPROCESSORS_CONF)) / (n_numa_node);

  // get command line arguments
  std::string fg_file = cmd_parser.fg_file;

  std::string weight_file = cmd_parser.weight_file;
  std::string variable_file = cmd_parser.variable_file;
  std::string factor_file = cmd_parser.factor_file;

  std::string output_folder = cmd_parser.output_folder;

  // check arguments
  if (fg_file == "" || weight_file == "" || variable_file == "" ||
      factor_file == "" || output_folder == "") {
    std::cout << "Some factor graph files not specified" << std::endl;
    exit(1);
  }

  int n_learning_epoch = cmd_parser.n_learning_epoch;
  int n_samples_per_learning_epoch = cmd_parser.n_samples_per_learning_epoch;
  int n_inference_epoch = cmd_parser.n_inference_epoch;

  double stepsize = cmd_parser.stepsize;
  double stepsize2 = cmd_parser.stepsize2;
  // hack to support two parameters to specify step size
  if (stepsize == 0.01) stepsize = stepsize2;
  double decay = cmd_parser.decay;

  int n_datacopy = cmd_parser.n_datacopy;
  double reg_param = cmd_parser.reg_param;
  bool is_quiet = cmd_parser.should_be_quiet;
  bool sample_evidence = cmd_parser.should_sample_evidence;
  int burn_in = cmd_parser.burn_in;
  bool learn_non_evidence = cmd_parser.should_learn_non_evidence;
  regularization reg = cmd_parser.regularization == "l1" ? REG_L1 : REG_L2;

  Meta meta = read_meta(fg_file);

  if (is_quiet) {
    std::cout << "Running in quiet mode..." << std::endl;
  } else {
    std::cout << std::endl;
    std::cout << "#################MACHINE CONFIG#################"
              << std::endl;
    std::cout << "# # NUMA Node        : " << n_numa_node << std::endl;
    std::cout << "# # Thread/NUMA Node : " << n_thread_per_numa << std::endl;
    std::cout << "################################################"
              << std::endl;
    std::cout << std::endl;
    std::cout << "#################GIBBS SAMPLING#################"
              << std::endl;
    std::cout << "# fg_file            : " << fg_file << std::endl;
    std::cout << "# weight_file        : " << weight_file << std::endl;
    std::cout << "# variable_file      : " << variable_file << std::endl;
    std::cout << "# factor_file        : " << factor_file << std::endl;
    std::cout << "# output_folder      : " << output_folder << std::endl;
    std::cout << "# n_learning_epoch   : " << n_learning_epoch << std::endl;
    std::cout << "# n_samples/l. epoch : " << n_samples_per_learning_epoch
              << std::endl;
    std::cout << "# n_inference_epoch  : " << n_inference_epoch << std::endl;
    std::cout << "# stepsize           : " << stepsize << std::endl;
    std::cout << "# decay              : " << decay << std::endl;
    std::cout << "# regularization     : " << reg_param << std::endl;
    std::cout << "################################################"
              << std::endl;
    std::cout << "# IGNORE -s (n_samples/l. epoch). ALWAYS -s 1. #"
              << std::endl;
    std::cout << "# IGNORE -t (threads). ALWAYS USE ALL THREADS. #"
              << std::endl;
    std::cout << "################################################"
              << std::endl;

    std::cout << "# nvar               : " << meta.num_variables << std::endl;
    std::cout << "# nfac               : " << meta.num_factors << std::endl;
    std::cout << "# nweight            : " << meta.num_weights << std::endl;
    std::cout << "# nedge              : " << meta.num_edges << std::endl;
    std::cout << "################################################"
              << std::endl;
  }

  // Run on NUMA node 0
  numa_run_on_node(0);
  numa_set_localalloc();

  // Initialize Gibbs sampling application.
  dd::GibbsSampling gibbs(&cmd_parser, sample_evidence, burn_in,
                          learn_non_evidence);

  if (cmd_parser.should_use_snapshot) {
    dprintf("Resuming factor graph from checkpoint...\n");
    gibbs.do_resume(is_quiet, n_datacopy, meta.num_variables, meta.num_factors,
                    meta.num_weights, meta.num_edges);
  } else {
    // Load factor graph
    dprintf("Initializing factor graph...\n");
    dd::FactorGraph fg(meta.num_variables, meta.num_factors, meta.num_weights,
                       meta.num_edges);

    fg.load_variables(cmd_parser.variable_file);
    fg.load_weights(cmd_parser.weight_file);
    fg.load_domains(cmd_parser.domain_file);
    fg.load_factors(cmd_parser.factor_file);
    fg.safety_check();

    assert(fg.is_usable());

    if (!is_quiet) {
      std::cout << "Printing FactorGraph statistics:" << std::endl;
      std::cout << fg << std::endl;
    }

    dd::CompiledFactorGraph cfg(meta.num_variables, meta.num_factors,
                                meta.num_weights, meta.num_edges);
    fg.compile(cfg);

    gibbs.init(&cfg, n_datacopy);
  }

  // number of learning epochs
  // the factor graph is copied on each NUMA node, so the total epochs =
  // epochs specified / number of NUMA nodes
  int numa_aware_n_learning_epoch =
      compute_n_epochs(n_learning_epoch, n_numa_node, n_datacopy);

  // learning
  gibbs.learn(numa_aware_n_learning_epoch, n_samples_per_learning_epoch,
              stepsize, decay, reg_param, is_quiet, reg);

  // dump weights
  gibbs.dump_weights(is_quiet);

  // number of inference epochs
  int numa_aware_n_epoch =
      compute_n_epochs(n_inference_epoch, n_numa_node, n_datacopy);

  // inference
  gibbs.inference(numa_aware_n_epoch, is_quiet);
  gibbs.aggregate_results_and_dump(is_quiet);

  if (cmd_parser.should_use_snapshot) {
    gibbs.do_checkpoint(is_quiet);
  }

  return 0;
}
