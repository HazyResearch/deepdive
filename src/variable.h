#ifndef DIMMWITTED_VARIABLE_H_
#define DIMMWITTED_VARIABLE_H_

#include <vector>
#include <unordered_map>

namespace dd {

#define DTYPE_BOOLEAN 0x00
#define DTYPE_REAL 0x01
#define DTYPE_MULTINOMIAL 0x04

typedef size_t VariableValue;
typedef size_t VariableIndex;

class Variable;
class RawVariable;

/**
 * A variable in the compiled factor graph.
 */
class Variable {
 public:
  long id;              // variable id
  int domain_type;      // variable domain type, can be DTYPE_BOOLEAN or
                        // DTYPE_MULTINOMIAL
  bool is_evid;         // whether the variable is evidence
  bool is_observation;  // observed variable (fixed)
  int cardinality;      // cardinality

  VariableValue assignment_evid;  // assignment, while keeping evidence
                                  // variables unchanged
  VariableValue assignment_free;  // assignment, free to change any variable

  int n_factors;           // number of factors the variable connects to
  long n_start_i_factors;  // id of the first factor

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

  // map from value to index in the domain vector
  std::unordered_map<VariableValue, int>* domain_map;

  // inverse of domain_map, constructed on demand by get_domain_value_at (to
  // save RAM)
  // currently used only by bin2text
  // TODO: try to remove this pointer altogether
  std::vector<VariableValue>* domain_list;

  Variable();

  /**
   * Constructs a variable with only the important information from a
   * temporary variable
   */
  Variable(RawVariable& raw_variable);

  // get the index of the value
  inline int get_domain_index(int v) const {
    return domain_map ? domain_map->at(v) : v;
  }

  // inverse of get_domain_index
  inline int get_domain_value_at(int idx) {
    if (!domain_list && domain_map) {
      domain_list = new std::vector<VariableValue>(domain_map->size());
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
  // This list is used only during loading time. Allocate and destroy to save
  // space.
  // Life starts: binary_parser.read_factors (via add_factor_id)
  // Life ends: FactorGraph::organize_graph_by_edge (via clear_tmp_factor_ids)
  std::vector<long>* tmp_factor_ids;  // factor ids the variable connects to

  RawVariable();

  RawVariable(const long _id, const int _domain_type, const bool _is_evid,
              const int _cardinality, const VariableValue _init_value,
              const VariableValue _current_value, const int _n_factors,
              bool _is_observation);

  inline void add_factor_id(long factor_id) {
    if (!tmp_factor_ids) {
      tmp_factor_ids = new std::vector<long>();
    }
    tmp_factor_ids->push_back(factor_id);
  }

  inline void clear_tmp_factor_ids() {
    if (tmp_factor_ids) {
      delete tmp_factor_ids;
      tmp_factor_ids = NULL;
    }
  }
};

/**
 * Encapsulates a variable inside a factor
 */
class VariableInFactor {
 public:
  long vid;          // variable id
  int n_position;    // position of the variable inside factor
  bool is_positive;  // whether the variable is positive or negated
  // the variable's predicate value. A variable is "satisfied" if its value
  // equals equal_to
  VariableValue equal_to;

  /**
   * Returns whether the variable's predicate is satisfied using the given value
   */
  bool satisfiedUsing(int value) const;

  VariableInFactor();

  VariableInFactor(const long& _vid, const int& _n_position,
                   const bool& _is_positive, const VariableValue& _equal_to);
};

}  // namespace dd

#endif  // DIMMWITTED_VARIABLE_H_
