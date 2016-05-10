#include "dstruct/factor_graph/variable.h"

namespace dd {

RawVariable::RawVariable() {}

RawVariable::RawVariable(const long _id, const int _domain_type,
                         const bool _is_evid, const int _cardinality,
                         const VariableValue _init_value,
                         const VariableValue _current_value,
                         const int _n_factors, bool _is_observation) {
  this->id = _id;
  this->domain_type = _domain_type;
  this->is_evid = _is_evid;
  this->is_observation = _is_observation;
  this->cardinality = _cardinality;
  this->assignment_evid = _init_value;
  this->assignment_free = _current_value;
  this->n_factors = _n_factors;

  this->domain_map = NULL;
  this->isactive = false;
  this->component_id = -1;
}

Variable::Variable() {}

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
      isactive(rv.isactive),
      component_id(rv.component_id) {}

bool VariableInFactor::satisfiedUsing(int value) const {
  return is_positive ? equal_to == value : !(equal_to == value);
}

VariableInFactor::VariableInFactor() {}

VariableInFactor::VariableInFactor(const long &_vid, const int &_n_position,
                                   const bool &_is_positive,
                                   const VariableValue &_equal_to) {
  this->vid = _vid;
  this->n_position = _n_position;
  this->is_positive = _is_positive;
  this->equal_to = _equal_to;
}
}
