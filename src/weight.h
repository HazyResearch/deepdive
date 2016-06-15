#ifndef DIMMWITTED_WEIGHT_H_
#define DIMMWITTED_WEIGHT_H_

#include <cstdlib>

namespace dd {

typedef size_t WeightIndex;
typedef double weight_value_t;

/**
 * Encapsulates a weight for factors.
 */
class Weight {
 public:
  WeightIndex id;         // weight id
  weight_value_t weight;  // weight value
  bool isfixed;           // whether the weight is fixed

  Weight(const WeightIndex id, const weight_value_t weight, const bool isfixed);

  Weight();
};

}  // namespace dd

#endif  // DIMMWITTED_WEIGHT_H_
