#ifndef DIMMWITTED_CMD_PARSER_H_
#define DIMMWITTED_CMD_PARSER_H_

#include "common.h"
#include "factor.h"

#include <algorithm>
#include <assert.h>
#include <iostream>
#include <string>

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
  std::string weight_file;
  std::string output_folder;
  std::string domain_file;

  num_epochs_t n_learning_epoch;
  num_samples_t n_samples_per_learning_epoch;
  num_epochs_t n_inference_epoch;
  size_t n_datacopy;
  num_epochs_t burn_in;
  double stepsize;
  double stepsize2;
  double decay;
  double reg_param;
  enum regularization regularization;

  bool should_be_quiet;
  bool should_sample_evidence;
  bool should_learn_non_evidence;

  // text2bin specific members
  std::string text2bin_mode;
  std::string text2bin_input;
  std::string text2bin_output;
  factor_function_type_t text2bin_factor_func_id;
  factor_arity_t text2bin_factor_arity;
  std::vector<variable_value_t> text2bin_factor_variables_should_equal_to;

  CmdParser(int argc, const char *const argv[]);

  /**
   * parses the given command line arguments
   */
  void parse(int argc, char **argv);

 private:
  friend std::ostream &operator<<(std::ostream &stream,
                                  const CmdParser &cmd_parser);
};

}  // namespace dd

#endif  // DIMMWITTED_CMD_PARSER_H_
