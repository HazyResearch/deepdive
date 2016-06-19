#ifndef DIMMWITTED_CMD_PARSER_H_
#define DIMMWITTED_CMD_PARSER_H_

#include "common.h"
#include "factor.h"

#include <algorithm>
#include <assert.h>
#include <iostream>
#include <string>
#include <tclap/CmdLine.h>

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

  std::string graph_snapshot_file;
  std::string weights_snapshot_file;

  size_t n_learning_epoch;
  size_t n_samples_per_learning_epoch;
  size_t n_inference_epoch;
  size_t n_datacopy;
  size_t burn_in;
  double stepsize;
  double stepsize2;
  double decay;
  double reg_param;
  enum regularization regularization;

  bool should_use_snapshot;
  bool should_be_quiet;
  bool should_sample_evidence;
  bool should_learn_non_evidence;

  // text2bin specific members
  std::string text2bin_mode;
  std::string text2bin_input;
  std::string text2bin_output;
  FACTOR_FUCNTION_TYPE text2bin_factor_func_id;
  size_t text2bin_factor_arity;
  std::vector<bool> text2bin_factor_positives_or_not;

  CmdParser(int argc, const char *const argv[]);

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

  TCLAP::ValueArg<std::string> *graph_snapshot_file_;
  TCLAP::ValueArg<std::string> *weights_snapshot_file_;

  TCLAP::MultiArg<int> *n_learning_epoch_;
  TCLAP::MultiArg<int> *n_samples_per_learning_epoch_;
  TCLAP::MultiArg<int> *n_inference_epoch_;

  TCLAP::MultiArg<double> *stepsize_;
  TCLAP::MultiArg<double> *stepsize2_;
  TCLAP::MultiArg<double> *decay_;

  TCLAP::MultiArg<int> *n_datacopy_;
  TCLAP::MultiArg<double> *reg_param_;
  TCLAP::MultiArg<int> *burn_in_;

  TCLAP::MultiSwitchArg *use_snapshot_;
  TCLAP::MultiSwitchArg *quiet_;
  TCLAP::MultiSwitchArg *sample_evidence_;
  TCLAP::MultiSwitchArg *learn_non_evidence_;

  TCLAP::MultiArg<std::string> *regularization_;

  TCLAP::CmdLine *cmd_;

  friend std::ostream &operator<<(std::ostream &stream,
                                  const CmdParser &cmd_parser);
};

}  // namespace dd

#endif  // DIMMWITTED_CMD_PARSER_H_
