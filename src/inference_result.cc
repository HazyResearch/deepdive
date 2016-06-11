#include "inference_result.h"
#include "factor_graph.h"
#include <iostream>

namespace dd {

InferenceResult::InferenceResult(size_t nvars, size_t nweights)
    : nvars(nvars),
      nweights(nweights),
      ntallies(0),
      multinomial_tallies(NULL),
      agg_means(new double[nvars]),
      agg_nsamples(new double[nvars]),
      assignments_free(new VariableValue[nvars]),
      assignments_evid(new VariableValue[nvars]),
      weight_values(new double[nweights]),
      weights_isfixed(new bool[nweights]) {}

InferenceResult::InferenceResult(const CompiledFactorGraph &fg,
                                 Weight *const weights)
    : InferenceResult(fg.size.num_variables, fg.size.num_weights) {
  ntallies = 0;
  for (long t = 0; t < nvars; t++) {
    const Variable &variable = fg.variables[t];
    assignments_free[variable.id] = variable.assignment_free;
    assignments_evid[variable.id] = variable.assignment_evid;
    agg_means[variable.id] = 0.0;
    agg_nsamples[variable.id] = 0.0;
    if (variable.domain_type == DTYPE_MULTINOMIAL) {
      ntallies += variable.cardinality;
    }
  }

  for (long t = 0; t < nweights; t++) {
    const Weight &weight = weights[t];
    weight_values[weight.id] = weight.weight;
    weights_isfixed[weight.id] = weight.isfixed;
  }

  multinomial_tallies = new int[ntallies];
  for (long i = 0; i < ntallies; i++) {
    multinomial_tallies[i] = 0;
  }
}

InferenceResult::InferenceResult(const InferenceResult &other)
    : InferenceResult(other.nvars, other.nweights) {
  memcpy(assignments_free, other.assignments_free,
         sizeof(*assignments_free) * nvars);
  memcpy(assignments_evid, other.assignments_evid,
         sizeof(*assignments_evid) * nvars);
  memcpy(agg_means, other.agg_means, sizeof(*agg_means) * nvars);
  memcpy(agg_nsamples, other.agg_nsamples, sizeof(*agg_nsamples) * nvars);

  memcpy(weight_values, other.weight_values, sizeof(*weight_values) * nweights);
  memcpy(weights_isfixed, other.weights_isfixed,
         sizeof(*weights_isfixed) * nweights);

  ntallies = other.ntallies;
  multinomial_tallies = new int[ntallies];
  for (long i = 0; i < ntallies; i++) {
    multinomial_tallies[i] = other.multinomial_tallies[i];
  }
}

}  // namespace dd
