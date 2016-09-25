#ifndef DIMMWITTED_VARIABLE_H_
#define DIMMWITTED_VARIABLE_H_

#include "common.h"

#include <memory>
#include <unordered_map>
#include <vector>

namespace dd {

// Used to store back refs to factors in a Variable at loading time.
// Deallocated in FactorGraph.construct_index.
class TempValueFactor {
 public:
  size_t value_dense;
  size_t factor_id;

  TempValueFactor(size_t value_dense_in, size_t factor_id_in)
      : value_dense(value_dense_in), factor_id(factor_id_in){};

  // Lexical sort order for indexing
  bool operator<(const TempValueFactor& other) const {
    return value_dense < other.value_dense ||
           (value_dense == other.value_dense && factor_id < other.factor_id);
  }
};

// Used as value in Variable.domain_map
typedef struct {
  size_t index;
  double truthiness;
} TempVarValue;

/**
 * A variable in the factor graph.
 */
class Variable {
 public:
  // While a categorical var has `cardinality` VariableToFactor objects,
  // a boolean var only has one VariableToFactor object in FactorGraph.values
  // and one entry in InferenceResult.sample_tallies.
  // BOOLEAN_DENSE_VALUE is used to index into those arrays.
  static constexpr size_t BOOLEAN_DENSE_VALUE = 0;
  static constexpr size_t INVALID_ID = (size_t)-1;
  static constexpr size_t INVALID_VALUE = (size_t)-1;

  size_t id;                // variable id
  DOMAIN_TYPE domain_type;  // can be DTYPE_BOOLEAN or DTYPE_CATEGORICAL
  bool is_evid;             // whether the variable is evidence
  size_t cardinality;       // cardinality; 2 for boolean

  // After loading (Factorgraph.construct_index), all assignment values
  // and all proposal values take the "dense" form:
  // - Boolean: {0, 1}
  // - Categorical: [0..cardinality)
  size_t assignment_dense;

  // Sum of all values' truthiness
  double total_truthiness;

  // We concatenate the list of "possible values" for each variable to form
  // FactorGraph.values (also InferenceResults.sample_tallies).
  // - For boolean, there is just one value, namely BOOLEAN_DENSE_VALUE
  // - For categorical, there are `cardinality` values
  //   - if domain is specified, it's the sorted list of domain values
  //   - otherwise, it's [0..cardinality)
  //
  // var_val_base is the start position into those lists.
  size_t var_val_base;

  // Map from value to index in the domain vector.
  // Populated at load time (FactorGraph.load_domains).
  // Deallocated in FactorGraph.construct_index.
  std::unique_ptr<std::unordered_map<size_t, TempVarValue>> domain_map;

  // Backrefs to factors (including factor's "equal_to" value).
  // Populated at load time (FactorGraph.load_factors).
  // Deallocated in FactorGraph.construct_index.
  std::unique_ptr<std::vector<TempValueFactor>>
      adjacent_factors;  // factor ids the variable connects to

  Variable();  // default constructor, necessary for
               // FactorGraph::variables

  Variable(size_t id, DOMAIN_TYPE domain_type, bool is_evidence,
           size_t cardinality, size_t init_value);

  /**
   * Constructs a variable with only the important information from a
   * temporary variable
   */
  Variable(const Variable& variable);
  Variable& operator=(const Variable& variable);

  inline void add_value_factor(size_t value_dense, size_t factor_id) {
    if (!adjacent_factors) {
      adjacent_factors.reset(new std::vector<TempValueFactor>());
    }
    adjacent_factors->push_back(TempValueFactor(value_dense, factor_id));
  }

  inline bool is_boolean() const { return domain_type == DTYPE_BOOLEAN; }

  inline bool has_truthiness() const {
    return !is_linear_zero(total_truthiness);
  }

  inline size_t internal_cardinality() const {
    return is_boolean() ? 1 : cardinality;
  }

  inline size_t var_value_offset(size_t value_dense) const {
    return is_boolean() ? BOOLEAN_DENSE_VALUE : value_dense;
  }

  // get the index of the value
  inline size_t get_domain_index(size_t v) const {
    return domain_map ? domain_map->at(v).index : v;
  }
};

class VariableToFactor {
 public:
  // original (sparse) value as assigned by DD grounding
  size_t value;
  // soft evidence weight. range: [0, 1].
  // NOTE: for categorical only
  double truthiness;
  // base offset into the factor index (FactorGraph.factor_index)
  size_t factor_index_base;
  // number of entries in the factor index
  size_t factor_index_length;

  VariableToFactor() : VariableToFactor(Variable::INVALID_ID, 0, -1, 0) {}

  VariableToFactor(size_t v, double t, size_t base, size_t len)
      : value(v),
        truthiness(t),
        factor_index_base(base),
        factor_index_length(len) {}
};

/**
 * Encapsulates a variable inside a factor
 */
class FactorToVariable {
 public:
  size_t vid;  // variable id
  // dense-form value binding to the var in the factor
  // - Boolean: {0, 1} depending on wheter atom is negated in rule head
  // - Categorical: [0..cardinality) depending on the var relation tuple
  size_t dense_equal_to;

  /**
   * Returns whether the variable's predicate is satisfied using the given
   * value.
   * Applies to both boolean and categorical.
   */
  inline bool satisfiedUsing(size_t dense_value) const {
    return dense_equal_to == dense_value;
  }

  FactorToVariable();

  FactorToVariable(size_t vid, size_t dense_equal_to);
};

}  // namespace dd

#endif  // DIMMWITTED_VARIABLE_H_
