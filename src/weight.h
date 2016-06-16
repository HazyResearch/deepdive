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
  weight_id_t id;         // weight id
  weight_value_t weight;  // weight value
  bool isfixed;           // whether the weight is fixed

  Weight(weight_id_t id, weight_value_t weight, bool isfixed);

  Weight();

  static constexpr weight_id_t INVALID_ID = (weight_id_t)-1;
};

}  // namespace dd

#endif  // DIMMWITTED_WEIGHT_H_
