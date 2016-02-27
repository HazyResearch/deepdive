
#include <iostream>
#include "io/binary_parser.h"
#include "dstruct/factor_graph/factor_graph.h"
#include "dstruct/factor_graph/factor.h"

#include <fstream>

// 64-bit big endian to little endian
#define bswap_64(x)                                                            \
  ((((x)&0xff00000000000000ull) >> 56) | (((x)&0x00ff000000000000ull) >> 40) | \
   (((x)&0x0000ff0000000000ull) >> 24) | (((x)&0x000000ff00000000ull) >> 8) |  \
   (((x)&0x00000000ff000000ull) << 8) | (((x)&0x0000000000ff0000ull) << 24) |  \
   (((x)&0x000000000000ff00ull) << 40) | (((x)&0x00000000000000ffull) << 56))

bool dd::FactorGraph::is_usable() {
  return this->sorted && this->safety_check_passed;
}

dd::FactorGraph::FactorGraph(long _n_var, long _n_factor, long _n_weight,
                             long _n_edge)
    : n_var(_n_var),
      n_factor(_n_factor),
      n_weight(_n_weight),
      n_edge(_n_edge),
      c_nvar(0),
      c_nfactor(0),
      c_nweight(0),
      n_evid(0),
      n_query(0),
      variables(new Variable[_n_var]),
      factors(new Factor[_n_factor]),
      weights(new Weight[_n_weight]),
      compact_factors(new CompactFactor[_n_edge]),
      compact_factors_weightids(new int[_n_edge]),
      factor_ids(new long[_n_edge]),
      vifs(new VariableInFactor[_n_edge]),
      infrs(new InferenceResult(_n_var, _n_weight)),
      sorted(false),
      safety_check_passed(false),
      old_weight_values(NULL) {}

void dd::FactorGraph::copy_from(const FactorGraph *const p_other_fg) {
  // copy each member from the given graph
  memcpy(variables, p_other_fg->variables, sizeof(Variable) * n_var);
  memcpy(factors, p_other_fg->factors, sizeof(Factor) * n_factor);
  memcpy(weights, p_other_fg->weights, sizeof(Weight) * n_weight);
  memcpy(factor_ids, p_other_fg->factor_ids, sizeof(long) * n_edge);
  memcpy(vifs, p_other_fg->vifs, sizeof(VariableInFactor) * n_edge);

  memcpy(compact_factors, p_other_fg->compact_factors,
         sizeof(CompactFactor) * n_edge);
  memcpy(compact_factors_weightids, p_other_fg->compact_factors_weightids,
         sizeof(int) * n_edge);

  if (p_other_fg->old_weight_values != NULL) {
    if (old_weight_values == NULL) {
      old_weight_values = new float[n_weight];
    }
    memcpy(old_weight_values, p_other_fg->old_weight_values,
           sizeof(float) * n_weight);
  }

  c_nvar = p_other_fg->c_nvar;
  c_nfactor = p_other_fg->c_nfactor;
  c_nweight = p_other_fg->c_nweight;
  c_edge = p_other_fg->c_edge;
  sorted = p_other_fg->sorted;
  safety_check_passed = p_other_fg->safety_check_passed;

  infrs->init(variables, weights);
  infrs->ntallies = p_other_fg->infrs->ntallies;
  infrs->multinomial_tallies = new int[p_other_fg->infrs->ntallies];
  for (long i = 0; i < infrs->ntallies; i++) {
    infrs->multinomial_tallies[i] = p_other_fg->infrs->multinomial_tallies[i];
  }
}

long dd::FactorGraph::get_multinomial_weight_id(
    const VariableValue *assignments, const CompactFactor &fs, long vid,
    long proposal) {
  /**
   * The weight ids are aligned in a continuous region according
   * to the numerical order of variable values.
   * Say for two variables v1, v2, v3, with cardinality d1, d2, and d3.
   * The numerical value is
   * v1 * d2 * d3 + v2 * d3 + index of v3 in domain vector.
   */
  long weight_offset = 0;
  // for each variable in the factor
  for (long i = fs.n_start_i_vif; i < fs.n_start_i_vif + fs.n_variables; i++) {
    const VariableInFactor &vif = vifs[i];
    Variable &variable = variables[vif.vid];
    if (vif.vid == vid) {
      weight_offset = weight_offset * variable.domain.size() +
                      variable.domain_map[proposal];
    } else {
      weight_offset = weight_offset * variable.domain.size() +
                      variable.domain_map[(int)assignments[vif.vid]];
    }
  }
  long base_offset = &fs - compact_factors;  // note c++ will auto scale by
                                             // sizeof(CompactFactor)
  return *(compact_factors_weightids + base_offset) + weight_offset;
}

