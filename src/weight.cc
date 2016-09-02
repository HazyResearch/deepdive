#include "weight.h"

namespace dd {

Weight::Weight(size_t id, double weight, bool isfixed)
    : id(id), weight(weight), isfixed(isfixed) {}

Weight::Weight() : Weight(INVALID_ID, 0.0, false) {}

}  // namespace dd
