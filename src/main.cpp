
#include <assert.h>
#include <iostream>
#include "common.h"
#include <unistd.h>

#include "io/cmd_parser.h"
#include "io/pb_parser.h"
#include "io/binary_parser.h"

#include "app/gibbs/gibbs_sampling.h"
#include "dstruct/factor_graph/factor_graph.h"

/*
 * Parse input arguments
 */
dd::CmdParser parse_input(int argc, char** argv){
  // if(argv == 1){
  //   std::cout << "ERROR: APP_NAME REQUIRED " << std::endl;
  //   std::cout << "AVAILABLE APP_NAME {gibbs}" << std::endl;
  //   std::cout << "USAGE ./dw APP_NAME" << std::endl;
  //   assert(false);
  // }
  std::vector<std::string> new_args;
  if (argc < 2 || strcmp(argv[1], "gibbs") != 0) {
    new_args.push_back(std::string(argv[0]) + " " + "gibbs");
    new_args.push_back("-h");
  } else {
    new_args.push_back(std::string(argv[0]) + " " + std::string(argv[1]));
    for(int i=2;i<argc;i++){
      new_args.push_back(std::string(argv[i]));
    }
  }
  char ** new_argv = new char*[new_args.size()];
  for(int i=0;i<new_args.size();i++){
    new_argv[i] = new char[new_args[i].length() + 1];
    std::copy(new_args[i].begin(), new_args[i].end(), new_argv[i]);
    new_argv[i][new_args[i].length()] = '\0';
  }
  dd::CmdParser cmd_parser("gibbs");
  cmd_parser.parse(new_args.size(), new_argv);
  return cmd_parser;
}

void gibbs(dd::CmdParser & cmd_parser){

  int n_numa_node = numa_max_node() + 1;
  int n_thread_per_numa = (sysconf(_SC_NPROCESSORS_CONF))/(n_numa_node);

  std::string fg_file = cmd_parser.fg_file->getValue();

  std::string weight_file = cmd_parser.weight_file->getValue();
  std::string variable_file = cmd_parser.variable_file->getValue();
  std::string factor_file = cmd_parser.factor_file->getValue();
  std::string edge_file = cmd_parser.edge_file->getValue();

  std::string output_folder = cmd_parser.output_folder->getValue();

  int n_learning_epoch = cmd_parser.n_learning_epoch->getValue();
  int n_samples_per_learning_epoch = cmd_parser.n_samples_per_learning_epoch->getValue();
  int n_inference_epoch = cmd_parser.n_inference_epoch->getValue();

  double stepsize = cmd_parser.stepsize->getValue();
  double stepsize2 = cmd_parser.stepsize2->getValue();
  if (stepsize == 0.01) stepsize = stepsize2;
  double decay = cmd_parser.decay->getValue();

  std::cout << std::endl;
  std::cout << "#################MACHINE CONFIG#################" << std::endl;
  std::cout << "# # NUMA Node        : " << n_numa_node << std::endl;
  std::cout << "# # Thread/NUMA Node : " << n_thread_per_numa << std::endl;
  std::cout << "################################################" << std::endl;
  std::cout << std::endl;
  std::cout << "#################GIBBS SAMPLING#################" << std::endl;
  std::cout << "# fg_file            : " << fg_file << std::endl;
  std::cout << "# edge_file          : " << edge_file << std::endl;
  std::cout << "# weight_file        : " << weight_file << std::endl;
  std::cout << "# variable_file      : " << variable_file << std::endl;
  std::cout << "# factor_file        : " << factor_file << std::endl;
  std::cout << "# output_folder      : " << output_folder << std::endl;
  std::cout << "# n_learning_epoch   : " << n_learning_epoch << std::endl;
  std::cout << "# n_samples/l. epoch : " << n_samples_per_learning_epoch << std::endl;
  std::cout << "# n_inference_epoch  : " << n_inference_epoch << std::endl;
  std::cout << "# stepsize           : " << stepsize << std::endl;
  std::cout << "# decay              : " << decay << std::endl;
  std::cout << "################################################" << std::endl;
  std::cout << "# IGNORE -s (n_samples/l. epoch). ALWAYS -s 1. #" << std::endl;
  std::cout << "# IGNORE -t (threads). ALWAYS USE ALL THREADS. #" << std::endl;
  std::cout << "################################################" << std::endl;


  //deepdive::FactorGraph meta = dd::read_single_pb<deepdive::FactorGraph>(fg_file);
  Meta meta = read_meta(fg_file); 
  std::cout << "# nvar               : " << meta.num_variables << std::endl;
  std::cout << "# nfac               : " << meta.num_factors << std::endl;
  std::cout << "# nweight            : " << meta.num_weights << std::endl;
  std::cout << "# nedge              : " << meta.num_edges << std::endl;
  std::cout << "################################################" << std::endl;

  // std::cout << "# nvar               : " << meta.numvariables() << std::endl;
  // std::cout << "# nfac               : " << meta.numfactors() << std::endl;
  // std::cout << "# nweight            : " << meta.numweights() << std::endl;
  // std::cout << "# nedge              : " << meta.numedges() << std::endl;
  // std::cout << "################################################" << std::endl;


  numa_run_on_node(0);
  numa_set_localalloc();
  // dd::FactorGraph fg(meta.numvariables(), meta.numfactors(), meta.numweights(), meta.numedges());
  dd::FactorGraph fg(meta.num_variables, meta.num_factors, meta.num_weights, meta.num_edges);
  fg.load(cmd_parser);
  dd::GibbsSampling gibbs(&fg, &cmd_parser);

  int numa_aware_n_learning_epoch = (int)(n_learning_epoch/n_numa_node) + 
                            (n_learning_epoch%n_numa_node==0?0:1);

  gibbs.learn(numa_aware_n_learning_epoch, n_samples_per_learning_epoch, stepsize, decay);
  gibbs.dump_weights();

  int numa_aware_n_epoch = (int)(n_inference_epoch/n_numa_node) + 
                            (n_inference_epoch%n_numa_node==0?0:1);

  gibbs.inference(numa_aware_n_epoch);
  gibbs.dump();

}

int main(int argv, char** argc){

  dd::CmdParser cmd_parser = parse_input(argv, argc);

  if(cmd_parser.app_name == "gibbs"){
    gibbs(cmd_parser);
  }

}







