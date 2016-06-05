#include "dstruct/factor_graph/variable.h"

namespace dd {

Variable::Variable() {
  this->domain_map = NULL;
  this->domain_list = NULL;
  this->tmp_factor_ids = NULL;
}

Variable::Variable(const long &_id, const int &_domain_type,
                   const bool &_is_evid, const int &cardinality,
                   const VariableValue &_init_value,
                   const VariableValue &_current_value, const int &_n_factors,
                   bool is_observation) {
  this->id = _id;
  this->domain_type = _domain_type;
  this->is_evid = _is_evid;
  this->is_observation = is_observation;
  this->cardinality = cardinality;
  this->assignment_evid = _init_value;
  this->assignment_free = _current_value;
  this->n_factors = _n_factors;
  this->domain_map = NULL;
  this->domain_list = NULL;
  this->tmp_factor_ids = NULL;
  this->isactive = false;
  this->component_id = -1;
}

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