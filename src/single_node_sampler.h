#ifndef DIMMWITTED_SINGLE_NODE_SAMPLER_H_
#define DIMMWITTED_SINGLE_NODE_SAMPLER_H_

#include "single_thread_sampler.h"
#include <stdlib.h>
#include <thread>
#include "common.h"

namespace dd {

/**
 * Class for a single NUMA node sampler
 */
class SingleNodeSampler {
 public:
  // factor graph
  CompiledFactorGraph& fg;
  InferenceResult infrs;
  // number of threads
  int nthread;
  // node id
  int nodeid;

  bool sample_evidence;
  int burn_in;
  bool learn_non_evidence;

  std::vector<std::thread> threads;

  /**
   * Constructs a SingleNodeSampler given factor graph, number of threads, and
   * node id.
   */
  SingleNodeSampler(CompiledFactorGraph& fg, const Weight weights[],
                    int nthread, int nodeid);
  SingleNodeSampler(CompiledFactorGraph& fg, const Weight weights[],
                    int nthread, int nodeid, bool sample_evidence, int burn_in);
  SingleNodeSampler(CompiledFactorGraph& fg, const Weight weights[],
                    int nthread, int nodeid, bool sample_evidence, int burn_in,
                    bool learn_non_evidence);

  /**
   * Clears the inference results in this sampler
   */
  void clear_variabletally();

  /**
   * Performs sample
   */
  void sample(int i_epoch);

  void sample_inc();

  /**
   * Waits for sample worker to finish
   */
  void wait();

  /**
   * Performs SGD
   */
  void sample_sgd(double stepsize);

  /**
   * Waits for sgd worker to finish
   */
  void wait_sgd();
};

}  // namespace dd

#endif  // DIMMWITTED_SINGLE_NODE_SAMPLER_H_
