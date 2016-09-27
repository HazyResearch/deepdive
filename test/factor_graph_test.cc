/**
 * Unit tests for factor graph functions
 *
 * Author: Feiran Wang
 */

#include "dimmwitted.h"
#include "factor_graph.h"
#include <fstream>
#include <gtest/gtest.h>

namespace dd {

// test fixture
// the factor graph used for test is from biased coin, which contains 18
// variables,
// 1 weight, 18 factors, and 18 edges. Variables of id 0-8 are evidence: id 0-7
// positive and id 8 negative.
class FactorGraphTest : public testing::Test {
 protected:
  std::unique_ptr<FactorGraph> cfg;
  std::unique_ptr<InferenceResult> infrs;
  std::unique_ptr<CmdParser> cmd_parser;

  virtual void SetUp() {
    const char* argv[] = {
        "dw",          "gibbs",
        "-w",          "./test/biased_coin/graph.weights",
        "-v",          "./test/biased_coin/graph.variables",
        "-f",          "./test/biased_coin/graph.factors",
        "-m",          "./test/biased_coin/graph.meta",
        "-o",          ".",
        "-l",          "100",
        "-i",          "100",
        "--alpha",     "0.1",
        "--reg_param", "0",
    };
    cmd_parser.reset(new CmdParser(sizeof(argv) / sizeof(*argv), argv));

    FactorGraph fg({18, 18, 1, 18});
    fg.load_variables(cmd_parser->variable_file);
    fg.load_weights(cmd_parser->weight_file);
    fg.load_domains(cmd_parser->domain_file);
    fg.load_factors(cmd_parser->factor_file);
    fg.safety_check();
    fg.construct_index();

    cfg.reset(new FactorGraph(fg));
    infrs.reset(new InferenceResult(*cfg, fg.weights.get(), *cmd_parser));
  }
};

// test sgd_on_variable function
TEST_F(FactorGraphTest, sgd_on_variable) {
  infrs->assignments_free[cfg->variables[0].id] = 0;

  cfg->sgd_on_variable(cfg->variables[0], *infrs, 0.1, false);
  std::cout << "The weight value is: " << infrs->weight_values[0] << std::endl;
  EXPECT_EQ(infrs->weight_values[0], 0.2);

  cfg->sgd_on_variable(cfg->variables[10], *infrs, 0.1, false);
  EXPECT_EQ(infrs->weight_values[0], 0.2);

  cfg->sgd_on_variable(cfg->variables[10], *infrs, 0.1, false);
  EXPECT_EQ(infrs->weight_values[0], 0.2);
}

}  // namespace dd
