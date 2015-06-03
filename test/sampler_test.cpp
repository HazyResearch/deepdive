/**
 * Unit tests for gibbs sampling
 *
 * Author: Feiran Wang
 */

#include "gtest/gtest.h"
#include "dstruct/factor_graph/factor_graph.h"
#include "app/gibbs/single_thread_sampler.h"
#include "gibbs.h"
#include <fstream>

// test fixture
class SamplerTest : public testing::Test {
protected:

	dd::FactorGraph fg;
	dd::SingleThreadSampler sampler;

	SamplerTest() : fg(dd::FactorGraph(18, 18, 1, 18)),
		sampler(dd::SingleThreadSampler(&fg, false, 0, false)) {}

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

// test for sample_sgd_single_variable
// the pseudo random number has been precalculated...
TEST_F(SamplerTest, sample_sgd_single_variable) {

	fg.update<true>(fg.variables[0], 1);
	fg.stepsize = 0.1;
	for (int i = 0; i < 3; i++) {
		sampler.p_rand_seed[i] = 1;
	}

	sampler.sample_sgd_single_variable(0);
	EXPECT_EQ(fg.infrs->weight_values[0], 0.1);

	sampler.sample_sgd_single_variable(0);
	EXPECT_EQ(fg.infrs->weight_values[0], 0.1);

	sampler.sample_sgd_single_variable(0);
	EXPECT_EQ(fg.infrs->weight_values[0], 0.1);

}

// test for sample_single_variable
TEST_F(SamplerTest, sample_single_variable) {
	fg.update<true>(fg.variables[0], 1);
	for (int i = 0; i < 3; i++) {
		sampler.p_rand_seed[i] = 1;
	}
	fg.infrs->weight_values[0] = 2;

	sampler.sample_single_variable(0);
	EXPECT_EQ(fg.infrs->assignments_evid[0], 1);

	sampler.sample_single_variable(10);
	EXPECT_EQ(fg.infrs->assignments_evid[10], 1);

	fg.infrs->weight_values[0] = 20;
	sampler.sample_single_variable(11);
	EXPECT_EQ(fg.infrs->assignments_evid[11], 1);

	sampler.sample_single_variable(12);
	EXPECT_EQ(fg.infrs->assignments_evid[12], 1);
}

