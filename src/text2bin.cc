/*
 * Transform a TSV format factor graph file and output corresponding binary
 * format used in DeepDive
 */

#include "text2bin.h"
#include "binary_format.h"
#include <assert.h>
#include <fstream>
#include <iostream>
#include <sstream>
#include <stdint.h>
#include <stdlib.h>
#include <vector>

namespace dd {

// read variables and convert to binary format
void text2bin_variables(std::string input_filename,
                        std::string output_filename) {
  std::ifstream fin(input_filename);
  std::ofstream fout(output_filename, std::ios::binary);
  num_variables_t count = 0;
  std::string line;
  while (getline(fin, line)) {
    std::istringstream ss(line);
    variable_id_t vid;
    int var_role;
    variable_value_t initial_value;
    int var_type;
    num_variable_values_t cardinality;
    assert(ss >> vid);
    assert(ss >> var_role);
    assert(ss >> initial_value);
    assert(ss >> var_type);
    assert(ss >> cardinality);
    uint8_t var_role_serialized = var_role;
    uint16_t var_type_serialized = var_type;
    write_be_or_die(fout, vid);
    write_be_or_die(fout, var_role_serialized);
    write_be_or_die(fout, initial_value);
    write_be_or_die(fout, var_type_serialized);
    write_be_or_die(fout, cardinality);
    ++count;
  }
  std::cout << count << std::endl;
}

// convert weights
void text2bin_weights(std::string input_filename, std::string output_filename) {
  std::ifstream fin(input_filename);
  std::ofstream fout(output_filename, std::ios::binary);
  num_weights_t count = 0;
  std::string line;
  while (getline(fin, line)) {
    std::istringstream ss(line);
    weight_id_t wid;
    int isfixed;
    weight_value_t initial_value;
    assert(ss >> wid);
    assert(ss >> isfixed);
    assert(ss >> initial_value);
    uint8_t isfixed_serialized = isfixed;
    write_be_or_die(fout, wid);
    write_be_or_die(fout, isfixed_serialized);
    write_be_or_die(fout, initial_value);
    ++count;
  }
  std::cout << count << std::endl;
}

constexpr size_t UNDEFINED_COUNT = (size_t)-1;

static inline size_t parse_pgarray(
    std::istream &input, std::function<void(const std::string &)> parse_element,
    size_t expected_count = UNDEFINED_COUNT) {
  if (input.peek() == '{') {
    input.get();
    std::string element;
    bool ended = false;
    size_t count = 0;
    while (getline(input, element, ',')) {
      if (element.at(element.length() - 1) == '}') {
        ended = true;
        element = element.substr(0, element.length() - 1);
      }
      parse_element(element);
      count++;
      if (ended) break;
    }
    assert(expected_count == UNDEFINED_COUNT || count == expected_count);
    return count;
  } else {
    return UNDEFINED_COUNT;
  }
}
static inline size_t parse_pgarray_or_die(
    std::istream &input, std::function<void(const std::string &)> parse_element,
    size_t expected_count = UNDEFINED_COUNT) {
  size_t count = parse_pgarray(input, parse_element, expected_count);
  if (count != UNDEFINED_COUNT) {
    return count;
  } else {
    std::cerr << "Expected an array '{' but found: " << input.get()
              << std::endl;
    std::abort();
  }
}

// load factors
// wid, vids
void text2bin_factors(
    std::string input_filename, std::string output_filename,
    factor_function_type_t funcid, factor_arity_t arity_expected,
    const std::vector<variable_value_t> &variables_should_equal_to) {
  std::ifstream fin(input_filename);
  std::ofstream fout(output_filename, std::ios::binary);
  num_edges_t total_edges = 0;
  std::vector<variable_id_t> vids;
  std::vector<variable_value_t> cids_per_wid;
  std::vector<weight_id_t> wids;
  std::vector<feature_value_t> feature_values;
  std::string line;
  std::string array_piece;
  while (getline(fin, line)) {
    std::string field;
    std::istringstream ss(line);
    vids.clear();
    // factor type
    uint16_t funcid_serialized = funcid;
    write_be_or_die(fout, funcid_serialized);
    // variable ids
    factor_arity_t arity = 0;
    auto parse_variableid = [&vids, &arity,
                             &total_edges](const std::string &element) {
      vids.push_back(atol(element.c_str()));
      ++total_edges;
      ++arity;
    };
    for (factor_arity_t i = 0; i < arity_expected; ++i) {
      assert(getline(ss, field, text_field_delim));
      // try parsing as an array first
      // FIXME remove this?  parsing vid arrays is probably broken since this
      // doesn't create a cross product of factors but simply widens the arity
      std::istringstream fieldinput(field);
      if (parse_pgarray(fieldinput, parse_variableid) == UNDEFINED_COUNT) {
        // otherwise, parse it as a single variable
        parse_variableid(field);
      }
    }
    write_be_or_die(fout, arity);
    for (factor_arity_t i = 0; i < vids.size(); ++i) {
      write_be_or_die(fout, vids[i]);
      variable_value_t should_equal_to = variables_should_equal_to.at(i);
      write_be_or_die(fout, should_equal_to);
    }
    // weight ids
    switch (funcid) {
      case FUNC_AND_CATEGORICAL: {
        cids_per_wid.clear();
        wids.clear();
        feature_values.clear();
        // IN  Format: NUM_WEIGHTS [VAR1 VAL ID] [VAR2 VAL ID] ... [WEIGHT ID]
        // OUT Format: NUM_WEIGHTS [[VAR1_VALi, VAR2_VALi, ..., WEIGHTi]]
        // 1. the run-length
        assert(getline(ss, field, text_field_delim));
        factor_weight_key_t num_weightids = atol(field.c_str());
        write_be_or_die(fout, num_weightids);
        // 2. parse var vals for each var
        for (factor_arity_t i = 0; i < arity; ++i) {
          assert(getline(ss, array_piece, text_field_delim));
          std::istringstream ass(array_piece);
          parse_pgarray_or_die(
              ass, [&cids_per_wid](const std::string &element) {
                variable_value_t cid = atoi(element.c_str());
                cids_per_wid.push_back(cid);
              }, num_weightids);
        }
        // 3. parse weights
        assert(getline(ss, array_piece, text_field_delim));
        std::istringstream ass(array_piece);
        parse_pgarray_or_die(ass, [&wids](const std::string &element) {
          weight_id_t wid = atol(element.c_str());
          wids.push_back(wid);
        }, num_weightids);
        // 4. parse feature values
        parse_pgarray_or_die(ss, [&feature_values](const std::string &element) {
          feature_value_t val = atof(element.c_str());
          feature_values.push_back(val);
        }, num_weightids);
        // 5. transpose into output format
        for (factor_weight_key_t i = 0; i < num_weightids; ++i) {
          for (factor_arity_t j = 0; j < arity; ++j) {
            variable_value_t cid = cids_per_wid[j * num_weightids + i];
            write_be_or_die(fout, cid);
          }
          write_be_or_die(fout, wids[i]);
          write_be_or_die(fout, feature_values[i]);
        }
        break;
      }
      default:
        // a single weight id
        assert(getline(ss, field, text_field_delim));
        weight_id_t wid = atol(field.c_str());
        write_be_or_die(fout, wid);
        assert(getline(ss, field, text_field_delim));
        feature_value_t val = atof(field.c_str());
        write_be_or_die(fout, val);
    }
  }
  std::cout << total_edges << std::endl;
}

// read categorical variable domains and convert to binary format
void text2bin_domains(std::string input_filename, std::string output_filename) {
  std::ifstream fin(input_filename);
  std::ofstream fout(output_filename, std::ios::binary);
  std::string line;
  while (getline(fin, line)) {
    std::istringstream ss(line);
    variable_id_t vid;
    num_variable_values_t cardinality;
    std::string domain;
    assert(ss >> vid);
    assert(ss >> cardinality);
    assert(ss >> domain);
    write_be_or_die(fout, vid);
    write_be_or_die(fout, cardinality);
    // an array of domain values
    std::istringstream domain_input(domain);
    parse_pgarray_or_die(domain_input, [&fout](const std::string &subfield) {
      variable_value_t value = atoi(subfield.c_str());
      write_be_or_die(fout, value);
    }, cardinality);
  }
}

int text2bin(const CmdParser &args) {
  // common arguments
  if (args.text2bin_mode == "variable") {
    text2bin_variables(args.text2bin_input, args.text2bin_output);
  } else if (args.text2bin_mode == "weight") {
    text2bin_weights(args.text2bin_input, args.text2bin_output);
  } else if (args.text2bin_mode == "factor") {
    text2bin_factors(args.text2bin_input, args.text2bin_output,
                     args.text2bin_factor_func_id, args.text2bin_factor_arity,
                     args.text2bin_factor_variables_should_equal_to);
  } else if (args.text2bin_mode == "domain") {
    text2bin_domains(args.text2bin_input, args.text2bin_output);
  } else {
    std::cerr << "Unsupported type" << std::endl;
    return 1;
  }
  return 0;
}

}  // namespace dd
