#include "dimmwitted.h"
#include "binary_format.h"
#include "gibbs_sampling.h"
#include "bin2text.h"
#include "factor_graph.h"
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

int gibbs(const dd::CmdParser &args) {
  // number of NUMA nodes
  int n_numa_node = numa_max_node() + 1;
  // number of max threads per NUMA node
  int n_thread_per_numa = (sysconf(_SC_NPROCESSORS_CONF)) / (n_numa_node);

  if (!args.should_be_quiet) {
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
    std::cout << "# fg_file            : " << args.fg_file << std::endl;
    std::cout << "# weight_file        : " << args.weight_file << std::endl;
    std::cout << "# variable_file      : " << args.variable_file << std::endl;
    std::cout << "# factor_file        : " << args.factor_file << std::endl;
    std::cout << "# output_folder      : " << args.output_folder << std::endl;
    std::cout << "# n_learning_epoch   : " << args.n_learning_epoch
              << std::endl;
    std::cout << "# n_samples/l. epoch : " << args.n_samples_per_learning_epoch
              << std::endl;
    std::cout << "# n_inference_epoch  : " << args.n_inference_epoch
              << std::endl;
    std::cout << "# stepsize           : " << args.stepsize << std::endl;
    std::cout << "# decay              : " << args.decay << std::endl;
    std::cout << "# regularization     : " << args.reg_param << std::endl;
    std::cout << "# burn_in            : " << args.burn_in << std::endl;
    std::cout << "# learn_non_evidence : " << args.should_learn_non_evidence
              << std::endl;
    std::cout << "################################################"
              << std::endl;
    std::cout << "# IGNORE -s (n_samples/l. epoch). ALWAYS -s 1. #"
              << std::endl;
    std::cout << "# IGNORE -t (threads). ALWAYS USE ALL THREADS. #"
              << std::endl;
  }

  Meta meta = read_meta(args.fg_file);
  if (!args.should_be_quiet) {
    std::cout << "################################################"
              << std::endl;
    std::cout << "# nvar               : " << meta.num_variables << std::endl;
    std::cout << "# nfac               : " << meta.num_factors << std::endl;
    std::cout << "# nweight            : " << meta.num_weights << std::endl;
    std::cout << "# nedge              : " << meta.num_edges << std::endl;
  }

  // Run on NUMA node 0
  numa_run_on_node(0);
  numa_set_localalloc();

  // Initialize Gibbs sampling application.
  dd::GibbsSampling gibbs(&args, args.should_sample_evidence, args.burn_in,
                          args.should_learn_non_evidence);

  if (args.should_use_snapshot) {
    dprintf("Resuming factor graph from checkpoint...\n");
    gibbs.do_resume(args.should_be_quiet, args.n_datacopy, meta.num_variables,
                    meta.num_factors, meta.num_weights, meta.num_edges);
  } else {
    // Load factor graph
    dprintf("Initializing factor graph...\n");
    dd::FactorGraph fg(meta.num_variables, meta.num_factors, meta.num_weights,
                       meta.num_edges);

    fg.load_variables(args.variable_file);
    fg.load_weights(args.weight_file);
    fg.load_domains(args.domain_file);
    fg.load_factors(args.factor_file);
    fg.safety_check();

    assert(fg.is_usable());

    if (!args.should_be_quiet) {
      std::cout << "Printing FactorGraph statistics:" << std::endl;
      std::cout << fg << std::endl;
    }

    dd::CompiledFactorGraph cfg(meta.num_variables, meta.num_factors,
                                meta.num_weights, meta.num_edges);
    fg.compile(cfg);

    gibbs.init(&cfg, args.n_datacopy);
  }

  // number of learning epochs
  // the factor graph is copied on each NUMA node, so the total epochs =
  // epochs specified / number of NUMA nodes
  int numa_aware_n_learning_epoch =
      compute_n_epochs(args.n_learning_epoch, n_numa_node, args.n_datacopy);

  // learning
  regularization reg = args.regularization == "l1" ? REG_L1 : REG_L2;
  gibbs.learn(numa_aware_n_learning_epoch, args.n_samples_per_learning_epoch,
              args.stepsize, args.decay, args.reg_param, args.should_be_quiet,
              reg);

  // dump weights
  gibbs.dump_weights(args.should_be_quiet);

  // number of inference epochs
  int numa_aware_n_epoch =
      compute_n_epochs(args.n_inference_epoch, n_numa_node, args.n_datacopy);

  // inference
  gibbs.inference(numa_aware_n_epoch, args.should_be_quiet);
  gibbs.aggregate_results_and_dump(args.should_be_quiet);

  if (args.should_use_snapshot) {
    gibbs.do_checkpoint(args.should_be_quiet);
  }

  return 0;
}
