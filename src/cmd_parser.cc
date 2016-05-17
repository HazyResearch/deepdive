
#include "cmd_parser.h"

namespace dd {

constexpr char DimmWittedVersion[] = "0.01";

// a handy way to get value from MultiArg
template <class T>
static inline T getLastValueOrDefault(TCLAP::MultiArg<T>* arg, T defaultValue) {
  if (arg->getValue().empty()) {
    return defaultValue;
  } else {
    return arg->getValue().back();
  }
}

CmdParser::CmdParser(int argc, const char* const argv[]) {
  const char* arg0 = argv[0];
  app_name = argc > 1 ? argv[1] : "";
  ++argv;
  --argc;

  // for TCLAP, see:
  // http://tclap.sourceforge.net/manual.html#FUNDAMENTAL_CLASSES

  if (app_name == "gibbs") {
    cmd_ = new TCLAP::CmdLine("DimmWitted GIBBS", ' ', DimmWittedVersion);

    fg_file_ = new TCLAP::ValueArg<std::string>("m", "fg_meta",
                                                "factor graph metadata file",
                                                false, "", "string", *cmd_);
    variable_file_ = new TCLAP::ValueArg<std::string>(
        "v", "variables", "variables file", false, "", "string", *cmd_);
    factor_file_ = new TCLAP::ValueArg<std::string>(
        "f", "factors", "factors file", false, "", "string", *cmd_);
    edge_file_ = new TCLAP::ValueArg<std::string>("e", "edges", "edges file",
                                                  false, "", "string", *cmd_);
    weight_file_ = new TCLAP::ValueArg<std::string>(
        "w", "weights", "weights file", false, "", "string", *cmd_);
    output_folder_ = new TCLAP::ValueArg<std::string>(
        "o", "outputFile", "Output Folder", false, "", "string", *cmd_);
    domain_file_ = new TCLAP::ValueArg<std::string>(
        "", "domains", "Multinomial domains", false, "", "string", *cmd_);

    original_folder_ = new TCLAP::ValueArg<std::string>(
        "r", "ori_folder", "Folder of original factor graph", false, "",
        "string", *cmd_);
    delta_folder_ = new TCLAP::ValueArg<std::string>(
        "j", "delta_folder", "Folder of delta factor graph", false, "",
        "string", *cmd_);

    graph_snapshot_file_ = new TCLAP::ValueArg<std::string>(
        "", "graph_snapshot_file", "Binary file containing graph snapshot",
        false, "", "string", *cmd_);
    weights_snapshot_file_ = new TCLAP::ValueArg<std::string>(
        "", "weight_snapshot_file",
        "Binary file containing snapshot of weight values", false, "", "string",
        *cmd_);

    n_learning_epoch_ = new TCLAP::MultiArg<int>("l", "n_learning_epoch",
                                                 "Number of Learning Epochs",
                                                 true, "int", *cmd_);
    n_samples_per_learning_epoch_ = new TCLAP::MultiArg<int>(
        "s", "n_samples_per_learning_epoch",
        "Number of Samples per Leraning Epoch", true, "int", *cmd_);
    n_inference_epoch_ = new TCLAP::MultiArg<int>(
        "i", "n_inference_epoch", "Number of Samples for Inference", true,
        "int", *cmd_);
    burn_in_ = new TCLAP::MultiArg<int>("", "burn_in", "Burn-in period", false,
                                        "int", *cmd_);
    n_datacopy_ = new TCLAP::MultiArg<int>("c", "n_datacopy",
                                           "Number of factor graph copies",
                                           false, "int", *cmd_);
    stepsize_ = new TCLAP::MultiArg<double>("a", "alpha", "Stepsize", false,
                                            "double", *cmd_);
    stepsize2_ = new TCLAP::MultiArg<double>("p", "stepsize", "Stepsize", false,
                                             "double", *cmd_);
    decay_ = new TCLAP::MultiArg<double>(
        "d", "diminish", "Decay of stepsize per epoch", false, "double", *cmd_);
    reg_param_ = new TCLAP::MultiArg<double>("b", "reg_param",
                                             "l2 regularization parameter",
                                             false, "double", *cmd_);
    regularization_ = new TCLAP::MultiArg<std::string>(
        "", "regularization", "Regularization (l1 or l2)", false, "string",
        *cmd_);

    use_snapshot_ = new TCLAP::MultiSwitchArg(
        "u", "use_snapshot",
        "resume computation from snapshotted graph and weights", *cmd_);
    quiet_ = new TCLAP::MultiSwitchArg("q", "quiet", "quiet output", *cmd_);
    sample_evidence_ = new TCLAP::MultiSwitchArg(
        "", "sample_evidence", "also sample evidence variables in inference",
        *cmd_);
    learn_non_evidence_ = new TCLAP::MultiSwitchArg(
        "", "learn_non_evidence", "sample non-evidence variables in learning",
        *cmd_);

    cmd_->parse(argc, argv);

    fg_file = fg_file_->getValue();
    variable_file = variable_file_->getValue();
    factor_file = factor_file_->getValue();
    edge_file = edge_file_->getValue();
    weight_file = weight_file_->getValue();
    output_folder = output_folder_->getValue();
    domain_file = domain_file_->getValue();

    delta_folder = delta_folder_->getValue();
    original_folder = original_folder_->getValue();

    n_learning_epoch = getLastValueOrDefault(n_learning_epoch_, -1);
    n_samples_per_learning_epoch =
        getLastValueOrDefault(n_samples_per_learning_epoch_, -1);
    n_inference_epoch = getLastValueOrDefault(n_inference_epoch_, -1);
    n_datacopy = getLastValueOrDefault(n_datacopy_, 0);
    burn_in = getLastValueOrDefault(burn_in_, 0);
    stepsize = getLastValueOrDefault(stepsize_, 0.01);
    stepsize2 = getLastValueOrDefault(stepsize2_, 0.01);
    if (stepsize == 0.01)
      stepsize =
          stepsize2;  // XXX hack to support two parameters to specify step size
    decay = getLastValueOrDefault(decay_, 0.95);
    reg_param = getLastValueOrDefault(reg_param_, 0.01);
    regularization = getLastValueOrDefault(regularization_, std::string("l2"));

    should_use_snapshot = use_snapshot_->getValue() > 0;
    should_be_quiet = quiet_->getValue() > 0;
    should_sample_evidence = sample_evidence_->getValue() > 0;
    should_learn_non_evidence = learn_non_evidence_->getValue() > 0;

  } else if (app_name == "text2bin") {
    cmd_ = new TCLAP::CmdLine("DimmWitted text2bin", ' ', DimmWittedVersion);
    TCLAP::UnlabeledValueArg<std::string> text2bin_mode_(
        "mode", "what to convert", true, "",
        "variable | domain | factor | weight", *cmd_);
    TCLAP::UnlabeledValueArg<std::string> text2bin_input_(
        "input", "path to an input file formatted in TSV, tab-separated values",
        true, "/dev/stdin", "input_file_path", *cmd_);
    TCLAP::UnlabeledValueArg<std::string> text2bin_output_(
        "output", "path to an output file", true, "/dev/stdout",
        "output_file_path", *cmd_);

    //  factor-specific arguments
    TCLAP::UnlabeledValueArg<int> text2bin_factor_func_id_(
        "func_id",
        "factor function id (See: "
        "https://github.com/HazyResearch/sampler/blob/master/src/dstruct/"
        "factor_graph/factor.h)",
        true, 0, "func_id");
    TCLAP::UnlabeledValueArg<int> text2bin_factor_arity_(
        "arity", "arity of the factor, e.g., 1 | 2 | ...", true, 1, "arity");
    TCLAP::UnlabeledValueArg<std::string> text2bin_factor_ignored_(
        "ignored", "original", true, "original", "ignored");
    TCLAP::UnlabeledMultiArg<int> text2bin_factor_positives_or_not_(
        "var_is_positive",
        "whether each variable in position is positive or not, 1 or 0", 1,
        "var_is_positive");
    if (argc > 0 && std::string(argv[1]) == "factor") {
      cmd_->add(text2bin_factor_func_id_);
      cmd_->add(text2bin_factor_arity_);
      cmd_->add(text2bin_factor_ignored_);
      cmd_->add(text2bin_factor_positives_or_not_);
    }

    cmd_->parse(argc, argv);

    text2bin_mode = text2bin_mode_.getValue();
    text2bin_input = text2bin_input_.getValue();
    text2bin_output = text2bin_output_.getValue();
    text2bin_factor_func_id =
        static_cast<FACTOR_FUCNTION_TYPE>(text2bin_factor_func_id_.getValue());
    text2bin_factor_arity = text2bin_factor_arity_.getValue();
    for (int positive_or_not : text2bin_factor_positives_or_not_.getValue()) {
      text2bin_factor_positives_or_not.push_back(positive_or_not != 0);
    }

  } else if (app_name == "bin2text") {
    cmd_ = new TCLAP::CmdLine("DimmWitted bin2text", ' ', DimmWittedVersion);

    fg_file_ = new TCLAP::ValueArg<std::string>("m", "fg_meta",
                                                "factor graph metadata file",
                                                false, "", "string", *cmd_);
    variable_file_ = new TCLAP::ValueArg<std::string>(
        "v", "variables", "variables file", false, "", "string", *cmd_);
    factor_file_ = new TCLAP::ValueArg<std::string>(
        "f", "factors", "factors file", false, "", "string", *cmd_);
    weight_file_ = new TCLAP::ValueArg<std::string>(
        "w", "weights", "weights file", false, "", "string", *cmd_);
    output_folder_ = new TCLAP::ValueArg<std::string>(
        "o", "outputFile", "Output Folder", false, "", "string", *cmd_);
    domain_file_ = new TCLAP::ValueArg<std::string>(
        "", "domains", "Multinomial domains", false, "", "string", *cmd_);

    cmd_->parse(argc, argv);

    fg_file = fg_file_->getValue();
    variable_file = variable_file_->getValue();
    factor_file = factor_file_->getValue();
    weight_file = weight_file_->getValue();
    output_folder = output_folder_->getValue();
    domain_file = domain_file_->getValue();

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
}
