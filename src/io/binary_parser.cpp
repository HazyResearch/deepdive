#include <iostream>
#include <fstream>
#include <stdint.h>
#include "binary_parser.h"
#include "dstruct/factor_graph/factor.h"

// Read meta data file, return Meta struct
Meta read_meta(string meta_file) {
  ifstream file;
  file.open(meta_file.c_str());
  string buf;
  Meta meta;
  getline(file, buf, ',');
  meta.num_weights = atoll(buf.c_str());
  getline(file, buf, ',');
  meta.num_variables = atoll(buf.c_str());
  getline(file, buf, ',');
  meta.num_factors = atoll(buf.c_str());
  getline(file, buf, ',');
  meta.num_edges = atoll(buf.c_str());
  getline(file, meta.weights_file, ',');
  getline(file, meta.variables_file, ',');
  getline(file, meta.factors_file, ',');
  getline(file, meta.edges_file, ',');
  file.close();
  return meta;
}

// Read weights and load into factor graph
long long read_weights(string filename, dd::FactorGraph &fg) {
  ifstream file;
  file.open(filename.c_str(), ios::in | ios::binary);
  long long count = 0;
  long long id;
  bool isfixed;
  char padding;
  double initial_value;
  while (file.good()) {
    // read fields
    file.read((char *)&id, 8);
    file.read((char *)&padding, 1);
    if (!file.read((char *)&initial_value, 8)) break;
    // convert endian
    id = be64toh(id);
    isfixed = padding;
    long long tmp = be64toh(*(uint64_t *)&initial_value);
    initial_value = *(double *)&tmp;

    // load into factor graph
    fg.weights[id] = dd::Weight(id, initial_value, isfixed);
    fg.c_nweight++;
    count++;
  }
  file.close();
  return count;
}

// Read variables
long long read_variables(string filename, dd::FactorGraph &fg) {
  ifstream file;
  file.open(filename.c_str(), ios::in | ios::binary);
  long long count = 0;
  long long id;
  char isevidence;
  double initial_value;
  short type;
  long long edge_count;
  long long cardinality;
  while (file.good()) {
    // read fields
    file.read((char *)&id, 8);
    file.read((char *)&isevidence, 1);
    file.read((char *)&initial_value, 8);
    file.read((char *)&type, 2);
    file.read((char *)&edge_count, 8);
    if (!file.read((char *)&cardinality, 8)) break;

    // convert endian
    id = be64toh(id);
    type = be16toh(type);
    long long tmp = be64toh(*(uint64_t *)&initial_value);
    initial_value = *(double *)&tmp;
    edge_count = be64toh(edge_count);
    cardinality = be64toh(cardinality);

    // printf("----- id=%lli isevidence=%d initial=%f type=%d edge_count=%lli
    // cardinality=%lli\n", id, isevidence, initial_value, type, edge_count,
    // cardinality);

    count++;

    int type_const;
    switch (type) {
      case 0: type_const = DTYPE_BOOLEAN; break;
      case 1: type_const = DTYPE_MULTINOMIAL; break;
      default:
        cerr
          << "[ERROR] Only Boolean and Multinomial variables are supported now!"
          << endl;
        abort();
    }
    bool is_evidence = isevidence >= 1;
    bool is_observation = isevidence == 2;
    double init_value = is_evidence ? initial_value : 0;

    fg.variables[id] =
        dd::Variable(id, type_const, is_evidence, cardinality, init_value,
                     init_value, edge_count, is_observation);

    // set up the default domains for multinomial
    for (int i = 0; i < cardinality; i++) {
      fg.variables[id].domain.push_back(i);
      fg.variables[id].domain_map[i] = i;
    }

    fg.c_nvar++;
    if (is_evidence) {
      fg.n_evid++;
    } else {
      fg.n_query++;
    }
  }
  file.close();

  return count;
}

