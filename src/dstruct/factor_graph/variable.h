
#include <vector>
#include <unordered_map>

#ifndef _VARIABLE_H_
#define _VARIABLE_H_

#define DTYPE_BOOLEAN     0x00
#define DTYPE_REAL        0x01
#define DTYPE_MULTINOMIAL 0x04

namespace dd {

  typedef int VariableValue;
  typedef long VariableIndex;
  typedef long FactorIndex;

  class Variable;
  class RawVariable;

  /**
   * A variable in the compiled factor graph.
   */
  class Variable {
  public:
    long id;                        // variable id
    int domain_type;                // variable domain type, can be DTYPE_BOOLEAN or
                                    // DTYPE_MULTINOMIAL
    bool is_evid;                   // whether the variable is evidence
    bool is_observation;            // observed variable (fixed)
    int cardinality;                // cardinality

    VariableValue assignment_evid;  // assignment, while keeping evidence variables unchanged
    VariableValue assignment_free;  // assignment, free to change any variable

    int n_factors;                  // number of factors the variable connects to
    long n_start_i_factors;         // id of the first factor

    /*
     * The values of multinomial variables are stored in an array that looks
     * roughly like this:
     *
     *   [v11 v12 ... v1m v21 ... v2n ...]
     *
     * where v1, v2 are two multinomial variables, with domain 1-m, and 1-n
     * respectively.
     */

    // n_start_i_tally is the start position for the variable values in the array
    long n_start_i_tally;

    /* FIXME: Ugh how to deal with this? */
    std::unordered_map<VariableValue, int> *domain_map; // map from value to index in the domain vector

    bool isactive;

    long long component_id;

    int next_sample;

    Variable();

    /**
     * Constructs a variable with only the important information from a
     * temporary variable
     */
    Variable(RawVariable& raw_variable);

    // get the index of the value
    inline int get_domain_index(int v) const {
      return domain_map ? (*domain_map)[v] : (int)v;
    }
  };

  /**
   * A variable in the raw factor graph.
   *
   * In addition to keeping track of all information a variable does, it also
   * tracks of the temporary vector of factor IDs for the purposes of
   * compilation.
   */
  class RawVariable: public Variable {
  public:
    std::vector<long> tmp_factor_ids; // factor ids the variable connects to

    RawVariable();

    RawVariable(const long _id, const int _domain_type,
             const bool _is_evid, const int _cardinality,
             const VariableValue _init_value,
             const VariableValue _current_value, const int _n_factors,
             bool _is_observation);
  };

  /**
   * Encapsulates a variable inside a factor
   */
  class VariableInFactor {
  public:
    long vid;               // variable id
    int n_position;         // position of the variable inside factor
    bool is_positive;       // whether the variable is positive or negated
    // the variable's predicate value. A variable is "satisfied" if its value
    // equals equal_to
    VariableValue equal_to;

    /**
     * Returns whether the variable's predicate is satisfied using the given value
     */
    bool satisfiedUsing(int value) const;

    VariableInFactor();

    VariableInFactor(const long & _vid, const int & _n_position, 
                     const bool & _is_positive, const VariableValue & _equal_to);
  };
}

#endif
