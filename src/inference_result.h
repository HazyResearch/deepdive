#ifndef DIMMWITTED_INFERENCE_RESULT_H_
#define DIMMWITTED_INFERENCE_RESULT_H_

#include "variable.h"
#include "weight.h"

namespace dd {

class CompiledFactorGraph;

/**
 * Encapsulates inference result statistics
 */
class InferenceResult {
 public:
  long nvars;     // number of variables
  long nweights;  // number of weights
  long ntallies;

  int *multinomial_tallies;  // this might be slow...

  // array of sum of samples for each variable
  double *agg_means;
  // array of number of samples for each variable
  double *agg_nsamples;
  // assignment to variables, see variable.h for more detail
  VariableValue *assignments_free;
  VariableValue *assignments_evid;

  double *weight_values;  // array of weight values
  bool *weights_isfixed;  // array of whether weight is fixed

  InferenceResult(const CompiledFactorGraph &fg, Weight *const weights);

  // copy constructor
  InferenceResult(const InferenceResult &other);

 private:
  InferenceResult(size_t nvars, size_t nweights);
};

}  // namespace dd

#endif  // DIMMWITTED_INFERENCE_RESULT_H_
