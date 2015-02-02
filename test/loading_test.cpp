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
// the factor graph used for test is from biased coin, which contains 18 variables,
// 1 weight, 18 factors, and 18 edges. Variables of id 0-8 are evidence: id 0-7 
// positive and id 8 negative.
class LoadingTest : public testing::Test {
protected:

	dd::FactorGraph fg;

	LoadingTest() : fg(dd::FactorGraph(18, 18, 1, 18)) {}

	virtual void SetUp() {
		const char* argv[23] = {
			"dw", "gibbs", "-w", "./test/coin/graph.weights", "-v", "./test/coin/graph.variables", 
			"-f", "./test/coin/graph.factors", "-e", "./test/coin/graph.edges", "-m", "./test/coin/graph.meta",
			"-o", ".", "-l", "100", "-i", "100", "-s", "1", "--alpha", "0.1", ""
		};
		dd::CmdParser cmd_parser = parse_input(23, (char **)argv);
		fg.load(cmd_parser, false);
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

// test for FactorGraph::sort_by_id function
TEST_F(LoadingTest, sort_by_id) {
	fg.sort_by_id();
	for (int i = 0; i < fg.c_nvar; i++) {
		EXPECT_EQ(fg.variables[i].id, i);
	}
	for (int i = 0; i < fg.n_factor; i++) {
		EXPECT_EQ(fg.factors[i].id, i);
	}
	for (int i = 0; i < fg.n_weight; i++) {
		EXPECT_EQ(fg.weights[i].id, i);
	}
}

// test for FactorGraph::organize_graph_by_edge function
TEST_F(LoadingTest, organize_graph_by_edge) {
	fg.sort_by_id();
	fg.organize_graph_by_edge();
	fg.safety_check();
}

// test for FactorGraph::copy_from function
TEST_F(LoadingTest, copy_from) {
	fg.sort_by_id();
	fg.organize_graph_by_edge();

	dd::FactorGraph fg2(18, 18, 1, 18);
	fg2.copy_from(&fg);

	EXPECT_TRUE(memcmp(&fg, &fg2, sizeof(fg)));
}

