#include "gtest/gtest.h"
#include "dstruct/factor_graph/factor_graph.h"
#include "gibbs.h"
#include <fstream>

using namespace dd;

// test fixture
class FactorGraphTest : public testing::Test {
protected:

	dd::FactorGraph fg;

	FactorGraphTest() : fg(dd::FactorGraph(18, 18, 1, 18)) {}

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

TEST_F(FactorGraphTest, update_variable) {

	fg.update<true>(fg.variables[0], 1);
	EXPECT_EQ(fg.infrs->assignments_free[0], 1);
	EXPECT_EQ(fg.infrs->assignments_evid[0], 1);

	fg.update<true>(fg.variables[0], 0);
	EXPECT_EQ(fg.infrs->assignments_free[0], 0);
	EXPECT_EQ(fg.infrs->assignments_evid[0], 1);

	fg.update<false>(fg.variables[10], 1);
	EXPECT_EQ(fg.infrs->assignments_free[10], 0);
	EXPECT_EQ(fg.infrs->assignments_evid[10], 1);

}

TEST_F(FactorGraphTest, update_weight) {
	fg.stepsize = 0.1;
	fg.update<true>(fg.variables[0], 0);

	fg.update_weight(fg.variables[0]);
	EXPECT_EQ(fg.infrs->weight_values[0], 0.1);

	fg.update_weight(fg.variables[10]);
	EXPECT_EQ(fg.infrs->weight_values[0], 0.1);

	fg.update_weight(fg.variables[10]);
	EXPECT_EQ(fg.infrs->weight_values[0], 0.1);

}

