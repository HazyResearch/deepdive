#include "gibbs_sampler.h"

namespace dd {

GibbsSampler::GibbsSampler(std::unique_ptr<FactorGraph> _pfg,
                           const Weight weights[], const NumaNodes &numa_nodes,
                           size_t nthread, size_t nodeid, const CmdParser &opts)
    : pfg(std::move(_pfg)),
      pinfrs(new InferenceResult(*pfg, weights, opts)),
      numa_nodes_(numa_nodes),
      fg(*pfg),
      infrs(*pinfrs),
      nthread(nthread),
      nodeid(nodeid) {
  assert(nthread > 0);
  for (size_t i = 0; i < nthread; ++i)
    workers.push_back(GibbsSamplerThread(fg, infrs, i, nthread, opts));
}

void GibbsSampler::sample(size_t i_epoch) {
  numa_nodes_.bind();
  for (auto &worker : workers) {
    threads.push_back(std::thread([&worker]() { worker.sample(); }));
  }
}

void GibbsSampler::sample_sgd(double stepsize) {
  numa_nodes_.bind();
  for (auto &worker : workers) {
    threads.push_back(
        std::thread([&worker, stepsize]() { worker.sample_sgd(stepsize); }));
  }
}

void GibbsSampler::wait() {
  for (auto &t : threads) t.join();
  threads.clear();
}

GibbsSamplerThread::GibbsSamplerThread(FactorGraph &fg, InferenceResult &infrs,
                                       size_t ith_shard, size_t n_shards,
                                       const CmdParser &opts)
    : varlen_potential_buffer_(0),
      fg(fg),
      infrs(infrs),
      sample_evidence(opts.should_sample_evidence),
      learn_non_evidence(opts.should_learn_non_evidence),
      is_noise_aware(opts.is_noise_aware) {
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
  for (size_t vid = start; vid < end; ++vid) {
    sample_single_variable(vid);
  }
}

void GibbsSamplerThread::sample_sgd(double stepsize) {
  for (size_t vid = start; vid < end; ++vid) {
    sample_sgd_single_variable(vid, stepsize);
  }
}

}  // namespace dd
