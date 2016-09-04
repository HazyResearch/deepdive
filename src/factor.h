#ifndef DIMMWITTED_FACTOR_H_
#define DIMMWITTED_FACTOR_H_

#include "common.h"
#include "variable.h"
#include "weight.h"

#include <cassert>
#include <cmath>
#include <iostream>

namespace dd {

static constexpr double DEFAULT_FEATURE_VALUE = 1;

/**
 * Encapsulates a factor function in the factor graph.
 */
class Factor {
 public:
  size_t id;                     // factor id
  double feature_value;          // feature value
  size_t weight_id;              // weight id
  FACTOR_FUNCTION_TYPE func_id;  // factor function id
  size_t num_vars;               // number of variables
  size_t vif_base;               // start variable id in FactorGraph.vifs

  static constexpr size_t INVALID_ID = -1;

  /**
   * Turns out the no-arg constructor is still required, since we're
   * initializing arrays of these objects inside FactorGraph and
   * FactorGraph.
   */
  Factor();

  Factor(size_t id, double value, size_t weight_id,
         FACTOR_FUNCTION_TYPE func_id, size_t num_vars);

  Factor(const Factor &other);

  Factor &operator=(const Factor &other);

  inline bool is_categorical() const { return func_id == FUNC_AND_CATEGORICAL; }

#define POTENTIAL_SIGN(func_id) _potential_sign_##func_id

  /**
   * Returns potential of the factor.
   * (potential is the value of the factor)
   * The potential is calculated using the proposal value for variable with
   * id vid, and assignments for other variables in the factor.
   *
   * vifs pointer to variables in the factor graph
   * assignments pointer to variable values (array)
   * vid variable id to be calculated with proposal
   * proposal the proposed value.
   */
  inline double potential(
      const FactorToVariable vifs[], const size_t assignments[],
      const size_t vid = Variable::INVALID_ID,
      const size_t proposal = Variable::INVALID_VALUE) const {
#define RETURN_POTENTIAL_FOR(func_id)                                  \
  case func_id:                                                        \
    return POTENTIAL_SIGN(func_id)(vifs, assignments, vid, proposal) * \
           this->feature_value
#define RETURN_POTENTIAL_FOR2(func_id, func_id2) \
  case func_id2:                                 \
    RETURN_POTENTIAL_FOR(func_id)
    switch (func_id) {
      RETURN_POTENTIAL_FOR(FUNC_IMPLY_MLN);
      RETURN_POTENTIAL_FOR(FUNC_IMPLY_NATURAL);
      RETURN_POTENTIAL_FOR2(FUNC_AND, FUNC_ISTRUE);
      RETURN_POTENTIAL_FOR(FUNC_OR);
      RETURN_POTENTIAL_FOR(FUNC_EQUAL);
      RETURN_POTENTIAL_FOR(FUNC_AND_CATEGORICAL);
      RETURN_POTENTIAL_FOR(FUNC_LINEAR);
      RETURN_POTENTIAL_FOR(FUNC_RATIO);
      RETURN_POTENTIAL_FOR(FUNC_LOGICAL);
#undef RETURN_POTENTIAL_FOR
      default:
        std::cout << "Unsupported FACTOR_FUNCTION_TYPE = " << func_id
                  << std::endl;
        std::abort();
    }
  }

 private:
  FRIEND_TEST(FactorTest, ONE_VAR_FACTORS);
  FRIEND_TEST(FactorTest, TWO_VAR_FACTORS);
  FRIEND_TEST(FactorTest, THREE_VAR_IMPLY);

  // whether a variable's value or proposal satisfies the is_equal condition
  inline bool is_variable_satisfied(const FactorToVariable &vif,
                                    const size_t &vid,
                                    const size_t assignments[],
                                    const size_t &proposal) const {
    return (vif.vid == vid) ? vif.satisfiedUsing(proposal)
                            : vif.satisfiedUsing(assignments[vif.vid]);
  }

#define DEFINE_POTENTIAL_SIGN_FOR(func_id)                       \
  inline double POTENTIAL_SIGN(func_id)(                         \
      const FactorToVariable vifs[], const size_t assignments[], \
      const size_t vid, const size_t &proposal) const

  /** Return the value of the "equality test" of the variables in the factor,
   * with the variable of index vid (wrt the factor) is set to the value of
   * the 'proposal' argument.
   *
   */
  DEFINE_POTENTIAL_SIGN_FOR(FUNC_EQUAL) {
    const FactorToVariable &vif = vifs[vif_base];
    /* We use the value of the first variable in the factor as the "gold"
     * standard" */
    const bool firstsat =
        is_variable_satisfied(vif, vid, assignments, proposal);

    /* Iterate over the factor variables */
    for (size_t i_vif = vif_base; i_vif < vif_base + num_vars; ++i_vif) {
      const FactorToVariable &vif = vifs[i_vif];
      const bool satisfied =
          is_variable_satisfied(vif, vid, assignments, proposal);
      /* Early return as soon as we find a mismatch */
      if (satisfied != firstsat) return -1;
    }
    return 1;
  }

  /** Return the value of the logical AND of the variables in the factor, with
   * the variable of index vid (wrt the factor) is set to the value of the
   * 'proposal' argument.
   *
   */
  DEFINE_POTENTIAL_SIGN_FOR(FUNC_AND) {
    /* Iterate over the factor variables */
    for (size_t i_vif = vif_base; i_vif < vif_base + num_vars; ++i_vif) {
      const FactorToVariable &vif = vifs[i_vif];
      const bool satisfied =
          is_variable_satisfied(vif, vid, assignments, proposal);
      /* Early return as soon as we find a variable that is not satisfied */
      if (!satisfied) return -1;
    }
    return 1;
  }

