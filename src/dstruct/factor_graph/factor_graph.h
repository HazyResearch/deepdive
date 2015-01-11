
#include "dstruct/factor_graph/factor_graph.pb.h"
#include "io/cmd_parser.h"
#include "common.h"

#include "dstruct/factor_graph/variable.h"
#include "dstruct/factor_graph/factor.h"
#include "dstruct/factor_graph/weight.h"
#include "dstruct/factor_graph/meta.h"

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
    double * const assignments_free;
    double * const assignments_evid;
    double * const weight_values;
    bool * const weights_isfixed;

    InferenceResult(long _nvars, long _nweights):
      assignments_free(new double[_nvars]),
      assignments_evid(new double[_nvars]),
      agg_means(new double[_nvars]),
      agg_nsamples(new double[_nvars]),
      weight_values(new double [_nweights]),
      weights_isfixed(new bool [_nweights]),
      nweights(_nweights),
      nvars(_nvars){}

    void init(Variable * variables, Weight * const weights){

      ntallies = 0;
      for(long t=0;t<nvars;t++){
        const Variable & variable = variables[t];
        assignments_free[variable.id] = variable.assignment_free;
        assignments_evid[variable.id] = variable.assignment_evid;
        agg_means[variable.id] = 0.0;
        agg_nsamples[variable.id] = 0.0;
        if(variable.domain_type == DTYPE_MULTINOMIAL){
          //variable.n_start_i_tally = ntallies;
          ntallies += variable.upper_bound - variable.lower_bound + 1;
        }
      }

      multinomial_tallies = new int[ntallies];
      for(long i=0;i<ntallies;i++){
        multinomial_tallies[i] = 0;
      }

      for(long t=0;t<nweights;t++){
        const Weight & weight = weights[t];
        weight_values[weight.id] = weight.weight;
        weights_isfixed[weight.id] = weight.isfixed;
      }
    }
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

    FactorGraph(long _n_var, long _n_factor, long _n_weight, long _n_edge) : 
      variables(new Variable[_n_var]),
      factors(new Factor[_n_factor]),
      weights(new Weight[_n_weight]),
      n_var(_n_var), n_factor(_n_factor), n_weight(_n_weight),
      infrs(new InferenceResult(_n_var, _n_weight)),
      n_edge(_n_edge),
      factor_ids(new long[_n_edge]),
      factors_dups(new CompactFactor[_n_edge]),
      vifs(new VariableInFactor[_n_edge]),
      factors_dups_weightids(new int[_n_edge])
    {
      this->loading_finalized = false;
      this->safety_check_passed = false;
      c_nvar = 0;
      c_nfactor = 0;
      c_nweight = 0;
      n_evid = 0;
      n_query = 0;
    }

    void copy_from(const FactorGraph * const p_other_fg){

      memcpy(variables, p_other_fg->variables, sizeof(Variable)*n_var);
      memcpy(factors, p_other_fg->factors, sizeof(Factor)*n_factor);
      memcpy(weights, p_other_fg->weights, sizeof(Weight)*n_weight);
      memcpy(factor_ids, p_other_fg->factor_ids, sizeof(long)*n_edge);
      memcpy(vifs, p_other_fg->vifs, sizeof(VariableInFactor)*n_edge);

      memcpy(factors_dups, p_other_fg->factors_dups, sizeof(CompactFactor)*n_edge);
      memcpy(factors_dups_weightids, p_other_fg->factors_dups_weightids, sizeof(int)*n_edge);

      c_nvar = p_other_fg->c_nvar;
      c_nfactor = p_other_fg->c_nfactor;
      c_nweight = p_other_fg->c_nweight;
      c_edge = p_other_fg->c_edge;
      loading_finalized = p_other_fg->loading_finalized;
      safety_check_passed = p_other_fg->safety_check_passed;

      infrs->init(variables, weights);
      infrs->ntallies = p_other_fg->infrs->ntallies;
      infrs->multinomial_tallies = new int[p_other_fg->infrs->ntallies];
      for(long i=0;i<infrs->ntallies;i++){
        infrs->multinomial_tallies[i] = p_other_fg->infrs->multinomial_tallies[i];
      }
    }

    /*
     * For multinomial weights, given a factor and variable assignment,
     * return corresponding weight id
     */
    long get_weightid(const double *assignments, const CompactFactor& fs, long vid, long proposal) {
      long weight_offset = 0;
      for (long i = fs.n_start_i_vif; i < fs.n_start_i_vif + fs.n_variables; i++) {
        const VariableInFactor & vif = vifs[i];
        if (vif.vid == vid) {
          weight_offset = weight_offset * (variables[vif.vid].upper_bound+1) + proposal;
        } else {
          weight_offset = weight_offset * (variables[vif.vid].upper_bound+1) + assignments[vif.vid];
        }
      }
      long base_offset = &fs - factors_dups; // note c++ will auto scale by sizeof(CompactFactor)
      return *(factors_dups_weightids + base_offset) + weight_offset;
    }

    void update_weight(const Variable & variable){
      CompactFactor * const fs = factors_dups + variable.n_start_i_factors;
      const int * const ws = factors_dups_weightids + variable.n_start_i_factors;
      for(long i=0;i<variable.n_factors;i++){
        if (variable.domain_type == DTYPE_BOOLEAN) {
          if(infrs->weights_isfixed[ws[i]] == false){
            if(fs[i].func_id != 20){
              infrs->weight_values[ws[i]] += 
                stepsize * (this->template potential<false>(fs[i]) - this->template potential<true>(fs[i]));
            }else{

              const int & dimension = vifs[fs[i].n_start_i_vif].dimension;

              const long & cvid = vifs[fs[i].n_start_i_vif].vid;
              const long & wid = ws[i];
              const long & bvid = vifs[fs[i].n_start_i_vif+1].vid;

              const long assignments_evid_bvid = infrs->assignments_evid[bvid];
              const long assignments_free_bvid = infrs->assignments_free[bvid];

              if(assignments_evid_bvid != assignments_free_bvid){
                for(int j=0;j<dimension;j++){
                  infrs->weight_values[wid + j] +=
                      stepsize * (assignments_evid_bvid * infrs->assignments_evid[cvid+j]
                        - assignments_free_bvid * infrs->assignments_free[cvid+j]);
                }
              }
            }
          }
        } else if (variable.domain_type == DTYPE_MULTINOMIAL) {
          // two weights need to be updated
          // sample with evidence fixed, I0, with variable assignment d0
          // sample without evidence unfixed, I1, with variable assigment d1 
          // gradient of wd0 = f(I0) - I(d0==d1)f(I1)
          // gradient of wd1 = I(d0==d1)f(I0) - f(I1)
          if(fs[i].func_id != 20){
            long wid1 = get_weightid(infrs->assignments_evid, fs[i], -1, -1);
            long wid2 = get_weightid(infrs->assignments_free, fs[i], -1, -1);
            int equal = infrs->assignments_evid[variable.id] == infrs->assignments_free[variable.id];

            if(infrs->weights_isfixed[wid1] == false){
              infrs->weight_values[wid1] += 
                stepsize * (this->template potential<false>(fs[i]) - equal * this->template potential<true>(fs[i]));
            }

            if(infrs->weights_isfixed[wid2] == false){
              infrs->weight_values[wid2] += 
                stepsize * (equal * this->template potential<false>(fs[i]) - this->template potential<true>(fs[i]));
            }
          }else{

            const int & dimension = vifs[fs[i].n_start_i_vif].dimension;
            const long & cvid = vifs[fs[i].n_start_i_vif].vid;
            const long & bvid = vifs[fs[i].n_start_i_vif+1].vid;

            const long assignments_evid_bvid = infrs->assignments_evid[bvid];
            const long assignments_free_bvid = infrs->assignments_free[bvid];

            if(assignments_evid_bvid != assignments_free_bvid){

              // udpate the weight for assignments_evid_bvid
              long wid = ws[i] + dimension * assignments_evid_bvid;
              for(int j=0;j<dimension;j++){
                infrs->weight_values[wid + j] +=
                    stepsize * (infrs->assignments_evid[cvid+j]);
              }

              // update the wight for assignments_free_bvid
              wid = ws[i] + dimension * assignments_free_bvid;
              for(int j=0;j<dimension;j++){
                infrs->weight_values[wid + j] +=
                    stepsize * (-infrs->assignments_evid[cvid+j]);
              }

            }

          }
        }
      }
    }

    // void update_weight_multinomial(const Variable & variable){
    //   const CompactFactor * const fs = factors_dups + variable.n_start_i_factors;
    //   const int * const ws = factors_dups_weightids + variable.n_start_i_factors;

    //   // for each factor
    //   for(long i = 0; i < variable.n_factors; i++){
    //     // two weights need to be updated
    //     // sample with evidence fixed, I0, with variable assignment d0
    //     // sample without evidence unfixed, I1, with variable assigment d1 
    //     // gradient of wd0 = f(I0) - I(d0==d1)f(I1)
    //     // gradient of wd1 = I(d0==d1)f(I0) - f(I1)
    //     long wid1 = get_weightid(false, fs[i], -1, -1);
    //     long wid2 = get_weightid(true, fs[i], -1, -1);
    //     int equal = infrs->assignments_evid[variable.id] == infrs->assignments_free[variable.id];

    //     if(infrs->weights_isfixed[wid1] == false){
    //       infrs->weight_values[wid1] += 
    //         stepsize * (this->template potential<false>(fs[i]) - equal * this->template potential<true>(fs[i]));
    //     }

    //     if(infrs->weights_isfixed[wid2] == false){
    //       infrs->weight_values[wid2] += 
    //         stepsize * (equal * this->template potential<false>(fs[i]) - this->template potential<true>(fs[i]));
    //     }
    //   }
    // }

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

    bool is_usable(){
      return this->loading_finalized && this->safety_check_passed;
    }

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




