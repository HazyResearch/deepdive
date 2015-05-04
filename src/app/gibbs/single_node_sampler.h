
#include "worker/single_node_worker.h"
#include "app/gibbs/single_thread_sampler.h"
#include <stdlib.h>
#include "common.h"

#ifndef _SINGLE_NODE_SAMPLER_H
#define _SINGLE_NODE_SAMPLER_H

namespace dd{
  // single thread sample task
  void gibbs_single_thread_task(FactorGraph * const _p_fg, int i_worker, int n_worker);
  // single thread sgd task
  void gibbs_single_thread_sgd_task(FactorGraph * const _p_fg, int i_worker, int n_worker);

  /**
   * Class for a single NUMA node sampler
   */
  class SingleNodeSampler{

  public:
    // factor graph
    FactorGraph * const p_fg;
    // number of threads
    int nthread;
    // node id
    int nodeid;

    std::vector<std::thread> threads;

    // worker for sampling (used in inference)
    SingeNodeWorker<FactorGraph, gibbs_single_thread_task> * sample_worker;
    // worker for learning
    SingeNodeWorker<FactorGraph, gibbs_single_thread_sgd_task> * sgd_worker;

    /**
     * Constructs a SingleNodeSampler given factor graph, number of threads, and
     * node id.
     */
    SingleNodeSampler(FactorGraph * _p_fg, int _nthread, int _nodeid);

    /**
     * Clears the inference results in this sampler
     */
    void clear_variabletally();

    /**
     * Performs sample
     */
    void sample();

    /**
     * Waits for sample worker to finish
     */
    void wait();

    /**
     * Performs SGD
     */
    void sample_sgd();

    /**
     * Waits for sgd worker to finish
     */
    void wait_sgd();

  };
}

#endif






