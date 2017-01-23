#ifndef DIMMWITTED_CMD_PARSER_H_
#define DIMMWITTED_CMD_PARSER_H_

#include "common.h"

#include <string>
#include <vector>
#include <map>
#include <ostream>

namespace dd {

/**
 * Command line argument parser
 */
class CmdParser {
 private:
  size_t num_errors_;

  /**
   * A handy way to check conditions, record errors, and produce error messages.
   */
  std::ostream &check(bool condition);
  /**
   * A handy way to generate warning messages but don't count them as errors.
   */
  std::ostream &recommend(bool condition);

 public:
  // all the arguments are defined in cmd_parser.cpp
  std::string app_name;

  std::string fg_file;
  std::vector<std::string> variable_file;
  std::vector<std::string> domain_file;
  std::vector<std::string> factor_file;
  std::vector<std::string> weight_file;
  std::string output_folder;

  size_t n_learning_epoch;
  size_t n_inference_epoch;
  size_t n_datacopy;
  size_t n_threads;
  size_t burn_in;
  double stepsize;
  double stepsize2;
  double decay;
  double reg_param;
  regularization_t regularization;

  bool should_be_quiet;
  bool should_sample_evidence;
  bool should_learn_non_evidence;

  // when on, train with VariableToFactor.truthiness instead of
  // Variable.assignment_dense
  bool is_noise_aware;

  // text2bin specific members
  std::string text2bin_mode;
  std::string text2bin_input;
  std::string text2bin_output;
  std::string text2bin_count_output;
  FACTOR_FUNCTION_TYPE text2bin_factor_func_id;
  size_t text2bin_factor_arity;
  std::vector<size_t> text2bin_factor_variables_should_equal_to;

  size_t num_errors() { return num_errors_; }

  /**
   * Constructs by parsing the given command line arguments
   */
  CmdParser(
      int argc, const char *const argv[],
      const std::map<std::string, int (*)(const CmdParser &)> &modes = {});
};

template <typename T>
std::ostream &operator<<(std::ostream &out, const std::vector<T> &v);

std::ostream &operator<<(std::ostream &stream, const CmdParser &cmd_parser);

}  // namespace dd

#endif  // DIMMWITTED_CMD_PARSER_H_
