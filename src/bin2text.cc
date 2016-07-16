/*
 * Transform a TSV format factor graph file and output corresponding binary
 * format used in DeepDive
 */

#include "bin2text.h"
#include "text2bin.h"
#include "binary_format.h"
#include "dimmwitted.h"
#include "factor_graph.h"

#include <assert.h>
#include <fstream>
#include <iostream>
#include <map>
#include <sstream>
#include <stdint.h>
#include <stdlib.h>
#include <vector>

namespace dd {

void dump_variables(const FactorGraph &fg, const std::string &filename) {
  std::ofstream fout(filename);
  for (variable_id_t i = 0; i < fg.size.num_variables; ++i) {
    Variable &v = fg.variables[i];
    // variable id
    fout << v.id;
    // is_evidence
    fout << text_field_delim << (v.is_evid ? "1" : "0");
    // value
    fout << text_field_delim << v.assignment_evid;
    // data type
    std::string type_str;
    switch (v.domain_type) {
      case DTYPE_BOOLEAN:
        type_str = "0";
        break;
      case DTYPE_CATEGORICAL:
        type_str = "1";
        break;
      default:
        std::abort();
    }
    fout << text_field_delim << type_str;
    // cardinality
    fout << text_field_delim << v.cardinality;
    fout << std::endl;
  }
}

void dump_domains(const FactorGraph &fg, const std::string &filename) {
  std::ofstream fout(filename);
  for (variable_id_t i = 0; i < fg.size.num_variables; ++i) {
    Variable &v = fg.variables[i];
    if (v.domain_map) {
      fout << v.id;
      fout << text_field_delim << v.domain_map->size();
      fout << text_field_delim;
      fout << "{";
      v.get_domain_value_at(0);  // trigger construction of domain_list
      variable_value_t j = 0;
      for (const auto &value : *v.domain_list) {
        if (j > 0) fout << ",";
        fout << value;
        ++j;
      }
      fout << "}";
      fout << std::endl;
    }
  }
}

void dump_factors(const FactorGraph &fg, const std::string &filename) {
  std::ofstream fout(filename);
  std::vector<variable_value_t> vals;
  std::vector<weight_id_t> weights;
  std::vector<factor_value_t> factor_values;
  std::string element;
  for (factor_id_t i = 0; i < fg.size.num_factors; ++i) {
    const auto &f = fg.factors[i];
    // FIXME this output is lossy since it drops the f.func_id and
    // f.tmp_variables[*].is_positive
    // variable ids the factor is defined over
    for (const auto &v : f.tmp_variables) {
      fout << v.vid;
      fout << text_field_delim;
    }
    switch (f.func_id) {
      case FUNC_AND_CATEGORICAL: {
        // Format: NUM_WEIGHTS [VAR1 VAL ID] [VAR2 VAL ID] ... [WEIGHT ID]
        // transpose tuples; sort to ensure consistency
        vals.clear();
        weights.clear();
        factor_values.clear();
        vals.reserve(f.n_variables * f.factor_params->size());
        weights.reserve(f.factor_params->size());
        factor_values.reserve(f.factor_params->size());
        std::map<factor_weight_key_t, FactorParams> ordered(
            f.factor_params->begin(), f.factor_params->end());
        factor_weight_key_t w = 0;
        for (const auto &item : ordered) {
          factor_weight_key_t key = item.first;
          weight_id_t wid = item.second.wid;
          factor_value_t fval = item.second.value;
          for (factor_arity_t k = f.n_variables; k > 0;) {
            --k;  // turning it into a correct index
            variable_id_t vid = f.tmp_variables.at(k).vid;
            Variable &var = fg.variables[vid];
            variable_value_t val_idx = key % var.cardinality;
            variable_value_t val = var.get_domain_value_at(val_idx);
            key = key / var.cardinality;
            vals[w * f.n_variables + k] = val;
          }
          weights[w] = wid;
          factor_values[w] = fval;
          ++w;
        }
        // output num_weights
        fout << f.factor_params->size();
        fout << text_field_delim;
        // output values per var
        for (factor_arity_t k = 0; k < f.n_variables; ++k) {
          fout << "{";
          for (factor_weight_key_t j = 0; j < f.factor_params->size(); ++j) {
            if (j > 0) fout << ",";
            fout << vals[j * f.n_variables + k];
          }
          fout << "}";
          fout << text_field_delim;
        }
        // output weights
        fout << "{";
        factor_weight_key_t j = 0;
        for (weight_id_t wid : weights) {
          if (j > 0) fout << ",";
          fout << wid;
          ++j;
        }
        fout << "}";
        fout << text_field_delim;
        // output factor values
        fout << "{";
        j = 0;
        for (factor_value_t fval : factor_values) {
          if (j > 0) fout << ",";
          fout << fval;
          ++j;
        }
        fout << "}";
        break;
      }
      default:
        // followed by a weight id
        fout << f.weight_id;
        fout << text_field_delim;
        fout << f.value;
    }
    fout << std::endl;
  }
}

void dump_weights(const FactorGraph &fg, const std::string &filename) {
  std::ofstream fout(filename);
  for (weight_id_t i = 0; i < fg.size.num_weights; ++i) {
    Weight &w = fg.weights[i];
    // weight id
    fout << w.id;
    // whether it's fixed
    fout << text_field_delim << w.isfixed;
    // weight value
    fout << text_field_delim << w.weight;
    fout << std::endl;
  }
}

void dump_meta(const FactorGraph &fg, const std::string &filename) {
  std::ofstream fout(filename);
  fout << fg.size.num_weights;
  fout << "," << fg.size.num_variables;
  fout << "," << fg.size.num_factors;
  fout << "," << fg.size.num_edges;
  // XXX dummy file names
  fout << ","
       << "graph.weights";
  fout << ","
       << "graph.variables";
  fout << ","
       << "graph.factors";
  fout << std::endl;
}

void dump_factorgraph(const FactorGraph &fg, const std::string &output_dir) {
  dump_variables(fg, output_dir + "/variables.tsv");
  dump_domains(fg, output_dir + "/domains.tsv");
  dump_factors(fg, output_dir + "/factors.tsv");
  dump_weights(fg, output_dir + "/weights.tsv");
  dump_meta(fg, output_dir + "/graph.meta");
}

int bin2text(const CmdParser &cmd_parser) {
  FactorGraphDescriptor meta = read_meta(cmd_parser.fg_file);
  // load factor graph
  FactorGraph fg(meta);
  fg.load_variables(cmd_parser.variable_file);
  fg.load_weights(cmd_parser.weight_file);
  fg.load_domains(cmd_parser.domain_file);
  fg.load_factors(cmd_parser.factor_file);
  fg.safety_check();

  dump_factorgraph(fg, cmd_parser.output_folder);

  return 0;
}

}  // namespace dd
