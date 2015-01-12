
#include "io/cmd_parser.h"
#include "common.h"

#include "dstruct/factor_graph/variable.h"
#include "dstruct/factor_graph/factor.h"
#include "dstruct/factor_graph/weight.h"

#include <xmmintrin.h>

#ifndef _FACTOR_GRAPH_H_
#define _FACTOR_GRAPH_H_

#define DTYPE_BOOLEAN     0x00
#define DTYPE_REAL        0x01
#define DTYPE_MULTINOMIAL 0x04

namespace dd{

  class InferenceResult{
  public:

    long nvars;
    long nweights;
    long ntallies;

    int * multinomial_tallies; // this might be slow...

    double * const agg_means;
    double * const agg_nsamples; 
    VariableValue * const assignments_free;
    VariableValue * const assignments_evid;
    double * const weight_values;
    bool * const weights_isfixed;

    InferenceResult(long _nvars, long _nweights);

    void init(Variable * variables, Weight * const weights);
  };

  class FactorGraph {
  public:

    Variable * const variables;
    Factor * const factors;
    Weight * const weights;

    CompactFactor * const factors_dups;
    int * const factors_dups_weightids;
    long * const factor_ids;
    VariableInFactor * const vifs;

    long n_var;
    long n_factor;
    long n_weight;
    long n_edge;
    long n_tally;

    long c_nvar;
    long c_nfactor;
    long c_nweight;
    long c_edge;

    long n_evid;
    long n_query;

    bool loading_finalized;
    bool safety_check_passed;

    InferenceResult * const infrs ;

    double stepsize;

    int tmp;

    FactorGraph(long _n_var, long _n_factor, long _n_weight, long _n_edge);

    void copy_from(const FactorGraph * const p_other_fg);

    /*
     * For multinomial weights, given a factor and variable assignment,
     * return corresponding weight id
     */
    long get_weightid(const VariableValue *assignments, const CompactFactor& fs, long vid, long proposal);

    void update_weight(const Variable & variable);

    template<bool does_change_evid>
    inline double potential(const CompactFactor & factor){
      if(does_change_evid == true){
        return factor.potential(vifs, infrs->assignments_free, -1, -1);
      }else{
        return factor.potential(vifs, infrs->assignments_evid, -1, -1);
      }
    }
  
    template<bool does_change_evid>
    inline void update(Variable & variable, const double & new_value);

    template<bool does_change_evid>
    inline double potential(const Variable & variable, const double & proposal){
      double pot = 0.0;  
      double tmp;
      long wid = 0;
      CompactFactor * const fs = &factors_dups[variable.n_start_i_factors];
      const int * const ws = &factors_dups_weightids[variable.n_start_i_factors];   
      if (variable.domain_type == DTYPE_BOOLEAN) {   
        for(long i=0;i<variable.n_factors;i++){
          if(fs[i].func_id != 20){
            if(does_change_evid == true){
              tmp = fs[i].potential(
                  vifs, infrs->assignments_free, variable.id, proposal);
            }else{
              tmp = fs[i].potential(
                  vifs, infrs->assignments_evid, variable.id, proposal);
            }
            pot += infrs->weight_values[ws[i]] * tmp;
          }else{
            tmp = 0.0;
            const int & dimension = vifs[fs[i].n_start_i_vif].dimension;
            //std::cout << "~~~~~" << dimension << std::endl;
            const long & cvid = vifs[fs[i].n_start_i_vif].vid;
            const long & wid = ws[i];

            if(proposal != 0){
              for(int j=0;j<dimension;j++){
                if(does_change_evid == true){
                  tmp += infrs->weight_values[wid + j] * 
                    proposal * infrs->assignments_free[cvid+j];
                  //tmp += infrs->weight_values[ws[i] + j] * fs[i].potential(
                  //    vifs, infrs->assignments_free, variable.id, proposal);
                }else{
                  tmp += infrs->weight_values[wid + j] * 
                    proposal * infrs->assignments_evid[cvid+j];

                  //tmp += infrs->weight_values[ws[i] + j] * fs[i].potential(
                  //    vifs, infrs->assignments_evid, variable.id, proposal);
                }
              }
            }

            pot += tmp;

          }
        }
      } else if (variable.domain_type == DTYPE_MULTINOMIAL) {
        for (long i = 0; i < variable.n_factors; i++) {
          if(fs[i].func_id != 20){
            if(does_change_evid == true) {
              tmp = fs[i].potential(vifs, infrs->assignments_free, variable.id, proposal);
              wid = get_weightid(infrs->assignments_free, fs[i], variable.id, proposal);
            } else {
              tmp = fs[i].potential(vifs, infrs->assignments_evid, variable.id, proposal);
              wid = get_weightid(infrs->assignments_evid, fs[i], variable.id, proposal);
              // for (int j = 0; j < n_var; j++) {
              //   printf("%f ", infrs->assignments_evid[j]);
              // }
              // printf("proposal = %d, weight id = %d\n", proposal, weight);
            }
            pot += infrs->weight_values[wid] * tmp;
          }else{

            tmp = 0.0;
            const int & dimension = vifs[fs[i].n_start_i_vif].dimension;
            //std::cout << "~~~~~" << dimension << std::endl;
            const long & cvid = vifs[fs[i].n_start_i_vif].vid;
            const long & wid = ws[i] + dimension * proposal;

            for(int j=0;j<dimension;j++){
              if(does_change_evid == true){
                tmp += infrs->weight_values[wid + j] * 
                  infrs->assignments_free[cvid+j];
                //tmp += infrs->weight_values[ws[i] + j] * fs[i].potential(
                //    vifs, infrs->assignments_free, variable.id, proposal);
              }else{
                tmp += infrs->weight_values[wid + j] * 
                  infrs->assignments_evid[cvid+j];

                //tmp += infrs->weight_values[ws[i] + j] * fs[i].potential(
                //    vifs, infrs->assignments_evid, variable.id, proposal);
              }
            }

            pot += tmp;

          }
        }
      }
      return pot;
    }

    void load(const CmdParser & cmd);

    void finalize_loading();

    void safety_check();

    bool is_usable();
  };

  template<>
  inline void FactorGraph::update<true>(Variable & variable, const double & new_value){
    infrs->assignments_free[variable.id] = new_value;
  }

  template<>
  inline void FactorGraph::update<false>(Variable & variable, const double & new_value){
    infrs->assignments_evid[variable.id] = new_value;
    infrs->agg_means[variable.id] += new_value;
    infrs->agg_nsamples[variable.id]  ++ ;
    if(variable.domain_type == DTYPE_MULTINOMIAL){
      infrs->multinomial_tallies[variable.n_start_i_tally + (int)(new_value)] ++;
    }
  }



}

#endif




