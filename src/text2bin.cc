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

constexpr char field_delim = '\t';  // tsv file delimiter

// read variables and convert to binary format
void load_var(std::string input_filename, std::string output_filename) {
  std::ifstream fin(input_filename.c_str());
  std::ofstream fout(output_filename.c_str(), std::ios::binary | std::ios::out);
  num_variables_t count = 0;

  int is_evidence;
  uint16_t var_type;
  variable_id_t vid;
  variable_value_t initial_value;
  num_variable_values_t cardinality;

  while (fin >> vid >> is_evidence >> initial_value >> var_type >>
         cardinality) {
    write_be(fout, vid);
    write_be<uint8_t>(fout, is_evidence);
    write_be(fout, initial_value);
    write_be(fout, var_type);
    write_be(fout, cardinality);

    ++count;
  }
  std::cout << count << std::endl;

  fin.close();
  fout.close();
}

// convert weights
void load_weight(std::string input_filename, std::string output_filename) {
  std::ifstream fin(input_filename.c_str());
  std::ofstream fout(output_filename.c_str(), std::ios::binary | std::ios::out);
  long count = 0;

  int isfixed;
  weight_id_t wid;
  weight_value_t initial_value;

  while (fin >> wid >> isfixed >> initial_value) {
    write_be(fout, wid);
    write_be<uint8_t>(fout, isfixed);
    write_be(fout, initial_value);

    ++count;
  }
  std::cout << count << std::endl;

  fin.close();
  fout.close();
}

static inline long parse_pgarray(
    std::istream &input, std::function<void(const std::string &)> parse_element,
    long expected_count = -1) {
  if (input.peek() == '{') {
    input.get();
    std::string element;
    bool ended = false;
    long count = 0;
    while (getline(input, element, ',')) {
      if (element.at(element.length() - 1) == '}') {
        ended = true;
        element = element.substr(0, element.length() - 1);
      }
      parse_element(element);
      count++;
      if (ended) break;
    }
    assert(expected_count < 0 || count == expected_count);
    return count;
  } else {
    return -1;
  }
}
static inline long parse_pgarray_or_die(
    std::istream &input, std::function<void(const std::string &)> parse_element,
    long expected_count = -1) {
  long count = parse_pgarray(input, parse_element, expected_count);
  if (count >= 0) {
    return count;
  } else {
    std::cerr << "Expected an array '{' but found: " << input.get()
              << std::endl;
    std::abort();
  }
}

