#include "inference_result.h"
#include <iostream>

namespace dd {

InferenceResult::InferenceResult(long _nvars, long _nweights)
    : nvars(_nvars),
      nweights(_nweights),
      agg_means(new double[_nvars]),
      agg_nsamples(new double[_nvars]),
      assignments_free(new VariableValue[_nvars]),
      assignments_evid(new VariableValue[_nvars]),
      weight_values(new double[_nweights]),
      weights_isfixed(new bool[_nweights]) {}

void InferenceResult::init(Variable *variables, Weight *const weights) {
  ntallies = 0;
  for (long t = 0; t < nvars; t++) {
    const Variable &variable = variables[t];
    assignments_free[variable.id] = variable.assignment_free;
    assignments_evid[variable.id] = variable.assignment_evid;
    agg_means[variable.id] = 0.0;
    agg_nsamples[variable.id] = 0.0;
    if (variable.domain_type == DTYPE_MULTINOMIAL) {
      ntallies += variable.cardinality;
    }
  }

  multinomial_tallies = new int[ntallies];
  for (long i = 0; i < ntallies; i++) {
    multinomial_tallies[i] = 0;
  }

  for (long t = 0; t < nweights; t++) {
    const Weight &weight = weights[t];
    weight_values[weight.id] = weight.weight;
    weights_isfixed[weight.id] = weight.isfixed;
  }
}

void InferenceResult::copy_from(InferenceResult &other) {
  for (long i = 0; i < nvars; i++) {
    assignments_free[i] = other.assignments_free[i];
    assignments_evid[i] = other.assignments_evid[i];
    agg_means[i] = other.agg_means[i];
    agg_nsamples[i] = other.agg_nsamples[i];
  }

  ntallies = other.ntallies;
  multinomial_tallies = new int[ntallies];
  for (long i = 0; i < ntallies; i++) {
    multinomial_tallies[i] = 0;
  }

  for (long i = 0; i < nweights; i++) {
    weight_values[i] = other.weight_values[i];
    weights_isfixed[i] = other.weights_isfixed[i];
  }
}

}  // namespace dd
