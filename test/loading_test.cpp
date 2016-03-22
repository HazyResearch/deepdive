/**
 * Unit tests for loading factor graphs
 *
 * Author: Feiran Wang
 */

#include "gtest/gtest.h"
#include "dstruct/factor_graph/factor_graph.h"
#include "gibbs.h"
#include <fstream>

using namespace dd;

// test fixture
// the factor graph used for test is from biased coin, which contains 18
// variables,
// 1 weight, 18 factors, and 18 edges. Variables of id 0-8 are evidence: id 0-7
// positive and id 8 negative.
class LoadingTest : public testing::Test {
 protected:
  dd::FactorGraph fg;

  LoadingTest() : fg(dd::FactorGraph(18, 18, 1, 18)) {}

  virtual void SetUp() {
    system(
        "./text2bin variable test/biased_coin/variables.tsv "
        "test/biased_coin/graph.variables");
    system(
        "./text2bin factor test/biased_coin/factors.tsv "
        "test/biased_coin/graph.factors 4 1 0 "
        "1");
    system(
        "./text2bin weight test/biased_coin/weights.tsv "
        "test/biased_coin/graph.weights");
    const char *argv[21] = {"dw",      "gibbs",
                            "-w",      "./test/biased_coin/graph.weights",
                            "-v",      "./test/biased_coin/graph.variables",
                            "-f",      "./test/biased_coin/graph.factors",
                            "-m",      "./test/biased_coin/graph.meta",
                            "-o",      ".",
                            "-l",      "100",
                            "-i",      "100",
                            "-s",      "1",
                            "--alpha", "0.1",
                            ""};
    dd::CmdParser cmd_parser = parse_input(21, (char **)argv);
    fg.load(cmd_parser, false, 0);
  }
};

// test for loading a factor graph
TEST_F(LoadingTest, load_factor_graph) {
  EXPECT_EQ(fg.c_nvar, 18);
  EXPECT_EQ(fg.n_evid, 9);
  EXPECT_EQ(fg.n_query, 9);
  EXPECT_EQ(fg.c_nfactor, 18);
  EXPECT_EQ(fg.c_nweight, 1);
  EXPECT_EQ(fg.c_edge, 18);
}

// test for FactorGraph::organize_graph_by_edge function
TEST_F(LoadingTest, organize_graph_by_edge) {
  fg.organize_graph_by_edge();
  fg.safety_check();
}

// test for FactorGraph::copy_from function
TEST_F(LoadingTest, copy_from) {
  dd::FactorGraph fg2(18, 18, 1, 18);
  fg2.copy_from(&fg);

  EXPECT_TRUE(memcmp(&fg, &fg2, sizeof(fg)));
}
