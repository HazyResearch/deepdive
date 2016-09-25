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

  // Vanilla / traditional Gibbs chain
  // NOT used for inference (traditional "Gibbs sampling")
  // NOT used for inference (traditional "Gibbs sampling")
  // NOT used for inference (traditional "Gibbs sampling")
  // GUESS WHAT'S USED INSTEAD? assignments_evid
  // GIBBS IS WEAK FOR CORRELATIONS / MULTI-VAR FACTORS,
  // BUT IS THAT THE BEST WAY TO ADDRESS THE WEAKNESS?
  std::unique_ptr<size_t[]> assignments_free;

  // separate Gibbs chain CONDITIONED ON EVIDENCE
  // used for inference (traditional "Gibbs sampling")
  // withtout this, test/partial_observation would fail
  // TODO: HAVING BOTH assignments_free AND assignments_evid
  // IS VERY VERY CONFUSING AND UNNERVING. WE HAVE TO CLEAN
  // THEM UP. WE HAVE TO MORE PRECISELY DEFINE "evidence",
  // "observation", "training data" etc.
  // THIS IS GETTING PARTICULARLY MESSY AS WE MAKE
  // LEARNING "NOISE-AWARE".
  // MAYBE test/partial_observation SHOULD BE CONSIDERED PATHOLOGICAL
  // AND NOT REPRESENTATIVE OF REAL-WORLD APPLICATIONS.
  std::unique_ptr<size_t[]> assignments_evid;

  std::unique_ptr<double[]> weight_values;  // array of weight values
  std::unique_ptr<bool[]> weights_isfixed;  // array of whether weight is fixed

  InferenceResult(const FactorGraph &fg, const Weight weights[],
                  const CmdParser &opts);

  // copy constructor
  InferenceResult(const InferenceResult &other);

  void merge_weights_from(const InferenceResult &other);
  void average_weights(size_t count);
  void copy_weights_to(InferenceResult &other) const;
  void show_weights_snippet(std::ostream &output) const;
  void dump_weights_in_text(std::ostream &text_output) const;

  void clear_variabletally();
  void aggregate_marginals_from(const InferenceResult &other);
  void show_marginal_snippet(std::ostream &output) const;
  void show_marginal_histogram(std::ostream &output) const;
  void dump_marginals_in_text(std::ostream &text_output) const;

  inline void update_weight(size_t wid, double stepsize, double gradient) {
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
    weight -= stepsize * gradient;
    weight_values[wid] = weight;
  }

 private:
  InferenceResult(const FactorGraph &fg, const CmdParser &opts);
};

}  // namespace dd

#endif  // DIMMWITTED_INFERENCE_RESULT_H_
