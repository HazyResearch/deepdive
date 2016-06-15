#include "gibbs_sampler.h"

namespace dd {

GibbsSampler::GibbsSampler(std::unique_ptr<CompactFactorGraph> pfg_,
                           const Weight weights[], size_t nthread,
                           size_t nodeid, const CmdParser &opts)
    : pfg(std::move(pfg_)),
      opts(opts),
      fg(*pfg),
      infrs(fg, weights, opts),
      nthread(nthread),
      nodeid(nodeid) {}

void GibbsSampler::sample(size_t i_epoch) {
  numa_run_on_node(nodeid);
  for (size_t i = 0; i < nthread; ++i) {
    threads.push_back(std::thread([this, i]() {
      // TODO try to share instances across epochs
      GibbsSamplerThread(fg, infrs, i, nthread, opts).sample();
    }));
  }
}

void GibbsSampler::sample_sgd(double stepsize) {
  numa_run_on_node(nodeid);
  for (size_t i = 0; i < nthread; ++i) {
    threads.push_back(std::thread([this, i, stepsize]() {
      // TODO try to share instances across epochs
      GibbsSamplerThread(fg, infrs, i, nthread, opts).sample_sgd(stepsize);
    }));
  }
}

void GibbsSampler::wait() {
  for (auto &t : threads) t.join();
  threads.clear();
}

GibbsSamplerThread::GibbsSamplerThread(CompactFactorGraph &fg,
                                       InferenceResult &infrs, size_t ith_shard,
                                       size_t n_shards, const CmdParser &opts)
    : varlen_potential_buffer_(0),
      fg(fg),
      infrs(infrs),
      sample_evidence(opts.should_sample_evidence),
      learn_non_evidence(opts.should_learn_non_evidence) {
  set_random_seed(rand(), rand(), rand());
  size_t nvar = fg.size.num_variables;
  // calculates the start and end id in this partition
  start = ((size_t)(nvar / n_shards) + 1) * ith_shard;
  end = ((size_t)(nvar / n_shards) + 1) * (ith_shard + 1);
  end = end > nvar ? nvar : end;
}

void GibbsSamplerThread::set_random_seed(unsigned short seed0,
                                         unsigned short seed1,
                                         unsigned short seed2) {
  p_rand_seed[0] = seed0;
  p_rand_seed[1] = seed1;
  p_rand_seed[2] = seed2;
}

void GibbsSamplerThread::sample() {
  // sample each variable in the partition
  for (long i = start; i < end; ++i) sample_single_variable(i);
}

void GibbsSamplerThread::sample_sgd(double stepsize) {
  for (long i = start; i < end; ++i) sample_sgd_single_variable(i, stepsize);
}
}  // namespace dd
