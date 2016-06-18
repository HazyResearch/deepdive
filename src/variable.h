#ifndef DIMMWITTED_VARIABLE_H_
#define DIMMWITTED_VARIABLE_H_

#include "common.h"

#include <memory>
#include <vector>
#include <unordered_map>

namespace dd {

class Variable;
class RawVariable;

/**
 * A variable in the compiled factor graph.
 */
class Variable {
 public:
  variable_id_t id;  // variable id
  variable_domain_type_t
      domain_type;      // variable domain type, can be DTYPE_BOOLEAN or
                        // DTYPE_CATEGORICAL
  bool is_evid;         // whether the variable is evidence
  bool is_observation;  // observed variable (fixed)
  size_t cardinality;   // cardinality

  variable_value_t assignment_evid;  // assignment, while keeping evidence
                                     // variables unchanged
  variable_value_t assignment_free;  // assignment, free to change any variable

  size_t n_factors;          // number of factors the variable connects to
  size_t n_start_i_factors;  // id of the first factor

  /*
   * The values of categorical variables are stored in an array that looks
   * roughly like this:
   *
   *   [v11 v12 ... v1m v21 ... v2n ...]
   *
   * where v1, v2 are two categorical variables, with domain 1-m, and 1-n
   * respectively.
   */

  // n_start_i_tally is the start position for the variable values in the array
  size_t n_start_i_tally;

  // map from value to index in the domain vector
  std::unique_ptr<std::unordered_map<variable_value_t, size_t>> domain_map;

  /**
   * The inverse of domain_map, constructed on demand by get_domain_value_at
   * (to save RAM) currently used only by bin2text
   */
  std::unique_ptr<std::vector<variable_value_t>> domain_list;

  static constexpr variable_id_t INVALID_ID = (variable_id_t)-1;
  static constexpr variable_value_t INVALID_VALUE = (variable_value_t)-1;

  Variable();  // default constructor, necessary for
               // CompactFactorGraph::variables

  Variable(variable_id_t id, variable_domain_type_t domain_type,
           bool is_evidence, size_t cardinality, variable_value_t init_value,
           variable_value_t current_value, size_t n_factors,
           bool is_observation);

  /**
   * Constructs a variable with only the important information from a
   * temporary variable
   */
  Variable(const Variable& variable);
  Variable& operator=(const Variable& variable);

  // get the index of the value
  inline size_t get_domain_index(variable_value_t v) const {
    return domain_map ? domain_map->at(v) : v;
  }

  // inverse of get_domain_index
  inline variable_value_t get_domain_value_at(size_t idx) {
    if (!domain_list && domain_map) {
      domain_list.reset(new std::vector<variable_value_t>(domain_map->size()));
      for (const auto& item : *domain_map) {
        domain_list->at(item.second) = item.first;
      }
    }
    return domain_list ? domain_list->at(idx) : idx;
  }
};

/**
 * A variable in the raw factor graph.
 *
 * In addition to keeping track of all information a variable does, it also
 * tracks of the temporary vector of factor IDs for the purposes of
 * compilation.
 */
class RawVariable : public Variable {
 public:
  std::vector<factor_id_t>
      tmp_factor_ids;  // factor ids the variable connects to

  RawVariable();  // default constructor, necessary for FactorGraph::variables

  RawVariable(variable_id_t id, variable_domain_type_t domain_type,
              bool is_evidence, size_t cardinality, variable_value_t init_value,
              variable_value_t current_value, size_t n_factors,
              bool is_observation);

  inline void add_factor_id(factor_id_t factor_id) {
    tmp_factor_ids.push_back(factor_id);
  }
};

/**
 * Encapsulates a variable inside a factor
 */
class VariableInFactor {
 public:
  variable_id_t vid;  // variable id
  size_t n_position;  // position of the variable inside factor
  bool is_positive;   // whether the variable is positive or negated
  // the variable's predicate value. A variable is "satisfied" if its value
  // equals equal_to
  variable_value_t equal_to;

  /**
   * Returns whether the variable's predicate is satisfied using the given value
   */
  bool satisfiedUsing(variable_value_t value) const;

  VariableInFactor();

  VariableInFactor(variable_id_t vid, size_t n_position, bool is_positive,
                   variable_value_t equal_to);
};

}  // namespace dd

#endif  // DIMMWITTED_VARIABLE_H_
