/*
 * Transform a TSV format factor graph file and output corresponding binary
 * format used in DeepDive
 */

#include "bin2text.h"
#include "binary_format.h"
#include "factor_graph.h"
#include "dimmwitted.h"

#include <iostream>
#include <fstream>
#include <sstream>
#include <stdlib.h>
#include <stdint.h>
#include <assert.h>
#include <vector>

constexpr char field_delim = '\t';  // tsv file delimiter

void dump_variables(const dd::FactorGraph &fg, const std::string &filename) {
  std::ofstream fout(filename);
  for (int i = 0; i < fg.n_var; ++i) {
    dd::Variable &v = fg.variables[i];
    // variable id
    fout << v.id;
    // is_evidence
    fout << field_delim << (v.is_evid ? "1" : "0");
    // value
    fout << field_delim << v.assignment_evid;
    // data type
    std::string type_str;
    switch (v.domain_type) {
      case DTYPE_BOOLEAN:
        type_str = "0";
        break;
      case DTYPE_MULTINOMIAL:
        type_str = "1";
        break;
      default:
        abort();
    }
    fout << field_delim << type_str;
    // cardinality
    fout << field_delim << v.cardinality;
    fout << std::endl;
  }
}

void dump_domains(const dd::FactorGraph &fg, const std::string &filename) {
  std::ofstream fout(filename);
  for (int i = 0; i < fg.n_var; ++i) {
    dd::Variable &v = fg.variables[i];
    if (v.domain_map) {
      fout << v.id;
      fout << field_delim << v.domain_map->size();
      std::vector<dd::VariableValue> values;
      values.reserve(v.domain_map->size());
      for (const auto &entry : *v.domain_map) {
        values.push_back(entry.first);
      }
      std::sort(values.begin(), values.end());
      fout << field_delim;
      fout << "{";
      int j = 0;
      for (const auto &value : values) {
        if (j > 0) fout << ",";
        fout << value;
        ++j;
      }
      fout << "}";
      fout << std::endl;
    }
  }
}

void dump_factors(const dd::FactorGraph &fg, const std::string &filename) {
  std::ofstream fout(filename);
  for (int i = 0; i < fg.n_factor; ++i) {
    const auto &f = fg.factors[i];
    // FIXME this output is lossy since it drops the f.func_id and
    // f.tmp_variables[*].is_positive
    // variable ids the factor is defined over
    for (const auto &v : f.tmp_variables) {
      fout << v.vid;
      fout << field_delim;
    }
    switch (f.func_id) {
      case dd::FUNC_SPARSE_MULTINOMIAL: {
        // followed by a number of weight ids
        fout << f.weight_ids.size();
        fout << field_delim;
        fout << "{";
        int j = 0;
        for (const auto &wid : f.weight_ids) {
          if (j > 0) fout << ",";
          fout << wid;
          ++j;
        }
        fout << "}";
        break;
      }
      default:
        // followed by a weight id
        fout << f.weight_id;
    }
    fout << std::endl;
  }
}

void dump_weights(const dd::FactorGraph &fg, const std::string &filename) {
  std::ofstream fout(filename);
  for (int i = 0; i < fg.n_weight; ++i) {
    dd::Weight &w = fg.weights[i];
    // weight id
    fout << w.id;
    // whether it's fixed
    fout << field_delim << w.isfixed;
    // weight value
    fout << field_delim << w.weight;
    fout << std::endl;
  }
}

void dump_meta(const dd::FactorGraph &fg, const std::string &filename) {
  std::ofstream fout(filename);
  fout << fg.n_weight;
  fout << "," << fg.n_var;
  fout << "," << fg.n_factor;
  fout << "," << fg.n_edge;
  // XXX dummy file names
  fout << ","
       << "graph.weights";
  fout << ","
       << "graph.variables";
  fout << ","
       << "graph.factors";
  fout << std::endl;
}

void dump_factorgraph(const dd::FactorGraph &fg,
                      const std::string &output_dir) {
  dump_variables(fg, output_dir + "/variables.tsv");
  dump_factors(fg, output_dir + "/factors.tsv");
  dump_weights(fg, output_dir + "/weights.tsv");
  dump_domains(fg, output_dir + "/domains.tsv");
  dump_meta(fg, output_dir + "/graph.meta");
}

int bin2text(const dd::CmdParser &cmd_parser) {
  Meta meta = read_meta(cmd_parser.fg_file);
  // load factor graph
  dd::FactorGraph fg(meta.num_variables, meta.num_factors, meta.num_weights,
                     meta.num_edges);
  fg.load_variables(cmd_parser.variable_file);
  fg.load_weights(cmd_parser.weight_file);
  fg.load_domains(cmd_parser.domain_file);
  fg.load_factors(cmd_parser.factor_file);
  fg.safety_check();

  dump_factorgraph(fg, cmd_parser.output_folder);

  return 0;
}
