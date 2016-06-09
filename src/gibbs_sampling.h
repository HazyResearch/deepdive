#ifndef DIMMWITTED_GIBBS_SAMPLING_H_
#define DIMMWITTED_GIBBS_SAMPLING_H_

#include "cmd_parser.h"
#include "factor_graph.h"
#include <iostream>

namespace dd {

/**
 * Class for (NUMA-aware) gibbs sampling
 *
 * This class encapsulates gibbs learning and inference, and dumping results.
 * Note the factor graph is copied on each NUMA node.
 */
class GibbsSampling {
 public:
  // factor graph
  CompiledFactorGraph* p_fg;

  // command line parser
  const CmdParser* const p_cmd_parser;

  // the highest node number available
  // actually, number of NUMA nodes = n_numa_nodes + 1
  int n_numa_nodes;

  // number of threads per NUMA node
  int n_thread_per_numa;

  // factor graph copies
  std::vector<CompiledFactorGraph> factorgraphs;

  // sample evidence in inference
  bool sample_evidence;

  // burn-in period
  int burn_in;

  // whether sample non-evidence during learning
  bool learn_non_evidence;

  /**
   * Constructs GibbsSampling class with given factor graph, command line
   * parser,
   * and number of data copies. Allocate factor graph to NUMA nodes.
   * n_datacopy number of factor graph copies. n_datacopy = 1 means only
   * keeping one factor graph.
   */
  GibbsSampling(const CmdParser* const _p_cmd_parser, bool sample_evidence,
                int burn_in, bool learn_non_evidence);

  void init(CompiledFactorGraph* const _p_cfg, int n_datacopy);

  void do_resume(bool is_quiet, int n_datacopy, long n_var, long n_factor,
                 long n_weight, long n_edge);

  /* TODO: Implement checkpoint method */
  void do_checkpoint(bool is_quiet);

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
  void learn(const int& n_epoch, const int& n_sample_per_epoch,
             const double& stepsize, const double& decay,
             const double reg_param, const bool is_quiet,
             const regularization reg);

  /**
   * Performs inference
   * n_epoch number of epochs. A epoch is one pass over data
   * is_quiet whether to compress information display
   */
  void inference(const int& n_epoch, const bool is_quiet);

  /**
   * Aggregates results from different NUMA nodes
   * Dumps the inference result for variables
   * is_quiet whether to compress information display
   */
  void aggregate_results_and_dump(const bool is_quiet);

  /**
   * Dumps the learned weights
   * is_quiet whether to compress information display
   */
  void dump_weights(const bool is_quiet);

  // private: // FIXME not ready yet
  /**
   * Saves the weights snapshot into a binary file.
   *
   * FIXME: This has the same logic as dump_weights() below. Potentially
   * merge both functions.
   *
   * FIXME: This function and its loading counterpart is located in this
   * class since we have to take into account the weights from all the
   * nodes.
   */
  void save_weights_snapshot(const bool is_quiet);

  /**
   * Loads the weights snapshot into all copies of the factor graph from the
   * specified file. Useful in the context of running the sampler on
   * different non-overlapping partitions of the same factor graph.
   */
  void load_weights_snapshot(const bool is_quiet);
};

}  // namespace dd

#endif  // DIMMWITTED_GIBBS_SAMPLING_H_
