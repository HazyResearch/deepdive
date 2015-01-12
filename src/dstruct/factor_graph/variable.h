
#include <vector>

#ifndef _VARIABLE_H_
#define _VARIABLE_H_

namespace dd{

  typedef int VariableValue;
  typedef long VariableIndex;
  typedef long FactorIndex;

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

    Variable();

    Variable(const long & _id, const int & _domain_type, 
             const bool & _is_evid, const VariableValue & _lower_bound,
             const VariableValue & _upper_bound, const VariableValue & _init_value, 
             const VariableValue & _current_value, const int & _n_factors);
  };

  class VariableInFactor {
  public:
    long vid;
    int n_position;
    bool is_positive;
    VariableValue equal_to; 

    int dimension;

    bool satisfiedUsing(int value) const;

    VariableInFactor();


    VariableInFactor(int dummy,
                      const int & _dimension, 
                      const long & _vid, const int & _n_position, 
                     const bool & _is_positive);


    VariableInFactor(const long & _vid, const int & _n_position, 
                     const bool & _is_positive);

    VariableInFactor(const long & _vid, const int & _n_position, 
                     const bool & _is_positive, const VariableValue & _equal_to);
  };
}

#endif