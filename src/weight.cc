#include "weight.h"

namespace dd {

dd::Weight::Weight(const WeightIndex id, const weight_value_t weight,
                   const bool isfixed)
    : id(id), weight(weight), isfixed(isfixed) {}

dd::Weight::Weight() {}

}  // namespace dd
