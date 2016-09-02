#include "factor_graph.h"
#include "binary_format.h"
#include "factor.h"

#include <iostream>
#include <algorithm>

namespace dd {

FactorGraphDescriptor::FactorGraphDescriptor()
    : FactorGraphDescriptor(0, 0, 0, 0) {}

FactorGraphDescriptor::FactorGraphDescriptor(size_t n_var, size_t n_fac,
                                             size_t n_wgt, size_t n_edg)
    : num_variables(n_var),
      num_factors(n_fac),
      num_edges(n_edg),
      num_weights(n_wgt),
      num_values(0),
      num_variables_evidence(0),
      num_variables_query(0) {}

std::ostream &operator<<(std::ostream &stream,
                         const FactorGraphDescriptor &size) {
  stream << "#V=" << size.num_variables;
  if (size.num_variables_query + size.num_variables_evidence > 0) {
    stream << "(";
    stream << "#Vqry=" << size.num_variables_query;
    stream << " ";
    stream << "#Vevd=" << size.num_variables_evidence;
    stream << ")";
  }
  stream << " "
         << "#F=" << size.num_factors;
  stream << " "
         << "#W=" << size.num_weights;
  stream << " "
         << "#E=" << size.num_edges;
  stream << " "
         << "#Val=" << size.num_values;
  return stream;
}

FactorGraph::FactorGraph(const FactorGraphDescriptor &capacity)
    : capacity(capacity),
      size(),
      weights(new Weight[capacity.num_weights]),
      factors(new Factor[capacity.num_factors]),
      vifs(new VariableInFactor[capacity.num_edges]),
      variables(new Variable[capacity.num_variables]),
      factor_index(new size_t[capacity.num_edges]),
      values(new VariableValue[capacity.num_values]) {}

void FactorGraph::construct_index() {
  size_t num_values = 0;
  for (size_t i = 0; i < size.num_variables; ++i) {
    num_values += variables[i].internal_cardinality();
  }
  size.num_values = capacity.num_values = num_values;

  values.reset(new VariableValue[num_values]);

  size_t factor_index_base = 0, value_index_base = 0;
  std::vector<size_t> value_list;

  // For each variable, sort and uniq adjacent factors by value.
  // We deallocate "domain_map" and "adjacent_factors".
  for (size_t i = 0; i < size.num_variables; ++i) {
    Variable &v = variables[i];
    v.var_val_base = value_index_base;

    if (v.is_boolean()) {
      values[value_index_base] =
          VariableValue(Variable::BOOLEAN_DENSE_VALUE, 0, 0);
      ++value_index_base;
    } else {
      if (v.domain_map) {
        // explicitly listed domain values; recover the list from map.
        value_list.assign(v.cardinality, Variable::INVALID_VALUE);
        for (const auto &item : *v.domain_map) {
          value_list.at(item.second) = item.first;
        }
        for (size_t j = 0; j < v.cardinality; ++j) {
          values[value_index_base] = VariableValue(value_list[j], 0, 0);
          ++value_index_base;
        }
        v.domain_map.reset();  // reclaim memory
      } else {
        // implicit [0...(cardinality-1)] domain values
        for (size_t j = 0; j < v.cardinality; ++j) {
          values[value_index_base] = VariableValue(j, 0, 0);
          ++value_index_base;
        }
      }
    }

    if (v.adjacent_factors) {
      // sort by <value, fid>
      std::sort(v.adjacent_factors->begin(), v.adjacent_factors->end());
      size_t value_dense = Variable::INVALID_VALUE;
      size_t last_factor_id = Factor::INVALID_ID;
      for (const auto &item : *v.adjacent_factors) {
        if (item.value_dense != value_dense) {
          value_dense = item.value_dense;
          assert(value_dense < v.cardinality);
          values[v.var_val_base + value_dense].factor_index_base =
              factor_index_base;
        } else if (item.factor_id == last_factor_id) {
          continue;  // dedupe
        }
        factor_index[factor_index_base] = item.factor_id;
        ++factor_index_base;
        ++values[v.var_val_base + value_dense].factor_index_length;
        last_factor_id = item.factor_id;
      }
      v.adjacent_factors.reset();  // reclaim memory
    }
  }
}

void FactorGraph::safety_check() {
  // check if any space is wasted
  assert(capacity.num_variables == size.num_variables);
  assert(capacity.num_factors == size.num_factors);
  assert(capacity.num_edges == size.num_edges);
  assert(capacity.num_weights == size.num_weights);

  // check whether variables, factors, and weights are stored
  // in the order of their id
  for (size_t i = 0; i < size.num_variables; ++i) {
    assert(this->variables[i].id == i);
  }
  for (size_t i = 0; i < size.num_factors; ++i) {
    assert(this->factors[i].id == i);
  }
  for (size_t i = 0; i < size.num_weights; ++i) {
    assert(this->weights[i].id == i);
  }
}

FactorGraph::FactorGraph(const FactorGraph &other)
    : FactorGraph(other.capacity) {
  size = other.size;
  // copy each member from the given graph
  COPY_ARRAY_UNIQUE_PTR_MEMBER(variables, size.num_variables);
  COPY_ARRAY_UNIQUE_PTR_MEMBER(factors, size.num_factors);
  COPY_ARRAY_UNIQUE_PTR_MEMBER(factor_index, size.num_edges);
  COPY_ARRAY_UNIQUE_PTR_MEMBER(vifs, size.num_edges);
  COPY_ARRAY_UNIQUE_PTR_MEMBER(weights, size.num_weights);
  COPY_ARRAY_UNIQUE_PTR_MEMBER(values, size.num_values);
}

// Inline by defined here; accessible only from current file.
inline void FactorGraph::sgd_on_factor(size_t factor_id, double stepsize,
                                       InferenceResult &infrs) {
  const Factor &factor = factors[factor_id];
  if (infrs.weights_isfixed[factor.weight_id]) {
    return;
  }
  // stochastic gradient ascent
  // decrement weight with stepsize * gradient of weight
  // gradient of weight = E[f] - E[f|D], where D is evidence variables,
  // f is the factor function, E[] is expectation. Expectation is
  // calculated using a sample of the variable.
  double pot_evid = factor.potential(vifs.get(), infrs.assignments_evid.get());
  double pot_free = factor.potential(vifs.get(), infrs.assignments_free.get());
  double gradient = pot_free - pot_evid;
  infrs.update_weight(factor.weight_id, stepsize, gradient);
}

void FactorGraph::update_weight(const Variable &variable,
                                InferenceResult &infrs, double stepsize) {
  // Look up the var-value-factor index to find adjacent factors
  // to this var and the evid|free values
  VariableValue *const var_value_base = &values[variable.var_val_base];
  size_t offset_evid =
      variable.var_value_offset(infrs.assignments_evid[variable.id]);
  size_t offset_free =
      variable.var_value_offset(infrs.assignments_free[variable.id]);
  const VariableValue &var_value_evid = *(var_value_base + offset_evid);
  const VariableValue &var_value_free = *(var_value_base + offset_free);

  if (offset_free == offset_evid) {
    // all adjacent factors in one chunk in factor_index
    for (size_t i = 0; i < var_value_evid.factor_index_length; ++i) {
      size_t factor_id = factor_index[var_value_evid.factor_index_base + i];
      sgd_on_factor(factor_id, stepsize, infrs);
    }
  } else {
    // adjacent factors appear in two chunks. merge and dedupe them.
    size_t factor_id, last_factor_id = Factor::INVALID_ID;
    size_t i = 0, j = 0;
    while (i < var_value_evid.factor_index_length &&
           j < var_value_free.factor_index_length) {
      factor_id = factor_index[var_value_evid.factor_index_base + i];
      if (factor_id > factor_index[var_value_free.factor_index_base + j]) {
        factor_id = factor_index[var_value_free.factor_index_base + j];
        ++j;
      } else {
        ++i;
      }
      if (factor_id != last_factor_id) {
        sgd_on_factor(factor_id, stepsize, infrs);
        last_factor_id = factor_id;
      }
    }
    while (i < var_value_evid.factor_index_length) {
      factor_id = factor_index[var_value_evid.factor_index_base + i];
      if (factor_id != last_factor_id) {
        sgd_on_factor(factor_id, stepsize, infrs);
      }
      ++i;
    }
    while (j < var_value_free.factor_index_length) {
      factor_id = factor_index[var_value_free.factor_index_base + j];
      if (factor_id != last_factor_id) {
        sgd_on_factor(factor_id, stepsize, infrs);
      }
      ++j;
    }
  }
}

}  // namespace dd
