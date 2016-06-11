#include "single_node_sampler.h"

namespace dd {

SingleNodeSampler::SingleNodeSampler(CompiledFactorGraph &fg,
                                     const Weight weights[], int nthread,
                                     int nodeid)
    : SingleNodeSampler(fg, weights, nthread, nodeid, false, false) {}

SingleNodeSampler::SingleNodeSampler(CompiledFactorGraph &fg,
                                     const Weight weights[], int nthread,
                                     int nodeid, bool sample_evidence,
                                     int burn_in)
    : SingleNodeSampler(fg, weights, nthread, nodeid, sample_evidence, burn_in,
                        false) {}

SingleNodeSampler::SingleNodeSampler(CompiledFactorGraph &fg,
                                     const Weight weights[], int nthread,
                                     int nodeid, bool sample_evidence,
                                     int burn_in, bool learn_non_evidence)
    : fg(fg),
      infrs(fg, weights),
      nthread(nthread),
      nodeid(nodeid),
      sample_evidence(sample_evidence),
      burn_in(burn_in),
      learn_non_evidence(learn_non_evidence) {}

void SingleNodeSampler::clear_variabletally() {
  for (long i = 0; i < fg.size.num_variables; i++) {
    infrs.agg_means[i] = 0.0;
    infrs.agg_nsamples[i] = 0.0;
  }
  for (long i = 0; i < infrs.ntallies; i++) {
    infrs.multinomial_tallies[i] = 0;
  }
}

void SingleNodeSampler::sample(int i_epoch) {
  numa_run_on_node(this->nodeid);

  this->threads.clear();
  bool is_burn_in = i_epoch < burn_in;

  for (int i = 0; i < this->nthread; i++) {
    this->threads.push_back(std::thread([&]() {
      SingleThreadSampler sampler =
          SingleThreadSampler(fg, infrs, 0.0, sample_evidence, burn_in, false);
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
    this->threads.push_back(std::thread([&]() {
      SingleThreadSampler sampler = SingleThreadSampler(
          fg, infrs, stepsize, false, 0, learn_non_evidence);
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
