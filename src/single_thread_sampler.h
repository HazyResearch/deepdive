#ifndef DIMMWITTED_SINGLE_THREAD_SAMPLER_H_
#define DIMMWITTED_SINGLE_THREAD_SAMPLER_H_

#include "factor_graph.h"
#include "timer.h"
#include "common.h"

namespace dd {

/**
 * Class for single thread sampler
 */
class GibbsSamplerThread {
 private:
  // shard and variable id range assigned to this one
  size_t start, end;

  // RNG seed
  unsigned short p_rand_seed[3];

  // potential for each proposals for multinomial
  std::vector<double> varlen_potential_buffer_;

  // references and cached flags
  CompiledFactorGraph &fg;
  InferenceResult &infrs;
  bool sample_evidence;
  bool learn_non_evidence;

 public:
  /**
   * Constructs a GibbsSamplerThread with given factor graph
   */
  GibbsSamplerThread(CompiledFactorGraph &fg, InferenceResult &infrs,
                      int i_sharding, int n_sharding, const CmdParser &opts);

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
  inline void sample_sgd_single_variable(long vid, double stepsize);

  /**
   * Samples a single variable with id vid
   */
  inline void sample_single_variable(long vid);

  // sample a single variable
  inline int draw_sample(Variable &variable, const VariableValue assignments[],
                         const double weight_values[]);

  /**
    * Resets RNG seed to given values
    */
  void set_random_seed(unsigned short s0, unsigned short s1, unsigned short s2);
};

inline void GibbsSamplerThread::sample_sgd_single_variable(long vid,
                                                            double stepsize) {
  // stochastic gradient ascent
  // gradient of weight = E[f|D] - E[f], where D is evidence variables,
  // f is the factor function, E[] is expectation. Expectation is calculated
  // using a sample of the variable.

  Variable &variable = fg.variables[vid];
  if (variable.is_observation) return;

  if (!learn_non_evidence && !variable.is_evid) return;

  int proposal = 0;

  // sample the variable with evidence unchanged
  proposal = variable.is_evid
                 ? variable.assignment_evid
                 : draw_sample(variable, infrs.assignments_evid.get(),
                               infrs.weight_values.get());

  infrs.assignments_evid[variable.id] = proposal;

  // sample the variable regardless of whether it's evidence
  proposal = draw_sample(variable, infrs.assignments_free.get(),
                         infrs.weight_values.get());
  infrs.assignments_free[variable.id] = proposal;

  fg.update_weight(variable, infrs, stepsize);
}

inline void GibbsSamplerThread::sample_single_variable(long vid) {
  // this function uses the same sampling technique as in
  // sample_sgd_single_variable

  Variable &variable = fg.variables[vid];
  if (variable.is_observation) return;

  if (!variable.is_evid || sample_evidence) {
    int proposal = draw_sample(variable, infrs.assignments_evid.get(),
                               infrs.weight_values.get());
    infrs.assignments_evid[variable.id] = proposal;

    // bookkeep aggregates for computing marginals
    infrs.agg_nsamples[variable.id]++;
    switch (variable.domain_type) {
      case DTYPE_BOOLEAN:
        infrs.agg_means[variable.id] += proposal;
        break;
      case DTYPE_MULTINOMIAL:
        infrs.multinomial_tallies[variable.n_start_i_tally +
                                  variable.get_domain_index((int)proposal)]++;
        break;
      default:
        abort();
    }
  }
}

inline int dd::GibbsSamplerThread::draw_sample(
    Variable &variable, const VariableValue assignments[],
    const double weight_values[]) {
  int proposal = 0;

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

    case DTYPE_MULTINOMIAL: {
      varlen_potential_buffer_.reserve(variable.cardinality);
      double sum = -100000.0;

      proposal = -1;

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
      if (variable.domain_map) {  // sparse case
        COMPUTE_PROPOSAL((const auto &entry
                          : *variable.domain_map),
                         entry.first, entry.second);
      } else {  // dense case
        COMPUTE_PROPOSAL((size_t i = 0; i < variable.cardinality; ++i), i, i);
      }

      assert(proposal != -1);
      break;
    }

    default:
      // unsupported variable types
      abort();
  }

  return proposal;
}

}  // namespace dd

#endif  // DIMMWITTED_SINGLE_THREAD_SAMPLER_H_