  // potential for AND factor over categorical variables
  DEFINE_POTENTIAL_SIGN_FOR(FUNC_AND_CATEGORICAL) {
    /* Iterate over the factor variables */
    for (size_t i_vif = vif_base; i_vif < vif_base + num_vars; ++i_vif) {
      const FactorToVariable &vif = vifs[i_vif];
      const bool satisfied =
          is_variable_satisfied(vif, vid, assignments, proposal);
      /* Early return as soon as we find a variable that is not satisfied */
      if (!satisfied) return 0;
    }
    return 1;
  }

  /** Return the value of the logical OR of the variables in the factor, with
   * the variable of index vid (wrt the factor) is set to the value of the
   * 'proposal' argument.
   *
   */
  DEFINE_POTENTIAL_SIGN_FOR(FUNC_OR) {
    /* Iterate over the factor variables */
    for (size_t i_vif = vif_base; i_vif < vif_base + num_vars; ++i_vif) {
      const FactorToVariable &vif = vifs[i_vif];
      const bool satisfied =
          is_variable_satisfied(vif, vid, assignments, proposal);
      /* Early return as soon as we find a variable that is satisfied */
      if (satisfied) return 1;
    }
    return -1;
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
  DEFINE_POTENTIAL_SIGN_FOR(FUNC_IMPLY_MLN) {
    /* Compute the value of the body of the rule */
    bool bBody = true;
    for (size_t i_vif = vif_base; i_vif < vif_base + num_vars - 1; ++i_vif) {
      const FactorToVariable &vif = vifs[i_vif];
      // If it is the proposal variable, we use the truth value of the proposal
      bBody &= is_variable_satisfied(vif, vid, assignments, proposal);
    }

    if (!bBody) {
      // Early return if the body is not satisfied
      return 1;
    } else {
      // Compute the value of the head of the rule
      const FactorToVariable &vif =
          vifs[vif_base + num_vars - 1];  // encoding of the head, should
                                          // be more structured.
      const bool bHead = is_variable_satisfied(vif, vid, assignments, proposal);
      return bHead ? 1 : 0;
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
  DEFINE_POTENTIAL_SIGN_FOR(FUNC_IMPLY_NATURAL) {
    /* Compute the value of the body of the rule */
    bool bBody = true;
    for (size_t i_vif = vif_base; i_vif < vif_base + num_vars - 1; ++i_vif) {
      const FactorToVariable &vif = vifs[i_vif];
      // If it is the proposal variable, we use the truth value of the proposal
      bBody &= is_variable_satisfied(vif, vid, assignments, proposal);
    }

    if (!bBody) {
      // Early return if the body is not satisfied
      return 0;
    } else {
      // Compute the value of the head of the rule */
      const FactorToVariable &vif =
          vifs[vif_base + num_vars - 1];  // encoding of the head, should
                                          // be more structured.
      bool bHead = is_variable_satisfied(vif, vid, assignments, proposal);
      return bHead ? 1 : -1;
    }
  }

  // potential for linear expression
  DEFINE_POTENTIAL_SIGN_FOR(FUNC_LINEAR) {
    double res = 0.0;
    bool bHead = is_variable_satisfied(vifs[vif_base + num_vars - 1], vid,
                                       assignments, proposal);
    /* Compute the value of the body of the rule */
    for (size_t i_vif = vif_base; i_vif < vif_base + num_vars - 1; ++i_vif) {
      const FactorToVariable &vif = vifs[i_vif];
      const bool satisfied =
          is_variable_satisfied(vif, vid, assignments, proposal);
      res += ((1 - satisfied) || bHead);
    }
    if (num_vars == 1)
      return double(bHead);
    else
      return res;
  }

  // potential for linear expression
  DEFINE_POTENTIAL_SIGN_FOR(FUNC_RATIO) {
    double res = 1.0;
    bool bHead = is_variable_satisfied(vifs[vif_base + num_vars - 1], vid,
                                       assignments, proposal);
    /* Compute the value of the body of the rule */
    for (size_t i_vif = vif_base; i_vif < vif_base + num_vars - 1; ++i_vif) {
      const FactorToVariable &vif = vifs[i_vif];
      const bool satisfied =
          is_variable_satisfied(vif, vid, assignments, proposal);
      res += ((1 - satisfied) || bHead);
    }
    if (num_vars == 1) return log2(res + double(bHead));
    return log2(res);
  }

  // potential for linear expression
  DEFINE_POTENTIAL_SIGN_FOR(FUNC_LOGICAL) {
    double res = 0.0;
    bool bHead = is_variable_satisfied(vifs[vif_base + num_vars - 1], vid,
                                       assignments, proposal);
    /* Compute the value of the body of the rule */
    for (size_t i_vif = vif_base; i_vif < vif_base + num_vars - 1; ++i_vif) {
      const FactorToVariable &vif = vifs[i_vif];
      const bool satisfied =
          is_variable_satisfied(vif, vid, assignments, proposal);
      res += ((1 - satisfied) || bHead);
    }
    if (num_vars == 1)
      return double(bHead);
    else {
      if (res > 0.0)
        return 1.0;
      else
        return 0.0;
    }
  }

#undef DEFINE_POTENTIAL_SIGN_FOR
};

}  // namespace dd

#endif  // DIMMWITTED_FACTOR_H_
