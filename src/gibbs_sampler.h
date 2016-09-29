#ifndef DIMMWITTED_GIBBS_SAMPLER_H_
#define DIMMWITTED_GIBBS_SAMPLER_H_

#include "common.h"
#include "factor_graph.h"
#include "numa_nodes.h"
#include "timer.h"
#include <stdlib.h>
#include <thread>

namespace dd {

class GibbsSamplerThread;

/**
 * Class for a single NUMA node sampler
 */
class GibbsSampler {
 private:
  std::unique_ptr<FactorGraph> pfg;
  std::unique_ptr<InferenceResult> pinfrs;
  NumaNodes numa_nodes_;
  std::vector<GibbsSamplerThread> workers;
  std::vector<std::thread> threads;

 public:
  FactorGraph &fg;
  InferenceResult &infrs;

  // number of threads
  size_t nthread;
  // node id
  size_t nodeid;

  /**
   * Constructs a GibbsSampler given factor graph, number of threads, and
   * node id.
   */
  GibbsSampler(std::unique_ptr<FactorGraph> pfg, const Weight weights[],
               const NumaNodes &numa_nodes, size_t nthread, size_t nodeid,
               const CmdParser &opts);

  /**
   * Performs sample
   */
  void sample(size_t i_epoch);

  /**
   * Performs SGD
   */
  void sample_sgd(double stepsize);

  /**
   * Waits for sample worker to finish
   */
  void wait();
};

/**
 * Class for single thread sampler
 */
class GibbsSamplerThread {
 private:
  // shard and variable id range assigned to this one
  size_t start, end;

  // RNG seed
  unsigned short p_rand_seed[3];

  // potential for each proposals for categorical
  std::vector<double> varlen_potential_buffer_;

  // references and cached flags
  FactorGraph &fg;
  InferenceResult &infrs;
  bool sample_evidence;
  bool learn_non_evidence;
  bool is_noise_aware;

 public:
  /**
   * Constructs a GibbsSamplerThread with given factor graph
   */
  GibbsSamplerThread(FactorGraph &fg, InferenceResult &infrs, size_t i_sharding,
                     size_t n_sharding, const CmdParser &opts);

  /**
   * Samples variables. The variables are divided into n_sharding equal
   * partitions
   * based on their ids. This function samples variables in the i_sharding-th
   * partition.
   */
  void sample();

  /**
   * Performs SGD with by sampling variables.  The variables are divided into
   * n_sharding equal partitions based on their ids. This function samples
   * variables
   * in the i_sharding-th partition.
   */
  void sample_sgd(double stepsize);

  /**
   * Performs SGD by sampling a single variable with id vid
   */
  inline void sample_sgd_single_variable(size_t vid, double stepsize);

  /**
   * Samples a single variable with id vid
   */
  inline void sample_single_variable(size_t vid);

  // sample an "evidence" variable (parallel Gibbs conditioned on evidence)
  inline size_t sample_evid(const Variable &variable);

  // sample a single variable (regular Gibbs)
  inline size_t draw_sample(const Variable &variable,
                            const size_t assignments[],
                            const double weight_values[]);

