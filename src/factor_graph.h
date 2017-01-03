#ifndef DIMMWITTED_FACTOR_GRAPH_H_
#define DIMMWITTED_FACTOR_GRAPH_H_

#include "cmd_parser.h"
#include "common.h"

#include "factor.h"
#include "inference_result.h"
#include "variable.h"
#include "weight.h"

#include <xmmintrin.h>

namespace dd {

/** Meta data describing the dimension of a factor graph */
class FactorGraphDescriptor {
 public:
  FactorGraphDescriptor();

  FactorGraphDescriptor(size_t num_variables, size_t num_factors,
                        size_t num_weights, size_t num_edges);

  /** number of all variables */
  size_t num_variables;

  /** number of factors */
  size_t num_factors;
  /** number of edges */
  size_t num_edges;

  /** number of all weights */
  size_t num_weights;

  /** number of all values; populated in FactorGraph.construct_index */
  size_t num_values;

  /** number of evidence variables */
  size_t num_variables_evidence;
  /** number of query variables */
  size_t num_variables_query;
};

std::ostream& operator<<(std::ostream& stream,
                         const FactorGraphDescriptor& size);

/**
 * Class for a factor graph
 */
class FactorGraph {
 public:
  /** Capacity to allow multi-step loading */
  FactorGraphDescriptor capacity;
  /** Actual count of things */
  FactorGraphDescriptor size;

  // distinct weights
  std::unique_ptr<Weight[]> weights;

  // factors and each factor's variables (with "equal_to" values)
  std::unique_ptr<Factor[]> factors;
  std::unique_ptr<FactorToVariable[]> vifs;

  // variables, each variable's values, and index into factor IDs
  // factor_index follows sort order of adjacent <var, val>
  // |factor_index| may be smaller than |edges| because we deduplicate (see
  // construct_index)
  std::unique_ptr<Variable[]> variables;
  std::unique_ptr<size_t[]> factor_index;
  std::unique_ptr<VariableToFactor[]> values;

  void load_weights(const std::vector<std::string>& filenames);
  void load_variables(const std::vector<std::string>& filenames);
  void load_factors(const std::vector<std::string>& filenames);
  void load_domains(const std::vector<std::string>& filenames);

  // count data structures to ensure consistency with declared size
  void safety_check();
  // construct "values" and "factor_index" for var-to-factor lookups
  void construct_index();

  void construct_index_part(size_t v_start, size_t v_end, size_t val_base,
                            size_t fac_base);

  inline size_t get_var_value_at(const Variable& var, size_t idx) const {
    return values[var.var_val_base + idx].value;
  }

  inline const FactorToVariable& get_factor_vif_at(const Factor& factor,
                                                   size_t idx) const {
    return vifs[factor.vif_base + idx];
  }

  /**
   * Constructs a new factor graph with given number number of variables,
   * factors, weights, and edges
   */
  FactorGraph(const FactorGraphDescriptor& capacity);

  // copy constructor
  FactorGraph(const FactorGraph& other);

  ~FactorGraph();

  /**
   * Given a variable, updates the weights associated with the factors that
   * connect to the variable.
   * Used in learning phase, after sampling one variable,
   * update corresponding weights (stochastic gradient descent).
   */
  void sgd_on_variable(const Variable& variable, InferenceResult& infrs,
                       double stepsize, bool is_noise_aware);

  // perform SGD step for weight learning on one factor
  inline void sgd_on_factor(size_t factor_id, double stepsize, size_t vid,
                            size_t evidence_value, InferenceResult& infrs);

  /**
   * Returns log-linear weighted potential of the all factors for the given
   * variable using the propsal value.
   */
  inline double potential(const Variable& variable, const size_t proposal,
                          const size_t assignments[],
                          const double weight_values[]);
};

inline double FactorGraph::potential(const Variable& variable, size_t proposal,
                                     const size_t assignments[],
                                     const double weight_values[]) {
  double pot = 0.0;

  VariableToFactor* const var_value_base = &values[variable.var_val_base];
  size_t offset = variable.var_value_offset(proposal);
  const VariableToFactor& var_value = *(var_value_base + offset);

  // all adjacent factors in one chunk in factor_index
  for (size_t i = 0; i < var_value.factor_index_length; ++i) {
    size_t factor_id = factor_index[var_value.factor_index_base + i];
    const Factor& factor = factors[factor_id];
    double weight = weight_values[factor.weight_id];
    pot += weight *
           factor.potential(vifs.get(), assignments, variable.id, proposal);
  }
  return pot;
}

inline std::ostream& operator<<(std::ostream& out, FactorGraph const& fg) {
  out << "FactorGraph(";
  out << fg.size;
  out << ")";
  return out;
}

}  // namespace dd

#endif  // DIMMWITTED_FACTOR_GRAPH_H_
