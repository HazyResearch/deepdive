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
 private:
  std::unique_ptr<CompiledFactorGraph> pfg;
  std::vector<std::thread> threads;
  const CmdParser& opts;

 public:
  // factor graph
  CompiledFactorGraph& fg;  // TODO replace fg.* with fg->*
  InferenceResult infrs;
  // number of threads
  int nthread;
  // node id
  int nodeid;

  /**
   * Constructs a SingleNodeSampler given factor graph, number of threads, and
   * node id.
   */
  SingleNodeSampler(std::unique_ptr<CompiledFactorGraph> pfg,
                    const Weight weights[], int nthread, int nodeid,
                    const CmdParser& opts);

  /**
   * Performs sample
   */
  void sample(int i_epoch);

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
