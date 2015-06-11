
#include "app/gibbs/single_node_sampler.h"

namespace dd{

  void gibbs_single_thread_task(FactorGraph * const _p_fg, int i_worker, 
    int n_worker, bool _sample_evidence, bool burn_in){
    SingleThreadSampler sampler = SingleThreadSampler(_p_fg, _sample_evidence, burn_in, false);
    sampler.sample(i_worker,n_worker);
  }

  void gibbs_single_thread_sgd_task(FactorGraph * const _p_fg, int i_worker, int n_worker,
    bool learn_non_evidence) {
    SingleThreadSampler sampler = SingleThreadSampler(_p_fg, false, 0, learn_non_evidence);
    sampler.sample_sgd(i_worker,n_worker);
  }

  SingleNodeSampler::SingleNodeSampler(FactorGraph * _p_fg, int _nthread, int _nodeid) :
    p_fg (_p_fg), nthread(_nthread), nodeid(_nodeid) {}

  SingleNodeSampler::SingleNodeSampler(FactorGraph * _p_fg, int _nthread, int _nodeid,
    bool sample_evidence, int burn_in) :
    p_fg (_p_fg), nthread(_nthread), nodeid(_nodeid), sample_evidence(sample_evidence),
    burn_in(burn_in) {}

  SingleNodeSampler::SingleNodeSampler(FactorGraph * _p_fg, int _nthread, int _nodeid,
    bool sample_evidence, int burn_in, bool learn_non_evidence) :
    p_fg (_p_fg), nthread(_nthread), nodeid(_nodeid), sample_evidence(sample_evidence),
    burn_in(burn_in), learn_non_evidence(learn_non_evidence) {}

  void SingleNodeSampler::clear_variabletally(){
    for(long i=0;i<p_fg->n_var;i++){
      p_fg->infrs->agg_means[i] = 0.0;
      p_fg->infrs->agg_nsamples[i] = 0.0;
    }
    for(long i=0;i<p_fg->infrs->ntallies;i++){
      p_fg->infrs->multinomial_tallies[i] = 0;
    }
  }

  void SingleNodeSampler::sample(int i_epoch){
    numa_run_on_node(this->nodeid);

    this->threads.clear();
    bool is_burn_in = i_epoch < burn_in;

    for(int i=0;i<this->nthread;i++){
      this->threads.push_back(std::thread(gibbs_single_thread_task, p_fg, i,
        nthread, sample_evidence, is_burn_in));
    }
  }

  void SingleNodeSampler::wait(){
    for(int i=0;i<this->nthread;i++){
      this->threads[i].join();
    }
  }

  void SingleNodeSampler::sample_sgd(){
    numa_run_on_node(this->nodeid);

    this->threads.clear();

    for(int i=0;i<this->nthread;i++){
      this->threads.push_back(std::thread(gibbs_single_thread_sgd_task, p_fg, i,
        nthread, learn_non_evidence));
    }
  }

  void SingleNodeSampler::wait_sgd(){
    for(int i=0;i<this->nthread;i++){
      this->threads[i].join();
    }
  }

}





