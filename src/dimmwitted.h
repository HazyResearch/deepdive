#ifndef DIMMWITTED_DIMMWITTED_H_
#define DIMMWITTED_DIMMWITTED_H_

#include "cmd_parser.h"
#include "factor_graph.h"
#include "gibbs_sampler.h"
#include <memory>
#include <zmq.hpp>

namespace dd {

/**
 * Command-line interface.
 */
int dw(int argc, const char* const argv[]);

/**
 * Runs gibbs sampling using the given command line parser
 */
int gibbs(const CmdParser& cmd_parser);

/**
 * Class for (NUMA-aware) gibbs sampling
 *
 * This class encapsulates gibbs learning and inference, and dumping results.
 * Note the factor graph is copied on each NUMA node.
 */
class DimmWitted {
 private:
  const size_t n_samplers_;

 public:
  const Weight* const weights;  // TODO clarify ownership

  // command line parser
  const CmdParser& opts;  // TODO clarify ownership

  // Whether we are running as a distributed worker
  const bool is_distributed;

  // factor graph copies per NUMA node
  std::vector<GibbsSampler> samplers;

  /**
   * Constructs DimmWitted class with given factor graph, command line
   * parser,
   * and number of data copies. Allocate factor graph to NUMA nodes.
   * n_datacopy number of factor graph copies. n_datacopy = 1 means only
   * keeping one factor graph.
   */
  DimmWitted(FactorGraph* p_cfg, const Weight weights[], const CmdParser& opts);

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

  /**
   * Connect to the parameter server.
   */
  void connect_param_server();

  // Send a simple message (a string) to PS
  void ps_send_msg(std::string message);

  // Initialize weights from PS
  void ps_reset_weights();

 private:
  /**
   * Connection to paramemter server
   */
  std::unique_ptr<zmq::context_t> ps_context;
  std::unique_ptr<zmq::socket_t> ps_socket;

  // Standalone update
  bool update_weights(InferenceResult& infrs, double elapsed, double stepsize,
                      const std::unique_ptr<double[]>& prev_weights);

  // Distributed update:
  // Send gradients to PS and refresh local weights
  // Returns true if we can stop learning
  bool ps_update_weights(InferenceResult& infrs, int epochs);

  // Fetch latest weights from PS
  void ps_fetch_weights();

  size_t compute_n_epochs(size_t n_epoch);
};

}  // namespace dd

#endif  // DIMMWITTED_DIMMWITTED_H_
