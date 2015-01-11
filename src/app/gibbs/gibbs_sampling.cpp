
#include "app/gibbs/gibbs_sampling.h"
#include "app/gibbs/single_node_sampler.h"
#include "io/pb_parser.h"
#include "common.h"
#include <unistd.h>
#include <fstream>
#include "timer.h"

/*!
 * \brief In this function, the factor graph is located to each NUMA node.
 * 
 * TODO: in the near future, this allocation should be abstracted
 * into a higher-level class to avoid writing similar things
 * for Gibbs, NN, SGD etc. However, this is the task of next pass
 * of refactoring.
 */
void dd::GibbsSampling::prepare(){

  //n_numa_nodes = numa_max_node();
  n_numa_nodes = 0;
  n_thread_per_numa = (sysconf(_SC_NPROCESSORS_CONF))/(n_numa_nodes+1);
  //n_thread_per_numa /= 2;
  //if(n_thread_per_numa == 0){
  //  n_thread_per_numa = 1;
  //}
  //n_thread_per_numa = 1;

  this->factorgraphs.push_back(*p_fg);
  for(int i=1;i<=n_numa_nodes;i++){

    numa_run_on_node(i);
    numa_set_localalloc();

    std::cout << "CREATE FG ON NODE ..." <<  i << std::endl;
    dd::FactorGraph fg(p_fg->n_var, p_fg->n_factor, p_fg->n_weight, p_fg->n_edge);
    
    fg.copy_from(p_fg);
    //fg.load(*p_cmd_parser);

    this->factorgraphs.push_back(fg);
  }

}

void dd::GibbsSampling::inference(const int & n_epoch){

  Timer t_total;

  Timer t;
  int nvar = this->factorgraphs[0].n_var;
  int nnode = n_numa_nodes + 1;

  std::vector<SingleNodeSampler> single_node_samplers;
  for(int i=0;i<=n_numa_nodes;i++){
    single_node_samplers.push_back(SingleNodeSampler(&this->factorgraphs[i], 
      n_thread_per_numa, i));
  }

  for(int i=0;i<=n_numa_nodes;i++){
    single_node_samplers[i].clear_variabletally();
  }

  for(int i_epoch=0;i_epoch<n_epoch;i_epoch++){

    std::cout << std::setprecision(2) << "INFERENCE EPOCH " << i_epoch * nnode <<  "~" 
      << ((i_epoch+1) * nnode) << "...." << std::flush;

    t.restart();

    for(int i=0;i<nnode;i++){
      single_node_samplers[i].sample();
    }

    for(int i=0;i<nnode;i++){
      single_node_samplers[i].wait();
    }
    double elapsed = t.elapsed();
    std::cout << ""  << elapsed << " sec." ;
    std::cout << ","  << (nvar*nnode)/elapsed << " vars/sec" << std::endl;
  }

  double elapsed = t_total.elapsed();
  std::cout << "TOTAL INFERENCE TIME: " << elapsed << " sec." << std::endl;

}

void dd::GibbsSampling::learn(const int & n_epoch, const int & n_sample_per_epoch, 
                              const double & stepsize, const double & decay){

  Timer t_total;

  double current_stepsize = stepsize;

  Timer t;
  int nvar = this->factorgraphs[0].n_var;
  int nnode = n_numa_nodes + 1;
  int nweight = this->factorgraphs[0].n_weight;

  std::vector<SingleNodeSampler> single_node_samplers;
  for(int i=0;i<=n_numa_nodes;i++){
    single_node_samplers.push_back(SingleNodeSampler(&this->factorgraphs[i], n_thread_per_numa, i));
  }

  double * ori_weights = new double[nweight];
  memcpy(ori_weights, this->factorgraphs[0].infrs->weight_values, sizeof(double)*nweight);


  for(int i_epoch=0;i_epoch<n_epoch;i_epoch++){

    std::cout << std::setprecision(2) << "LEARNING EPOCH " << i_epoch * nnode <<  "~" 
      << ((i_epoch+1) * nnode) << "...." << std::flush;

    t.restart();
    
    for(int i=0;i<nnode;i++){
      single_node_samplers[i].p_fg->stepsize = current_stepsize;
    }

    for(int i=0;i<nnode;i++){
      single_node_samplers[i].sample_sgd();
    }

    for(int i=0;i<nnode;i++){
      single_node_samplers[i].wait_sgd();
    }

    FactorGraph & cfg = this->factorgraphs[0];
    for(int i=1;i<=n_numa_nodes;i++){
      FactorGraph & cfg_other = this->factorgraphs[i];
      for(int j=0;j<nweight;j++){
        cfg.infrs->weight_values[j] += cfg_other.infrs->weight_values[j];
      }
    }

    for(int j=0;j<nweight;j++){
      cfg.infrs->weight_values[j] /= nnode;
      if(cfg.infrs->weights_isfixed[j] == false){
        cfg.infrs->weight_values[j] *= (1.0/(1.0+0.01*current_stepsize));
      }
    }

    for(int i=1;i<=n_numa_nodes;i++){
      FactorGraph &cfg_other = this->factorgraphs[i];
      for(int j=0;j<nweight;j++){
        if(cfg.infrs->weights_isfixed[j] == false){
          cfg_other.infrs->weight_values[j] = cfg.infrs->weight_values[j];
        }
      }
    }    


    double lmax = -1000000;
    double l2=0.0;
    for(int i=0;i<nweight;i++){
      double diff = fabs(ori_weights[i] - cfg.infrs->weight_values[i]);
      ori_weights[i] = cfg.infrs->weight_values[i];
      l2 += diff*diff;
      if(lmax < diff){
        lmax = diff;
      }
    }
    lmax = lmax/current_stepsize;
    
    double elapsed = t.elapsed();
    std::cout << "" << elapsed << " sec.";
    std::cout << ","  << (nvar*nnode)/elapsed << " vars/sec." << ",stepsize=" << current_stepsize << ",lmax=" << lmax << ",l2=" << sqrt(l2)/current_stepsize << std::endl;
    //std::cout << "     " << this->compact_factors[0].fg_mutable->weights[0] << std::endl;

    current_stepsize = current_stepsize * decay;

  }

  double elapsed = t_total.elapsed();
  std::cout << "TOTAL LEARNING TIME: " << elapsed << " sec." << std::endl;

}

