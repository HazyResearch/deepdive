#ifndef DIMMWITTED_SINGLE_THREAD_SAMPLER_H_
#define DIMMWITTED_SINGLE_THREAD_SAMPLER_H_

#include "factor_graph.h"
#include "timer.h"
#include "common.h"

namespace dd {

/**
 * Class for single thread sampler
 */
class SingleThreadSampler {
 public:
  /**
   * Constructs a SingleThreadSampler with given factor graph
   */
  SingleThreadSampler(CompiledFactorGraph &fg, InferenceResult &infrs,
                      const CmdParser &opts);

  /**
   * Samples variables. The variables are divided into n_sharding equal
   * partitions
   * based on their ids. This function samples variables in the i_sharding-th
   * partition.
   */
  void sample(const int &i_sharding, const int &n_sharding);

  /**
   * Performs SGD with by sampling variables.  The variables are divided into
   * n_sharding equal partitions based on their ids. This function samples
   * variables
   * in the i_sharding-th partition.
   */
  void sample_sgd(const int &i_sharding, const int &n_sharding,
                  double stepsize);

  // factor graph
  CompiledFactorGraph &fg;
  InferenceResult &infrs;

  // random number
  unsigned short p_rand_seed[3];

  // potential for each proposals for multinomial
  std::vector<double> varlen_potential_buffer_;

  bool sample_evidence;
  bool learn_non_evidence;

  /**
   * Performs SGD by sampling a single variable with id vid
   */
  void sample_sgd_single_variable(long vid, double stepsize);

  /**
   * Samples a single variable with id vid
   */
  void sample_single_variable(long vid);

  // sample a single variable
  inline int draw_sample(Variable &variable, const VariableValue assignments[],
                         const double weight_values[]);
};

}  // namespace dd

#endif  // DIMMWITTED_SINGLE_THREAD_SAMPLER_H_
