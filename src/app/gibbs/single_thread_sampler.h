
#include "dstruct/factor_graph/factor_graph.h"
#include "timer.h"
#include "common.h"

#ifndef _SINGLE_THREAD_SAMPLER_H
#define _SINGLE_THREAD_SAMPLER_H

namespace dd{
  
  /** 
   * Class for single thread sampler
   */
  class SingleThreadSampler{

  public:
    // factor graph
    CompiledFactorGraph * const p_fg;

    // random number
    unsigned short p_rand_seed[3];

    // potential for each proposals for multinomial
    std::vector<double> varlen_potential_buffer_;

    // these are used for calculating potentials and probabilities
    // see single_thread_sampler.cpp for more detail
    int multi_proposal;

    bool sample_evidence;
    bool burn_in;
    bool learn_non_evidence;

    /**
     * Constructs a SingleThreadSampler with given factor graph
     */ 
    // SingleThreadSampler(CompiledFactorGraph * _p_fg);
    SingleThreadSampler(CompiledFactorGraph * _p_fg, bool sample_evidence, bool burn_in,
        bool learn_non_evidence);

    /**
     * Samples variables. The variables are divided into n_sharding equal partitions
     * based on their ids. This function samples variables in the i_sharding-th 
     * partition.
     */ 
    void sample(const int & i_sharding, const int & n_sharding);

    /**
     * Performs SGD with by sampling variables.  The variables are divided into 
     * n_sharding equal partitions based on their ids. This function samples variables 
     * in the i_sharding-th partition.
     */
    void sample_sgd(const int & i_sharding, const int & n_sharding);

    /**
     * Performs SGD by sampling a single variable with id vid
     */
    void sample_sgd_single_variable(long vid);

    /**
     * Samples a single variable with id vid
     */
    void sample_single_variable(long vid);

    // sample a single variable
    inline int draw_sample(Variable &variable, bool is_free_sample);

  };

}


#endif

