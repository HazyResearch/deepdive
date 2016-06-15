#ifndef DIMMWITTED_DIMMWITTED_H_
#define DIMMWITTED_DIMMWITTED_H_

#include "cmd_parser.h"
#include "factor_graph.h"
#include "gibbs_sampler.h"
#include <memory>

namespace dd {

/**
 * Command-line interface.
 */
int dw(int argc, const char* const argv[]);

/**
 * Runs gibbs sampling using the given command line parser
 */
int gibbs(const dd::CmdParser& cmd_parser);

/**
 * Class for (NUMA-aware) gibbs sampling
 *
 * This class encapsulates gibbs learning and inference, and dumping results.
 * Note the factor graph is copied on each NUMA node.
 */
class DimmWitted {
 public:
  const Weight* const weights;  // TODO clarify ownership

  // command line parser
  const CmdParser& opts;  // TODO clarify ownership

  // the highest node number available
  // actually, number of NUMA nodes = n_numa_nodes + 1
  int n_numa_nodes;

  // number of threads per NUMA node
  int n_thread_per_numa;

  // factor graph copies per NUMA node
  std::vector<GibbsSampler> sampler;

  /**
   * Constructs DimmWitted class with given factor graph, command line
   * parser,
   * and number of data copies. Allocate factor graph to NUMA nodes.
   * n_datacopy number of factor graph copies. n_datacopy = 1 means only
   * keeping one factor graph.
   */
  DimmWitted(std::unique_ptr<CompactFactorGraph> p_cfg, const Weight weights[],
             const CmdParser& opts);

  /**
   * Performs learning
   * n_epoch number of epochs. A epoch is one pass over data
   * n_sample_per_epoch not used any more.
   * stepsize starting step size for weight update (aka learning rate)
   * decay after each epoch, the stepsize is updated as stepsize = stepsize *
   * decay
   * reg_param regularization parameter
   * is_quiet whether to compress information display
   */
  void learn();

  /**
   * Performs inference
   * n_epoch number of epochs. A epoch is one pass over data
   * is_quiet whether to compress information display
   */
  void inference();

  /**
   * Aggregates results from different NUMA nodes
   * Dumps the inference result for variables
   * is_quiet whether to compress information display
   */
  void aggregate_results_and_dump();

  /**
   * Dumps the learned weights
   * is_quiet whether to compress information display
   */
  void dump_weights();

 private:
  int compute_n_epochs(int n_epoch);
};

}  // namespace dd

#endif  // DIMMWITTED_DIMMWITTED_H_
