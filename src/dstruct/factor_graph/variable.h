
#ifndef _VARIABLE_H_
#define _VARIABLE_H_

namespace dd{

  typedef int VariableValue;

  class Variable {
  public:
    long id;
    int domain_type;
    bool is_evid;
    VariableValue lower_bound;
    VariableValue upper_bound;
    
    VariableValue assignment_evid;
    VariableValue assignment_free;

    int n_factors;
    long n_start_i_factors;

    long n_start_i_tally;

    std::vector<long> tmp_factor_ids;

    double agg_mean;
    long n_sample;

    Variable(){

    }

    Variable(const long & _id, const int & _domain_type, 
             const bool & _is_evid, const VariableValue & _lower_bound,
             const VariableValue & _upper_bound, const VariableValue & _init_value, 
             const VariableValue & _current_value, const int & _n_factors){

      this->id = _id;
      this->domain_type = _domain_type;
      this->is_evid = _is_evid;
      this->lower_bound = _lower_bound;
      this->upper_bound = _upper_bound;
      this->assignment_evid = _init_value;
      this->assignment_free = _current_value;

      this->n_factors = _n_factors;

      this->n_sample = 0;
      this->agg_mean = 0.0;
    }
  };

  class VariableInFactor {
  public:
    long vid;
    int n_position;
    bool is_positive;
    VariableValue equal_to; 

    int dimension;

    bool satisfiedUsing(int value) const{
      return is_positive ? equal_to == value : !(equal_to == value); 
    }

    VariableInFactor(){

    }


    VariableInFactor(int dummy,
                      const int & _dimension, 
                      const long & _vid, const int & _n_position, 
                     const bool & _is_positive){
      this->dimension = _dimension;
      this->vid = _vid;
      this->n_position = _n_position;
      this->is_positive = _is_positive;
      this->equal_to = 1.0;
    }


    VariableInFactor(const long & _vid, const int & _n_position, 
                     const bool & _is_positive){
      this->dimension = -1;
      this->vid = _vid;
      this->n_position = _n_position;
      this->is_positive = _is_positive;
      this->equal_to = 1.0;
    }

    VariableInFactor(const long & _vid, const int & _n_position, 
                     const bool & _is_positive, const VariableValue & _equal_to){
      this->dimension = -1;
      this->vid = _vid;
      this->n_position = _n_position;
      this->is_positive = _is_positive;
      this->equal_to = _equal_to;
    }
  };
}

#endif