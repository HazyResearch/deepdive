#ifndef DIMMWITTED_COMMON_H_
#define DIMMWITTED_COMMON_H_

#include <gtest/gtest_prod.h>
#include <stdint.h>
#include <stdio.h>

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
  DTYPE_BOOLEAN = 0,
  DTYPE_CATEGORICAL = 1,
} DOMAIN_TYPE;

typedef enum {
  FUNC_IMPLY_NATURAL = 0,
  FUNC_OR = 1,
  FUNC_AND = 2,
  FUNC_EQUAL = 3,
  FUNC_ISTRUE = 4,
  FUNC_LINEAR = 7,
  FUNC_RATIO = 8,
  FUNC_LOGICAL = 9,
  FUNC_AND_CATEGORICAL = 12,
  FUNC_IMPLY_MLN = 13,

  FUNC_UNDEFINED = -1,
} FACTOR_FUNCTION_TYPE;

/**
 * Handy way to copy arrays of objects correctly, pointed by unique_ptr
 */
#define COPY_ARRAY_UNIQUE_PTR_MEMBER(array_up, size) \
  COPY_ARRAY(other.array_up.get(), size, array_up.get())
#define COPY_ARRAY(src, size, dst) std::copy(src, src + size, dst)

/**
 * Explicitly say things are unused if they are actually unused.
 */
#define UNUSED(var) (void)(var)

enum regularization_t { REG_L1, REG_L2 };

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
