#include "single_thread_sampler.h"

namespace dd {

SingleThreadSampler::SingleThreadSampler(CompiledFactorGraph *_p_fg,
                                         bool sample_evidence, bool burn_in,
                                         bool learn_non_evidence)
    : p_fg(_p_fg),
      varlen_potential_buffer_(0),
      sample_evidence(sample_evidence),
      learn_non_evidence(learn_non_evidence) {
  p_rand_seed[0] = rand();
  p_rand_seed[1] = rand();
  p_rand_seed[2] = rand();
}

void SingleThreadSampler::sample(const int &i_sharding, const int &n_sharding) {
  long nvar = p_fg->size.num_variables;
  // calculates the start and end id in this partition
  long start = ((long)(nvar / n_sharding) + 1) * i_sharding;
  long end = ((long)(nvar / n_sharding) + 1) * (i_sharding + 1);
  end = end > nvar ? nvar : end;

  // sample each variable in the partition
  for (long i = start; i < end; i++) {
    this->sample_single_variable(i);
  }
}

void SingleThreadSampler::sample_sgd(const int &i_sharding,
                                     const int &n_sharding) {
  long nvar = p_fg->size.num_variables;
  long start = ((long)(nvar / n_sharding) + 1) * i_sharding;
  long end = ((long)(nvar / n_sharding) + 1) * (i_sharding + 1);
  end = end > nvar ? nvar : end;
  for (long i = start; i < end; i++) {
    this->sample_sgd_single_variable(i);
  }
}

void SingleThreadSampler::sample_sgd_single_variable(long vid) {
  // stochastic gradient ascent
  // gradient of weight = E[f|D] - E[f], where D is evidence variables,
  // f is the factor function, E[] is expectation. Expectation is calculated
  // using a sample of the variable.

  Variable &variable = p_fg->variables[vid];
  if (variable.is_observation) return;

  if (!learn_non_evidence && !variable.is_evid) return;

  int proposal = 0;

  // sample the variable with evidence unchanged
  proposal = variable.is_evid
                 ? variable.assignment_evid
                 : draw_sample(variable, p_fg->infrs->assignments_evid.get());
  p_fg->infrs->assignments_evid[variable.id] = proposal;

  // sample the variable regardless of whether it's evidence
  proposal = draw_sample(variable, p_fg->infrs->assignments_free.get());
  p_fg->infrs->assignments_free[variable.id] = proposal;

  p_fg->update_weight(variable, p_fg->infrs);
}

void SingleThreadSampler::sample_single_variable(long vid) {
  // this function uses the same sampling technique as in
  // sample_sgd_single_variable

  Variable &variable = this->p_fg->variables[vid];
  if (variable.is_observation) return;

  if (!variable.is_evid || sample_evidence) {
    int proposal = draw_sample(variable, p_fg->infrs->assignments_evid.get());
    p_fg->infrs->assignments_evid[variable.id] = proposal;

    // bookkeep aggregates for computing marginals
    p_fg->infrs->agg_nsamples[variable.id]++;
    switch (variable.domain_type) {
      case DTYPE_BOOLEAN:
        p_fg->infrs->agg_means[variable.id] += proposal;
        break;
      case DTYPE_MULTINOMIAL:
        p_fg->infrs
            ->multinomial_tallies[variable.n_start_i_tally +
                                  variable.get_domain_index((int)proposal)]++;
        break;
      default:
        abort();
    }
  }
}

inline int dd::SingleThreadSampler::draw_sample(
    Variable &variable, const VariableValue assignments[]) {
  int proposal = 0;

  switch (variable.domain_type) {
    case DTYPE_BOOLEAN: {
      double potential_pos;
      double potential_neg;
      potential_pos = p_fg->potential(variable, 1, assignments);
      potential_neg = p_fg->potential(variable, 0, assignments);

      double r = erand48(this->p_rand_seed);
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
#define COMPUTE_PROPOSAL(EACH_DOMAIN_VALUE, DOMAIN_VALUE, DOMAIN_INDEX) \
  do {                                                                  \
          for                                                           \
      EACH_DOMAIN_VALUE {                                               \
        varlen_potential_buffer_[DOMAIN_INDEX] =                        \
            p_fg->potential(variable, DOMAIN_VALUE, assignments);       \
        sum = logadd(sum, varlen_potential_buffer_[DOMAIN_INDEX]);      \
      }                                                                 \
    double r = erand48(this->p_rand_seed);                              \
        for                                                             \
      EACH_DOMAIN_VALUE {                                               \
        r -= exp(varlen_potential_buffer_[DOMAIN_INDEX] - sum);         \
        if (r <= 0) {                                                   \
          proposal = DOMAIN_VALUE;                                      \
          break;                                                        \
        }                                                               \
      }                                                                 \
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
