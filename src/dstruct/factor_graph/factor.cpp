
#include "factor.h"

namespace dd{

    CompactFactor::CompactFactor(){}

    CompactFactor::CompactFactor(const long & _id){
      id = _id;
    }

    Factor::Factor(){

    }

    Factor::Factor(const FactorIndex & _id,
           const WeightIndex & _weight_id,
           const int & _func_id,
           const int & _n_variables){
      this->id = _id;
      this->weight_id = _weight_id;
      this->func_id = _func_id;
      this->n_variables = _n_variables;
    }
 
}