// load factors
// wid, vids
void load_factor(std::string input_filename, std::string output_filename,
                 factor_function_type_t funcid, factor_arity_t arity_expected,
                 const std::vector<bool> &positives_vec) {
  std::ifstream fin(input_filename.c_str());
  std::ofstream fout(output_filename.c_str(), std::ios::binary | std::ios::out);

  num_edges_t total_edges = 0;

  std::vector<variable_id_t> vids;
  std::vector<weight_id_t> vals_and_weights;  // FIXME separate the two

  std::string line;
  std::string array_piece;
  while (getline(fin, line)) {
    std::string field;
    istringstream ss(line);
    vids.clear();

    // factor type
    write_be<uint16_t>(fout, funcid);

    // variable ids
    factor_arity_t arity = 0;
    auto parse_variableid = [&vids, &arity,
                             &total_edges](const std::string &element) {
      vids.push_back(atol(element.c_str()));
      ++total_edges;
      ++arity;
    };
    for (factor_arity_t i = 0; i < arity_expected; ++i) {
      getline(ss, field, field_delim);
      // try parsing as an array first
      // FIXME remove this?  parsing vid arrays is probably broken since this
      // doesn't create a cross product of factors but simply widens the arity
      istringstream fieldinput(field);
      if (parse_pgarray(fieldinput, parse_variableid) < 0) {
        // otherwise, parse it as a single variable
        parse_variableid(field);
      }
    }
    write_be(fout, arity);
    for (factor_arity_t i = 0; i < vids.size(); ++i) {
      write_be(fout, vids[i]);

      variable_value_t should_equal_to = positives_vec.at(i) ? 1 : 0;
      write_be(fout, should_equal_to);
    }

    // weight ids
    switch (funcid) {
      case FUNC_AND_CATEGORICAL: {
        vals_and_weights.clear();
        // IN  Format: NUM_WEIGHTS [VAR1 VAL ID] [VAR2 VAL ID] ... [WEIGHT ID]
        // OUT Format: NUM_WEIGHTS [[VAR1_VALi, VAR2_VALi, ..., WEIGHTi]]
        // first, the run-length
        getline(ss, field, field_delim);
        factor_weight_key_t num_weightids = atol(field.c_str());
        write_be<uint32_t /*FIXME factor_weight_key_t*/>(fout, num_weightids);

        // second, parse var vals for each var
        // TODO: hard coding cid length (4) for now
        for (factor_arity_t i = 0; i < arity; ++i) {
          getline(ss, array_piece, field_delim);
          istringstream ass(array_piece);
          parse_pgarray_or_die(
              ass, [&vals_and_weights](const std::string &element) {
                variable_value_t cid = atoi(element.c_str());
                vals_and_weights.push_back(cid);
              }, num_weightids);
        }
        // third, parse weights
        parse_pgarray_or_die(
            ss, [&vals_and_weights](const std::string &element) {
              weight_id_t weightid = atol(element.c_str());
              vals_and_weights.push_back(weightid);
            }, num_weightids);

        // fourth, transpose into output format
        for (factor_weight_key_t i = 0; i < num_weightids; ++i) {
          for (factor_arity_t j = 0; j < arity; ++j) {
            variable_value_t cid =
                (variable_value_t)vals_and_weights[j * num_weightids + i];
            write_be(fout, cid);
          }
          weight_id_t weightid = vals_and_weights[arity * num_weightids + i];
          write_be(fout, weightid);
        }
        break;
      }

      default:
        // a single weight id
        getline(ss, field, field_delim);
        weight_id_t weightid = atol(field.c_str());
        write_be(fout, weightid);
    }
  }

  std::cout << total_edges << std::endl;

  fin.close();
  fout.close();
}

// read categorical variable domains and convert to binary format
void load_domain(std::string input_filename, std::string output_filename) {
  std::ifstream fin(input_filename.c_str());
  std::ofstream fout(output_filename.c_str(), std::ios::binary | std::ios::out);

  std::string line;
  while (getline(fin, line)) {
    istringstream line_input(line);
    variable_id_t vid;
    num_variable_values_t cardinality;
    std::string domain;
    assert(line_input >> vid >> cardinality >> domain);

    write_be(fout, vid);
    write_be(fout, cardinality);

    // an array of domain values
    istringstream domain_input(domain);
    parse_pgarray_or_die(domain_input, [&fout](const std::string &subfield) {
      variable_value_t value = atoi(subfield.c_str());
      write_be(fout, value);
    }, cardinality);
  }

  fin.close();
  fout.close();
}

int text2bin(const CmdParser &args) {
  // common arguments
  if (args.text2bin_mode == "variable") {
    load_var(args.text2bin_input, args.text2bin_output);
  } else if (args.text2bin_mode == "weight") {
    load_weight(args.text2bin_input, args.text2bin_output);
  } else if (args.text2bin_mode == "factor") {
    load_factor(args.text2bin_input, args.text2bin_output,
                args.text2bin_factor_func_id, args.text2bin_factor_arity,
                args.text2bin_factor_positives_or_not);
  } else if (args.text2bin_mode == "domain") {
    load_domain(args.text2bin_input, args.text2bin_output);
  } else {
    std::cerr << "Unsupported type" << std::endl;
    return 1;
  }
  return 0;
}

}  // namespace dd
