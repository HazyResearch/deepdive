
#include "io/cmd_parser.h"

namespace dd {

CmdParser::CmdParser(std::string _app_name) {
  app_name = _app_name;

  if (app_name == "mat" || app_name == "gibbs" || app_name == "inc") {
    cmd_ = new TCLAP::CmdLine("DimmWitted GIBBS", ' ', "0.01");

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

    quiet_ = new TCLAP::MultiSwitchArg("q", "quiet", "quiet output", *cmd_);
    sample_evidence_ = new TCLAP::MultiSwitchArg(
        "", "sample_evidence", "also sample evidence variables in inference",
        *cmd_);
    learn_non_evidence_ = new TCLAP::MultiSwitchArg(
        "", "learn_non_evidence", "sample non-evidence variables in learning",
        *cmd_);
  } else {
    std::cout << "ERROR: UNKNOWN APP NAME " << app_name << std::endl;
    std::cout << "AVAILABLE APP {gibbs/mat/inc}" << app_name << std::endl;
    assert(false);
  }
}

// a handy way to get value from MultiArg
template <class T>
static inline T getLastValueOrDefault(TCLAP::MultiArg<T> *arg, T defaultValue) {
  if (arg->getValue().empty()) {
    return defaultValue;
  } else {
    return arg->getValue().back();
  }
}

void CmdParser::parse(int argc, char **argv) {
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
  decay = getLastValueOrDefault(decay_, 0.95);
  reg_param = getLastValueOrDefault(reg_param_, 0.01);
  regularization = getLastValueOrDefault(regularization_, std::string("l2"));

  should_be_quiet = quiet_->getValue() > 0;
  should_sample_evidence = sample_evidence_->getValue() > 0;
  should_learn_non_evidence = learn_non_evidence_->getValue() > 0;
}
}
