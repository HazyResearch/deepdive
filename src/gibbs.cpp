
#include "gibbs.h"
#include "common.h"
#include <cmath>

/*
 * Parse input arguments
 */
dd::CmdParser parse_input(int argc, char **argv) {
  std::vector<std::string> new_args;
  if (argc < 2 ||
      (strcmp(argv[1], "gibbs") != 0 && strcmp(argv[1], "mat") != 0 &&
       strcmp(argv[1], "inc") != 0)) {
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
  dd::CmdParser cmd_parser(app_name.c_str());
  cmd_parser.parse(new_args.size(), new_argv);
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

/**
* The incremental function does the following
   (1) load the original graph
   (2) load whether a variable is used for learning or not
   (3) run learning for all variables that used for learning
   (4) caluclate the delta weight
   (5) add delta weight's factor to variables used for inference
   (6) load "compact factor" obtained by relaxation to variables used for
inference
   (7) run inference on all variables
*
**/
void inc(dd::CmdParser &cmd_parser) {
  // number of NUMA nodes
  int n_numa_node = numa_max_node() + 1;

  // get command line arguments
  std::string original_folder = cmd_parser.original_folder;
  std::string delta_folder = cmd_parser.delta_folder;

  // check arguments
  if (original_folder == "" || delta_folder == "") {
    std::cout << "original folder or delta folder not specified" << std::endl;
    exit(1);
  }

  // get command line arguments
  std::string fg_file = original_folder + "/graph.meta";
  std::string weight_file = original_folder + "/graph.weights";
  std::string variable_file = original_folder + "/graph.variables";
  std::string factor_file = original_folder + "/graph.factors";
  std::string edge_file = original_folder + "/graph.edges";

  std::string delta_fg_file = delta_folder + "/graph.meta";

  std::string output_folder = delta_folder;

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

  regularization reg = cmd_parser.regularization == "l1" ? REG_L1 : REG_L2;

  Meta meta = read_meta(fg_file);
  Meta meta2 = read_meta(delta_fg_file);

  // run on NUMA node 0
  numa_run_on_node(0);
  numa_set_localalloc();

  // std::cout << "#######" << meta.num_factors+meta2.num_factors << std::endl;

  // load factor graph
  dd::FactorGraph fg(
      meta.num_variables +
          meta2.num_variables, /* THis is REALLY UGLY!!!! FIX!!!!*/
      meta.num_factors + meta2.num_factors,
      meta.num_weights + meta2.num_weights, meta.num_edges + meta2.num_edges);
  fg.load(cmd_parser, is_quiet, 2);

  dd::CompiledFactorGraph cfg(
      meta.num_variables +
          meta2.num_variables, /* THis is REALLY UGLY!!!! FIX!!!!*/
      meta.num_factors + meta2.num_factors,
      meta.num_weights + meta2.num_weights, meta.num_edges + meta2.num_edges);
  fg.compile(cfg);

  dd::GibbsSampling gibbs(&cmd_parser, false, 0, false);
  gibbs.init(&cfg, n_datacopy);

  // number of learning epochs
  // the factor graph is copied on each NUMA node, so the total epochs =
  // epochs specified / number of NUMA nodes
  int numa_aware_n_learning_epoch =
      compute_n_epochs(n_learning_epoch, n_numa_node, n_datacopy);

  // learning
  gibbs.learn(numa_aware_n_learning_epoch, n_samples_per_learning_epoch,
              stepsize, decay, reg_param, is_quiet, true, reg);

  // dump weights
  gibbs.dump_weights(is_quiet, 2);

  // number of inference epochs
  int numa_aware_n_epoch =
      compute_n_epochs(n_inference_epoch, n_numa_node, n_datacopy);

  // inference
  gibbs.inference(numa_aware_n_epoch, is_quiet, false, true);
  gibbs.aggregate_results_and_dump(is_quiet, 2);
}

void mat(dd::CmdParser &cmd_parser) {
  // number of NUMA nodes
  int n_numa_node = numa_max_node() + 1;

  // get command line arguments
  std::string original_folder = cmd_parser.original_folder;

  // check arguments
  if (original_folder == "") {
    std::cout << "original folder not specified" << std::endl;
    exit(1);
  }

  // get command line arguments
  std::string fg_file = original_folder + "/graph.meta";
  std::string weight_file = original_folder + "/graph.weights";
  std::string variable_file = original_folder + "/graph.variables";
  std::string factor_file = original_folder + "/graph.factors";
  std::string edge_file = original_folder + "/graph.edges";

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
  regularization reg = cmd_parser.regularization == "l1" ? REG_L1 : REG_L2;

  Meta meta = read_meta(fg_file);

  // run on NUMA node 0
  numa_run_on_node(0);
  numa_set_localalloc();

  // load factor graph
  dd::FactorGraph fg(meta.num_variables, meta.num_factors, meta.num_weights,
                     meta.num_edges);
  fg.load(cmd_parser, is_quiet, 1);

  dd::CompiledFactorGraph cfg(meta.num_variables, meta.num_factors,
                              meta.num_weights, meta.num_edges);

  fg.compile(cfg);

  dd::GibbsSampling gibbs(&cmd_parser, false, 0, false);
  gibbs.init(&cfg, n_datacopy);

  // number of learning epochs
  // the factor graph is copied on each NUMA node, so the total epochs =
  // epochs specified / number of NUMA nodes
  int numa_aware_n_learning_epoch =
      compute_n_epochs(n_learning_epoch, n_numa_node, n_datacopy);

  // learning
  gibbs.learn(numa_aware_n_learning_epoch, n_samples_per_learning_epoch,
              stepsize, decay, reg_param, is_quiet, false, reg);

  // dump weights
  gibbs.dump_weights(is_quiet, 1);

  // number of inference epochs
  int numa_aware_n_epoch =
      compute_n_epochs(n_inference_epoch, n_numa_node, n_datacopy);

  // inference
  gibbs.inference(numa_aware_n_epoch, is_quiet, true, false);
  gibbs.aggregate_results_and_dump(is_quiet, 1);
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
              stepsize, decay, reg_param, is_quiet, false, reg);

  // dump weights
  gibbs.dump_weights(is_quiet, 0);

  // number of inference epochs
  int numa_aware_n_epoch =
      compute_n_epochs(n_inference_epoch, n_numa_node, n_datacopy);

  // inference
  gibbs.inference(numa_aware_n_epoch, is_quiet, false, false);
  gibbs.aggregate_results_and_dump(is_quiet, 0);

  if (cmd_parser.should_use_snapshot) {
    gibbs.do_checkpoint(is_quiet);
  }
}
