#include "dimmwitted.h"
#include "binary_format.h"
#include "gibbs_sampling.h"
#include "text2bin.h"
#include "bin2text.h"
#include "factor_graph.h"
#include <cmath>
#include <map>
#include "assert.h"
#include "unistd.h"

namespace dd {

// the command-line entry point
int dw(int argc, const char *const argv[]) {
  // available modes
  const std::map<std::string, int (*)(const dd::CmdParser &)> MODES = {
      {"gibbs", gibbs},  // to do the learning and inference with Gibbs sampling
      {"text2bin", text2bin},  // to generate binary factor graphs from TSV
      {"bin2text", bin2text},  // to dump TSV of binary factor graphs
  };

  // parse command-line arguments
  dd::CmdParser cmd_parser(argc, argv);

  // dispatch to the correct function
  const auto &mode = MODES.find(cmd_parser.app_name);
  return (mode != MODES.end()) ? mode->second(cmd_parser) : 1;
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
    std::cout << args << std::endl;
  }

  FactorGraphDescriptor meta = read_meta(args.fg_file);
  if (!args.should_be_quiet) {
    std::cout << meta << std::endl;
  }

  // Run on NUMA node 0
  numa_run_on_node(0);
  numa_set_localalloc();

  // Load factor graph
  dprintf("Initializing factor graph...\n");
  dd::FactorGraph fg(meta);

  fg.load_variables(args.variable_file);
  fg.load_weights(args.weight_file);
  fg.load_domains(args.domain_file);
  fg.load_factors(args.factor_file);
  fg.safety_check();

  if (!args.should_be_quiet) {
    std::cout << "Printing FactorGraph statistics:" << std::endl;
    std::cout << fg << std::endl;
  }

  // Initialize Gibbs sampling application.
  DimmWitted dw(
      std::unique_ptr<CompiledFactorGraph>(new CompiledFactorGraph(fg)),
      fg.weights.get(), args);

  // learning
  dw.learn();

  // dump weights
  dw.dump_weights();

  // inference
  dw.inference();
  dw.aggregate_results_and_dump();

  return 0;
}

}  // namespace dd
