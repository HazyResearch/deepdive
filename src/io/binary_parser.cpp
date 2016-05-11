#include "common.h"
#include "binary_parser.h"
#include "dstruct/factor_graph/factor.h"
#include "dstruct/factor_graph/variable.h"
#include <fstream>
#include <iostream>
#include <stdint.h>

// Read meta data file, return Meta struct
Meta read_meta(string meta_file) {
  Meta meta;
  ifstream file;
  file.open(meta_file.c_str());
  string buf;
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
      case 0:
        type_const = DTYPE_BOOLEAN;
        break;
      case 1:
        type_const = DTYPE_MULTINOMIAL;
        break;
      default:
        cerr << "[ERROR] Only Boolean and Multinomial variables are supported "
                "now!"
             << endl;
        abort();
    }
    bool is_evidence = isevidence >= 1;
    bool is_observation = isevidence == 2;
    double init_value = is_evidence ? initial_value : 0;

    fg.variables[id] =
        dd::RawVariable(id, type_const, is_evidence, cardinality, init_value,
                        init_value, edge_count, is_observation);

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

    fg.factors[fg.c_nfactor] =
        dd::RawFactor(fg.c_nfactor, -1, type, edge_count);

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
    fg.factors[fg.c_nfactor] = dd::RawFactor(id, weightid, type, edge_count);
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
    if (!file.good()) {
      return;
    }

    id = be64toh(id);
    dd::RawVariable &variable = fg.variables[id];

    long domain_size;
    file.read((char *)&domain_size, 8);
    domain_size = be64toh(domain_size);
    assert(variable.cardinality == domain_size);

    std::vector<int> domain(domain_size);
    /* Notice that this is the first time variable.domain_map is initialized */
    variable.domain_map = new std::unordered_map<dd::VariableValue, int>();

    for (int i = 0; i < domain_size; i++) {
      file.read((char *)&value, 8);
      value = be64toh(value);
      domain[i] = value;
    }

    std::sort(domain.begin(), domain.end());
    for (int i = 0; i < domain_size; i++) {
      (*variable.domain_map)[domain[i]] = i;
    }

    // adjust the initial assignments to a valid one instead of zero for query
    // variables
    if (!variable.is_evid) {
      variable.assignment_free = variable.assignment_evid = domain[0];
    }
  }
}

void resume(string filename, int i, dd::CompiledFactorGraph &cfg) {

  return;
}

void checkpoint(string filename, vector<dd::CompiledFactorGraph> &cfgs) {
  std::ofstream outf;
  outf.open(filename, ios::out | ios::binary);

  int n_cfgs = cfgs.size();
  const auto &first_cfg = cfgs[0];

  /* First write the count of factor graphs */
  outf.write((char *)&n_cfgs, sizeof(int));

  /* Now write all the common things that a CompiledFactorGraph has. */
  /* TODO: Need to convert to network order!? */
  outf.write((char *)&first_cfg.n_var, sizeof(long));
  outf.write((char *)&first_cfg.n_factor, sizeof(long));
  outf.write((char *)&first_cfg.n_weight, sizeof(long));
  outf.write((char *)&first_cfg.n_edge, sizeof(long));
  outf.write((char *)&first_cfg.n_tally, sizeof(long));

  outf.write((char *)&first_cfg.c_nvar, sizeof(long));
  outf.write((char *)&first_cfg.c_nfactor, sizeof(long));
  outf.write((char *)&first_cfg.c_nweight, sizeof(long));
  outf.write((char *)&first_cfg.c_edge, sizeof(long));

  outf.write((char *)&first_cfg.n_evid, sizeof(long));
  outf.write((char *)&first_cfg.n_query, sizeof(long));

  outf.write((char *)&first_cfg.n_query, sizeof(long));

  for (auto i = 0; i < cfgs.size(); i++) {
    const auto &cfg = cfgs[i];
    UNUSED(cfg);
  }
  return;
}
