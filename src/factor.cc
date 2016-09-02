#include "factor.h"

namespace dd {

Factor::Factor()
    : Factor(INVALID_ID, DEFAULT_FEATURE_VALUE, Weight::INVALID_ID,
             FUNC_UNDEFINED, 0) {}

Factor::Factor(size_t id, double feature_value, size_t weight_id,
               FACTOR_FUNCTION_TYPE func_id, size_t num_vars)
    : id(id),
      feature_value(feature_value),
      weight_id(weight_id),
      func_id(func_id),
      num_vars(num_vars),
      vif_base(INVALID_ID) {}

Factor::Factor(const Factor &other) { *this = other; }

Factor &Factor::operator=(const Factor &other) {
  id = other.id;
  feature_value = other.feature_value;
  weight_id = other.weight_id;
  func_id = other.func_id;
  num_vars = other.num_vars;
  vif_base = other.vif_base;

  return *this;
}

}  // namespace dd
