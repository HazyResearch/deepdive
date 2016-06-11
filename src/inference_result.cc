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
                                 const Weight weights[])
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

  multinomial_tallies.reset(new int[ntallies]);
  for (long i = 0; i < ntallies; i++) {
    multinomial_tallies[i] = 0;
  }
}

InferenceResult::InferenceResult(const InferenceResult &other)
    : InferenceResult(other.nvars, other.nweights) {
  memcpy(assignments_evid.get(), other.assignments_evid.get(),
         sizeof(*assignments_evid.get()) * nvars);
  memcpy(agg_means.get(), other.agg_means.get(),
         sizeof(*agg_means.get()) * nvars);
  memcpy(agg_nsamples.get(), other.agg_nsamples.get(),
         sizeof(*agg_nsamples.get()) * nvars);

  memcpy(weight_values.get(), other.weight_values.get(),
         sizeof(*weight_values.get()) * nweights);
  memcpy(weights_isfixed.get(), other.weights_isfixed.get(),
         sizeof(*weights_isfixed.get()) * nweights);

  ntallies = other.ntallies;
  multinomial_tallies.reset(new int[ntallies]);
  for (long i = 0; i < ntallies; i++) {
    multinomial_tallies[i] = other.multinomial_tallies[i];
  }
}

}  // namespace dd
