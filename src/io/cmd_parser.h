
#include <string>
#include <iostream>
#include <algorithm>
#include <assert.h>
#include <tclap/CmdLine.h>

#ifndef _CMD_PARSER_H_
#define _CMD_PARSER_H_

namespace dd {

/**
 * Command line argument parser
 */
class CmdParser {
 public:
  // all the arguments are defined in cmd_parser.cpp
  std::string app_name;

  std::string fg_file;
  std::string variable_file;
  std::string factor_file;
  std::string edge_file;
  std::string weight_file;
  std::string output_folder;
  std::string domain_file;

  std::string original_folder;
  std::string delta_folder;

  int n_learning_epoch;
  int n_samples_per_learning_epoch;
  int n_inference_epoch;
  int n_datacopy;
  int burn_in;
  double stepsize;
  double stepsize2;
  double decay;
  double reg_param;
  std::string regularization;

  bool should_be_quiet;
  bool should_sample_evidence;
  bool should_learn_non_evidence;

  CmdParser(std::string _app_name);

  /**
   * parses the given command line arguments
   */
  void parse(int argc, char **argv);

 private:
  TCLAP::ValueArg<std::string> *fg_file_;

  TCLAP::ValueArg<std::string> *original_folder_;
  TCLAP::ValueArg<std::string> *delta_folder_;

  TCLAP::ValueArg<std::string> *edge_file_;
  TCLAP::ValueArg<std::string> *weight_file_;
  TCLAP::ValueArg<std::string> *variable_file_;
  TCLAP::ValueArg<std::string> *factor_file_;
  TCLAP::ValueArg<std::string> *output_folder_;
  TCLAP::ValueArg<std::string> *domain_file_;

  TCLAP::MultiArg<int> *n_learning_epoch_;
  TCLAP::MultiArg<int> *n_samples_per_learning_epoch_;
  TCLAP::MultiArg<int> *n_inference_epoch_;

  TCLAP::MultiArg<double> *stepsize_;
  TCLAP::MultiArg<double> *stepsize2_;
  TCLAP::MultiArg<double> *decay_;

  TCLAP::MultiArg<int> *n_datacopy_;
  TCLAP::MultiArg<double> *reg_param_;
  TCLAP::MultiArg<int> *burn_in_;

  TCLAP::MultiSwitchArg *quiet_;
  TCLAP::MultiSwitchArg *sample_evidence_;
  TCLAP::MultiSwitchArg *learn_non_evidence_;

  TCLAP::MultiArg<std::string> *regularization_;

  TCLAP::CmdLine *cmd_;
};
}

#endif
