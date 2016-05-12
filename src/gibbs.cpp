#include "gibbs.h"
#include "io/binary_format.h"
#include "app/gibbs/gibbs_sampling.h"
#include "dstruct/factor_graph/factor_graph.h"
#include <cmath>
#include "assert.h"
#include "common.h"
#include "unistd.h"

/*
 * Parse input arguments
 */
dd::CmdParser parse_input(int argc, char **argv) {
  std::vector<std::string> new_args;
  if (argc < 2 ||
      (strcmp(argv[1], "gibbs") != 0 && strcmp(argv[1], "mat") != 0 &&
       strcmp(argv[1], "inc") != 0 && strcmp(argv[1], "bin2text") != 0)) {
    new_args.push_back(std::string(argv[0]) + " " + "gibbs");
    new_args.push_back("-h");
  } else {
    new_args.push_back(std::string(argv[0]) + " " + std::string(argv[1]));
    for (int i = 2; i < argc; i++) {
      new_args.push_back(std::string(argv[i]));
    }
  }
  char **new_argv = new char *[new_args.size()];
  for (size_t i = 0; i < new_args.size(); i++) {
    new_argv[i] = new char[new_args[i].length() + 1];
    std::copy(new_args[i].begin(), new_args[i].end(), new_argv[i]);
    new_argv[i][new_args[i].length()] = '\0';
  }
  std::string app_name = argc < 2 ? "" : argv[1];
  dd::CmdParser cmd_parser(app_name.c_str(), new_args.size(), new_argv);
  return cmd_parser;
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

void gibbs(dd::CmdParser &cmd_parser) {
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
    dprintf("Resuming computation from snapshot...");
    gibbs.do_resume(is_quiet, n_datacopy, meta.num_variables, meta.num_factors,
                    meta.num_weights, meta.num_edges);
  } else {
    dprintf("Initializing...");
    // Load factor graph
    dd::FactorGraph fg(meta.num_variables, meta.num_factors, meta.num_weights,
                       meta.num_edges);
    fg.load(cmd_parser, is_quiet, false);

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
}
