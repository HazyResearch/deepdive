/**
 * Integration test for binary biased coin
 *
 * Author: Ce Zhang
 */

#include <limits.h>
#include "gibbs.h"
#include "gtest/gtest.h"
#include <fstream>

using namespace dd;

// the factor graph used for test is from biased coin, which contains 18 variables,
// 1 weight, 18 factors, and 18 edges. Variables of id 0-8 are evidence: id 0-7 
// positive and id 8 negative.
TEST(LogisticRegressionTest, INFERENCE) {

	const char* argv[23] = {
		"dw", "gibbs", "-w", "./test/coin/graph.weights", "-v", "./test/coin/graph.variables", 
		"-f", "./test/coin/graph.factors", "-e", "./test/coin/graph.edges", "-m", "./test/coin/graph.meta",
		"-o", ".", "-l", "800", "-i", "300", "-s", "1", "--alpha", "0.1", "--diminish 0.995"
	};

	dd::CmdParser cmd_parser = parse_input(23, (char **)argv);
	gibbs(cmd_parser);

	std::ifstream fin("./inference_result.out.text");
	int nvar = 0;
	int id, e;
	double prob;
	while(fin >> id >> e >> prob){
		EXPECT_NEAR(prob, 0.89, 0.1);
		nvar ++;
	}
	EXPECT_EQ(nvar, 9);

	std::ifstream fin_weight("./inference_result.out.weights.text");
	int nweight = 0;
	double weight;
	while(fin_weight >> id >> weight){
		EXPECT_NEAR(weight, 2.0, 0.3);
		nweight ++;
	}
	EXPECT_EQ(nweight, 1);

}

