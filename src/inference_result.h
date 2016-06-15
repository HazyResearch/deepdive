#ifndef DIMMWITTED_INFERENCE_RESULT_H_
#define DIMMWITTED_INFERENCE_RESULT_H_

#include "variable.h"
#include "weight.h"
#include "cmd_parser.h"
#include <memory>

namespace dd {

class CompactFactorGraph;

/**
 * Encapsulates inference result statistics
 */
class InferenceResult {
 private:
  const CompactFactorGraph &fg;
  const CmdParser &opts;
  size_t weight_values_normalizer;

 public:
  VariableIndex nvars;   // number of variables
  WeightIndex nweights;  // number of weights
  size_t ntallies;

  std::unique_ptr<size_t[]> multinomial_tallies;  // this might be slow...

  // array of sum of samples for each variable
  std::unique_ptr<VariableValue[]> agg_means;
  // array of number of samples for each variable
  std::unique_ptr<size_t[]> agg_nsamples;
  // assignment to variables, see variable.h for more detail
  std::unique_ptr<VariableValue[]> assignments_free;
  std::unique_ptr<VariableValue[]> assignments_evid;

  std::unique_ptr<weight_value_t[]> weight_values;  // array of weight values
  std::unique_ptr<bool[]> weights_isfixed;  // array of whether weight is fixed

  InferenceResult(const CompactFactorGraph &fg, const Weight weights[],
                  const CmdParser &opts);

  // copy constructor
  InferenceResult(const InferenceResult &other);

  void merge_weights_from(const InferenceResult &other);
  void average_regularize_weights(double current_stepsize);
  void copy_weights_to(InferenceResult &other) const;
  void show_weights_snippet(std::ostream &output) const;
  void dump_weights_in_text(std::ostream &text_output) const;

  void clear_variabletally();
  void aggregate_marginals_from(const InferenceResult &other);
  void show_marginal_snippet(std::ostream &output) const;
  void show_marginal_histogram(std::ostream &output) const;
  void dump_marginals_in_text(std::ostream &text_output) const;

 private:
  InferenceResult(const CompactFactorGraph &fg, const CmdParser &opts);
};

}  // namespace dd

#endif  // DIMMWITTED_INFERENCE_RESULT_H_
