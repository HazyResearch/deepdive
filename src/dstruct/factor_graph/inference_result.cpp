#include "dstruct/factor_graph/inference_result.h"

dd::InferenceResult::InferenceResult(long _nvars, long _nweights):
  nvars(_nvars),
  nweights(_nweights),
  agg_means(new double[_nvars]),
  agg_nsamples(new double[_nvars]),
  assignments_free(new VariableValue[_nvars]),
  assignments_evid(new VariableValue[_nvars]),
  weight_values(new double [_nweights]),
  weights_isfixed(new bool [_nweights]) {}

void dd::InferenceResult::init(Variable * variables, Weight * const weights){

  ntallies = 0;
  for(long t=0;t<nvars;t++){
    const Variable & variable = variables[t];
    assignments_free[variable.id] = variable.assignment_free;
    assignments_evid[variable.id] = variable.assignment_evid;
    agg_means[variable.id] = 0.0;
    agg_nsamples[variable.id] = 0.0;
    if(variable.domain_type == DTYPE_MULTINOMIAL){
      ntallies += variable.upper_bound - variable.lower_bound + 1;
    }
  }

  multinomial_tallies = new int[ntallies];
  for(long i=0;i<ntallies;i++){
    multinomial_tallies[i] = 0;
  }

  for(long t=0;t<nweights;t++){
    const Weight & weight = weights[t];
    weight_values[weight.id] = weight.weight;
    weights_isfixed[weight.id] = weight.isfixed;
  }
}
