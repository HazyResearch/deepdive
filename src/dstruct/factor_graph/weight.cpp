#include "dstruct/factor_graph/weight.h"

dd::Weight::Weight(const long & _id,
       const double & _weight,
       const bool & _isfixed):
  id(_id),
  weight(_weight),
  isfixed(_isfixed)
{}

dd::Weight::Weight(){
  
}
