#ifndef DIMMWITTED_INFERENCE_RESULT_H_
#define DIMMWITTED_INFERENCE_RESULT_H_

#include "cmd_parser.h"
#include "variable.h"
#include "weight.h"
#include <memory>

namespace dd {

class FactorGraph;

/**
 * Encapsulates inference result statistics
 */
class InferenceResult {
 private:
  const FactorGraph &fg;
  const CmdParser &opts;

 public:
  size_t nvars;     // number of variables
  size_t nweights;  // number of weights
  size_t ntallies;  // number of tallies

  // tallies for each var value (see Variable.var_val_base)
  std::unique_ptr<size_t[]> sample_tallies;

  // array of number of samples for each variable
  std::unique_ptr<size_t[]> agg_nsamples;

  // vanilla / traditional Gibbs chain
  std::unique_ptr<size_t[]> assignments_free;

  // separate Gibbs chain CONDITIONED ON EVIDENCE
  std::unique_ptr<size_t[]> assignments_evid;

  // array of weight values
  std::unique_ptr<double[]> weight_values;
  // array of weight gradients, used in distributed learning
  std::unique_ptr<float[]> weight_grads;
  // array of whether weight is fixed
  std::unique_ptr<bool[]> weights_isfixed;

  InferenceResult(const FactorGraph &fg, const Weight weights[],
                  const CmdParser &opts);

  // copy constructor
  InferenceResult(const InferenceResult &other);

  void merge_gradients_from(const InferenceResult &other);
  void reset_gradients();
  void merge_weights_from(const InferenceResult &other);
  void average_weights(size_t count);
  void copy_weights_to(InferenceResult &other) const;
  void show_weights_snippet(std::ostream &output) const;
  void dump_weights_in_text(std::ostream &text_output) const;

  void clear_variabletally();
  void aggregate_marginals_from(const InferenceResult &other);
  void show_marginal_snippet(std::ostream &output) const;
  void show_marginal_histogram(std::ostream &output,
                               const size_t bins = 10) const;
  void dump_marginals_in_text(std::ostream &text_output) const;

  inline void update_weight(size_t wid, double stepsize, double gradient) {
    double diff = stepsize * gradient;
    weight_grads[wid] += diff;
    double weight = weight_values[wid];
    switch (opts.regularization) {
      case REG_L2: {
        // bounded approx of 1 - opts.reg_param * stepsize
        weight *= (1.0 / (1.0 + opts.reg_param * stepsize));
        break;
      }
      case REG_L1: {
        weight += opts.reg_param * (weight < 0);
        break;
      }
      default:
        std::abort();
    }
    weight -= diff;
    weight_values[wid] = weight;
  }

 private:
  InferenceResult(const FactorGraph &fg, const CmdParser &opts);
};

}  // namespace dd

#endif  // DIMMWITTED_INFERENCE_RESULT_H_
