/**
 * This file contains binary formatting methods for FactorGraphs and
 * CompactFactorGraphs. We think this file is a good place to put the
 * definitions of these methods as it conveys the intentions quite clearly
 * (i.e. for binary formatting of these objects).
 */
#include "common.h"
#include "binary_format.h"
#include "factor_graph.h"
#include "factor.h"
#include "variable.h"
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

std::ostream &operator<<(std::ostream &stream, const Meta &meta) {
  stream << "################################################" << std::endl;
  stream << "# nvar               : " << meta.num_variables << std::endl;
  stream << "# nfac               : " << meta.num_factors << std::endl;
  stream << "# nweight            : " << meta.num_weights << std::endl;
  stream << "# nedge              : " << meta.num_edges << std::endl;
  return stream;
}

void dd::FactorGraph::load_weights(const std::string filename) {
  ifstream file;
  file.open(filename, ios::in | ios::binary);

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
    weights[id] = dd::Weight(id, initial_value, isfixed);
    c_nweight++;
    count++;
  }
  file.close();

  assert(n_weight == c_nweight);
}

void dd::FactorGraph::load_variables(const std::string filename) {
  ifstream file;
  file.open(filename, ios::in | ios::binary);

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

    dprintf(
        "----- id=%lli isevidence=%d initial=%f type=%d edge_count=%lli"
        "cardinality=%lli\n",
        id, isevidence, initial_value, type, edge_count, cardinality);

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

    variables[id] =
        dd::RawVariable(id, type_const, is_evidence, cardinality, init_value,
                        init_value, edge_count, is_observation);

    c_nvar++;
    if (is_evidence) {
      n_evid++;
    } else {
      n_query++;
    }
  }
  file.close();

  assert(n_var == c_nvar);
}

void dd::FactorGraph::load_factors(string filename) {
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

    factors[c_nfactor] = dd::RawFactor(c_nfactor, -1, type, edge_count);

    for (long long position = 0; position < edge_count; position++) {
      file.read((char *)&variable_id, 8);
      file.read((char *)&ispositive, 1);
      variable_id = be64toh(variable_id);

      // check for wrong id
      assert(variable_id < n_var && variable_id >= 0);

      // add variables to factors
      factors[c_nfactor].tmp_variables.push_back(dd::VariableInFactor(
          variable_id, position, ispositive, equal_predicate));
      variables[variable_id].tmp_factor_ids.push_back(c_nfactor);
    }

    switch (type) {
      case (dd::FUNC_SPARSE_MULTINOMIAL): {
        long n_weights = 0;
        file.read((char *)&n_weights, 8);
        n_weights = be64toh(n_weights);

        factors[c_nfactor].weight_ids.reserve(n_weights);
        for (long j = 0; j < n_weights; j++) {
          file.read((char *)&weightid, 8);
          weightid = be64toh(weightid);
          factors[c_nfactor].weight_ids.push_back(weightid);
        }
        break;
      }

      default:
        file.read((char *)&weightid, 8);
        weightid = be64toh(weightid);
        factors[c_nfactor].weight_id = weightid;
    }

    c_nfactor++;
  }
  file.close();

  assert(n_factor == c_nfactor);
}

