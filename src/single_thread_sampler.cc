#include "single_thread_sampler.h"

namespace dd {

SingleThreadSampler::SingleThreadSampler(CompiledFactorGraph &fg,
                                         InferenceResult &infrs, int ith_shard,
                                         int n_shards, const CmdParser &opts)
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

void SingleThreadSampler::set_random_seed(unsigned short seed0,
                                          unsigned short seed1,
                                          unsigned short seed2) {
  p_rand_seed[0] = seed0;
  p_rand_seed[1] = seed1;
  p_rand_seed[2] = seed2;
}

void SingleThreadSampler::sample() {
  // sample each variable in the partition
  for (long i = start; i < end; ++i) sample_single_variable(i);
}

void SingleThreadSampler::sample_sgd(double stepsize) {
  for (long i = start; i < end; ++i) sample_sgd_single_variable(i, stepsize);
}

}  // namespace dd
