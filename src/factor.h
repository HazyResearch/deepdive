#ifndef DIMMWITTED_FACTOR_H_
#define DIMMWITTED_FACTOR_H_

#include "common.h"
#include "variable.h"
#include "weight.h"

#include <iostream>
#include <unordered_map>
#include <vector>
#include <cassert>
#include <cmath>

namespace dd {

class CompactFactor;
class Factor;
class RawFactor;

/**
 * Encapsulates a factor function in the factor graph.
 *
 * This looks strikingly similar to Factor, but the purpose of this class is
 * to indicate that this is accessed through edge-based array.
 */
class CompactFactor {
 public:
  factor_id_t id;                  // factor id
  factor_function_type_t func_id;  // function type id
  size_t n_variables;              // number of variables in the factor
  size_t n_start_i_vif;  // the id of the first variable.  the variables of a
                         // factor
                         // have sequential ids starting from n_start_i_vif to
                         // n_start_i_vif+num_variables-1

  /**
   * Default constructor
   */
  CompactFactor();

  /**
   * Constructs a CompactFactor with given factor id
   */
  CompactFactor(factor_id_t id);

  /**
   * Returns the potential of continousLR factor function. See factor.hxx for
   * more detail
   */
  inline double _potential_continuousLR(
      const VariableInFactor *const vifs,
      const variable_value_t *const var_values, const variable_id_t &,
      const variable_value_t &) const;

  /**
   * Returns the potential of or factor function. See factor.hxx for more detail
   */
  inline double _potential_or(const VariableInFactor *const vifs,
                              const variable_value_t *const var_values,
                              const variable_id_t &,
                              const variable_value_t &) const;

  /**
   * Returns the potential of and factor function. See factor.hxx for more
   * detail
   */
  inline double _potential_and(const VariableInFactor *const vifs,
                               const variable_value_t *const var_values,
                               const variable_id_t &,
                               const variable_value_t &) const;

  /**
   * Returns the potential of equal factor function. See factor.hxx for more
   * detail
   */
  inline double _potential_equal(const VariableInFactor *const vifs,
                                 const variable_value_t *const var_values,
                                 const variable_id_t &,
                                 const variable_value_t &) const;

  /**
   * Returns the potential of MLN style imply factor function. See factor.hxx
   * for more detail
   */
  inline double _potential_imply_mln(const VariableInFactor *const vifs,
                                     const variable_value_t *const var_values,
                                     const variable_id_t &,
                                     const variable_value_t &) const;

  /**
   * Returns the potential of imply factor function. See factor.hxx for more
   * detail
   */
  inline double _potential_imply(const VariableInFactor *const vifs,
                                 const variable_value_t *const var_values,
                                 const variable_id_t &,
                                 const variable_value_t &) const;

  /**
   * Returns the potential of multinomial factor function. See factor.hxx for
   * more detail
   */
  inline double _potential_multinomial(const VariableInFactor *const vifs,
                                       const variable_value_t *const var_values,
                                       const variable_id_t &,
                                       const variable_value_t &) const;

  /**
   * Returns the potential of oneIsTrue factor function. See factor.hxx for more
   * detail
   */
  inline double _potential_oneistrue(const VariableInFactor *const vifs,
                                     const variable_value_t *const var_values,
                                     const variable_id_t &,
                                     const variable_value_t &) const;

  /**
   * Returns the potential of linear factor function. See factor.hxx for more
   * detail
   */
  inline double _potential_linear(const VariableInFactor *const vifs,
                                  const variable_value_t *const var_values,
                                  const variable_id_t &,
                                  const variable_value_t &) const;

  /**
   * Returns the potential of ratio factor function. See factor.hxx for more
   * detail
   */
  inline double _potential_ratio(const VariableInFactor *const vifs,
                                 const variable_value_t *const var_values,
                                 const variable_id_t &,
                                 const variable_value_t &) const;

  /**
   * Returns the potential of logical factor function. See factor.hxx for more
   * detail
   */
  inline double _potential_logical(const VariableInFactor *const vifs,
                                   const variable_value_t *const var_values,
                                   const variable_id_t &,
                                   const variable_value_t &) const;

