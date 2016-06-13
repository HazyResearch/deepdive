#include "single_node_sampler.h"

namespace dd {

SingleNodeSampler::SingleNodeSampler(std::unique_ptr<CompiledFactorGraph> pfg_,
                                     const Weight weights[], int nthread,
                                     int nodeid, bool sample_evidence,
                                     int burn_in, bool learn_non_evidence)
    : pfg(std::move(pfg_)),
      fg(*pfg),
      infrs(fg, weights),
      nthread(nthread),
      nodeid(nodeid),
      sample_evidence(sample_evidence),
      burn_in(burn_in),
      learn_non_evidence(learn_non_evidence) {}

void SingleNodeSampler::sample(int i_epoch) {
  numa_run_on_node(this->nodeid);

  this->threads.clear();
  bool is_burn_in = i_epoch < burn_in;

  for (int i = 0; i < this->nthread; i++) {
    this->threads.push_back(std::thread([this, i]() {
      SingleThreadSampler sampler(fg, infrs, 0.0, sample_evidence, burn_in,
                                  false);
      sampler.sample(i, nthread);
    }));
  }
}

void SingleNodeSampler::wait() {
  for (int i = 0; i < this->nthread; i++) {
    this->threads[i].join();
  }
}

void SingleNodeSampler::sample_sgd(double stepsize) {
  numa_run_on_node(this->nodeid);

  this->threads.clear();

  for (int i = 0; i < this->nthread; i++) {
    this->threads.push_back(std::thread([this, i, stepsize]() {
      SingleThreadSampler sampler(fg, infrs, stepsize, false, 0,
                                  learn_non_evidence);
      sampler.sample_sgd(i, nthread);
    }));
  }
}

void SingleNodeSampler::wait_sgd() {
  for (int i = 0; i < this->nthread; i++) {
    this->threads[i].join();
  }
}

}  // namespace dd
