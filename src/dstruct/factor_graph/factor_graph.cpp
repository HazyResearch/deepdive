
#include <iostream>
#include "io/binary_parser.h"
#include "dstruct/factor_graph/factor_graph.h"
#include "dstruct/factor_graph/factor.h"

template <class OBJTOSORT>
class idsorter : public std::binary_function<OBJTOSORT, OBJTOSORT, bool>{
public:
  inline bool operator()(const OBJTOSORT & left, const OBJTOSORT & right){
    return left.id < right.id;  
  }
};


dd::InferenceResult::InferenceResult(long _nvars, long _nweights):
  nvars(_nvars),
  nweights(_nweights),
  agg_means(new double[_nvars]),
  agg_nsamples(new double[_nvars]),
  assignments_free(new VariableValue[_nvars]),
  assignments_evid(new VariableValue[_nvars]),
  weight_values(new double [_nweights]),
  weights_isfixed(new bool [_nweights]) {}

void dd::InferenceResult::init(Variable * variables, Weight * const weights){

  ntallies = 0;
  for(long t=0;t<nvars;t++){
    const Variable & variable = variables[t];
    assignments_free[variable.id] = variable.assignment_free;
    assignments_evid[variable.id] = variable.assignment_evid;
    agg_means[variable.id] = 0.0;
    agg_nsamples[variable.id] = 0.0;
    if(variable.domain_type == DTYPE_MULTINOMIAL){
      //variable.n_start_i_tally = ntallies;
      ntallies += variable.upper_bound - variable.lower_bound + 1;
    }
  }

  multinomial_tallies = new int[ntallies];
  for(long i=0;i<ntallies;i++){
    multinomial_tallies[i] = 0;
  }

  for(long t=0;t<nweights;t++){
    const Weight & weight = weights[t];
    weight_values[weight.id] = weight.weight;
    weights_isfixed[weight.id] = weight.isfixed;
  }
}


bool dd::FactorGraph::is_usable(){
  return this->sorted && this->safety_check_passed;
}

dd::FactorGraph::FactorGraph(long _n_var, long _n_factor, long _n_weight, long _n_edge) : 
  n_var(_n_var), n_factor(_n_factor), n_weight(_n_weight), n_edge(_n_edge),
  c_nvar(0), c_nfactor(0), c_nweight(0), n_evid(0), n_query(0),
  variables(new Variable[_n_var]),
  factors(new Factor[_n_factor]),
  weights(new Weight[_n_weight]),
  factors_dups(new CompactFactor[_n_edge]),
  factors_dups_weightids(new int[_n_edge]),
  factor_ids(new long[_n_edge]),
  vifs(new VariableInFactor[_n_edge]),
  infrs(new InferenceResult(_n_var, _n_weight)),
  sorted(false),
  safety_check_passed(false) {}

void dd::FactorGraph::copy_from(const FactorGraph * const p_other_fg){
  // copy each member from the given graph
  memcpy(variables, p_other_fg->variables, sizeof(Variable)*n_var);
  memcpy(factors, p_other_fg->factors, sizeof(Factor)*n_factor);
  memcpy(weights, p_other_fg->weights, sizeof(Weight)*n_weight);
  memcpy(factor_ids, p_other_fg->factor_ids, sizeof(long)*n_edge);
  memcpy(vifs, p_other_fg->vifs, sizeof(VariableInFactor)*n_edge);

  memcpy(factors_dups, p_other_fg->factors_dups, sizeof(CompactFactor)*n_edge);
  memcpy(factors_dups_weightids, p_other_fg->factors_dups_weightids, sizeof(int)*n_edge);

  c_nvar = p_other_fg->c_nvar;
  c_nfactor = p_other_fg->c_nfactor;
  c_nweight = p_other_fg->c_nweight;
  c_edge = p_other_fg->c_edge;
  sorted = p_other_fg->sorted;
  safety_check_passed = p_other_fg->safety_check_passed;

  infrs->init(variables, weights);
  infrs->ntallies = p_other_fg->infrs->ntallies;
  infrs->multinomial_tallies = new int[p_other_fg->infrs->ntallies];
  for(long i=0;i<infrs->ntallies;i++){
    infrs->multinomial_tallies[i] = p_other_fg->infrs->multinomial_tallies[i];
  }
}

long dd::FactorGraph::get_weightid(const VariableValue *assignments, const CompactFactor& fs, long vid, long proposal) {
  /**
   * The weight ids are aligned in a continuous region according
   * to the numerical order of variable values. 
   * Say for two variables v1, v2, v3, with cardinality d. The numerical value is
   * v1 * d^2 + v2 * d + v3.
   */
  long weight_offset = 0;
  // for each variable in the factor
  for (long i = fs.n_start_i_vif; i < fs.n_start_i_vif + fs.n_variables; i++) {
    const VariableInFactor & vif = vifs[i];
    if (vif.vid == vid) {
      weight_offset = weight_offset * (variables[vif.vid].upper_bound+1) + proposal;
    } else {
      weight_offset = weight_offset * (variables[vif.vid].upper_bound+1) + assignments[vif.vid];
    }
  }
  long base_offset = &fs - factors_dups; // note c++ will auto scale by sizeof(CompactFactor)
  return *(factors_dups_weightids + base_offset) + weight_offset;
}


