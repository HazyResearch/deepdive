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
#include <algorithm>

namespace dd {

// Read meta data file, return Meta struct
FactorGraphDescriptor read_meta(const std::string &meta_file) {
  FactorGraphDescriptor meta;
  std::ifstream file(meta_file);
  std::string buf;
  getline(file, buf, ',');
  meta.num_weights = atoll(buf.c_str());
  getline(file, buf, ',');
  meta.num_variables = atoll(buf.c_str());
  getline(file, buf, ',');
  meta.num_factors = atoll(buf.c_str());
  getline(file, buf, ',');
  meta.num_edges = atoll(buf.c_str());
  return meta;
}

void FactorGraph::load_weights(const std::string &filename) {
  std::ifstream file(filename, std::ios::binary);
  num_weights_t count = 0;
  while (file && file.peek() != EOF) {
    // read fields
    weight_id_t wid;
    uint8_t isfixed;
    weight_value_t initial_value;
    read_be_or_die(file, wid);
    read_be_or_die(file, isfixed);
    read_be_or_die(file, initial_value);
    // load into factor graph
    weights[wid] = Weight(wid, initial_value, isfixed);
    ++count;
  }
  size.num_weights += count;
}

void FactorGraph::load_variables(const std::string &filename) {
  std::ifstream file(filename, std::ios::binary);
  num_variables_t count = 0;
  while (file && file.peek() != EOF) {
    variable_id_t vid;
    uint8_t role_serialized;
    variable_value_t initial_value;
    uint16_t
        dtype_serialized;  // TODO shouldn't this come before the actual value?
    num_variable_values_t cardinality;
    // read fields
    read_be_or_die(file, vid);
    read_be_or_die(file, role_serialized);
    read_be_or_die(file, initial_value);
    read_be_or_die(file, dtype_serialized);
    read_be_or_die(file, cardinality);
    // map serialized to internal values
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
}

void FactorGraph::load_factors(const std::string &filename) {
  std::ifstream file(filename, std::ios::binary);
  while (file && file.peek() != EOF) {
    uint16_t type;
    factor_arity_t arity;
    // read fields
    read_be_or_die(file, type);
    read_be_or_die(file, arity);
    // register the factor
    factors[size.num_factors] =
        RawFactor(size.num_factors, Factor::DEFAULT_VALUE, Weight::INVALID_ID,
                  (factor_function_type_t)type, arity);
    for (factor_arity_t position = 0; position < arity; ++position) {
      // read fields for each variable reference
      variable_id_t variable_id;
      variable_value_t should_equal_to;
      read_be_or_die(file, variable_id);
      read_be_or_die(file, should_equal_to);
      assert(variable_id < capacity.num_variables && variable_id >= 0);
      // add to adjacency lists
      factors[size.num_factors].add_variable_in_factor(
          VariableInFactor(variable_id, position, should_equal_to));
      variables[variable_id].add_factor_id(size.num_factors);
    }
    size.num_edges += arity;
    switch (type) {
      case FUNC_AND_CATEGORICAL: {
        // weight references for categorical factors
        factor_weight_key_t n_weights = 0;
        read_be_or_die(file, n_weights);
        factors[size.num_factors].factor_params =
            new std::unordered_map<factor_weight_key_t, FactorParams>(
                n_weights);
        for (factor_weight_key_t i = 0; i < n_weights; ++i) {
          // calculate radix-based key into factor_params (see also
          // FactorGraph::get_categorical_factor_params)
          // TODO: refactor the above formula into a shared routine. (See also
          // FactorGraph::get_categorical_factor_params)
          factor_weight_key_t key = 0;
          for (factor_arity_t j = 0; j < arity; ++j) {
            const Variable &var =
                variables[factors[size.num_factors].tmp_variables.at(j).vid];
            variable_value_t value_id;
            read_be_or_die(file, value_id);
            key *= var.cardinality;
            key += var.get_domain_index(value_id);
          }
          FactorParams fparam;
          read_be_or_die(file, fparam.wid);
          read_be_or_die(file, fparam.value);
          (*factors[size.num_factors].factor_params)[key] = fparam;
        }
        break;
      }

      default:
        // weight reference is simple for Boolean factors
        weight_id_t wid;
        read_be_or_die(file, wid);
        factors[size.num_factors].weight_id = wid;
        factor_value_t val;
        read_be_or_die(file, val);
        factors[size.num_factors].value = val;
    }
    ++size.num_factors;
  }
}

void FactorGraph::load_domains(const std::string &filename) {
  std::ifstream file(filename, std::ios::binary);
  while (file && file.peek() != EOF) {
    // read field to find which categorical variable this block is for
    variable_id_t vid;
    read_be_or_die(file, vid);
    RawVariable &variable = variables[vid];
    // read all category values for this variables
    num_variable_values_t domain_size;
    read_be_or_die(file, domain_size);
    assert(variable.cardinality == domain_size);
    std::vector<variable_value_t> domain_list(domain_size);
    variable.domain_map.reset(
        new std::unordered_map<variable_value_t, variable_value_index_t>());
    for (variable_value_index_t i = 0; i < domain_size; ++i) {
      variable_value_t value;
      read_be_or_die(file, value);
      domain_list[i] = value;
    }
    // populate the mapping from value to index
    // TODO this indexing may be unnecessary (we can use the variable_value_t
    // directly for computing keys
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
