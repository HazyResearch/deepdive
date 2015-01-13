
#include <vector>

#ifndef _VARIABLE_H_
#define _VARIABLE_H_

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
    int domain_type;                // variable domain type
    bool is_evid;                   // whether the variable is evidence
    VariableValue lower_bound;      // lower bound
    VariableValue upper_bound;      // upper bound
    
    VariableValue assignment_evid;  // assignment, while keeping evidence variables unchanged
    VariableValue assignment_free;  // assignment, free to change any variable

    int n_factors;                  // number of factors the variable connects to
    long n_start_i_factors;         // id of the first factor

    long n_start_i_tally;

    std::vector<long> tmp_factor_ids; // factor ids the variable connects to

    double agg_mean;                // unused
    long n_sample;                  // unused

    Variable();

    /**
     * Constructs a variable with id, domain type, is evidence, lower bound, 
     * upper bound, initial value, current value, number of factors 
     */
    Variable(const long & _id, const int & _domain_type, 
             const bool & _is_evid, const VariableValue & _lower_bound,
             const VariableValue & _upper_bound, const VariableValue & _init_value, 
             const VariableValue & _current_value, const int & _n_factors);
  };

  /**
   * Encapsulates a variable inside a factor
   */
  class VariableInFactor {
  public:
    long vid;               // variable id
    int n_position;         // position of the variable inside factor
    bool is_positive;       // whether the variable is positive or negated
    VariableValue equal_to; // the variabl's predicate value

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