
#include "worker/single_node_worker.h"
#include "app/gibbs/single_thread_sampler.h"
#include <stdlib.h>
#include "common.h"

#ifndef _SINGLE_NODE_SAMPLER_H
#define _SINGLE_NODE_SAMPLER_H

namespace dd{
  void gibbs_single_thread_task(FactorGraph * const _p_fg, int i_worker, int n_worker){
    //numa_set_localalloc();
    //int cpu = sched_getcpu();
    //int node = numa_node_of_cpu(cpu);
    //std::cout << node << std::endl;
    SingleThreadSampler sampler = SingleThreadSampler(_p_fg);
    sampler.sample(i_worker,n_worker);
  }

  void gibbs_single_thread_sgd_task(FactorGraph * const _p_fg, int i_worker, int n_worker){
    //numa_set_localalloc();
    SingleThreadSampler sampler = SingleThreadSampler(_p_fg);
    sampler.sample_sgd(i_worker,n_worker);
  }

  class SingleNodeSampler{

  public:
    FactorGraph * const p_fg;

    int nthread;
    int nodeid;

    SingeNodeWorker<FactorGraph, gibbs_single_thread_task> * sample_worker;
    SingeNodeWorker<FactorGraph, gibbs_single_thread_sgd_task> * sgd_worker;

    SingleNodeSampler(FactorGraph * _p_fg, int _nthread, int _nodeid) :
      p_fg (_p_fg)    
    {
      this->nthread = _nthread;
      this->nodeid = _nodeid;
      this->sample_worker = new SingeNodeWorker<FactorGraph, gibbs_single_thread_task>(this->p_fg, 
        this->nthread, this->nodeid);
      this->sgd_worker = new SingeNodeWorker<FactorGraph, gibbs_single_thread_sgd_task>(this->p_fg, 
        this->nthread, this->nodeid);
    }

    void clear_variabletally(){
      for(long i=0;i<p_fg->n_var;i++){
        //p_fg->variables[i].agg_mean = 0.0;
        //p_fg->variables[i].n_sample = 0.0;
        p_fg->infrs->agg_means[i] = 0.0;
        p_fg->infrs->agg_nsamples[i] = 0.0;
      }
      for(long i=0;i<p_fg->infrs->ntallies;i++){
        p_fg->infrs->multinomial_tallies[i] = 0;
      }
    }

    void sample(){
      this->sample_worker->execute();
    }

    void wait(){
      this->sample_worker->wait();
    }

    void sample_sgd(){
      this->sgd_worker->execute();
    }


    void wait_sgd(){
      this->sgd_worker->wait();
    }



  };
}

#endif






