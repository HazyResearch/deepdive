
#include <iostream>
#include <vector>
#include <assert.h>
#include "dstruct/factor_graph/variable.h"
#include "dstruct/factor_graph/weight.h"

#ifndef _FACTOR_H_
#define _FACTOR_H_

namespace dd{

  // enumeration for factor function types
  enum FACTOR_FUCNTION_TYPE{
    FUNC_IMPLY_MLN = 0,
    FUNC_IMPLY_neg1_1 = 11,
    FUNC_OR         = 1,
    FUNC_AND        = 2,
    FUNC_EQUAL      = 3,
    FUNC_ISTRUE     = 4,
    FUNC_MULTINOMIAL= 5,
    FUNC_ONEISTRUE  = 6,
    FUNC_SQLSELECT  = 10,
    FUNC_ContLR     = 20
  };

  /**
   * Encapsulates a factor function in the factor graph
   */
  class CompactFactor{
  public:
    FactorIndex id;       // factor id
    int func_id;          // function type id
    int n_variables;      // number of variables in the factor
    long n_start_i_vif;   // the id of the first variable.  the variables of a factor
                          // have sequential ids starting from n_start_i_vif to n_start_i_vif+num_variables-1

    /**
     * Default constructor
     */
    CompactFactor();

    /**
     * Constructs a CompactFactor with given factor id
     */
    CompactFactor(const FactorIndex & _id);

    /**
     * Returns the potential of continousLR factor function. See factor.hxx for more detail
     */
    inline double _potential_continuousLR(const VariableInFactor * const vifs,
                                   const VariableValue * const var_values, 
                                   const VariableIndex &, const VariableValue &) const;

    /**
     * Returns the potential of or factor function. See factor.hxx for more detail
     */
    inline double _potential_or(const VariableInFactor * const vifs,
                                   const VariableValue * const var_values, 
                                   const VariableIndex &, const VariableValue &) const;

    /**
     * Returns the potential of and factor function. See factor.hxx for more detail
     */
    inline double _potential_and(const VariableInFactor * const vifs,
                                   const VariableValue * const var_values, 
                                   const VariableIndex &, const VariableValue &) const;

    /**
     * Returns the potential of equal factor function. See factor.hxx for more detail
     */
    inline double _potential_equal(const VariableInFactor * const vifs,
                                   const VariableValue * const var_values, 
                                   const VariableIndex &, const VariableValue &) const;

    /**
     * Returns the potential of MLN style imply factor function. See factor.hxx for more detail
     */
    inline double _potential_imply_mln(const VariableInFactor * const vifs,
                                   const VariableValue * const var_values, 
                                   const VariableIndex &, const VariableValue &) const;

    /**
     * Returns the potential of imply factor function. See factor.hxx for more detail
     */
    inline double _potential_imply(const VariableInFactor * const vifs,
                                   const VariableValue * const var_values, 
                                   const VariableIndex &, const VariableValue &) const;
    
    /**
     * Returns the potential of multinomial factor function. See factor.hxx for more detail
     */
    inline double _potential_multinomial(const VariableInFactor * const vifs,
                                   const VariableValue * const var_values, 
                                   const VariableIndex &, const VariableValue &) const;

    /**
     * Returns the potential of oneIsTrue factor function. See factor.hxx for more detail
     */
    inline double _potential_oneistrue(const VariableInFactor * const vifs,
                                       const VariableValue * const var_values,
                                       const VariableIndex &, const VariableValue &) const;

    /** 
     * Returns potential of the factor. 
     * (potential is the value of the factor) 
     * The potential is calculated using the proposal value for variable with id vid, and 
     * var_values for other variables in the factor
     *
     * vifs pointer to variables in the factor graph
     * var_values pointer to variable values (array)
     * vid variable id to be calculated with proposal
     * proposal the proposed value
     *
     * This function is defined in the head to make sure
     * it gets inlined
     */
    inline double potential(const VariableInFactor * const vifs,
      const VariableValue * const var_values,
      const VariableIndex & vid, const VariableValue & proposal) const{
      switch (func_id) {
        case FUNC_IMPLY_MLN   :return _potential_imply_mln(vifs, var_values, vid, proposal);
        case FUNC_IMPLY_neg1_1: return _potential_imply(vifs, var_values, vid, proposal);
        case FUNC_ISTRUE      : return _potential_and(vifs, var_values, vid, proposal);
        case FUNC_OR          : return _potential_or(vifs, var_values, vid, proposal);
        case FUNC_AND         : return _potential_and(vifs, var_values, vid, proposal);   
        case FUNC_EQUAL       : return _potential_equal(vifs, var_values, vid, proposal);  
        case FUNC_MULTINOMIAL : return _potential_multinomial(vifs, var_values, vid, proposal);
        case FUNC_ONEISTRUE   : return _potential_oneistrue(vifs, var_values, vid, proposal);
        case FUNC_SQLSELECT   : std::cout << "SQLSELECT Not supported yet!" << std::endl; assert(false); return 0;  
        case FUNC_ContLR   : std::cout << "ContinuousLR Not supported yet!" << std::endl; assert(false); return 0;  
        std::cout << "Unsupported Factor Function ID= " << func_id << std::endl;
        assert(false);
      }

      return 0.0;
    }


  private:
    inline bool is_variable_satisfied(const VariableInFactor& vif, const VariableIndex& vid, 
      const VariableValue * const var_values, const VariableValue & proposal) const;

  };

  /**
   * A factor in the factor graph
   */
  class Factor {
  public:
    FactorIndex id;         // factor id
    WeightIndex weight_id;  // weight id
    int func_id;            // factor function id
    int n_variables;        // number of variables

    long n_start_i_vif;     // start variable id

    std::vector<VariableInFactor> tmp_variables; // variables in the factor

    Factor();

    /**
     * Construct a factor with given id, weight id, function id, number of variables
     */
    Factor(const FactorIndex & _id,
           const WeightIndex & _weight_id,
           const int & _func_id,
           const int & _n_variables);

  };

}

// TODO: Find better approach than a .hxx file
#include "dstruct/factor_graph/factor.hxx"

#endif