void dd::GibbsSampling::dump_weights(){

  std::cout << "LEARNING SNIPPETS (QUERY WEIGHTS):" << std::endl;
  FactorGraph const & cfg = this->factorgraphs[0];
  int ct = 0;
  for(size_t i=0;i<cfg.infrs->nweights;i++){
    ct ++;
    std::cout << "   " << i << " " << cfg.infrs->weight_values[i] << std::endl;
    if(ct % 10 == 0){
      break;
    }
  }
  std::cout << "   ..." << std::endl; 

  std::string filename_protocol = p_cmd_parser->output_folder->getValue() 
    + "/inference_result.out.weights";
  std::string filename_text = p_cmd_parser->output_folder->getValue() 
    + "/inference_result.out.weights.text";

  std::cout << "DUMPING... PROTOCOL: " << filename_protocol << std::endl;
  std::cout << "DUMPING... TEXT    : " << filename_text << std::endl;

  std::ofstream fout_text(filename_text.c_str());
  std::ofstream mFs(filename_protocol.c_str(),std::ios::out | std::ios::binary);
  google::protobuf::io::OstreamOutputStream *_OstreamOutputStream = 
    new google::protobuf::io::OstreamOutputStream(&mFs);
  google::protobuf::io::CodedOutputStream *_CodedOutputStream = 
    new google::protobuf::io::CodedOutputStream(_OstreamOutputStream);
  deepdive::WeightInferenceResult msg;
  for(size_t i=0;i<cfg.infrs->nweights;i++){
    fout_text << i << " " << cfg.infrs->weight_values[i] << std::endl;
    msg.set_id(i);
    msg.set_value(cfg.infrs->weight_values[i]);
    _CodedOutputStream->WriteVarint32(msg.ByteSize());
    if ( !msg.SerializeToCodedStream(_CodedOutputStream) ){
      std::cout << "SerializeToCodedStream error " << std::endl;
      assert(false);
    } 
  }
  delete _CodedOutputStream;
  delete _OstreamOutputStream;
  mFs.close();
  fout_text.close();

}


