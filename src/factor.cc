#include "factor.h"

namespace dd {

CompactFactor::CompactFactor() {}

CompactFactor::CompactFactor(const long &_id) { id = _id; }

RawFactor::RawFactor() : tmp_variables(NULL) {}

RawFactor::RawFactor(const FactorIndex _id, const WeightIndex _weight_id,
                     const int _func_id, const int _n_variables)
    : Factor() {
  this->id = _id;
  this->weight_id = _weight_id;
  this->func_id = _func_id;
  this->n_variables = _n_variables;

  this->n_start_i_vif = 0;
}

Factor::Factor() : weight_ids(NULL) {}

Factor::Factor(RawFactor &rf)
    : id(rf.id),
      weight_id(rf.weight_id),
      func_id(rf.func_id),
      n_variables(rf.n_variables),
      n_start_i_vif(rf.n_start_i_vif),
      weight_ids(rf.weight_ids) {}

}  // namespace dd
