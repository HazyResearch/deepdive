
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
    FactorGraph * const p_fg;

    // positive and negative potential for boolean variables
    // NOTE In the context of this file and single_thread_sampler.cpp,
    // a potential for a variable value or proposal means the potential of the
    // factors the variable connects to 
    // TODO: these shouldn't be class members
    double potential_pos;
    double potential_neg;

    double potential_pos_freeevid;
    double potential_neg_freeevid;

    // not used
    double proposal_freevid;
    double proposal_fixevid;

    // random number
    double r;
    unsigned short p_rand_seed[3];
    double * const p_rand_obj_buf;

    // potentials for each possible value a variable takes on 
    // (used for multinomial), see .cpp for more detail
    // TODO: this shouldn't be a class member
    std::vector<double> varlen_potential_buffer;

    // these are used for calculating potentials and probabilities
    // see single_thread_sampler.cpp for more detail
    // TODO: these shouldn't be class members
    double sum;
    double acc;
    int multi_proposal;

    bool sample_evidence;
    bool burn_in;
    bool learn_non_evidence;

    /**
     * Constructs a SingleThreadSampler with given factor graph
     */ 
    // SingleThreadSampler(FactorGraph * _p_fg);
    SingleThreadSampler(FactorGraph * _p_fg, bool sample_evidence, bool burn_in,
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

  };

}


#endif

