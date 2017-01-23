#ifndef DIMMWITTED_WEIGHT_H_
#define DIMMWITTED_WEIGHT_H_

#include "common.h"

#include <cstdlib>

namespace dd {

/**
 * Encapsulates a weight for factors.
 */
class Weight {
 public:
  size_t id;      // weight id
  double weight;  // weight value
  bool isfixed;   // whether the weight is fixed

  Weight(size_t id, double weight, bool isfixed);

  Weight();

  static constexpr size_t INVALID_ID = (size_t)-1;
};

}  // namespace dd

#endif  // DIMMWITTED_WEIGHT_H_
