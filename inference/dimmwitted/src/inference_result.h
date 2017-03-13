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

  // number of factors for each weight
  // used to amortize regularization term
  std::unique_ptr<size_t[]> weight_freqs;

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
    weight_grads[wid] += gradient;
    double weight = weight_values[wid];
    size_t total_factors = weight_freqs[wid];

    // apply l1 or l2 regularization
    switch (opts.regularization) {
      case REG_L2: {
        weight *= (1.0 - opts.reg_param * stepsize / total_factors);
        break;
      }
      case REG_L1: {
        if (weight > 0) {
          weight =
              std::max(0.0, weight - opts.reg_param * stepsize / total_factors);
        } else if (weight < 0) {
          weight =
              std::min(0.0, weight + opts.reg_param * stepsize / total_factors);
        }
        break;
      }
      default:
        std::abort();
    }

    // apply gradient
    weight -= stepsize * gradient;

    // clamp
    if (weight > 10)
      weight = 10;
    else if (weight < -10)
      weight = -10;

    weight_values[wid] = weight;
  }

 private:
  InferenceResult(const FactorGraph &fg, const CmdParser &opts);
};

}  // namespace dd

#endif  // DIMMWITTED_INFERENCE_RESULT_H_
