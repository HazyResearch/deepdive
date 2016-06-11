#ifndef DIMMWITTED_FACTOR_GRAPH_H_
#define DIMMWITTED_FACTOR_GRAPH_H_

#include "cmd_parser.h"
#include "common.h"

#include "variable.h"
#include "factor.h"
#include "weight.h"
#include "inference_result.h"

#include <xmmintrin.h>

namespace dd {

class FactorGraph;
class CompiledFactorGraph;

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

  // variables, factors, weights
  std::unique_ptr<RawVariable[]> variables;
  std::unique_ptr<RawFactor[]> factors;
  std::unique_ptr<Weight[]> weights;

  /**
   * Constructs a new factor graph with given number number of variables,
   * factors, weights, and edges
   */
  FactorGraph(const FactorGraphDescriptor& capacity);

  /**
   * Loads the corresponding array of objects (weights, variables, factors,
   * domains) into the factor graph.
   *
   * From some old documentation, seems like the proper way to do this is:
   *   1. read variables
   *   2. read weights
   *   3. sort variables and weights by id
   *   4. read factors
   *
   * Where Step 3 seems to be obsolete, since we're reading in variables and
   * weights that are in order.
   */
  void load_weights(const std::string filename);
  void load_variables(const std::string filename);
  void load_factors(const std::string filename);
  void load_domains(const std::string filename);

  /**
   * Compiles the factor graph into a format that's more appropriate for
   * inference and learning.
   *
   * Since the original factor graph initializes the new factor graph,
   * it also has to transfer the variable, factor, and weight counts,
   * and other statistics as well.
   */
  void compile(CompiledFactorGraph& cfg);

  /**
   * Checks whether the edge-based store is correct
   */
  void safety_check();
};

inline std::ostream& operator<<(std::ostream& out, FactorGraph const& fg) {
  out << "FactorGraph(";
  out << fg.size << " / " << fg.capacity;
  out << ")";
  return out;
}

class CompiledFactorGraph {
 public:
  FactorGraphDescriptor size;

  std::unique_ptr<Variable[]> variables;
  std::unique_ptr<Factor[]> factors;

  // For each edge, we store the factor, weight id, factor id, and the variable,
  // in the same index of seperate arrays. The edges are ordered so that the
  // edges for a variable is in a continuous region (sequentially).
  // This allows us to access factors given variables, and access variables
  // given factors faster.
  std::unique_ptr<CompactFactor[]> compact_factors;
  std::unique_ptr<int[]> compact_factors_weightids;
  std::unique_ptr<long[]> factor_ids;
  std::unique_ptr<VariableInFactor[]> vifs;

  // pointer to inference result
  std::unique_ptr<InferenceResult> infrs;

  CompiledFactorGraph();

  /* Produces an empty factor graph to be initialized by resume() */
  CompiledFactorGraph(const FactorGraphDescriptor& size);

  // copy constructor
  CompiledFactorGraph(const CompiledFactorGraph& other);

  /*
   * Given a factor and variable assignment, returns corresponding multinomial
   * factor weight id, using proposal value for the variable with id vid.
   * For multinomial weights, for each possible assignment, there is a
   * corresponding
   * indicator function and weight.
   */
  long get_multinomial_weight_id(const VariableValue assignments[],
                                 const CompactFactor& fs, long vid,
                                 long proposal);

  /**
   * Given a variable, updates the weights associated with the factors that
   * connect to the variable.
   * Used in learning phase, after sampling one variable,
   * update corresponding weights (stochastic gradient descent).
   */
  void update_weight(const Variable& variable, InferenceResult& infrs,
                     double stepsize);

  /**
   * Returns potential of the given factor
   *
   * does_change_evid = true, use the free assignment. Otherwise, use the
   * evid assignement.
   */
  inline double potential(const CompactFactor& factor,
                          const VariableValue assignments[]);

  /**
   * Returns log-linear weighted potential of the all factors for the given
   * variable using the propsal value.
   *
   * does_change_evid = true, use the free assignment. Otherwise, use the
   * evid assignement.
   */
  inline double potential(const Variable& variable, const double& proposal,
                          const VariableValue assignments[]);
};

inline double CompiledFactorGraph::potential(
    const CompactFactor& factor, const VariableValue assignments[]) {
  return factor.potential(vifs.get(), assignments, -1, -1);
}

inline double CompiledFactorGraph::potential(
    const Variable& variable, const double& proposal,
    const VariableValue assignments[]) {
  // potential
  double pot = 0.0;
  // pointer to the first factor the given variable connects to
  // the factors that the given variable connects to are stored in a continuous
  // region of the array
  CompactFactor* const fs = &compact_factors[variable.n_start_i_factors];
  switch (variable.domain_type) {
    case DTYPE_BOOLEAN: {
      // the weights, corresponding to the factor with the same index
      const int* const ws =
          &compact_factors_weightids[variable.n_start_i_factors];
      // for all factors that the variable connects to, calculate the
      // weighted potential
      for (long i = 0; i < variable.n_factors; ++i) {
        long wid = ws[i];
        pot += infrs->weight_values[wid] *
               fs[i].potential(vifs.get(), assignments, variable.id, proposal);
      }
      break;
    }
    case DTYPE_MULTINOMIAL: {
      for (long i = 0; i < variable.n_factors; ++i) {
        // get weight id associated with this factor and variable
        // assignment
        long wid = get_multinomial_weight_id(assignments, fs[i], variable.id,
                                             proposal);
        if (wid == -1) continue;
        pot += infrs->weight_values[wid] *
               fs[i].potential(vifs.get(), assignments, variable.id, proposal);
      }
      break;
    }
    default:
      abort();
  }
  return pot;
}

// sort variable in factor by their position
bool compare_position(const VariableInFactor& x, const VariableInFactor& y);

}  // namespace dd

#endif  // DIMMWITTED_FACTOR_GRAPH_H_
