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
        abort();
      case FUNC_ContLR:
        std::cout << "ContinuousLR Not supported yet!" << std::endl;
        abort();
      default:
        std::cout << "Unsupported Factor Function ID= " << func_id << std::endl;
        abort();
    }
  }

 private:
  FRIEND_TEST(FactorTest, ONE_VAR_FACTORS);
  FRIEND_TEST(FactorTest, TWO_VAR_FACTORS);
  FRIEND_TEST(FactorTest, THREE_VAR_IMPLY);

  // whether a variable's value or proposal satisfies the is_equal condition
  inline bool is_variable_satisfied(const VariableInFactor &vif,
                                    const variable_id_t &vid,
                                    const variable_value_t *const var_values,
                                    const variable_value_t &proposal) const {
    return (vif.vid == vid) ? vif.satisfiedUsing(proposal)
                            : vif.satisfiedUsing(var_values[vif.vid]);
  }

  /** Return the value of the "equality test" of the variables in the factor,
   * with the variable of index vid (wrt the factor) is set to the value of
   * the 'proposal' argument.
   *
   */
  inline double _potential_equal(const VariableInFactor *const vifs,
                                 const variable_value_t *const var_values,
                                 const variable_id_t &vid,
                                 const variable_value_t &proposal) const {
    const VariableInFactor &vif = vifs[n_start_i_vif];
    /* We use the value of the first variable in the factor as the "gold"
     * standard" */
    const bool firstsat = is_variable_satisfied(vif, vid, var_values, proposal);

    /* Iterate over the factor variables */
    for (size_t i_vif = n_start_i_vif; i_vif < n_start_i_vif + n_variables;
         ++i_vif) {
      const VariableInFactor &vif = vifs[i_vif];
      const bool satisfied =
          is_variable_satisfied(vif, vid, var_values, proposal);
      /* Early return as soon as we find a mismatch */
      if (satisfied != firstsat) return 0.0;
    }
    return 1.0;
  }

  /** Return the value of the logical AND of the variables in the factor, with
   * the variable of index vid (wrt the factor) is set to the value of the
   * 'proposal' argument.
   *
   */
  inline double _potential_and(const VariableInFactor *const vifs,
                               const variable_value_t *const var_values,
                               const variable_id_t &vid,
                               const variable_value_t &proposal) const {
    /* Iterate over the factor variables */
    for (size_t i_vif = n_start_i_vif; i_vif < n_start_i_vif + n_variables;
         ++i_vif) {
      const VariableInFactor &vif = vifs[i_vif];
      const bool satisfied =
          is_variable_satisfied(vif, vid, var_values, proposal);
      /* Early return as soon as we find a variable that is not satisfied */
      if (!satisfied) return 0.0;
    }
    return 1.0;
  }

  /** Return the value of the logical OR of the variables in the factor, with
   * the variable of index vid (wrt the factor) is set to the value of the
   * 'proposal' argument.
   *
   */
  inline double _potential_or(const VariableInFactor *const vifs,
                              const variable_value_t *const var_values,
                              const variable_id_t &vid,
                              const variable_value_t &proposal) const {
    /* Iterate over the factor variables */
    for (size_t i_vif = n_start_i_vif; i_vif < n_start_i_vif + n_variables;
         ++i_vif) {
      const VariableInFactor &vif = vifs[i_vif];
      const bool satisfied =
          is_variable_satisfied(vif, vid, var_values, proposal);
      /* Early return as soon as we find a variable that is satisfied */
      if (satisfied) return 1.0;
    }
    return 0.0;
  }

  /** Return the value of the 'imply (MLN version)' of the variables in the
   * factor, with the variable of index vid (wrt the factor) is set to the
   * value of the 'proposal' argument.
   *
   * The head of the 'imply' rule is stored as the *last* variable in the
   * factor.
   *
   * The truth table of the 'imply (MLN version)' function requires to return
   * 0.0 if the body of the imply is satisfied but the head is not, and to
   * return 1.0 if the body is not satisfied.
   *
   */
  inline double _potential_imply_mln(const VariableInFactor *const vifs,
                                     const variable_value_t *const var_values,
                                     const variable_id_t &vid,
                                     const variable_value_t &proposal) const {
    /* Compute the value of the body of the rule */
    bool bBody = true;
    for (size_t i_vif = n_start_i_vif; i_vif < n_start_i_vif + n_variables - 1;
         ++i_vif) {
      const VariableInFactor &vif = vifs[i_vif];
      // If it is the proposal variable, we use the truth value of the proposal
      bBody &= is_variable_satisfied(vif, vid, var_values, proposal);
    }

    if (!bBody) {
      // Early return if the body is not satisfied
      return 1.0;
    } else {
      // Compute the value of the head of the rule
      const VariableInFactor &vif = vifs[n_start_i_vif + n_variables -
                                         1];  // encoding of the head, should
                                              // be more structured.
      const bool bHead = is_variable_satisfied(vif, vid, var_values, proposal);
      return bHead ? 1.0 : 0.0;
    }
  }

  /** Return the value of the 'imply' of the variables in the factor, with the
   * variable of index vid (wrt the factor) is set to the value of the
   * 'proposal' argument.
   *
   * The head of the 'imply' rule is stored as the *last* variable in the
   * factor.
   *
   * The truth table of the 'imply' function requires to return
   * -1.0 if the body of the rule is satisfied but the head is not, and to
   *  return 0.0 if the body is not satisfied.
   *
   */
  inline double _potential_imply(const VariableInFactor *const vifs,
                                 const variable_value_t *const var_values,
                                 const variable_id_t &vid,
                                 const variable_value_t &proposal) const {
    /* Compute the value of the body of the rule */
    bool bBody = true;
    for (size_t i_vif = n_start_i_vif; i_vif < n_start_i_vif + n_variables - 1;
         ++i_vif) {
      const VariableInFactor &vif = vifs[i_vif];
      // If it is the proposal variable, we use the truth value of the proposal
      bBody &= is_variable_satisfied(vif, vid, var_values, proposal);
    }

    if (!bBody) {
      // Early return if the body is not satisfied
      return 0.0;
    } else {
      // Compute the value of the head of the rule */
      const VariableInFactor &vif = vifs[n_start_i_vif + n_variables -
                                         1];  // encoding of the head, should
                                              // be more structured.
      bool bHead = is_variable_satisfied(vif, vid, var_values, proposal);
      return bHead ? 1.0 : -1.0;
    }
  }

  // potential for multinomial variable
  inline double _potential_multinomial(const VariableInFactor *vifs,
                                       const variable_value_t *var_values,
                                       const variable_id_t &vid,
                                       const variable_value_t &proposal) const {
    return 1.0;
  }

  /** Return the value of the oneIsTrue of the variables in the factor, with
   * the variable of index vid (wrt the factor) is set to the value of the
   * 'proposal' argument.
   *
   */
  inline double _potential_oneistrue(const VariableInFactor *const vifs,
                                     const variable_value_t *const var_values,
                                     const variable_id_t &vid,
                                     const variable_value_t &proposal) const {
    bool found = false;
    /* Iterate over the factor variables */
    for (size_t i_vif = n_start_i_vif; i_vif < n_start_i_vif + n_variables;
         ++i_vif) {
      const VariableInFactor &vif = vifs[i_vif];
      const bool satisfied =
          is_variable_satisfied(vif, vid, var_values, proposal);

      if (satisfied) {
        if (!found) found = true;
        /* Early return if we find that two variables are satisfied */
        else
          return -1.0;
      }
    }
    if (found)
      return 1.0;
    else
      return -1.0;
  }

  // potential for linear expression
  inline double _potential_linear(const VariableInFactor *const vifs,
                                  const variable_value_t *const var_values,
                                  const variable_id_t &vid,
                                  const variable_value_t &proposal) const {
    double res = 0.0;
    bool bHead = is_variable_satisfied(vifs[n_start_i_vif + n_variables - 1],
                                       vid, var_values, proposal);
    /* Compute the value of the body of the rule */
    for (size_t i_vif = n_start_i_vif; i_vif < n_start_i_vif + n_variables - 1;
         ++i_vif) {
      const VariableInFactor &vif = vifs[i_vif];
      const bool satisfied =
          is_variable_satisfied(vif, vid, var_values, proposal);
      res += ((1 - satisfied) || bHead);
    }
    if (n_variables == 1)
      return double(bHead);
    else
      return res;
  }

  // potential for linear expression
  inline double _potential_ratio(const VariableInFactor *const vifs,
                                 const variable_value_t *const var_values,
                                 const variable_id_t &vid,
                                 const variable_value_t &proposal) const {
    double res = 1.0;
    bool bHead = is_variable_satisfied(vifs[n_start_i_vif + n_variables - 1],
                                       vid, var_values, proposal);
    /* Compute the value of the body of the rule */
    for (size_t i_vif = n_start_i_vif; i_vif < n_start_i_vif + n_variables - 1;
         ++i_vif) {
      const VariableInFactor &vif = vifs[i_vif];
      const bool satisfied =
          is_variable_satisfied(vif, vid, var_values, proposal);
      res += ((1 - satisfied) || bHead);
    }
    if (n_variables == 1) return log2(res + double(bHead));
    return log2(res);
  }

  // potential for linear expression
  inline double _potential_logical(const VariableInFactor *const vifs,
                                   const variable_value_t *const var_values,
                                   const variable_id_t &vid,
                                   const variable_value_t &proposal) const {
    double res = 0.0;
    bool bHead = is_variable_satisfied(vifs[n_start_i_vif + n_variables - 1],
                                       vid, var_values, proposal);
    /* Compute the value of the body of the rule */
    for (size_t i_vif = n_start_i_vif; i_vif < n_start_i_vif + n_variables - 1;
         ++i_vif) {
      const VariableInFactor &vif = vifs[i_vif];
      const bool satisfied =
          is_variable_satisfied(vif, vid, var_values, proposal);
      res += ((1 - satisfied) || bHead);
    }
    if (n_variables == 1)
      return double(bHead);
    else {
      if (res > 0.0)
        return 1.0;
      else
        return 0.0;
    }
  }
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

#endif  // DIMMWITTED_FACTOR_H_
