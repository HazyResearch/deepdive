
#include <iostream>
#include <vector>
#include <assert.h>
#include "dstruct/factor_graph/variable.h"
#include "dstruct/factor_graph/weight.h"

#ifndef _FACTOR_H_
#define _FACTOR_H_

namespace dd{

  enum FACTOR_FUCNTION_TYPE{
    FUNC_IMPLY_neg1_1 = 0,
    FUNC_OR         = 1,
    FUNC_AND        = 2,
    FUNC_EQUAL      = 3,
    FUNC_ISTRUE     = 4,
    FUNC_MULTINOMIAL= 5,
    FUNC_SQLSELECT  = 10,
    FUNC_ContLR     = 20
  };

  template<bool does_change_evid>
  inline const VariableValue & get_vassign(const Variable & v);

  template<>
  inline const VariableValue & get_vassign<true>(const Variable & v){
    return v.assignment_free;
  }

  template<>
  inline const VariableValue & get_vassign<false>(const Variable & v){
    return v.assignment_evid;
  }

  class CompactFactor{
  public:
    FactorIndex id;
    int func_id; 
    int n_variables;
    long n_start_i_vif;

    CompactFactor();

    CompactFactor(const FactorIndex & _id);
    inline double _potential_continuousLR(const VariableInFactor * const vifs,
                                   const VariableValue * const var_values, 
                                   const VariableIndex &, const VariableValue &) const;


    inline double _potential_or(const VariableInFactor * const vifs,
                                   const VariableValue * const var_values, 
                                   const VariableIndex &, const VariableValue &) const;


    inline double _potential_and(const VariableInFactor * const vifs,
                                   const VariableValue * const var_values, 
                                   const VariableIndex &, const VariableValue &) const;


    inline double _potential_equal(const VariableInFactor * const vifs,
                                   const VariableValue * const var_values, 
                                   const VariableIndex &, const VariableValue &) const;


    inline double _potential_imply(const VariableInFactor * const vifs,
                                   const VariableValue * const var_values, 
                                   const VariableIndex &, const VariableValue &) const;
    
    inline double _potential_multinomial(const VariableInFactor * const vifs,
                                   const VariableValue * const var_values, 
                                   const VariableIndex &, const VariableValue &) const;

    // This function is defined in the head to make sure
    // it gets inlined
    inline double potential(const VariableInFactor * const vifs,
      const VariableValue * const var_values,
      const VariableIndex & vid, const VariableValue & proposal) const{
      switch (func_id) {
        case FUNC_IMPLY_neg1_1   : return _potential_imply(vifs, var_values, vid, proposal);
        case FUNC_ISTRUE      : return _potential_and(vifs, var_values, vid, proposal);
        case FUNC_OR          : return _potential_or(vifs, var_values, vid, proposal);
        case FUNC_AND         : return _potential_and(vifs, var_values, vid, proposal);   
        case FUNC_EQUAL       : return _potential_equal(vifs, var_values, vid, proposal);  
        case FUNC_MULTINOMIAL : return _potential_multinomial(vifs, var_values, vid, proposal);
        case FUNC_SQLSELECT   : std::cout << "SQLSELECT Not supported yet!" << std::endl; assert(false); return 0;  
        case FUNC_ContLR   : std::cout << "ContinuousLR Not supported yet!" << std::endl; assert(false); return 0;  
        std::cout << "Unsupported Factor Function ID= " << func_id << std::endl;
        assert(false);
      }

      return 0.0;

    }

  };

  class Factor {
  public:
    FactorIndex id;
    WeightIndex weight_id;
    int func_id;
    int n_variables;

    long n_start_i_vif;

    std::vector<VariableInFactor> tmp_variables;

    Factor();

    Factor(const FactorIndex & _id,
           const WeightIndex & _weight_id,
           const int & _func_id,
           const int & _n_variables);

  };

}


inline double dd::CompactFactor::_potential_equal(
  const VariableInFactor * const vifs,
  const VariableValue * const var_values, 
  const VariableIndex & vid, const VariableValue & proposal) const{

  const VariableInFactor & vif = vifs[n_start_i_vif];
  bool firstsat;
  if(vif.vid == vid){
    firstsat = (vif.vid == vid) ? vif.satisfiedUsing(proposal) : vif.satisfiedUsing(var_values[vif.vid]) ;
  }else{
    firstsat = (vif.vid == vid) ? vif.satisfiedUsing(proposal) : vif.satisfiedUsing(var_values[vif.vid]) ;
  }

  for(long i_vif=n_start_i_vif; (i_vif<n_start_i_vif+n_variables);i_vif++){
    const VariableInFactor & vif = vifs[i_vif];
    bool satisfied = (vif.vid == vid) ? vif.satisfiedUsing(proposal) : vif.satisfiedUsing(var_values[vif.vid]) ;
    if(satisfied != firstsat) return 0.0;
  }
  return 1.0;
}

inline double dd::CompactFactor::_potential_and(
  const VariableInFactor * const vifs,
  const VariableValue * const var_values, 
  const VariableIndex & vid, 
  const VariableValue & proposal) const{

  for(long i_vif=n_start_i_vif; (i_vif<n_start_i_vif+n_variables);i_vif++){
    const VariableInFactor & vif = vifs[i_vif];
    bool satisfied = (vif.vid == vid) ? vif.satisfiedUsing(proposal) : vif.satisfiedUsing(var_values[vif.vid]) ;
    if(!satisfied) return 0.0;
  }
  return 1.0;

}


inline double dd::CompactFactor::_potential_or(
  const VariableInFactor * const vifs,
  const VariableValue * const var_values, 
  const VariableIndex & vid, 
  const VariableValue & proposal) const{

  for(long i_vif=n_start_i_vif; (i_vif<n_start_i_vif+n_variables);i_vif++){
    const VariableInFactor & vif = vifs[i_vif];
    bool satisfied = (vif.vid == vid) ? vif.satisfiedUsing(proposal) : vif.satisfiedUsing(var_values[vif.vid]) ;
    if(satisfied) return 1.0;
  }
  return 0.0;

}


inline double dd::CompactFactor::_potential_imply(
  const VariableInFactor * const vifs,
  const VariableValue * const var_values, 
  const VariableIndex & vid, 
  const VariableValue & proposal) const{

  bool bBody = true; // 
  for(long i_vif=n_start_i_vif; 
    (i_vif<n_start_i_vif+n_variables - 1) // shortcut && bBody
      ;i_vif++){
    const VariableInFactor & vif = vifs[i_vif];
    // if it is the proposal variable, we use the truth value of the proposal
    // This is a crazy abstraction here, why doesn't vif able to say that it is true or false
    //bool bTrue = (vif.vid == vif) ? vif.value.equal(proposal) : vif.value.equal(var_values[vif.vid]) ;
    bBody &= (vif.vid == vid) ? vif.satisfiedUsing(proposal) : vif.satisfiedUsing(var_values[vif.vid]) ;
  }
  if(bBody) {
    const VariableInFactor & vif = vifs[n_variables - 1]; // encoding of the head, should be more structured.
    // const VariableInFactor & vif = head(vifs); // encoding of the head, should be more structured.      
    bool bHead = (vif.vid == vid) ? vif.satisfiedUsing(proposal) : vif.satisfiedUsing(var_values[vif.vid]) ;
    return bHead ? 1.0 : -1.0;
  } else {
    return 0.0;
  }

}

// potential for multinomial variable
inline double dd::CompactFactor::_potential_multinomial(const VariableInFactor * vifs, 
  const VariableValue * var_values, const VariableIndex & vid, const VariableValue & proposal) const {

  return 1.0;
}


#endif




