
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

  /** 
   * Encapsulates inference result statistics
   */
  class InferenceResult{
  public:

    long nvars;     // number of variables
    long nweights;  // number of weights
    long ntallies;

    int * multinomial_tallies; // this might be slow...

    // array of sum of samples for each variable
    double * const agg_means; 
    // array of number of samples for each variable
    double * const agg_nsamples; 
    // assignment to variables, see variable.h for more detail
    VariableValue * const assignments_free;
    VariableValue * const assignments_evid;
    double * const weight_values; // array of weight values
    bool * const weights_isfixed; // array of whether weight is fixed

    InferenceResult(long _nvars, long _nweights);

    /**
     * Initialize the class with given variables and weights
     */
    void init(Variable * variables, Weight * const weights);
  };


  /**
   * Class for a factor graph
   */
  class FactorGraph {
  public:

    // countings for components of factor graph
    long n_var;
    long n_factor;
    long n_weight;
    long n_edge;
    long n_tally;

    long c_nvar;
    long c_nfactor;
    long c_nweight;
    long c_edge;

    // number of evidence variables, query variables
    long n_evid;
    long n_query;

    // learning weight update stepsize (learning rate)
    double stepsize;

    // variables, factors, weights
    Variable * const variables;
    Factor * const factors;
    Weight * const weights;

    // For each edge, we store the factor, weight id, factor id, and the variable, 
    // in the same index of seperate arrays. The edges are ordered so that the
    // edges for a variable is in a continuous region (sequentially). 
    // This allows us to access factors given variables, and access variables
    // given factors faster. 
    CompactFactor * const factors_dups;
    int * const factors_dups_weightids;
    long * const factor_ids;
    VariableInFactor * const vifs;

    // pointer to inference result
    InferenceResult * const infrs ;

    // whether the factor graph loading has been finalized
    // see sort_by_id() below
    bool sorted;
    // whether safety check has passed
    // see safety_check() below
    bool safety_check_passed;

    /**
     * Constructs a new factor graph with given number number of variables,
     * factors, weights, and edges
     */
    FactorGraph(long _n_var, long _n_factor, long _n_weight, long _n_edge);

    /**
     * Copys a factor graph from the given one
     */
    void copy_from(const FactorGraph * const p_other_fg);

    /*
     * Given a factor and variable assignment, returns corresponding multinomial 
     * factor weight id, using proposal value for the variable with id vid.
     * For multinomial weights, for each possible assignment, there is a corresponding 
     * indicator function and weight. 
     */
    long get_weightid(const VariableValue *assignments, const CompactFactor& fs, long vid, long proposal);

    /**
     * Given a variable, updates the weights associated with the factors that 
     * connect to the variable. 
     * Used in learning phase, after sampling one variable, 
     * update corresponding weights (stochastic gradient descent).
     */
    void update_weight(const Variable & variable);

    /**
     * Returns potential of the given factor
     *
     * does_change_evid = true, use the free assignment. Otherwise, use the
     * evid assignement. 
     */
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

    /**
     * Returns log-linear weighted potential of the all factors for the given 
     * variable using the propsal value.
     *
     * does_change_evid = true, use the free assignment. Otherwise, use the
     * evid assignement. 
     */
    template<bool does_change_evid>
    inline double potential(const Variable & variable, const double & proposal){
      // potential
      double pot = 0.0;  
      double tmp;
      // weight id
      long wid = 0;
      // pointer to the first factor the given variable connects to
      // the factors that the given variable connects to are stored in a continuous
      // region of the array
      CompactFactor * const fs = &factors_dups[variable.n_start_i_factors];
      // the weights, corresponding to the factor with the same index
      const int * const ws = &factors_dups_weightids[variable.n_start_i_factors];   
      
      // boolean type
      if (variable.domain_type == DTYPE_BOOLEAN) {   
        // for all factors that the variable connects to, calculate the 
        // weighted potential
        for(long i=0;i<variable.n_factors;i++){
          if(does_change_evid == true){
            tmp = fs[i].potential(
                vifs, infrs->assignments_free, variable.id, proposal);
          }else{
            tmp = fs[i].potential(
                vifs, infrs->assignments_evid, variable.id, proposal);
          }
          pot += infrs->weight_values[ws[i]] * tmp;
        }
      } else if (variable.domain_type == DTYPE_MULTINOMIAL) { // multinomial
        for (long i = 0; i < variable.n_factors; i++) {
          if(does_change_evid == true) {
            tmp = fs[i].potential(vifs, infrs->assignments_free, variable.id, proposal);
            // get weight id associated with this factor and variable assignment
            wid = get_weightid(infrs->assignments_free, fs[i], variable.id, proposal);
          } else {
            tmp = fs[i].potential(vifs, infrs->assignments_evid, variable.id, proposal);
            wid = get_weightid(infrs->assignments_evid, fs[i], variable.id, proposal);
          }
          pot += infrs->weight_values[wid] * tmp;
        }
      } // end if for variable type
      return pot;
    }

    /**
     * Loads the factor graph using arguments specified from command line
     */
    void load(const CmdParser & cmd);

    /**
     * Sorts the variables, factors, and weights in ascending id order.
     * This is important as later these components are stored in array, and
     * sorting them will faciliate accessing them by id.
     */
    void sort_by_id();

    /**
     * Construct the edge-based store of factor graph in factors_dups, etc.
     * Refer to the class member comments for more detail.
     */
    void organize_graph_by_edge();

    /**
     * Checks whether the edge-based store is correct
     */
    void safety_check();

    /**
     * Returns wether the factor graph is usable.
     * A factor graph is usable when gone through safety_check and sort_by_id()
     */
    bool is_usable();
  };

  /**
   * Updates the free variable assignment for the given variable using new_value
   */
  template<>
  inline void FactorGraph::update<true>(Variable & variable, const double & new_value){
    infrs->assignments_free[variable.id] = new_value;
  }

  /**
   * Updates the evid assignments for the given variable useing new_value
   */
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




