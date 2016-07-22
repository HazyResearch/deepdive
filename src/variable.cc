#include "variable.h"

namespace dd {

Variable::Variable()
    : Variable(INVALID_ID, DTYPE_BOOLEAN, false, 2, 0, 0, false) {}

Variable::Variable(variable_id_t id, variable_domain_type_t domain_type,
                   bool is_evidence, num_variable_values_t cardinality,
                   variable_value_t init_value, variable_value_t current_value,
                   bool is_observation)
    : id(id),
      domain_type(domain_type),
      is_evid(is_evidence),
      is_observation(is_observation),
      cardinality(cardinality),
      assignment_evid(init_value),
      assignment_free(current_value),
      n_factors(0),
      n_start_i_factors(-1),
      n_start_i_tally(-1) {}

Variable::Variable(const Variable &other) { *this = other; }

Variable &Variable::operator=(const Variable &other) {
  id = other.id;
  domain_type = other.domain_type;
  is_evid = other.is_evid;
  is_observation = other.is_observation;
  cardinality = other.cardinality;
  assignment_evid = other.assignment_evid;
  assignment_free = other.assignment_free;
  n_factors = other.n_factors;
  n_start_i_factors = other.n_start_i_factors;
  n_start_i_tally = other.n_start_i_tally;
  domain_map.reset(
      other.domain_map
          ? new std::unordered_map<variable_value_t, variable_value_index_t>(
                *other.domain_map)
          : nullptr);
  domain_list.reset(other.domain_list
                        ? new std::vector<variable_value_t>(*other.domain_list)
                        : nullptr);
  return *this;
}

RawVariable::RawVariable() : Variable() {}

RawVariable::RawVariable(variable_id_t id, variable_domain_type_t domain_type,
                         bool is_evidence, num_variable_values_t cardinality,
                         variable_value_t init_value,
                         variable_value_t current_value, bool is_observation)
    : Variable(id, domain_type, is_evidence, cardinality, init_value,
               current_value, is_observation) {}

bool VariableInFactor::satisfiedUsing(variable_value_t value) const {
  return equal_to == value;
}

VariableInFactor::VariableInFactor()
    : VariableInFactor(Variable::INVALID_ID, -1, Variable::INVALID_VALUE) {}

VariableInFactor::VariableInFactor(variable_id_t vid, factor_arity_t n_position,
                                   variable_value_t equal_to)
    : vid(vid), n_position(n_position), equal_to(equal_to) {}

}  // namespace dd