void dd::FactorGraph::update_weight(const Variable &variable) {
  // corresponding factors and weights in a continous region
  CompactFactor *const fs = compact_factors + variable.n_start_i_factors;
  const int *const ws = compact_factors_weightids + variable.n_start_i_factors;
  // for each factor
  for (long i = 0; i < variable.n_factors; i++) {
    // boolean variable
    if (variable.domain_type == DTYPE_BOOLEAN) {
      // only update weight when it is not fixed
      if (infrs->weights_isfixed[ws[i]] == false) {
        // stochastic gradient ascent
        // increment weight with stepsize * gradient of weight
        // gradient of weight = E[f|D] - E[f], where D is evidence variables,
        // f is the factor function, E[] is expectation. Expectation is
        // calculated
        // using a sample of the variable.
        infrs->weight_values[ws[i]] +=
            stepsize * (this->template potential<false>(fs[i]) -
                        this->template potential<true>(fs[i]));
      }
    } else if (variable.domain_type == DTYPE_MULTINOMIAL) {
      // two weights need to be updated
      // sample with evidence fixed, I0, with corresponding weight w1
      // sample without evidence unfixed, I1, with corresponding weight w2
      // gradient of wd0 = f(I0) - I(w1==w2)f(I1)
      // gradient of wd1 = I(w1==w2)f(I0) - f(I1)
      long wid1 =
          get_multinomial_weight_id(infrs->assignments_evid, fs[i], -1, -1);
      long wid2 =
          get_multinomial_weight_id(infrs->assignments_free, fs[i], -1, -1);
      int equal = (wid1 == wid2);

      if (infrs->weights_isfixed[wid1] == false) {
        infrs->weight_values[wid1] +=
            stepsize * (this->template potential<false>(fs[i]) -
                        equal * this->template potential<true>(fs[i]));
      }

      if (infrs->weights_isfixed[wid2] == false) {
        infrs->weight_values[wid2] +=
            stepsize * (equal * this->template potential<false>(fs[i]) -
                        this->template potential<true>(fs[i]));
      }
    }
  }
}

// sort according to id
template <class OBJTOSORT>
class idsorter : public std::binary_function<OBJTOSORT, OBJTOSORT, bool> {
 public:
  inline bool operator()(const OBJTOSORT &left, const OBJTOSORT &right) {
    return left.id < right.id;
  }
};

// Load factor graph from files
// It contains two different mode: original mode and incremental mode.
// The logic for this function is:
// original:
//   1. read variables
//   2. read weights
//   3. sort variables and weights by id
//   4. read factors
// incremental:
//   1. read variables
//   2. read weights
//   3. sort variables and weights by id
//   4. read factors
//   5. sort factors by id
//   6. read edges
void dd::FactorGraph::load(const CmdParser &cmd, const bool is_quiet, int inc) {
  // get factor graph file names from command line arguments
  std::string filename_edges;
  std::string filename_factors;
  std::string filename_variables;
  std::string filename_weights;
  if (inc) {
    filename_weights = cmd.original_folder + "/graph.weights";
    filename_variables = cmd.original_folder + "/graph.variables";
    filename_factors = cmd.original_folder + "/graph.factors";
    filename_edges = cmd.original_folder + "/graph.edges";
  } else {
    filename_weights = cmd.weight_file;
    filename_variables = cmd.variable_file;
    filename_factors = cmd.factor_file;
  }

  // load variables
  long long n_loaded = read_variables(filename_variables, *this);

  if (cmd.delta_folder != "") {
    std::cout << "Loading delta..." << std::endl;
    std::cout << cmd.delta_folder + "/graph.variables" << std::endl;
    n_loaded += read_variables(cmd.delta_folder + "/graph.variables", *this);
  }
  assert(n_loaded == n_var);
  if (!is_quiet) {
    std::cout << "LOADED VARIABLES: #" << n_loaded << std::endl;
    std::cout << "         N_QUERY: #" << n_query << std::endl;
    std::cout << "         N_EVID : #" << n_evid << std::endl;
  }

  // load weights
  n_loaded = read_weights(filename_weights, *this);
  if (cmd.delta_folder != "") {
    std::cout << "Loading delta..." << std::endl;
    n_loaded += read_weights(cmd.delta_folder + "/graph.weights", *this);
  }
  assert(n_loaded == n_weight);
  if (!is_quiet) {
    std::cout << "LOADED WEIGHTS: #" << n_loaded << std::endl;
  }

  this->sorted = true;
  infrs->init(variables, weights);

  // load factors
  if (inc)
    n_loaded = read_factors_inc(filename_factors, *this);
  else
    n_loaded = read_factors(filename_factors, *this);
  if (cmd.delta_folder != "") {
    std::cout << "Loading delta..." << cmd.delta_folder + "/graph.factors"
              << std::endl;
    n_loaded += read_factors_inc(cmd.delta_folder + "/graph.factors", *this);
  }

  assert(n_loaded == n_factor);
  if (!is_quiet) {
    std::cout << "LOADED FACTORS: #" << n_loaded << std::endl;
  }

  read_domains(cmd.domain_file, *this);

  if (inc) {
    // sort edges
    // NOTE This is very important, as read_edges assume variables,
    // factors and weights are ordered so that their id is the index
    // where they are stored in the array
    std::sort(&factors[0], &factors[n_factor], idsorter<Factor>());
    // load edges
    n_loaded = read_edges_inc(filename_edges, *this);
    if (cmd.delta_folder != "") {
      std::cout << "Loading delta..." << std::endl;
      n_loaded += read_edges_inc(cmd.delta_folder + "/graph.edges", *this);
    }

    if (!is_quiet) {
      std::cout << "LOADED EDGES: #" << n_loaded << std::endl;
    }
  }

  // load active variables
  std::string active_vars = cmd.original_folder + "/active.variables";
  long long active_id;
  std::ifstream file;
  file.open(active_vars.c_str(), ios::in | ios::binary);
  while (file.good()) {
    if (!file.read((char *)&active_id, 8)) break;
    active_id = bswap_64(active_id);
    this->variables[active_id].isactive = true;
  }

  // a slow, but good enough algorithm for connected components
  std::string useful_training =
      cmd.original_folder + "/mat_components_hasevids";
  std::ofstream fout2(useful_training.c_str());
  long long component_id = -1;
  for (long long vid = 0; vid < n_var; vid++) {
    const Variable &var = this->variables[vid];
    if (var.component_id != -1) continue;
    component_id++;
    bool isuseful_for_training = false;
    std::vector<long long> vars_to_work_on;
    vars_to_work_on.push_back(vid);
    long long var_to_work_on;
    while (vars_to_work_on.size() != 0) {
      var_to_work_on = vars_to_work_on.back();
      vars_to_work_on.pop_back();
      this->variables[var_to_work_on].component_id = component_id;
      if (this->variables[var_to_work_on].is_evid) {
        isuseful_for_training = true;
      }
      for (long long fid : this->variables[var_to_work_on].tmp_factor_ids) {
        const Factor &factor = this->factors[fid];
        for (const VariableInFactor &next_var : factor.tmp_variables) {
          if (this->variables[next_var.vid].component_id != -1) {
            assert(this->variables[next_var.vid].component_id == component_id);
          } else {
            this->variables[next_var.vid].component_id = component_id;
            vars_to_work_on.push_back(next_var.vid);
          }
        }
      }
    }
    fout2 << component_id << " " << isuseful_for_training << std::endl;
  }
  std::string component_file = cmd.original_folder + "/mat_active_components";
  std::ofstream fout(component_file.c_str());
  for (long long vid = 0; vid < n_var; vid++) {
    const Variable &var = this->variables[vid];
    assert(var.component_id != -1);
    if (var.isactive) {
      fout << var.id << " " << var.component_id << std::endl;
    }
  }

  // construct edge-based store
  this->organize_graph_by_edge();
  this->safety_check();

  assert(this->is_usable() == true);

  if (inc == 2) {
    std::cout << "LOADING PREVIOUS WEIGHT..." << std::endl;
    this->old_weight_values = new float[n_weight];
    std::ifstream fin(cmd.original_folder +
                      "/inference_result.out.weights.text");
    long long wid;
    float weight;
    while (fin >> wid >> weight) {
      this->weights[wid].weight = weight;
      this->old_weight_values[wid] = weight;
    }
    fin.close();
  }
}

