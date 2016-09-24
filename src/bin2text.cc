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
  for (size_t i = 0; i < fg.size.num_variables; ++i) {
    Variable &v = fg.variables[i];
    // variable id
    fout << v.id;
    // is_evidence
    fout << text_field_delim << (v.is_evid ? "1" : "0");
    // value
    fout << text_field_delim << v.assignment_dense;
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
  for (size_t i = 0; i < fg.size.num_variables; ++i) {
    Variable &v = fg.variables[i];
    if (!v.is_boolean()) {
      fout << v.id;
      fout << text_field_delim << v.cardinality;
      fout << text_field_delim;
      fout << "{";
      for (size_t j = 0; j < v.cardinality; j++) {
        if (j > 0) fout << ",";
        fout << fg.get_var_value_at(v, j);
      }
      fout << "}";
      fout << std::endl;
    }
  }
}

void dump_factors(const FactorGraph &fg, const std::string &filename) {
  std::ofstream fout(filename);
  for (size_t i = 0; i < fg.size.num_factors; ++i) {
    const auto &f = fg.factors[i];

    // var IDs
    for (size_t j = 0; j < f.num_vars; ++j) {
      const auto &vif = fg.get_factor_vif_at(f, j);
      fout << vif.vid;
      fout << text_field_delim;
    }

    // value IDs (for categorical only)
    if (f.is_categorical()) {
      for (size_t j = 0; j < f.num_vars; ++j) {
        const auto &vif = fg.get_factor_vif_at(f, j);
        fout << fg.get_var_value_at(fg.variables[vif.vid], vif.dense_equal_to);
        fout << text_field_delim;
      }
    }

    // weight ID and feature value
    fout << f.weight_id;
    fout << text_field_delim;
    fout << f.feature_value;
    fout << std::endl;
  }
}

void dump_weights(const FactorGraph &fg, const std::string &filename) {
  std::ofstream fout(filename);
  for (size_t i = 0; i < fg.size.num_weights; ++i) {
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
