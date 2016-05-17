#ifndef DIMMWITTED_WEIGHT_H_
#define DIMMWITTED_WEIGHT_H_

namespace dd {

typedef long WeightIndex;

/**
 * Encapsulates a weight for factors.
 */
class Weight {
 public:
  long id;        // weight id
  double weight;  // weight value
  bool isfixed;   // whether the weight is fixed

  Weight(const long& _id, const double& _weight, const bool& _isfixed);

  Weight();
};

}  // namespace dd

#endif  // DIMMWITTED_WEIGHT_H_
