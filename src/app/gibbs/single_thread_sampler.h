
#include "dstruct/factor_graph/factor_graph.h"
#include "timer.h"
#include "common.h"

#ifndef _SINGLE_THREAD_SAMPLER_H
#define _SINGLE_THREAD_SAMPLER_H

namespace dd{

  class SingleThreadSampler{

  public:
    FactorGraph * const p_fg;

    double potential_pos;
    double potential_neg;

    double potential_pos_freeevid;
    double potential_neg_freeevid;

    double proposal_freevid;
    double proposal_fixevid;

    double r;
    unsigned short p_rand_seed[3];
    double * const p_rand_obj_buf;

    std::vector<double> varlen_potential_buffer;

    double sum;
    double acc;
    double multi_proposal;

    SingleThreadSampler(FactorGraph * _p_fg);

    void sample(const int & i_sharding, const int & n_sharding);

    void sample_sgd(const int & i_sharding, const int & n_sharding);

  private:

    void sample_sgd_single_variable(long vid);

    void sample_single_variable(long vid);

  };

}


#endif