  /**
   * Returns potential of the factor.
   * (potential is the value of the factor)
   * The potential is calculated using the proposal value for variable with
   * id vid, and var_values for other variables in the factor.
   *
   * vifs pointer to variables in the factor graph
   * var_values pointer to variable values (array)
   * vid variable id to be calculated with proposal
   * proposal the proposed value
   *
   * This function is defined in the head to make sure
   * it gets inlined
   */
  inline double potential(const VariableInFactor *const vifs,
                          const variable_value_t *const var_values,
                          const variable_id_t &vid,
                          const variable_value_t &proposal) const {
    switch (func_id) {
      case FUNC_IMPLY_MLN:
        return _potential_imply_mln(vifs, var_values, vid, proposal);
      case FUNC_IMPLY_neg1_1:
        return _potential_imply(vifs, var_values, vid, proposal);
      case FUNC_ISTRUE:
        return _potential_and(vifs, var_values, vid, proposal);
      case FUNC_OR:
        return _potential_or(vifs, var_values, vid, proposal);
      case FUNC_AND:
        return _potential_and(vifs, var_values, vid, proposal);
      case FUNC_EQUAL:
        return _potential_equal(vifs, var_values, vid, proposal);
      case FUNC_SPARSE_MULTINOMIAL:
      case FUNC_MULTINOMIAL:
        return _potential_multinomial(vifs, var_values, vid, proposal);
      case FUNC_LINEAR:
        return _potential_linear(vifs, var_values, vid, proposal);
      case FUNC_RATIO:
        return _potential_ratio(vifs, var_values, vid, proposal);
      case FUNC_LOGICAL:
        return _potential_logical(vifs, var_values, vid, proposal);
      case FUNC_ONEISTRUE:
        return _potential_oneistrue(vifs, var_values, vid, proposal);
      case FUNC_SQLSELECT:
        std::cout << "SQLSELECT Not supported yet!" << std::endl;
        assert(false);
        return 0;
      case FUNC_ContLR:
        std::cout << "ContinuousLR Not supported yet!" << std::endl;
        assert(false);
        return 0;
      default:
        std::cout << "Unsupported Factor Function ID= " << func_id << std::endl;
        abort();
    }

    return 0.0;
  }

 private:
  inline bool is_variable_satisfied(const VariableInFactor &vif,
                                    const variable_id_t &vid,
                                    const variable_value_t *const var_values,
                                    const variable_value_t &proposal) const;
};

/**
 * A factor in the compiled factor graph.
 */
class Factor {
 public:
  factor_id_t id;                  // factor id
  weight_id_t weight_id;           // weight id
  factor_function_type_t func_id;  // factor function id
  size_t n_variables;              // number of variables

  size_t n_start_i_vif;  // start variable id

  static constexpr factor_id_t INVALID_ID = -1;
  static constexpr factor_function_type_t INVALID_FUNC_ID = FUNC_UNDEFINED;

  // Variable value dependent weights for sparse multinomial factors
  // Key: radix encoding of var values: (...((((0 * d1 + i1) * d2) + i2) * d3 +
  // i3) * d4 + ...) * dk + ik
  // Value: weight id
  // If a key (i.e., value assignment) is missing, it means this factor is
  // inactive (potential = 0).
  // TODO: handle key overflow...
  // An empty map takes 48 bytes, so we create it only as needed, i.e., when
  // func_id = FUNC_SPARSE_MULTINOMIAL
  // Allocated in binary_parser.read_factors. Never deallocated.
  std::unordered_map<variable_value_t, weight_id_t> *weight_ids;

  /**
   * Turns out the no-arg constructor is still required, since we're
   * initializing arrays of these objects inside FactorGraph and
   * CompactFactorGraph.
   */
  Factor();

  Factor(factor_id_t id, weight_id_t weight_id, factor_function_type_t func_id,
         size_t n_variables);

  /**
   * Copy constructor from a RawFactor.
   *
   * Explicit note: don't have a constructor that takes the individual
   * parameters such as id, weight_id, etc. since we expect Factors to be
   * constructed from RawFactors during factor graph compilation.
   */
  Factor(const Factor &rf);
};

/**
 * A raw factor is used when loading the factor graph the first time.
 */
class RawFactor : public Factor {
 public:
  /*
   * Populated when loading the factor graph, used when compiling the factor
   * graph, and after that, it's useless.
   * This list is used only during loading time. Allocate and destroy to save
   * space.
   * Life starts: binary_parser.read_factors (via add_variable_in_factor)
   * Life ends: FactorGraph::organize_graph_by_edge (via clear_tmp_variables)
   */
  std::vector<VariableInFactor> *tmp_variables;  // variables in the factor

  /**
   * Need default constructor to create arrays of uninitialized RawFactors.
   */
  RawFactor();

  /**
   */
  RawFactor(factor_id_t id, weight_id_t weight_id,
            factor_function_type_t func_id, size_t n_variables);

  inline void add_variable_in_factor(const VariableInFactor &vif);

  inline void clear_tmp_variables();
};

inline void RawFactor::add_variable_in_factor(const VariableInFactor &vif) {
  if (!tmp_variables) {
    tmp_variables = new std::vector<VariableInFactor>();
  }
  tmp_variables->push_back(vif);
}

inline void RawFactor::clear_tmp_variables() {
  if (tmp_variables) {
    delete tmp_variables;
    tmp_variables = NULL;
  }
}

}  // namespace dd

// TODO: Find better approach than a .hxx file
#include "factor.hh"

#endif  // DIMMWITTED_FACTOR_H_
