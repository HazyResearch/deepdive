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
void text2binum_vars(std::string input_filename, std::string output_filename,
                     std::string count_filename) {
  std::ifstream fin(input_filename);
  std::ofstream fout(output_filename, std::ios::binary);
  std::ofstream fout_count(count_filename);
  size_t count = 0;
  std::string line;
  while (getline(fin, line)) {
    std::istringstream ss(line);
    size_t vid;
    size_t var_role;
    size_t initial_value;
    size_t var_type;
    size_t cardinality;
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
  fout_count << count << std::endl;
}

// convert weights
void text2bin_weights(std::string input_filename, std::string output_filename,
                      std::string count_filename) {
  std::ifstream fin(input_filename);
  std::ofstream fout(output_filename, std::ios::binary);
  std::ofstream fout_count(count_filename);
  size_t count = 0;
  std::string line;
  while (getline(fin, line)) {
    std::istringstream ss(line);
    size_t wid;
    size_t isfixed;
    double initial_value;
    assert(ss >> wid);
    assert(ss >> isfixed);
    assert(ss >> initial_value);
    uint8_t isfixed_serialized = isfixed;
    write_be_or_die(fout, wid);
    write_be_or_die(fout, isfixed_serialized);
    write_be_or_die(fout, initial_value);
    ++count;
  }
  fout_count << count << std::endl;
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
void text2bin_factors(std::string input_filename, std::string output_filename,
                      FACTOR_FUNCTION_TYPE funcid, size_t arity_expected,
                      const std::vector<size_t> &variables_should_equal_to,
                      std::string count_filename) {
  std::ifstream fin(input_filename);
  std::ofstream fout(output_filename, std::ios::binary);
  std::ofstream fout_count(count_filename);
  size_t total_edges = 0;
  std::vector<size_t> vids;
  std::vector<size_t> values;
  std::string line;

  while (getline(fin, line)) {
    std::string field;
    std::istringstream ss(line);
    vids.clear();
    values.clear();

    // parse factor type
    uint16_t funcid_serialized = funcid;

    // parse variable ids
    size_t arity = 0;
    auto parse_variableid = [&vids, &arity,
                             &total_edges](const std::string &element) {
      vids.push_back(atol(element.c_str()));
      ++total_edges;
      ++arity;
    };
    for (size_t i = 0; i < arity_expected; ++i) {
      assert(getline(ss, field, text_field_delim));
      parse_variableid(field);
    }

    // parse variable values (categorical only)
    if (funcid == FUNC_AND_CATEGORICAL) {
      auto parse_value = [&values](const std::string &element) {
        values.push_back(atol(element.c_str()));
      };
      for (size_t i = 0; i < arity_expected; ++i) {
        assert(getline(ss, field, text_field_delim));
        parse_value(field);
      }
    }

    // parse weight id
    assert(getline(ss, field, text_field_delim));
    size_t wid = atol(field.c_str());

    // parse feature value
    assert(getline(ss, field, text_field_delim));
    double val = atof(field.c_str());

    // write out record
    write_be_or_die(fout, funcid_serialized);
    write_be_or_die(fout, arity);
    for (size_t i = 0; i < vids.size(); ++i) {
      write_be_or_die(fout, vids[i]);
      size_t should_equal_to = variables_should_equal_to.at(i);
      if (funcid == FUNC_AND_CATEGORICAL) {
        should_equal_to = values.at(i);
      }
      write_be_or_die(fout, should_equal_to);
    }
    write_be_or_die(fout, wid);
    write_be_or_die(fout, val);
  }
  fout_count << total_edges << std::endl;
}

// read categorical variable domains and convert to binary format
void text2bin_domains(std::string input_filename, std::string output_filename,
                      std::string count_filename) {
  std::ifstream fin(input_filename);
  std::ofstream fout(output_filename, std::ios::binary);
  std::ofstream fout_count(count_filename);
  size_t count = 0;
  std::string line;
  std::vector<size_t> value_list;
  std::vector<double> truthiness_list;
  while (getline(fin, line)) {
    std::istringstream ss(line);
    size_t vid;
    size_t cardinality;
    std::string domain_string, truthy_string;
    assert(ss >> vid);
    assert(ss >> cardinality);
    assert(ss >> domain_string);
    assert(ss >> truthy_string);

    value_list.reserve(cardinality);
    truthiness_list.reserve(cardinality);

    // an array of domain values
    std::istringstream domain_input(domain_string);
    size_t i = 0;
    parse_pgarray_or_die(
        domain_input, [&i, &value_list](const std::string &subfield) {
          size_t value = atol(subfield.c_str());
          value_list[i] = value;
          ++i;
        }, cardinality);

    // an array of truthiness
    std::istringstream truthy_input(truthy_string);
    i = 0;
    parse_pgarray_or_die(
        truthy_input, [&i, &truthiness_list](const std::string &subfield) {
          double truthiness = atof(subfield.c_str());
          truthiness_list[i] = truthiness;
          ++i;
        }, cardinality);

    write_be_or_die(fout, vid);
    write_be_or_die(fout, cardinality);
    for (i = 0; i < cardinality; ++i) {
      write_be_or_die(fout, value_list[i]);
      write_be_or_die(fout, truthiness_list[i]);
    }
    ++count;
  }
  fout_count << count << std::endl;
}

int text2bin(const CmdParser &args) {
  // common arguments
  if (args.text2bin_mode == "variable") {
    text2binum_vars(args.text2bin_input, args.text2bin_output,
                    args.text2bin_count_output);
  } else if (args.text2bin_mode == "weight") {
    text2bin_weights(args.text2bin_input, args.text2bin_output,
                     args.text2bin_count_output);
  } else if (args.text2bin_mode == "factor") {
    text2bin_factors(args.text2bin_input, args.text2bin_output,
                     args.text2bin_factor_func_id, args.text2bin_factor_arity,
                     args.text2bin_factor_variables_should_equal_to,
                     args.text2bin_count_output);
  } else if (args.text2bin_mode == "domain") {
    text2bin_domains(args.text2bin_input, args.text2bin_output,
                     args.text2bin_count_output);
  } else {
    std::cerr << "Unsupported type" << std::endl;
    return 1;
  }
  return 0;
}

}  // namespace dd
