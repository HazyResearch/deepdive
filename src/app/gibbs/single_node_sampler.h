
#include "worker/single_node_worker.h"
#include "app/gibbs/single_thread_sampler.h"
#include <stdlib.h>
#include "common.h"

#ifndef _SINGLE_NODE_SAMPLER_H
#define _SINGLE_NODE_SAMPLER_H

namespace dd{
  void gibbs_single_thread_task(FactorGraph * const _p_fg, int i_worker, int n_worker);

  void gibbs_single_thread_sgd_task(FactorGraph * const _p_fg, int i_worker, int n_worker);

  class SingleNodeSampler{

  public:
    FactorGraph * const p_fg;

    int nthread;
    int nodeid;

    SingeNodeWorker<FactorGraph, gibbs_single_thread_task> * sample_worker;
    SingeNodeWorker<FactorGraph, gibbs_single_thread_sgd_task> * sgd_worker;

    SingleNodeSampler(FactorGraph * _p_fg, int _nthread, int _nodeid);

    void clear_variabletally();

    void sample();

    void wait();

    void sample_sgd();

    void wait_sgd();

  };
}

#endif