bool dd::compare_position(const VariableInFactor &x,
                          const VariableInFactor &y) {
  return x.n_position < y.n_position;
}

void dd::FactorGraph::organize_graph_by_edge() {
  // number of edges
  c_edge = 0;
  // put variables into the edge-based variable array vifs
  for (long i = 0; i < n_factor; i++) {
    Factor &factor = factors[i];
    factor.n_start_i_vif = c_edge;
    // sort variables in factor by position in factor
    std::sort(factor.tmp_variables.begin(), factor.tmp_variables.end(),
              dd::compare_position);
    for (const VariableInFactor &vif : factor.tmp_variables) {
      vifs[c_edge] = vif;
      c_edge++;
    }
  }

  c_edge = 0;
  long ntallies = 0;
  // for each variable, put the factors into factor_dups
  for (long i = 0; i < n_var; i++) {
    Variable &variable = variables[i];
    variable.n_factors =
        variable.tmp_factor_ids.size();  // no edge count any more

    variable.n_start_i_factors = c_edge;
    if (variable.domain_type == DTYPE_MULTINOMIAL) {
      variable.n_start_i_tally = ntallies;
      ntallies += variable.domain.size();
    }
    for (const long &fid : variable.tmp_factor_ids) {
      factor_ids[c_edge] = fid;
      compact_factors[c_edge].id = factors[fid].id;
      compact_factors[c_edge].func_id = factors[fid].func_id;
      compact_factors[c_edge].n_variables = factors[fid].n_variables;
      compact_factors[c_edge].n_start_i_vif = factors[fid].n_start_i_vif;
      compact_factors_weightids[c_edge] = factors[fid].weight_id;
      c_edge++;
    }
  }
}

void dd::FactorGraph::safety_check() {
  // check whether variables, factors, and weights are stored
  // in the order of their id
  long s = n_var;
  for (long i = 0; i < s; i++) {
    assert(this->variables[i].id == i);
  }
  s = n_factor;
  for (long i = 0; i < s; i++) {
    assert(this->factors[i].id == i);
  }
  s = n_weight;
  for (long i = 0; i < s; i++) {
    assert(this->weights[i].id == i);
  }
  this->safety_check_passed = true;
}
