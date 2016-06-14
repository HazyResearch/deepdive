#include "single_node_sampler.h"

namespace dd {

SingleNodeSampler::SingleNodeSampler(std::unique_ptr<CompiledFactorGraph> pfg_,
                                     const Weight weights[], int nthread,
                                     int nodeid, const CmdParser &opts)
    : pfg(std::move(pfg_)),
      opts(opts),
      fg(*pfg),
      infrs(fg, weights, opts),
      nthread(nthread),
      nodeid(nodeid) {}

void SingleNodeSampler::sample(int i_epoch) {
  numa_run_on_node(nodeid);
  threads.clear();
  for (int i = 0; i < nthread; ++i) {
    threads.push_back(std::thread([this, i]() {
      // TODO try to share instances across epochs
      SingleThreadSampler sampler(fg, infrs, i, nthread, opts);
      sampler.sample();
    }));
  }
}

void SingleNodeSampler::wait() {
  for (auto &t : threads) t.join();
}

void SingleNodeSampler::sample_sgd(double stepsize) {
  numa_run_on_node(nodeid);
  threads.clear();
  for (int i = 0; i < nthread; ++i) {
    threads.push_back(std::thread([this, i, stepsize]() {
      // TODO try to share instances across epochs
      SingleThreadSampler sampler(fg, infrs, i, nthread, opts);
      sampler.sample_sgd(stepsize);
    }));
  }
}

void SingleNodeSampler::wait_sgd() {
  for (auto &t : threads) t.join();
}

}  // namespace dd
