/**
 * Unit tests for factor graph functions
 *
 * Author: Feiran Wang
 */

#include "factor_graph.h"
#include "dimmwitted.h"
#include <gtest/gtest.h>
#include <fstream>

namespace dd {

// test fixture
// the factor graph used for test is from biased coin, which contains 18
// variables,
// 1 weight, 18 factors, and 18 edges. Variables of id 0-8 are evidence: id 0-7
// positive and id 8 negative.
class FactorGraphTest : public testing::Test {
 protected:
  dd::CompiledFactorGraph cfg;

  FactorGraphTest() : cfg(dd::CompiledFactorGraph({18, 18, 1, 18})) {}

  virtual void SetUp() {
    const char* argv[] = {
        "dw",      "gibbs",
        "-w",      "./test/biased_coin/graph.weights",
        "-v",      "./test/biased_coin/graph.variables",
        "-f",      "./test/biased_coin/graph.factors",
        "-m",      "./test/biased_coin/graph.meta",
        "-o",      ".",
        "-l",      "100",
        "-i",      "100",
        "-s",      "1",
        "--alpha", "0.1",
    };
    dd::CmdParser cmd_parser(sizeof(argv) / sizeof(*argv), argv);

    dd::FactorGraph fg({18, 18, 1, 18});
    fg.load_variables(cmd_parser.variable_file);
    fg.load_weights(cmd_parser.weight_file);
    fg.load_domains(cmd_parser.domain_file);
    fg.load_factors(cmd_parser.factor_file);
    fg.safety_check();

    fg.compile(cfg);
  }
};

// test update_weight function
TEST_F(FactorGraphTest, update_weight) {
  cfg.infrs->assignments_free[cfg.variables[0].id] = 0;

  cfg.update_weight(cfg.variables[0], *cfg.infrs, 0.1);
  std::cout << "The weight value is: " << cfg.infrs->weight_values[0]
            << std::endl;
  EXPECT_EQ(cfg.infrs->weight_values[0], 0.1);

  cfg.update_weight(cfg.variables[10], *cfg.infrs, 0.1);
  EXPECT_EQ(cfg.infrs->weight_values[0], 0.1);

  cfg.update_weight(cfg.variables[10], *cfg.infrs, 0.1);
  EXPECT_EQ(cfg.infrs->weight_values[0], 0.1);
}

}  // namespace dd
