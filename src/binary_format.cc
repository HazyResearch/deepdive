/**
 * This file contains binary formatting methods for FactorGraphs and
 * CompactFactorGraphs. We think this file is a good place to put the
 * definitions of these methods as it conveys the intentions quite clearly
 * (i.e. for binary formatting of these objects).
 */
#include "binary_format.h"
#include "common.h"
#include "factor.h"
#include "factor_graph.h"
#include "variable.h"
#include <cstdint>
#include <fstream>
#include <iostream>

namespace dd {

// Read meta data file, return Meta struct
FactorGraphDescriptor read_meta(const std::string &meta_file) {
  FactorGraphDescriptor meta;
  std::ifstream file;
  file.open(meta_file.c_str());
  std::string buf;
  getline(file, buf, ',');
  meta.num_weights = atoll(buf.c_str());
  getline(file, buf, ',');
  meta.num_variables = atoll(buf.c_str());
  getline(file, buf, ',');
  meta.num_factors = atoll(buf.c_str());
  getline(file, buf, ',');
  meta.num_edges = atoll(buf.c_str());
  file.close();
  return meta;
}

void FactorGraph::load_weights(const std::string &filename) {
  std::ifstream file;
  file.open(filename, std::ios::in | std::ios::binary);
  num_weights_t count = 0;
  while (file.good()) {
    // read fields
    weight_id_t wid;
    uint8_t isfixed;
    weight_value_t initial_value;
    read_be(file, wid);
    read_be(file, isfixed);
    if (!read_be(file, initial_value)) break;

    // load into factor graph
    weights[wid] = Weight(wid, initial_value, isfixed);
    ++count;
  }
  size.num_weights += count;
  file.close();
}

void FactorGraph::load_variables(const std::string &filename) {
  std::ifstream file;
  file.open(filename, std::ios::in | std::ios::binary);
  num_variables_t count = 0;
  while (file.good()) {
    variable_id_t vid;
    uint8_t role_serialized;
    variable_value_t initial_value;
    uint16_t
        dtype_serialized;  // TODO shouldn't this come before the actual value?
    num_variable_values_t cardinality;
    // read fields
    read_be(file, vid);
    read_be(file, role_serialized);
    read_be(file, initial_value);
    read_be(file, dtype_serialized);
    if (!read_be(file, cardinality)) break;

    ++count;
    variable_domain_type_t dtype;
    switch (dtype_serialized) {
      case 0:
        dtype = DTYPE_BOOLEAN;
        break;
      case 1:
        dtype = DTYPE_CATEGORICAL;
        break;
      default:
        std::cerr
            << "[ERROR] Only Boolean and Categorical variables are supported "
               "now!"
            << std::endl;
        std::abort();
    }
    bool is_evidence = role_serialized >=
                       1;  // TODO interpret as bit vector instead of number?
    bool is_observation = role_serialized == 2;
    variable_value_t init_value = is_evidence ? initial_value : 0;

    variables[vid] = RawVariable(vid, dtype, is_evidence, cardinality,
                                 init_value, init_value, is_observation);

    ++size.num_variables;
    if (is_evidence) {
      ++size.num_variables_evidence;
    } else {
      ++size.num_variables_query;
    }
  }
  file.close();
}

void FactorGraph::load_factors(const std::string &filename) {
  std::ifstream file;
  file.open(filename.c_str(), std::ios::in | std::ios::binary);
  while (file.good()) {
    uint16_t type;
    factor_arity_t arity;
    // read fields
    read_be(file, type);
    if (!read_be(file, arity)) break;

    factors[size.num_factors] =
        RawFactor(size.num_factors, -1, (factor_function_type_t)type, arity);

    for (factor_arity_t position = 0; position < arity; ++position) {
      // read fields for each variable reference
      variable_id_t variable_id;
      variable_value_t should_equal_to;
      read_be(file, variable_id);
      read_be(file, should_equal_to);

      // check for wrong id
      assert(variable_id < capacity.num_variables && variable_id >= 0);

      // add variables to factors
      factors[size.num_factors].add_variable_in_factor(
          VariableInFactor(variable_id, position, should_equal_to));
      variables[variable_id].add_factor_id(size.num_factors);
    }
    size.num_edges += arity;

    switch (type) {
      case FUNC_AND_CATEGORICAL: {
        // weight references for categorical factors
        uint32_t n_weights = 0;  // FIXME factor_weight_key_t
        read_be(file, n_weights);
        factors[size.num_factors].weight_ids =
            new std::unordered_map<factor_weight_key_t, weight_id_t>(n_weights);
        for (factor_weight_key_t i = 0; i < n_weights; ++i) {
          // calculate radix-based key into weight_ids (see also
          // FactorGraph::get_categorical_weight_id)
          // TODO: refactor the above formula into a shared routine. (See also
          // FactorGraph::get_categorical_weight_id)
          factor_weight_key_t key = 0;
          for (factor_arity_t j = 0; j < arity; ++j) {
            const Variable &var =
                variables[factors[size.num_factors].tmp_variables.at(j).vid];
            variable_value_t value_id;
            read_be(file, value_id);
            key *= var.cardinality;
            key += var.get_domain_index(value_id);
          }
          weight_id_t wid;
          read_be(file, wid);
          (*factors[size.num_factors].weight_ids)[key] = wid;
        }
        break;
      }

      default:
        // weight reference is simple for Boolean factors
        weight_id_t wid;
        read_be(file, wid);
        factors[size.num_factors].weight_id = wid;
    }

    ++size.num_factors;
  }
  file.close();
}

void FactorGraph::load_domains(const std::string &filename) {
  std::ifstream file;
  file.open(filename.c_str(), std::ios::in | std::ios::binary);

  while (file.good()) {
    variable_id_t vid;
    read_be(file, vid);
    if (!file.good()) {
      return;
    }
    RawVariable &variable = variables[vid];

    num_variable_values_t domain_size;
    read_be(file, domain_size);
    assert(variable.cardinality == domain_size);

    std::vector<variable_value_t> domain_list(domain_size);
    variable.domain_map.reset(
        new std::unordered_map<variable_value_t, variable_value_index_t>());

    for (variable_value_index_t i = 0; i < domain_size; ++i) {
      variable_value_t value;
      read_be(file, value);
      domain_list[i] = value;
    }

    std::sort(domain_list.begin(), domain_list.end());
    for (variable_value_index_t i = 0; i < domain_size; ++i) {
      (*variable.domain_map)[domain_list[i]] = i;
    }

    // adjust the initial assignments to a valid one instead of zero for query
    // variables
    if (!variable.is_evid) {
      variable.assignment_free = variable.assignment_evid = domain_list[0];
    }
  }
}

}  // namespace dd
