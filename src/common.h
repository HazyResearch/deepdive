#ifndef DIMMWITTED_COMMON_H_
#define DIMMWITTED_COMMON_H_

#include <stdio.h>
#include <gtest/gtest_prod.h>

#ifndef __MACH__
#include <numa.h>
#include <numaif.h>
#endif

// The author knows that this is really ugly...
// But what can we do? Lets hope our Macbook
// supporting NUMA soon! A 2lbs laptop with
// 4 NUMA nodes, how cool is that!
#ifdef __MACH__
#include <math.h>
#include <stdlib.h>

#define numa_alloc_onnode(X, Y) malloc(X)

#define numa_max_node() 0

#define numa_run_on_node(X) 0

#define numa_set_localalloc() 0

#endif

#include <math.h>

#define LOG_2 0.693147180559945
#define MINUS_LOG_THRESHOLD -18.42

/**
 * To use, make with DEBUG flag turned on.
 */
#ifdef DEBUG
#define dprintf(fmt, ...) fprintf(stderr, fmt, ##__VA_ARGS__);
#else
#define dprintf(fmt, ...) (void)(0)
#endif

namespace dd {

typedef enum {
  DTYPE_BOOLEAN = 0x00,
  DTYPE_REAL = 0x01,
  DTYPE_MULTINOMIAL = 0x04,
} variable_domain_type_t;

typedef size_t variable_value_t;
typedef size_t variable_id_t;

// enumeration for factor function types
typedef enum FACTOR_FUCNTION_TYPE {
  FUNC_IMPLY_MLN = 0,
  FUNC_IMPLY_neg1_1 = 11,
  FUNC_OR = 1,
  FUNC_AND = 2,
  FUNC_EQUAL = 3,
  FUNC_ISTRUE = 4,
  FUNC_MULTINOMIAL = 5,
  FUNC_ONEISTRUE = 6,
  FUNC_LINEAR = 7,
  FUNC_RATIO = 8,
  FUNC_LOGICAL = 9,
  FUNC_SQLSELECT = 10,
  FUNC_SPARSE_MULTINOMIAL = 12,
  FUNC_ContLR = 20,

  FUNC_UNDEFINED = -1,
} factor_function_type_t;

typedef size_t factor_id_t;

typedef size_t weight_id_t;
typedef double weight_value_t;

/**
 * Explicitly say things are unused if they are actually unused.
 */
#define UNUSED(var) (void)(var)

enum inc_mode { ORIGINAL, MAT, INC };

enum regularization { REG_L1, REG_L2 };

inline bool fast_exact_is_equal(double a, double b) {
  return (a <= b && b <= a);
}

/**
 * Calculates log(exp(log_a) + exp(log_b))
 */
inline double logadd(double log_a, double log_b) {
  if (log_a < log_b) {  // swap them
    double tmp = log_a;
    log_a = log_b;
    log_b = tmp;
  } else if (fast_exact_is_equal(log_a, log_b)) {
    // Special case when log_a == log_b. In particular this works when both
    // log_a and log_b are (+-) INFINITY: it will return (+-) INFINITY
    // instead of NaN.
    return LOG_2 + log_a;
  }
  double negative_absolute_difference = log_b - log_a;
  if (negative_absolute_difference < MINUS_LOG_THRESHOLD) return (log_a);
  return (log_a + log1p(exp(negative_absolute_difference)));
}

}  // namespace dd

#endif  // DIMMWITTED_COMMON_H_
