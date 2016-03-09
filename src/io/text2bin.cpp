/*
 * Transform a TSV format factor graph file and output corresponding binary
 * format used in DeepDive
 */

#include <iostream>
#include <fstream>
#include <sstream>
#include <stdlib.h>
#include <stdint.h>
#include <assert.h>
#include <vector>
#include "io/binary_parser.h"

using namespace std;

constexpr char field_delim = '\t';  // tsv file delimiter
constexpr char array_delim = ',';   // array delimiter

// read variables and convert to binary format
void load_var(std::string input_filename, std::string output_filename) {
  std::ifstream fin(input_filename.c_str());
  std::ofstream fout(output_filename.c_str(), std::ios::binary | std::ios::out);
  long count = 0;

  long vid;
  int is_evidence;
  double initial_value;
  short type;
  long edge_count = -1;
  long cardinality;

  edge_count = htobe64(edge_count);

  while (fin >> vid >> is_evidence >> initial_value >> type >> cardinality) {
    // endianess
    vid = htobe64(vid);
    uint64_t initval = htobe64(*(uint64_t *)&initial_value);
    type = htobe16(type);
    cardinality = htobe64(cardinality);

    fout.write((char *)&vid, 8);
    fout.write((char *)&is_evidence, 1);
    fout.write((char *)&initval, 8);
    fout.write((char *)&type, 2);
    fout.write((char *)&edge_count, 8);
    fout.write((char *)&cardinality, 8);

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

  long wid;
  int isfixed;
  double initial_value;

  while (fin >> wid >> isfixed >> initial_value) {
    wid = htobe64(wid);
    uint64_t initval = htobe64(*(uint64_t *)&initial_value);

    fout.write((char *)&wid, 8);
    fout.write((char *)&isfixed, 1);
    fout.write((char *)&initval, 8);

    ++count;
  }
  std::cout << count << std::endl;

  fin.close();
  fout.close();
}

// load factors
// fid, wid, vids
void load_factor_with_fid(std::string input_filename,
                          std::string output_factors_filename,
                          std::string output_edges_filename, short funcid,
                          long nvar, char **positives) {
  std::ifstream fin(input_filename.c_str());
  std::ofstream fout(output_factors_filename.c_str(),
                     std::ios::binary | std::ios::out);
  std::ofstream fedgeout(output_edges_filename.c_str(),
                         std::ios::binary | std::ios::out);

  long factorid = 0;
  long weightid = 0;
  long variableid = 0;
  long nedge = 0;
  long nvars_big = htobe64(nvar);
  long predicate = funcid == 5 ? -1 : 1;
  vector<int> positives_vec;

  funcid = htobe16(funcid);

  for (int i = 0; i < nvar; i++) {
    positives_vec.push_back(atoi(positives[i]));
  }

  predicate = htobe64(predicate);

  string line;
  while (getline(fin, line)) {
    string field;
    istringstream ss(line);

    // factor id
    getline(ss, field, field_delim);
    factorid = atol(field.c_str());
    factorid = htobe64(factorid);

    // weightid
    getline(ss, field, field_delim);
    weightid = atol(field.c_str());
    weightid = htobe64(weightid);

    fout.write((char *)&factorid, 8);
    fout.write((char *)&weightid, 8);
    fout.write((char *)&funcid, 2);
    // fout.write((char *)&nvars_big, 8);

    uint64_t position = 0;
    uint64_t position_big;
    long n_vars = 0;

    for (long i = 0; i < nvar; i++) {
      getline(ss, field, field_delim);

      // array type
      if (field.at(0) == '{') {
        string subfield;
        istringstream ss1(field);
        ss1.get();  // get '{'
        bool ended = false;
        while (getline(ss1, subfield, array_delim)) {
          if (subfield.at(subfield.length() - 1) == '}') {
            ended = true;
            subfield = subfield.substr(0, subfield.length() - 1);
          }
          variableid = atol(subfield.c_str());
          variableid = htobe64(variableid);
          position_big = htobe64(position);

          fedgeout.write((char *)&variableid, 8);
          fedgeout.write((char *)&factorid, 8);
          fedgeout.write((char *)&position_big, 8);
          fedgeout.write((char *)&positives_vec[i], 1);
          fedgeout.write((char *)&predicate, 8);

          nedge++;
          position++;
          n_vars++;
          if (ended) break;
        }
      } else {
        variableid = atol(field.c_str());
        variableid = htobe64(variableid);
        position_big = htobe64(position);

        fedgeout.write((char *)&variableid, 8);
        fedgeout.write((char *)&factorid, 8);
        fedgeout.write((char *)&position_big, 8);
        fedgeout.write((char *)&positives_vec[i], 1);
        fedgeout.write((char *)&predicate, 8);

        nedge++;
        position++;
        n_vars++;
      }
    }
    n_vars = htobe64(n_vars);
    fout.write((char *)&n_vars, 8);
  }
  std::cout << nedge << std::endl;

  fin.close();
  fout.close();
  fedgeout.close();
}

static inline long parse_pgarray(
    std::istream &input, std::function<void(const string &)> parse_element,
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
    std::istream &input, std::function<void(const string &)> parse_element,
    long expected_count = -1) {
  long count = parse_pgarray(input, parse_element, expected_count);
  if (count >= 0) {
    return count;
  } else {
    std::cerr << "Expected an array '{' but found: " << input.get()
              << std::endl;
    abort();
  }
}

// load factors
// wid, vids
void load_factor(std::string input_filename, std::string output_filename,
                 short funcid, long nvar, char **positives) {
  std::ifstream fin(input_filename.c_str());
  std::ofstream fout(output_filename.c_str(), std::ios::binary | std::ios::out);

  long weightid = 0;
  long variableid = 0;
  long nedge = 0;
  long predicate = funcid == 5 ? -1 : 1;
  vector<int> positives_vec;
  vector<long> variables;

  funcid = htobe16(funcid);

  for (int i = 0; i < nvar; i++) {
    positives_vec.push_back(atoi(positives[i]));
  }

  predicate = htobe64(predicate);

  string line;
  while (getline(fin, line)) {
    string field;
    istringstream ss(line);
    variables.clear();

    // factor type
    fout.write((char *)&funcid, 2);

    // variable ids
    uint64_t position = 0;
    uint64_t position_big;
    long n_vars = 0;
    auto parse_variableid = [&variables, &n_vars,
                             &nedge](const string &element) {
      long variableid = atol(element.c_str());
      variableid = htobe64(variableid);
      variables.push_back(variableid);
      nedge++;
      n_vars++;
    };
    for (long i = 0; i < nvar; i++) {
      getline(ss, field, field_delim);
      // try parsing as an array first
      istringstream fieldinput(field);
      if (parse_pgarray(fieldinput, parse_variableid) < 0) {
        // otherwise, parse it as a single variable
        parse_variableid(field);
      }
    }
    n_vars = htobe64(n_vars);
    fout.write((char *)&predicate, 8);
    fout.write((char *)&n_vars, 8);
    for (long i = 0; i < variables.size(); i++) {
      fout.write((char *)&variables[i], 8);
      fout.write((char *)&positives_vec[i], 1);
    }

    // weight ids
    switch (funcid) {
      case dd::FUNC_SPARSE_MULTINOMIAL: {
        // a list of weight ids
        // first, the run-length
        getline(ss, field, field_delim);
        long num_weightids = atol(field.c_str());
        num_weightids = htobe64(num_weightids);
        fout.write((char *)&num_weightids, 8);
        // and that many weight ids
        parse_pgarray_or_die(ss, [&fout](const string &element) {
          long weightid = atol(element.c_str());
          weightid = htobe64(weightid);
          fout.write((char *)&weightid, 8);
        }, num_weightids);
        break;
      }

      default:
        // a single weight id
        getline(ss, field, field_delim);
        weightid = atol(field.c_str());
        weightid = htobe64(weightid);
        fout.write((char *)&weightid, 8);
    }
  }

  std::cout << nedge << std::endl;

  fin.close();
  fout.close();
}

void load_active(std::string input_filename, std::string output_filename) {
  std::ifstream fin(input_filename.c_str());
  std::ofstream fout(output_filename.c_str(), std::ios::binary);
  long count = 0;

  long id;
  while (fin >> id) {
    id = htobe64(id);
    fout.write((char *)&id, 8);
    ++count;
  }
  std::cout << count << std::endl;

  fin.close();
  fout.close();
}

// read multinomial variable domains and convert to binary format
void load_domain(std::string input_filename, std::string output_filename) {
  std::ifstream fin(input_filename.c_str());
  std::ofstream fout(output_filename.c_str(), std::ios::binary | std::ios::out);

  string line;
  while (getline(fin, line)) {
    istringstream line_input(line);
    long vid, cardinality, cardinality_big;
    std::string domain;
    assert(line_input >> vid >> cardinality >> domain);

    // endianess
    vid = htobe64(vid);
    cardinality_big = htobe64(cardinality);

    fout.write((char *)&vid, 8);
    fout.write((char *)&cardinality_big, 8);

    // an array of domain values
    istringstream domain_input(domain);
    parse_pgarray_or_die(domain_input, [&fout](const string &subfield) {
      long value = atol(subfield.c_str());
      value = htobe64(value);
      fout.write((char *)&value, 8);
    }, cardinality);
  }

  fin.close();
  fout.close();
}

int main(int argc, char **argv) {
  std::string app(argv[1]);
  if (app.compare("variable") == 0) {
    load_var(argv[2], argv[3]);
  } else if (app.compare("weight") == 0) {
    load_weight(argv[2], argv[3]);
  } else if (app.compare("factor") == 0) {
    std::string inc(argv[6]);
    if (inc.compare("inc") == 0 || inc.compare("mat") == 0)
      load_factor_with_fid(argv[2], argv[3], argv[7], atoi(argv[4]),
                           atoi(argv[5]), &argv[8]);
    else
      load_factor(argv[2], argv[3], atoi(argv[4]), atoi(argv[5]), &argv[7]);
  } else if (app.compare("active") == 0) {
    load_active(argv[2], argv[3]);
  } else if (app.compare("domain") == 0) {
    load_domain(argv[2], argv[3]);
  } else {
    std::cerr << "Unsupported type" << std::endl;
    exit(1);
  }
  return 0;
}