// Read factors (original mode)
// The format of each line in factor file is: weight_id, type, equal_predicate,
// edge_count, variable_id_1, padding_1, ..., variable_id_k, padding_k
// It is binary format without delimiter.
long long read_factors(string filename, dd::FactorGraph &fg) {
  ifstream file;
  file.open(filename.c_str(), ios::in | ios::binary);
  long long count = 0;
  long long variable_id;
  long long weightid;
  short type;
  long long edge_count;
  long long equal_predicate;
  bool ispositive;
  while (file.good()) {
    file.read((char *)&type, 2);
    file.read((char *)&equal_predicate, 8);
    if (!file.read((char *)&edge_count, 8)) break;

    type = be16toh(type);
    edge_count = be64toh(edge_count);
    equal_predicate = be64toh(equal_predicate);

    count++;

    fg.factors[fg.c_nfactor] = dd::Factor(fg.c_nfactor, -1, type, edge_count);

    for (long long position = 0; position < edge_count; position++) {
      file.read((char *)&variable_id, 8);
      file.read((char *)&ispositive, 1);
      variable_id = be64toh(variable_id);

      // wrong id
      if (variable_id >= fg.n_var || variable_id < 0) {
        assert(false);
      }

      // add variables to factors
      fg.factors[fg.c_nfactor].tmp_variables.push_back(dd::VariableInFactor(
          variable_id, position, ispositive, equal_predicate));
      fg.variables[variable_id].tmp_factor_ids.push_back(fg.c_nfactor);
    }

    switch (type) {
      case (dd::FUNC_SPARSE_MULTINOMIAL): {
        long n_weights = 0;
        file.read((char *)&n_weights, 8);
        n_weights = be64toh(n_weights);
        fg.factors[fg.c_nfactor].weight_ids.reserve(n_weights);
        for (long j = 0; j < n_weights; j++) {
          file.read((char *)&weightid, 8);
          weightid = be64toh(weightid);
          fg.factors[fg.c_nfactor].weight_ids.push_back(weightid);
        }
        break;
      }

      default:
        file.read((char *)&weightid, 8);
        weightid = be64toh(weightid);
        fg.factors[fg.c_nfactor].weight_id = weightid;
    }

    fg.c_nfactor++;
  }
  file.close();
  return count;
}

// Read factors (incremental mode)
// The format of each line in factor file is: factor_id, weight_id, type,
// edge_count
// It is binary format without delimiter.
long long read_factors_inc(string filename, dd::FactorGraph &fg) {
  ifstream file;
  file.open(filename.c_str(), ios::in | ios::binary);
  long long count = 0;
  long long id;
  long long weightid;
  short type;
  long long edge_count;
  while (file.good()) {
    file.read((char *)&id, 8);
    file.read((char *)&weightid, 8);
    file.read((char *)&type, 2);
    if (!file.read((char *)&edge_count, 8)) break;

    id = be64toh(id);
    weightid = be64toh(weightid);
    type = be16toh(type);
    edge_count = be64toh(edge_count);

    count++;
    fg.factors[fg.c_nfactor] = dd::Factor(id, weightid, type, edge_count);
    fg.c_nfactor++;
  }
  file.close();
  return count;
}

// Read edges (incremental mode)
// The format of each line in factor file is: variable_id, factor_id, position,
// padding
// It is binary format without delimiter.
long long read_edges_inc(string filename, dd::FactorGraph &fg) {
  ifstream file;
  file.open(filename.c_str(), ios::in | ios::binary);
  long long count = 0;
  long long variable_id;
  long long factor_id;
  long long position;
  bool ispositive;
  char padding;
  long long equal_predicate;
  while (file.good()) {
    // read fields
    file.read((char *)&variable_id, 8);
    file.read((char *)&factor_id, 8);
    file.read((char *)&position, 8);
    file.read((char *)&padding, 1);
    if (!file.read((char *)&equal_predicate, 8)) break;

    variable_id = be64toh(variable_id);
    factor_id = be64toh(factor_id);
    position = be64toh(position);
    ispositive = padding;
    equal_predicate = be64toh(equal_predicate);

    count++;
    // printf("varid=%lli, factorid=%lli, position=%lli, predicate=%lli\n",
    // variable_id, factor_id, position, equal_predicate);

    // wrong id
    if (variable_id >= fg.n_var || variable_id < 0) {
      assert(false);
    }

    if (factor_id >= fg.n_factor || factor_id < 0) {
      std::cout << "wrong fid = " << factor_id << std::endl;
      assert(false);
    }

    // add variables to factors
    fg.factors[factor_id].tmp_variables.push_back(dd::VariableInFactor(
        variable_id, position, ispositive, equal_predicate));
    fg.variables[variable_id].tmp_factor_ids.push_back(factor_id);
  }
  file.close();
  return count;
}

// read domains for multinomial
void read_domains(std::string filename, dd::FactorGraph &fg) {
  ifstream file;
  file.open(filename.c_str(), ios::in | ios::binary);
  long id, value;
  while (true) {
    file.read((char *)&id, 8);
    if (!file.good()) break;

    id = be64toh(id);
    dd::Variable &variable = fg.variables[id];

    long domain_size;
    file.read((char *)&domain_size, 8);
    domain_size = be64toh(domain_size);
    assert(variable.cardinality == domain_size);

    variable.domain.clear();
    variable.domain_map.clear();

    for (int i = 0; i < domain_size; i++) {
      file.read((char *)&value, 8);
      value = be64toh(value);
      variable.domain.push_back(value);
    }

    std::sort(variable.domain.begin(), variable.domain.end());
    for (int i = 0; i < domain_size; i++) {
      variable.domain_map[variable.domain[i]] = i;
    }
  }
}
