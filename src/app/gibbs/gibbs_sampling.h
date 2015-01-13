
#include <iostream>
#include "io/cmd_parser.h"
#include "dstruct/factor_graph/factor_graph.h"

#ifndef _GIBBS_SAMPLING_H_
#define _GIBBS_SAMPLING_H_

namespace dd{
  class GibbsSampling{
  public:

    FactorGraph * const p_fg;

    CmdParser * const p_cmd_parser;

    int n_numa_nodes;

    int n_thread_per_numa;

    std::vector<FactorGraph> factorgraphs;

    GibbsSampling(FactorGraph * const _p_fg, CmdParser * const _p_cmd_parser, int n_datacopy) 
      : p_fg(_p_fg), p_cmd_parser(_p_cmd_parser){
      prepare(n_datacopy);
    }

    void prepare(int n_datacopy);

    void inference(const int & n_epoch);

    void dump();

    void dump_weights();

    void learn(const int & n_epoch, const int & n_sample_per_epoch, 
      const double & stepsize, const double & decay);

  };
}




#endif