void dd::GibbsSampling::dump(){

  double * agg_means = new double[factorgraphs[0].n_var];
  double * agg_nsamples = new double[factorgraphs[0].n_var];
  int * multinomial_tallies = new int[factorgraphs[0].infrs->ntallies];

  for(long i=0;i<factorgraphs[0].n_var;i++){
    agg_means[i] = 0;
    agg_nsamples[i] = 0;
  }

  for(long i=0;i<factorgraphs[0].infrs->ntallies;i++){
    multinomial_tallies[i] = 0;
  }

  for(int i=0;i<=n_numa_nodes;i++){
    const FactorGraph & cfg = factorgraphs[i];
    for(long i=0;i<factorgraphs[0].n_var;i++){
      const Variable & variable = factorgraphs[0].variables[i];
      agg_means[variable.id] += cfg.infrs->agg_means[variable.id];
      agg_nsamples[variable.id] += cfg.infrs->agg_nsamples[variable.id];
    }
    for(long i=0;i<factorgraphs[0].infrs->ntallies;i++){
      multinomial_tallies[i] += cfg.infrs->multinomial_tallies[i];
    }
  }

  std::cout << "INFERENCE SNIPPETS (QUERY VARIABLES):" << std::endl;
  int ct = 0;
  for(long i=0;i<factorgraphs[0].n_var;i++){
    const Variable & variable = factorgraphs[0].variables[i];
    if(variable.is_evid == false){
      ct ++;
      std::cout << "   " << variable.id << " EXP=" 
                << agg_means[variable.id]/agg_nsamples[variable.id] << "  NSAMPLE=" 
                << agg_nsamples[variable.id] << std::endl;

      if(variable.domain_type != DTYPE_BOOLEAN){
        if(variable.domain_type == DTYPE_MULTINOMIAL){
          for(int j=0;j<=variable.upper_bound;j++){
            std::cout << "        @ " << j << " -> " << 1.0*multinomial_tallies[variable.n_start_i_tally + j]/agg_nsamples[variable.id] << std::endl;
          }
        }else{
          std::cout << "ERROR: Only support boolean variables for now!" << std::endl;
          assert(false);
        }
      }

      if(ct % 10 == 0){
        break;
      }
    }
  }
  std::cout << "   ..." << std::endl; 

  std::string filename_protocol = p_cmd_parser->output_folder->getValue() + 
    "/inference_result.out";
  std::string filename_text = p_cmd_parser->output_folder->getValue() + 
    "/inference_result.out.text";
  std::cout << "DUMPING... PROTOCOL: " << filename_protocol << std::endl;
  std::cout << "DUMPING... TEXT    : " << filename_text << std::endl;
  std::ofstream fout_text(filename_text.c_str());
  std::ofstream mFs(filename_protocol.c_str(),std::ios::out | std::ios::binary);
  google::protobuf::io::OstreamOutputStream *_OstreamOutputStream = 
    new google::protobuf::io::OstreamOutputStream(&mFs);
  google::protobuf::io::CodedOutputStream *_CodedOutputStream = 
    new google::protobuf::io::CodedOutputStream(_OstreamOutputStream);
  deepdive::VariableInferenceResult msg;
  for(long i=0;i<factorgraphs[0].n_var;i++){
    const Variable & variable = factorgraphs[0].variables[i];
    if(variable.is_evid == true){
      continue;
    }

    msg.set_id(variable.id);
    msg.set_category(1.0);
    msg.set_expectation(agg_means[variable.id]/agg_nsamples[variable.id]);
    
    if(variable.domain_type != DTYPE_BOOLEAN){
      if(variable.domain_type == DTYPE_MULTINOMIAL){
        for(int j=0;j<=variable.upper_bound;j++){
          msg.set_category(j);
          msg.set_expectation(1.0*multinomial_tallies[variable.n_start_i_tally + j]/agg_nsamples[variable.id]);
          
          fout_text << variable.id << " " << j << " " << (1.0*multinomial_tallies[variable.n_start_i_tally + j]/agg_nsamples[variable.id]) << std::endl;

          _CodedOutputStream->WriteVarint32(msg.ByteSize());
          if ( !msg.SerializeToCodedStream(_CodedOutputStream) ){
            std::cout << "SerializeToCodedStream error " << std::endl;
            assert(false);
          }
        }
      }else{
        std::cout << "ERROR: Only support boolean variables for now!" << std::endl;
        assert(false);
      }
    }else{
      fout_text << variable.id << " " << 1 << " " << (agg_means[variable.id]/agg_nsamples[variable.id]) << std::endl;

      _CodedOutputStream->WriteVarint32(msg.ByteSize());
      if ( !msg.SerializeToCodedStream(_CodedOutputStream) ){
        std::cout << "SerializeToCodedStream error " << std::endl;
        assert(false);
      }
    }
  }
  delete _CodedOutputStream;
  delete _OstreamOutputStream;
  mFs.close();
  fout_text.close();

  std::cout << "INFERENCE CALIBRATION (QUERY BINS):" << std::endl;
  std::vector<int> abc;
  for(int i=0;i<=10;i++){
    abc.push_back(0);
  }
  int bad = 0;
  for(long i=0;i<factorgraphs[0].n_var;i++){
    const Variable & variable = factorgraphs[0].variables[i];
    if(variable.is_evid == true){
      continue;
    }
    int bin = (int)(agg_means[variable.id]/agg_nsamples[variable.id]*10);
    if(bin >= 0 && bin <=10){
      abc[bin] ++;
    }else{
      //std::cout << variable.id << "   " << variable.agg_mean << "   " << variable.n_sample << std::endl;
      bad ++;
    }
  }
  abc[9] += abc[10];
  for(int i=0;i<10;i++){
    std::cout << "PROB BIN 0." << i << "~0." << (i+1) << "  -->  # " << abc[i] << std::endl;
  }

}