void dd::FactorGraph::update_weight(const Variable & variable){
  // corresponding factors and weights in a continous region
  CompactFactor * const fs = factors_dups + variable.n_start_i_factors;
  const int * const ws = factors_dups_weightids + variable.n_start_i_factors;
  // for each factor
  for(long i=0;i<variable.n_factors;i++){
    // boolean variable
    if (variable.domain_type == DTYPE_BOOLEAN) {
      // only update weight when it is not fixed
      if(infrs->weights_isfixed[ws[i]] == false){
        // stochastic gradient ascent 
        // increment weight with stepsize * gradient of weight
        // gradient of weight = E[f|D] - E[f], where D is evidence variables, 
        // f is the factor function, E[] is expectation. Expectation is calculated
        // using a sample of the variable.
        infrs->weight_values[ws[i]] += 
          stepsize * (this->template potential<false>(fs[i]) - this->template potential<true>(fs[i]));
      }
    } else if (variable.domain_type == DTYPE_MULTINOMIAL) {
      // two weights need to be updated
      // sample with evidence fixed, I0, with corresponding weight w1
      // sample without evidence unfixed, I1, with corresponding weight w2 
      // gradient of wd0 = f(I0) - I(w1==w2)f(I1)
      // gradient of wd1 = I(w1==w2)f(I0) - f(I1)
      long wid1 = get_weightid(infrs->assignments_evid, fs[i], -1, -1);
      long wid2 = get_weightid(infrs->assignments_free, fs[i], -1, -1);
      int equal = (wid1 == wid2);

      if(infrs->weights_isfixed[wid1] == false){
        infrs->weight_values[wid1] += 
          stepsize * (this->template potential<false>(fs[i]) - equal * this->template potential<true>(fs[i]));
      }

      if(infrs->weights_isfixed[wid2] == false){
        infrs->weight_values[wid2] += 
          stepsize * (equal * this->template potential<false>(fs[i]) - this->template potential<true>(fs[i]));
      }
    }
  }
}

void dd::FactorGraph::load(const CmdParser & cmd, const bool is_quiet){

  // get factor graph file names from command line arguments
  std::string weight_file = cmd.weight_file->getValue();
  std::string variable_file = cmd.variable_file->getValue();
  std::string factor_file = cmd.factor_file->getValue();
  std::string edge_file = cmd.edge_file->getValue();

  std::string filename_edges = edge_file;
  std::string filename_factors = factor_file;
  std::string filename_variables = variable_file;
  std::string filename_weights = weight_file;

  // load variables
  long long n_loaded = read_variables(filename_variables, *this);
  assert(n_loaded == n_var);
  if (!is_quiet) {
    std::cout << "LOADED VARIABLES: #" << n_loaded << std::endl;
    std::cout << "         N_QUERY: #" << n_query << std::endl;
    std::cout << "         N_EVID : #" << n_evid << std::endl;  
  }

  // load factors
  n_loaded = read_factors(filename_factors, *this);
  assert(n_loaded == n_factor);
  if (!is_quiet) {
    std::cout << "LOADED FACTORS: #" << n_loaded << std::endl;
  }

  // load weights
  n_loaded = read_weights(filename_weights, *this);
  assert(n_loaded == n_weight);
  if (!is_quiet) {
    std::cout << "LOADED WEIGHTS: #" << n_loaded << std::endl;
  }

  // sort the above components
  // NOTE This is very important, as read_edges assume variables,
  // factors and weights are ordered so that their id is the index 
  // where they are stored in the array
  this->sort_by_id();

  // load edges
  n_loaded = read_edges(edge_file, *this);
  if (!is_quiet) {
    std::cout << "LOADED EDGES: #" << n_loaded << std::endl;
  }

  // construct edge-based store
  this->organize_graph_by_edge();
  this->safety_check();

  assert(this->is_usable() == true);

}

void dd::FactorGraph::sort_by_id() {
  // sort variables, factors, and weights by id
  std::sort(&variables[0], &variables[n_var], idsorter<Variable>());
  std::sort(&factors[0], &factors[n_factor], idsorter<Factor>());
  std::sort(&weights[0], &weights[n_weight], idsorter<Weight>()); 
  this->sorted = true;
  infrs->init(variables, weights);
}

void dd::FactorGraph::organize_graph_by_edge() {
  // number of edges
  c_edge = 0;
  // put variables into the edge-based variable array vifs
  for(long i=0;i<n_factor;i++){
    Factor & factor = factors[i];
    factor.n_start_i_vif = c_edge;
    for(const VariableInFactor & vif : factor.tmp_variables){
      vifs[c_edge] = vif;
      c_edge ++;
    }
  }

  c_edge = 0;
  long ntallies = 0;
  // for each variable, put the factors into factor_dups
  for(long i=0;i<n_var;i++){
    Variable & variable = variables[i];
    variable.n_factors = variable.tmp_factor_ids.size();  // no edge count any more
    
    variable.n_start_i_factors = c_edge;
    if(variable.domain_type == DTYPE_MULTINOMIAL){
      variable.n_start_i_tally = ntallies;
      ntallies += variable.upper_bound - variable.lower_bound + 1;
    }
    for(const long & fid : variable.tmp_factor_ids){
      factor_ids[c_edge] = fid;
      factors_dups[c_edge].id = factors[fid].id;
      factors_dups[c_edge].func_id = factors[fid].func_id;
      factors_dups[c_edge].n_variables = factors[fid].n_variables;
      factors_dups[c_edge].n_start_i_vif = factors[fid].n_start_i_vif;
      factors_dups_weightids[c_edge] = factors[fid].weight_id;
      c_edge ++;
    }
  }
}

void dd::FactorGraph::safety_check(){

  // check whether variables, factors, and weights are stored 
  // in the order of their id
  long s = n_var;
  for(long i=0;i<s;i++){
    assert(this->variables[i].id == i);
  }
  s = n_factor;
  for(long i=0;i<s;i++){
    assert(this->factors[i].id == i);
  }
  s = n_weight;
  for(long i=0;i<s;i++){
    assert(this->weights[i].id == i);
  }
  this->safety_check_passed = true;
}






