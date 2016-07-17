#include "factor.h"

namespace dd {

CompactFactor::CompactFactor() : CompactFactor(Factor::INVALID_ID) {}

CompactFactor::CompactFactor(const factor_id_t id)
    : id(id),
      feature_value(DEFAULT_FEATURE_VALUE),
      func_id(Factor::INVALID_FUNC_ID),
      n_variables(0),
      n_start_i_vif(Factor::INVALID_ID) {}

Factor::Factor()
    : Factor(INVALID_ID, DEFAULT_FEATURE_VALUE, Weight::INVALID_ID,
             INVALID_FUNC_ID, 0) {}

Factor::Factor(factor_id_t id, feature_value_t feature_value,
               weight_id_t weight_id, factor_function_type_t func_id,
               factor_arity_t n_variables)
    : id(id),
      feature_value(feature_value),
      weight_id(weight_id),
      func_id(func_id),
      n_variables(n_variables),
      n_start_i_vif(INVALID_ID),
      factor_params(NULL) {}

Factor::Factor(const Factor &rf)
    : id(rf.id),
      feature_value(rf.feature_value),
      weight_id(rf.weight_id),
      func_id(rf.func_id),
      n_variables(rf.n_variables),
      n_start_i_vif(rf.n_start_i_vif),
      factor_params(rf.factor_params) {}

RawFactor::RawFactor() : Factor() {}

RawFactor::RawFactor(factor_id_t id, feature_value_t feature_value,
                     weight_id_t weight_id, factor_function_type_t func_id,
                     factor_arity_t n_variables)
    : Factor(id, feature_value, weight_id, func_id, n_variables) {}

}  // namespace dd