  /**
    * Resets RNG seed to given values
    */
  void set_random_seed(unsigned short s0, unsigned short s1, unsigned short s2);
};

inline void GibbsSamplerThread::sample_sgd_single_variable(size_t vid,
                                                           double stepsize) {
  // stochastic gradient ascent
  // gradient of weight = E[f|D] - E[f], where D is evidence variables,
  // f is the factor function, E[] is expectation. Expectation is calculated
  // using a sample of the variable.

  const Variable &variable = fg.variables[vid];

  // pick a value for the regular Gibbs chain
  size_t proposal = draw_sample(variable, infrs.assignments_free.get(),
                                infrs.weight_values.get());
  infrs.assignments_free[variable.id] = proposal;

  // pick a value for the (parallel) evid Gibbs chain
  infrs.assignments_evid[variable.id] = sample_evid(variable);

  if (!learn_non_evidence && ((!is_noise_aware && !variable.is_evid) ||
                              (is_noise_aware && !variable.has_truthiness())))
    return;

  fg.sgd_on_variable(variable, infrs, stepsize, is_noise_aware);
}

inline void GibbsSamplerThread::sample_single_variable(size_t vid) {
  // this function uses the same sampling technique as in
  // sample_sgd_single_variable

  const Variable &variable = fg.variables[vid];

  if (!variable.is_evid || sample_evidence) {
    size_t proposal = draw_sample(variable, infrs.assignments_evid.get(),
                                  infrs.weight_values.get());
    infrs.assignments_evid[variable.id] = proposal;

    // bookkeep aggregates for computing marginals
    ++infrs.agg_nsamples[variable.id];
    if (!variable.is_boolean() || proposal == 1) {
      ++infrs.sample_tallies[variable.var_val_base +
                             variable.var_value_offset(proposal)];
    }
  }
}

inline size_t GibbsSamplerThread::sample_evid(const Variable &variable) {
  if (!is_noise_aware && variable.is_evid) {
    // direct assignment of hard "evidence"
    return variable.assignment_dense;
  } else if (is_noise_aware && variable.has_truthiness()) {
    // truthiness-weighted sample of soft "evidence" values
    double r = erand48(p_rand_seed);
    double sum = 0;
    for (size_t i = 0; i < variable.cardinality; ++i) {
      double truthiness = fg.values[variable.var_val_base + i].truthiness;
      sum += truthiness;
      if (sum >= r) return i;
    }
    return 0;
  } else {
    // Gibbs sample on the assignments_evid chain
    return draw_sample(variable, infrs.assignments_evid.get(),
                       infrs.weight_values.get());
  }
}

inline size_t GibbsSamplerThread::draw_sample(const Variable &variable,
                                              const size_t assignments[],
                                              const double weight_values[]) {
  size_t proposal = 0;

  switch (variable.domain_type) {
    case DTYPE_BOOLEAN: {
      double potential_pos;
      double potential_neg;
      potential_pos = fg.potential(variable, 1, assignments, weight_values);
      potential_neg = fg.potential(variable, 0, assignments, weight_values);

      double r = erand48(p_rand_seed);
      // sample the variable
      // flip a coin with probability
      // (exp(potential_pos) + exp(potential_neg)) / exp(potential_neg)
      // = exp(potential_pos - potential_neg) + 1
      if (r * (1.0 + exp(potential_neg - potential_pos)) < 1.0) {
        proposal = 1;
      } else {
        proposal = 0;
      }
      break;
    }

    case DTYPE_CATEGORICAL: {
      varlen_potential_buffer_.reserve(variable.cardinality);
      double sum = -100000.0;
      proposal = Variable::INVALID_VALUE;
// calculate potential for each proposal given a way to iterate the domain
#define COMPUTE_PROPOSAL(EACH_DOMAIN_VALUE, DOMAIN_VALUE, DOMAIN_INDEX)       \
  do {                                                                        \
          for                                                                 \
      EACH_DOMAIN_VALUE {                                                     \
        varlen_potential_buffer_[DOMAIN_INDEX] =                              \
            fg.potential(variable, DOMAIN_VALUE, assignments, weight_values); \
        sum = logadd(sum, varlen_potential_buffer_[DOMAIN_INDEX]);            \
      }                                                                       \
    double r = erand48(p_rand_seed);                                          \
        for                                                                   \
      EACH_DOMAIN_VALUE {                                                     \
        r -= exp(varlen_potential_buffer_[DOMAIN_INDEX] - sum);               \
        if (r <= 0) {                                                         \
          proposal = DOMAIN_VALUE;                                            \
          break;                                                              \
        }                                                                     \
      }                                                                       \
  } while (0)
      // All sparse values have been converted into dense values in
      // FactorGraph.load_domains
      COMPUTE_PROPOSAL((size_t i = 0; i < variable.cardinality; ++i), i, i);

      assert(proposal != Variable::INVALID_VALUE);
      break;
    }

    default:
      // unsupported variable types
      std::abort();
  }

  return proposal;
}

}  // namespace dd

#endif  // DIMMWITTED_GIBBS_SAMPLER_H_
