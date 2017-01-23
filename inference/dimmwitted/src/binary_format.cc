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
#include <thread>
#include <mutex>
#include <atomic>

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

inline void parallel_load(
    const std::vector<std::string> &filenames,
    std::function<void(const std::string &filename)> loader) {
  std::vector<std::thread> threads;
  for (const auto &filename : filenames)
    threads.push_back(std::thread(loader, filename));
  for (auto &t : threads) t.join();
  threads.clear();
}

void FactorGraph::load_weights(const std::vector<std::string> &filenames) {
  std::mutex mtx;
  parallel_load(filenames, [this, &mtx](const std::string &filename) {
    std::ifstream file(filename, std::ios::binary);
    size_t count = 0;
    while (file && file.peek() != EOF) {
      // read fields
      size_t wid;
      uint8_t isfixed;
      double initial_value;
      read_be_or_die(file, wid);
      read_be_or_die(file, isfixed);
      read_be_or_die(file, initial_value);
      // load into factor graph
      weights[wid] = Weight(wid, initial_value, isfixed);
      ++count;
    }
    {  // commit counts
      std::lock_guard<std::mutex> lck(mtx);
      size.num_weights += count;
    }
  });
}

void FactorGraph::load_variables(const std::vector<std::string> &filenames) {
  std::mutex mtx;
  parallel_load(filenames, [this, &mtx](const std::string &filename) {
    std::ifstream file(filename, std::ios::binary);
    size_t num_variables = 0;
    size_t num_variables_evidence = 0;
    size_t num_variables_query = 0;
    while (file && file.peek() != EOF) {
      size_t vid;
      uint8_t role_serialized;
      size_t initial_value;
      uint16_t dtype_serialized;
      size_t cardinality;
      // read fields
      read_be_or_die(file, vid);
      read_be_or_die(file, role_serialized);
      read_be_or_die(file, initial_value);
      read_be_or_die(file, dtype_serialized);
      read_be_or_die(file, cardinality);
      // map serialized to internal values
      DOMAIN_TYPE dtype;
      switch (dtype_serialized) {
        case 0:
          dtype = DTYPE_BOOLEAN;
          break;
        case 1:
          dtype = DTYPE_CATEGORICAL;
          break;
        default:
          std::cerr << "[ERROR] Only Boolean and Categorical "
                       "variables are supported "
                       "now!"
                    << std::endl;
          std::abort();
      }
      bool is_evidence = role_serialized >= 1;

      size_t init_value = is_evidence ? initial_value : 0;
      variables[vid] =
          Variable(vid, dtype, is_evidence, cardinality, init_value);
      ++num_variables;
      if (is_evidence) {
        ++num_variables_evidence;
      } else {
        ++num_variables_query;
      }
    }
    {  // commit counts
      std::lock_guard<std::mutex> lck(mtx);
      size.num_variables += num_variables;
      size.num_variables_evidence += num_variables_evidence;
      size.num_variables_query += num_variables_query;
    }
  });
}

void FactorGraph::load_factors(const std::vector<std::string> &filenames) {
  std::mutex mtx;
  // an array of mutexes for serializing accesses to the variables
  std::unique_ptr<std::mutex[]> mtx_variables(
      new std::mutex[size.num_variables]);
  std::atomic<size_t> factor_cntr(0), edge_cntr(0);
  parallel_load(filenames, [this, &mtx, &mtx_variables, &factor_cntr,
                            &edge_cntr](const std::string &filename) {
    std::ifstream file(filename, std::ios::binary);
    size_t num_factors = 0;
    size_t num_edges = 0;
    while (file && file.peek() != EOF) {
      uint16_t type;
      size_t arity;
      // read fields
      read_be_or_die(file, type);
      read_be_or_die(file, arity);
      // register the factor
      size_t idx = factor_cntr.fetch_add(1, std::memory_order_relaxed);
      ++num_factors;
      factors[idx] = Factor(idx, DEFAULT_FEATURE_VALUE, Weight::INVALID_ID,
                            (FACTOR_FUNCTION_TYPE)type, arity);

      size_t edge_idx = edge_cntr.fetch_add(arity, std::memory_order_relaxed);
      num_edges += arity;
      factors[idx].vif_base = edge_idx;
      for (size_t position = 0; position < arity; ++position) {
        // read fields for each variable reference
        size_t variable_id;
        size_t should_equal_to;
        read_be_or_die(file, variable_id);
        read_be_or_die(file, should_equal_to);
        assert(variable_id < capacity.num_variables && variable_id >= 0);
        // convert original var value into dense value
        size_t dense_val =
            variables[variable_id].get_domain_index(should_equal_to);
        vifs[edge_idx] = FactorToVariable(variable_id, dense_val);
        // add to adjacency lists
        if (variables[variable_id].is_boolean()) {
          // normalize boolean var vals to 0 for indexing purpusoes
          dense_val = Variable::BOOLEAN_DENSE_VALUE;
        }
        {
          std::lock_guard<std::mutex> lck(mtx_variables[variable_id]);
          variables[variable_id].add_value_factor(dense_val, idx);
        }
        ++edge_idx;
      }

      size_t wid;
      read_be_or_die(file, wid);
      factors[idx].weight_id = wid;
      double val;
      read_be_or_die(file, val);
      factors[idx].feature_value = val;
    }
    {  // commit counts
      std::lock_guard<std::mutex> lck(mtx);
      size.num_factors += num_factors;
      size.num_edges += num_edges;
    }
  });
}

void FactorGraph::load_domains(const std::vector<std::string> &filenames) {
  parallel_load(filenames, [this](const std::string &filename) {
    std::ifstream file(filename, std::ios::binary);

    size_t value;
    double truthiness;
    while (file && file.peek() != EOF) {
      // read field to find which categorical variable this block is
      // for
      size_t vid;
      read_be_or_die(file, vid);
      Variable &variable = variables[vid];
      // read all category values for this variables
      size_t domain_size;
      read_be_or_die(file, domain_size);

      assert(!variable.is_boolean());
      assert(variable.cardinality == domain_size);

      variable.domain_map.reset(new std::unordered_map<size_t, TempVarValue>());
      for (size_t i = 0; i < domain_size; ++i) {
        read_be_or_die(file, value);
        read_be_or_die(file, truthiness);
        assert(truthiness >= 0 && truthiness <= 1);
        (*variable.domain_map)[value] = {i, truthiness};
      }

      // convert original var value into dense value
      if (variable.assignment_dense) {
        variable.assignment_dense =
            variable.get_domain_index(variable.assignment_dense);
      }
    }
  });
}

}  // namespace dd
