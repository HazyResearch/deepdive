
#include <vector>

#ifndef _VARIABLE_H_
#define _VARIABLE_H_

#define DTYPE_BOOLEAN     0x00
#define DTYPE_REAL        0x01
#define DTYPE_MULTINOMIAL 0x04

namespace dd{

  typedef int VariableValue;
  typedef long VariableIndex;
  typedef long FactorIndex;

  /**
   * A variable in factor graph
   */
  class Variable {
  public:
    long id;                        // variable id
    int domain_type;                // variable domain type, can be DTYPE_BOOLEAN or 
                                    // DTYPE_MULTINOMIAL
    bool is_evid;                   // whether the variable is evidence
    bool is_observation;            // observed variable (fixed)
    VariableValue lower_bound;      // lower bound
    VariableValue upper_bound;      // upper bound
    
    VariableValue assignment_evid;  // assignment, while keeping evidence variables unchanged
    VariableValue assignment_free;  // assignment, free to change any variable

    int n_factors;                  // number of factors the variable connects to
    long n_start_i_factors;         // id of the first factor

    // the values of multinomial variables are stored in an array like this
    // [v11 v12 ... v1m v21 ... v2n ...] 
    // where v1, v2 are two multinomial variables, with domain 1-m, and 1-n respectively.
    // n_start_i_tally is the start position for the variable values in the array
    long n_start_i_tally;

    std::vector<long> tmp_factor_ids; // factor ids the variable connects to

    bool isactive;

    long long component_id;

    int next_sample;

    Variable();

    /**
     * Constructs a variable with id, domain type, is evidence, lower bound, 
     * upper bound, initial value, current value, number of factors 
     */
    Variable(const long & _id, const int & _domain_type, 
             const bool & _is_evid, const VariableValue & _lower_bound,
             const VariableValue & _upper_bound, const VariableValue & _init_value, 
             const VariableValue & _current_value, const int & _n_factors,
             bool is_observation);
  };

  /**
   * Encapsulates a variable inside a factor
   */
  class VariableInFactor {
  public:
    long vid;               // variable id
    int n_position;         // position of the variable inside factor
    bool is_positive;       // whether the variable is positive or negated
    // the variable's predicate value. A variable is "satisfied" if its value equals equal_to
    VariableValue equal_to; 

    int dimension;

    /**
     * Returns whether the variable's predicate is satisfied using the given value
     */
    bool satisfiedUsing(int value) const;

    VariableInFactor();

    VariableInFactor(int dummy,
                      const int & _dimension, 
                      const long & _vid, const int & _n_position, 
                     const bool & _is_positive);


    VariableInFactor(const long & _vid, const int & _n_position, 
                     const bool & _is_positive);

    VariableInFactor(const long & _vid, const int & _n_position, 
                     const bool & _is_positive, const VariableValue & _equal_to);
  };
}

#endif