

#ifndef _COMMON_H
#define _COMMON_H


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

#define numa_alloc_onnode(X,Y) malloc(X)

#define numa_max_node() 0

#define numa_run_on_node(X) 0

#define numa_set_localalloc() 0

#endif

#include <math.h>

#define LOG_2   0.693147180559945
#define MINUS_LOG_THRESHOLD   -18.42

inline bool fast_exact_is_equal(double a, double b){
    return (a <= b && b <= a);
}

/**
 * Calculates log(exp(log_a) + exp(log_b)) 
 */
inline double logadd(double log_a, double log_b){
    
    if (log_a < log_b)
    { // swap them
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
    if (negative_absolute_difference < MINUS_LOG_THRESHOLD)
        return (log_a);
    return (log_a + log1p(exp(negative_absolute_difference)));
    
}


#endif