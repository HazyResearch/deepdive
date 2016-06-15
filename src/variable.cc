#include "variable.h"

namespace dd {

Variable::Variable()
    : id(INVALID_ID),
      domain_type(DTYPE_BOOLEAN),
      is_evid(false),
      is_observation(false),
      cardinality(2),
      assignment_evid(0),
      assignment_free(0),
      n_factors(0),
      domain_map(NULL),
      domain_list(NULL) {}

Variable::Variable(VariableIndex id, variable_domain_type_t domain_type,
                   bool is_evidence, VariableValue cardinality,
                   VariableValue init_value, VariableValue current_value,
                   size_t n_factors, bool is_observation)
    : id(id),
      domain_type(domain_type),
      is_evid(is_evidence),
      is_observation(is_observation),
      cardinality(cardinality),
      assignment_evid(init_value),
      assignment_free(current_value),
      n_factors(n_factors),
      domain_map(NULL),
      domain_list(NULL) {}

Variable::Variable(RawVariable &rv)
    : id(rv.id),
      domain_type(rv.domain_type),
      is_evid(rv.is_evid),
      is_observation(rv.is_observation),
      cardinality(rv.cardinality),
      assignment_evid(rv.assignment_evid),
      assignment_free(rv.assignment_free),
      n_factors(rv.n_factors),
      n_start_i_factors(rv.n_start_i_factors),
      n_start_i_tally(rv.n_start_i_tally),
      domain_map(rv.domain_map),
      domain_list(NULL) {}

RawVariable::RawVariable() : Variable() {}

RawVariable::RawVariable(VariableIndex id, variable_domain_type_t domain_type,
                         bool is_evidence, VariableValue cardinality,
                         VariableValue init_value, VariableValue current_value,
                         size_t n_factors, bool is_observation)
    : Variable(id, domain_type, is_evidence, cardinality, init_value,
               current_value, n_factors, is_observation),
      tmp_factor_ids(NULL) {}

bool VariableInFactor::satisfiedUsing(VariableValue value) const {
  return is_positive ? equal_to == value : !(equal_to == value);
}

VariableInFactor::VariableInFactor()
    : vid(Variable::INVALID_ID),
      n_position(-1),
      is_positive(true),
      equal_to(Variable::INVALID_VALUE) {}

VariableInFactor::VariableInFactor(VariableIndex vid, size_t n_position,
                                   bool is_positive, VariableValue equal_to)
    : vid(vid),
      n_position(n_position),
      is_positive(is_positive),
      equal_to(equal_to) {}

}  // namespace dd
