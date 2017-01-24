#include "variable.h"

namespace dd {

// To placate linker error "undefined reference"
// http://stackoverflow.com/questions/8016780/undefined-reference-to-static-constexpr-char
constexpr size_t Variable::INVALID_ID;
constexpr size_t Variable::INVALID_VALUE;

Variable::Variable() : Variable(INVALID_ID, DTYPE_BOOLEAN, false, 2, 0) {}

Variable::Variable(size_t id, DOMAIN_TYPE domain_type, bool is_evidence,
                   size_t cardinality, size_t init_value)
    : id(id),
      domain_type(domain_type),
      is_evid(is_evidence),
      cardinality(cardinality),
      assignment_dense(init_value),
      total_truthiness(0),
      var_val_base(-1) {}

Variable::Variable(const Variable &other) { *this = other; }

Variable &Variable::operator=(const Variable &other) {
  id = other.id;
  domain_type = other.domain_type;
  is_evid = other.is_evid;
  cardinality = other.cardinality;
  assignment_dense = other.assignment_dense;
  total_truthiness = other.total_truthiness;
  var_val_base = other.var_val_base;
  domain_map.reset(
      other.domain_map
          ? new std::unordered_map<size_t, TempVarValue>(*other.domain_map)
          : nullptr);
  adjacent_factors.reset(
      other.adjacent_factors
          ? new std::vector<TempValueFactor>(*other.adjacent_factors)
          : nullptr);
  return *this;
}

FactorToVariable::FactorToVariable()
    : FactorToVariable(Variable::INVALID_ID, Variable::INVALID_VALUE) {}

FactorToVariable::FactorToVariable(size_t vid, size_t dense_equal_to)
    : vid(vid), dense_equal_to(dense_equal_to) {}

}  // namespace dd