void dd::FactorGraph::load_domains(std::string filename) {
  ifstream file;
  file.open(filename.c_str(), ios::in | ios::binary);
  long id, value;

  while (true) {
    file.read((char *)&id, 8);
    if (!file.good()) {
      return;
    }

    id = be64toh(id);
    dd::RawVariable &variable = variables[id];

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

/* Also used in test, but not exported in .h */
const string get_copy_filename(const string &filename, int i) {
  return filename + ".part" + to_string(i);
}

void resume(string filename, int i, dd::CompiledFactorGraph &cfg) {
  ifstream inf;
  inf.open(get_copy_filename(filename, i), ios::in | ios::binary);

  /* Read metadata */
  inf.read((char *)&cfg.n_var, sizeof(long));
  inf.read((char *)&cfg.n_factor, sizeof(long));
  inf.read((char *)&cfg.n_weight, sizeof(long));
  inf.read((char *)&cfg.n_edge, sizeof(long));

  inf.read((char *)&cfg.c_nvar, sizeof(long));
  inf.read((char *)&cfg.c_nfactor, sizeof(long));
  inf.read((char *)&cfg.c_nweight, sizeof(long));
  inf.read((char *)&cfg.c_edge, sizeof(long));

  inf.read((char *)&cfg.n_evid, sizeof(long));
  inf.read((char *)&cfg.n_query, sizeof(long));

  inf.read((char *)&cfg.stepsize, sizeof(double));

  inf.read((char *)&cfg.is_inc, sizeof(int));

  /*
   * Note that at this point, we have recovered cfg.n_var. Plus, assume
   * that the CompiledFactorGraph has been partially initialized through
   * the graph.meta file, which should at least give us non-null arrays.
   */
  assert(cfg.variables);
  for (auto j = 0; j < cfg.n_var; j++) {
    inf.read((char *)&cfg.variables[j].id, sizeof(long));
    inf.read((char *)&cfg.variables[j].domain_type, sizeof(int));
    inf.read((char *)&cfg.variables[j].is_evid, sizeof(int));
    inf.read((char *)&cfg.variables[j].is_observation, sizeof(int));
    inf.read((char *)&cfg.variables[j].cardinality, sizeof(int));

    inf.read((char *)&cfg.variables[j].assignment_evid,
             sizeof(dd::VariableValue));
    inf.read((char *)&cfg.variables[j].assignment_free,
             sizeof(dd::VariableValue));

    inf.read((char *)&cfg.variables[j].n_factors, sizeof(int));
    inf.read((char *)&cfg.variables[j].n_start_i_factors, sizeof(long));

    inf.read((char *)&cfg.variables[j].n_start_i_tally, sizeof(long));

    /* XXX: Ignore last 3 components of Variable, might dump them anyways. */
    /* XXX: What to do about domain_map, though? */
  }

  assert(cfg.factors);
  for (auto j = 0; j < cfg.n_factor; j++) {
    inf.read((char *)&cfg.factors[j].id, sizeof(dd::FactorIndex));
    inf.read((char *)&cfg.factors[j].weight_id, sizeof(dd::WeightIndex));
    inf.read((char *)&cfg.factors[j].func_id, sizeof(int));
    inf.read((char *)&cfg.factors[j].n_variables, sizeof(int));
    inf.read((char *)&cfg.factors[j].n_start_i_vif, sizeof(long));

    /* XXX: Also ignoring weight_ids in Factors */
  }

  assert(cfg.compact_factors);
  assert(cfg.compact_factors_weightids);
  assert(cfg.factor_ids);
  assert(cfg.vifs);
  for (auto j = 0; j < cfg.n_edge; j++) {
    inf.read((char *)&cfg.compact_factors[j].id, sizeof(dd::FactorIndex));
    inf.read((char *)&cfg.compact_factors[j].func_id, sizeof(int));
    inf.read((char *)&cfg.compact_factors[j].n_variables, sizeof(int));
    inf.read((char *)&cfg.compact_factors[j].n_start_i_vif, sizeof(long));

    inf.read((char *)&cfg.compact_factors_weightids[j], sizeof(int));

    inf.read((char *)&cfg.factor_ids[j], sizeof(long));

    inf.read((char *)&cfg.vifs[j].vid, sizeof(long));
    inf.read((char *)&cfg.vifs[j].n_position, sizeof(int));
    inf.read((char *)&cfg.vifs[j].is_positive, sizeof(int));
    inf.read((char *)&cfg.vifs[j].equal_to, sizeof(dd::VariableValue));
  }

  assert(cfg.infrs);
  inf.read((char *)&cfg.infrs->ntallies, sizeof(long));
  for (auto j = 0; j < cfg.infrs->ntallies; j++) {
    inf.read((char *)&cfg.infrs->multinomial_tallies[j], sizeof(long));
  }

  for (auto j = 0; j < cfg.n_var; j++) {
    inf.read((char *)&cfg.infrs->agg_means[j], sizeof(double));
    inf.read((char *)&cfg.infrs->agg_nsamples[j], sizeof(double));
    inf.read((char *)&cfg.infrs->assignments_free[j],
             sizeof(dd::VariableValue));
    inf.read((char *)&cfg.infrs->assignments_evid[j],
             sizeof(dd::VariableValue));
  }

  /*
   * XXX: We don't write the weights in `infrs` on purpose, since they're
   * going to be recovered by a separate workflow.
   */

  return;
}

/**
 * For now, assume we use the checkpointing model where we write each factor
 * graph into a separate file.
 *
 * TODO: It's worth explaining this process, and some design decisions that
 * were made in much more detail. I will do that when I clean up the code.
 */
void checkpoint(string filename, vector<dd::CompiledFactorGraph> &cfgs) {
  auto n_cfgs = cfgs.size();
  for (auto i = 0; i < n_cfgs; i++) {
    ofstream outf;
    outf.open(get_copy_filename(filename, i), ios::out | ios::binary);

    const auto &cfg = cfgs[i];

    /* Now write all the common things that a CompiledFactorGraph has. */
    /* TODO: Need to convert to network order!? */
    outf.write((char *)&cfg.n_var, sizeof(long));
    outf.write((char *)&cfg.n_factor, sizeof(long));
    outf.write((char *)&cfg.n_weight, sizeof(long));
    outf.write((char *)&cfg.n_edge, sizeof(long));

    outf.write((char *)&cfg.c_nvar, sizeof(long));
    outf.write((char *)&cfg.c_nfactor, sizeof(long));
    outf.write((char *)&cfg.c_nweight, sizeof(long));
    outf.write((char *)&cfg.c_edge, sizeof(long));

    outf.write((char *)&cfg.n_evid, sizeof(long));
    outf.write((char *)&cfg.n_query, sizeof(long));

    outf.write((char *)&cfg.stepsize, sizeof(double));

    outf.write((char *)&cfg.is_inc, sizeof(int));

    for (auto j = 0; j < cfg.n_var; j++) {
      outf.write((char *)&cfg.variables[j].id, sizeof(long));
      outf.write((char *)&cfg.variables[j].domain_type, sizeof(int));
      outf.write((char *)&cfg.variables[j].is_evid, sizeof(int));
      outf.write((char *)&cfg.variables[j].is_observation, sizeof(int));
      outf.write((char *)&cfg.variables[j].cardinality, sizeof(int));

      outf.write((char *)&cfg.variables[j].assignment_evid,
                 sizeof(dd::VariableValue));
      outf.write((char *)&cfg.variables[j].assignment_free,
                 sizeof(dd::VariableValue));

      outf.write((char *)&cfg.variables[j].n_factors, sizeof(int));
      outf.write((char *)&cfg.variables[j].n_start_i_factors, sizeof(long));

      outf.write((char *)&cfg.variables[j].n_start_i_tally, sizeof(long));

      /* XXX: Ignore last 3 components of Variable, might dump them anyways. */
      /* XXX: What to do about domain_map, though? */
    }

    for (auto j = 0; j < cfg.n_factor; j++) {
      outf.write((char *)&cfg.factors[j].id, sizeof(dd::FactorIndex));
      outf.write((char *)&cfg.factors[j].weight_id, sizeof(dd::WeightIndex));
      outf.write((char *)&cfg.factors[j].func_id, sizeof(int));
      outf.write((char *)&cfg.factors[j].n_variables, sizeof(int));
      outf.write((char *)&cfg.factors[j].n_start_i_vif, sizeof(long));

      /* XXX: Also ignoring weight_ids in Factors */
    }

    for (auto j = 0; j < cfg.n_edge; j++) {
      outf.write((char *)&cfg.compact_factors[j].id, sizeof(dd::FactorIndex));
      outf.write((char *)&cfg.compact_factors[j].func_id, sizeof(int));
      outf.write((char *)&cfg.compact_factors[j].n_variables, sizeof(int));
      outf.write((char *)&cfg.compact_factors[j].n_start_i_vif, sizeof(long));

      outf.write((char *)&cfg.compact_factors_weightids[j], sizeof(int));

      outf.write((char *)&cfg.factor_ids[j], sizeof(long));

      outf.write((char *)&cfg.vifs[j].vid, sizeof(long));
      outf.write((char *)&cfg.vifs[j].n_position, sizeof(int));
      outf.write((char *)&cfg.vifs[j].is_positive, sizeof(int));
      outf.write((char *)&cfg.vifs[j].equal_to, sizeof(dd::VariableValue));
    }

    outf.write((char *)&cfg.infrs->ntallies, sizeof(long));
    for (auto j = 0; j < cfg.infrs->ntallies; j++) {
      outf.write((char *)&cfg.infrs->multinomial_tallies[j], sizeof(long));
    }

    for (auto j = 0; j < cfg.n_var; j++) {
      outf.write((char *)&cfg.infrs->agg_means[j], sizeof(double));
      outf.write((char *)&cfg.infrs->agg_nsamples[j], sizeof(double));
      outf.write((char *)&cfg.infrs->assignments_free[j],
                 sizeof(dd::VariableValue));
      outf.write((char *)&cfg.infrs->assignments_evid[j],
                 sizeof(dd::VariableValue));
    }

    /*
     * XXX: We don't write the weights in `infrs` on purpose, since they're
     * going to be recovered by a separate workflow.
     */

    outf.close();
  }
}
