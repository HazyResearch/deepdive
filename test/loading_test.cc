/**
 * Unit tests for loading factor graphs
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
class LoadingTest : public testing::Test {
 protected:
  FactorGraph fg;

  LoadingTest() : fg(FactorGraph({18, 18, 1, 18})) {}

  virtual void SetUp() {
    const char *argv[] = {
        "dw",      "gibbs",
        "-w",      "./test/biased_coin/graph.weights",
        "-v",      "./test/biased_coin/graph.variables",
        "-f",      "./test/biased_coin/graph.factors",
        "-m",      "./test/biased_coin/graph.meta",
        "-o",      ".",
        "-l",      "100",
        "-i",      "100",
        "--alpha", "0.1",
    };
    CmdParser cmd_parser(sizeof(argv) / sizeof(*argv), argv);
    fg.load_variables(cmd_parser.variable_file);
    fg.load_weights(cmd_parser.weight_file);
    fg.load_domains(cmd_parser.domain_file);
    fg.load_factors(cmd_parser.factor_file);
    fg.safety_check();
  }
};

// test for loading a factor graph
TEST_F(LoadingTest, load_factor_graph) {
  EXPECT_EQ(fg.size.num_variables, 18U);
  EXPECT_EQ(fg.size.num_variables_evidence, 9U);
  EXPECT_EQ(fg.size.num_variables_query, 9U);
  EXPECT_EQ(fg.size.num_factors, 18U);
  EXPECT_EQ(fg.size.num_weights, 1U);

  /* Due to how loading works in this new model, the factor graph is not
   * supposed to count the edges during loading. This only happens after
   * compile.
   */
  // EXPECT_EQ(fg.c_edge, 18);
}

// FN(6/5/2016): disabling this test because these steps are covered by
// fg.load() in SetUp()
//               and calling them for a second time results in seg fault
//               (due to dellocation of tmp_variables etc)
// // test for FactorGraph::compile() function
// TEST_F(LoadingTest, organize_graph_by_edge) {
//   CompactFactorGraph cfg(18, 18, 1, 18);
//   fg.compile(cfg);
//
//   EXPECT_EQ(cfg.size.num_variables, 18);
//   EXPECT_EQ(cfg.size.num_variables_evidence, 9);
//   EXPECT_EQ(cfg.size.num_variables_query, 9);
//   EXPECT_EQ(cfg.size.num_factors, 18);
//   EXPECT_EQ(cfg.size.num_weights, 1);
//   EXPECT_EQ(fg.c_edge, 18);
// }

// test for FactorGraph::copy_from function
TEST_F(LoadingTest, copy_from) {
  CompactFactorGraph cfg(fg);

  CompactFactorGraph cfg2 = cfg;

  EXPECT_TRUE(memcmp(&cfg, &cfg2, sizeof(cfg)));
}

}  // namespace dd
