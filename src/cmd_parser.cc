#include "cmd_parser.h"

#define HAVE_LONG_LONG  // necessary for uint64_t arg parsing
#include <tclap/CmdLine.h>

namespace dd {

constexpr char DimmWittedVersion[] = "0.01";

// a handy way to get value from MultiArg
template <typename T>
static inline const T& getLastValueOrDefault(TCLAP::MultiArg<T>& arg,
                                             const T& defaultValue) {
  return arg.getValue().empty() ? defaultValue : arg.getValue().back();
}

CmdParser::CmdParser(int argc, const char* const argv[]) {
  const char* arg0 = argv[0];
  app_name = argc > 1 ? argv[1] : "";
  ++argv;
  --argc;

  // for TCLAP, see:
  // http://tclap.sourceforge.net/manual.html#FUNDAMENTAL_CLASSES

  if (app_name == "gibbs") {
    TCLAP::CmdLine cmd_("DimmWitted GIBBS", ' ', DimmWittedVersion);

    TCLAP::ValueArg<std::string> fg_file_("m", "fg_meta",
                                          "factor graph metadata file", false,
                                          "", "string", cmd_);
    TCLAP::ValueArg<std::string> variable_file_(
        "v", "variables", "variables file", false, "", "string", cmd_);
    TCLAP::ValueArg<std::string> factor_file_("f", "factors", "factors file",
                                              false, "", "string", cmd_);
    TCLAP::ValueArg<std::string> weight_file_("w", "weights", "weights file",
                                              false, "", "string", cmd_);
    TCLAP::ValueArg<std::string> output_folder_(
        "o", "outputFile", "Output Folder", false, "", "string", cmd_);
    TCLAP::ValueArg<std::string> domain_file_(
        "", "domains", "Categorical domains", false, "", "string", cmd_);

    TCLAP::MultiArg<num_epochs_t> n_learning_epoch_("l", "n_learning_epoch",
                                                    "Number of Learning Epochs",
                                                    true, "int", cmd_);
    TCLAP::MultiArg<num_samples_t> n_samples_per_learning_epoch_(
        "s", "n_samples_per_learning_epoch",
        "Number of Samples per Leraning Epoch", true, "int", cmd_);
    TCLAP::MultiArg<num_epochs_t> n_inference_epoch_(
        "i", "n_inference_epoch", "Number of Samples for Inference", true,
        "int", cmd_);
    TCLAP::MultiArg<num_epochs_t> burn_in_("", "burn_in", "Burn-in period",
                                           false, "int", cmd_);
    TCLAP::MultiArg<size_t> n_datacopy_(
        "c", "n_datacopy",
        "Number of factor graph copies. Use 0 for all "
        "available NUMA nodes (default)",
        false, "int", cmd_);
    TCLAP::MultiArg<double> stepsize_("a", "alpha", "Stepsize", false, "double",
                                      cmd_);
    TCLAP::MultiArg<double> stepsize2_("p", "stepsize", "Stepsize", false,
                                       "double", cmd_);
    TCLAP::MultiArg<double> decay_(
        "d", "diminish", "Decay of stepsize per epoch", false, "double", cmd_);
    TCLAP::MultiArg<double> reg_param_(
        "b", "reg_param", "l2 regularization parameter", false, "double", cmd_);
    TCLAP::MultiArg<std::string> regularization_("", "regularization",
                                                 "Regularization (l1 or l2)",
                                                 false, "string", cmd_);

    TCLAP::MultiSwitchArg quiet_("q", "quiet", "quiet output", cmd_);
    TCLAP::MultiSwitchArg sample_evidence_(
        "", "sample_evidence", "also sample evidence variables in inference",
        cmd_);
    TCLAP::MultiSwitchArg learn_non_evidence_(
        "", "learn_non_evidence", "sample non-evidence variables in learning",
        cmd_);

    cmd_.parse(argc, argv);

    fg_file = fg_file_.getValue();
    variable_file = variable_file_.getValue();
    factor_file = factor_file_.getValue();
    weight_file = weight_file_.getValue();
    output_folder = output_folder_.getValue();
    domain_file = domain_file_.getValue();

    n_learning_epoch =
        getLastValueOrDefault(n_learning_epoch_, (num_epochs_t)0);
    n_samples_per_learning_epoch =
        getLastValueOrDefault(n_samples_per_learning_epoch_, (num_epochs_t)1);
    n_inference_epoch =
        getLastValueOrDefault(n_inference_epoch_, (num_epochs_t)0);
    n_datacopy = getLastValueOrDefault(n_datacopy_, (size_t)0);
    burn_in = getLastValueOrDefault(burn_in_, (num_epochs_t)0);
    stepsize = getLastValueOrDefault(stepsize_, 0.01);
    stepsize2 = getLastValueOrDefault(stepsize2_, 0.01);
    if (stepsize == 0.01)
      stepsize =
          stepsize2;  // XXX hack to support two parameters to specify step size
    decay = getLastValueOrDefault(decay_, 0.95);
    reg_param = getLastValueOrDefault(reg_param_, 0.01);
    regularization =
        getLastValueOrDefault(regularization_, std::string("l2")) == "l1"
            ? REG_L1
            : REG_L2;

    should_be_quiet = quiet_.getValue() > 0;
    should_sample_evidence = sample_evidence_.getValue() > 0;
    should_learn_non_evidence = learn_non_evidence_.getValue() > 0;

  } else if (app_name == "text2bin") {
    TCLAP::CmdLine cmd_("DimmWitted text2bin", ' ', DimmWittedVersion);
    TCLAP::UnlabeledValueArg<std::string> text2bin_mode_(
        "mode", "what to convert", true, "",
        "variable | domain | factor | weight", cmd_);
    TCLAP::UnlabeledValueArg<std::string> text2bin_input_(
        "input", "path to an input file formatted in TSV, tab-separated values",
        true, "/dev/stdin", "input_file_path", cmd_);
    TCLAP::UnlabeledValueArg<std::string> text2bin_output_(
        "output", "path to an output file", true, "/dev/stdout",
        "output_file_path", cmd_);

    //  factor-specific arguments
    // TODO turn these into labeled args
    TCLAP::UnlabeledValueArg<int> text2bin_factor_func_id_(
        "func_id",
        "factor function id (See: enum FACTOR_FUNCTION_TYPE in "
        "https://github.com/HazyResearch/sampler/blob/master/src/common.h)",
        true, 0, "func_id");
    TCLAP::UnlabeledValueArg<int> text2bin_factor_arity_(
        "arity", "arity of the factor, e.g., 1 | 2 | ...", true, 1, "arity");
    TCLAP::UnlabeledMultiArg<variable_value_t>
        text2bin_factor_variables_should_equal_to_(
            "var_is_positive",
            "whether each variable in position is positive or not, 1 or 0", 1,
            "var_is_positive");
    if (argc > 0 && std::string(argv[1]) == "factor") {
      cmd_.add(text2bin_factor_func_id_);
      cmd_.add(text2bin_factor_arity_);
      cmd_.add(text2bin_factor_variables_should_equal_to_);
    }

    cmd_.parse(argc, argv);

    text2bin_mode = text2bin_mode_.getValue();
    text2bin_input = text2bin_input_.getValue();
    text2bin_output = text2bin_output_.getValue();
    text2bin_factor_func_id = static_cast<factor_function_type_t>(
        text2bin_factor_func_id_.getValue());
    text2bin_factor_arity = text2bin_factor_arity_.getValue();
    for (const auto& value :
         text2bin_factor_variables_should_equal_to_.getValue()) {
      text2bin_factor_variables_should_equal_to.push_back(value);
    }

  } else if (app_name == "bin2text") {
    TCLAP::CmdLine cmd_("DimmWitted bin2text", ' ', DimmWittedVersion);

    TCLAP::ValueArg<std::string> fg_file_("m", "fg_meta",
                                          "factor graph metadata file", false,
                                          "", "string", cmd_);
    TCLAP::ValueArg<std::string> variable_file_(
        "v", "variables", "variables file", false, "", "string", cmd_);
    TCLAP::ValueArg<std::string> factor_file_("f", "factors", "factors file",
                                              false, "", "string", cmd_);
    TCLAP::ValueArg<std::string> weight_file_("w", "weights", "weights file",
                                              false, "", "string", cmd_);
    TCLAP::ValueArg<std::string> output_folder_(
        "o", "outputFile", "Output Folder", false, "", "string", cmd_);
    TCLAP::ValueArg<std::string> domain_file_(
        "", "domains", "Categorical domains", false, "", "string", cmd_);

    cmd_.parse(argc, argv);

    fg_file = fg_file_.getValue();
    variable_file = variable_file_.getValue();
    factor_file = factor_file_.getValue();
    weight_file = weight_file_.getValue();
    output_folder = output_folder_.getValue();
    domain_file = domain_file_.getValue();

  } else {
    if (argc > 0) std::cerr << app_name << ": Unrecognized MODE" << std::endl;
    std::cerr << "Usage: " << arg0 << " [ gibbs | bin2text ]" << std::endl;
  }
}

std::ostream& operator<<(std::ostream& stream, const CmdParser& args) {
  stream << "#################GIBBS SAMPLING#################" << std::endl;
  stream << "# fg_file            : " << args.fg_file << std::endl;
  stream << "# weight_file        : " << args.weight_file << std::endl;
  stream << "# variable_file      : " << args.variable_file << std::endl;
  stream << "# factor_file        : " << args.factor_file << std::endl;
  stream << "# output_folder      : " << args.output_folder << std::endl;
  stream << "# n_learning_epoch   : " << args.n_learning_epoch << std::endl;
  stream << "# n_samples/l. epoch : " << args.n_samples_per_learning_epoch
         << std::endl;
  stream << "# n_inference_epoch  : " << args.n_inference_epoch << std::endl;
  stream << "# stepsize           : " << args.stepsize << std::endl;
  stream << "# decay              : " << args.decay << std::endl;
  stream << "# regularization     : " << args.reg_param << std::endl;
  stream << "# burn_in            : " << args.burn_in << std::endl;
  stream << "# learn_non_evidence : " << args.should_learn_non_evidence
         << std::endl;
  stream << "################################################" << std::endl;
  stream << "# IGNORE -s (n_samples/l. epoch). ALWAYS -s 1. #" << std::endl;
  stream << "# IGNORE -t (threads). ALWAYS USE ALL THREADS. #" << std::endl;
  return stream;
}

}  // namespace dd
