/**
 * Integration test for biased multinomial
 *
 * Author: Feiran Wang
 */

#include <limits.h>
#include "gibbs.h"
#include "gtest/gtest.h"
#include <fstream>
#include <cmath>

using namespace dd;

// the factor graph used for test is from biased multinomial, which contains 20 variables,
// Variable has cardinality of 4. Evidence variables: 0: 1, 1: 2, 2: 3, 3: 4
// where the first one is value, the second is count
TEST(MultinomialTest, LearningAndInference) {

	const char* argv[23] = {
		"dw", "gibbs", "-w", "./test/multinomial/graph.weights", "-v", "./test/multinomial/graph.variables", 
		"-f", "./test/multinomial/graph.factors", "-e", "./test/multinomial/graph.edges", "-m", "./test/multinomial/graph.meta",
		"-o", "./test/multinomial/", "-l", "2000", "-i", "2000", "-s", "1", "--alpha", "0.01", "--diminish 0.999"
	};

	dd::CmdParser cmd_parser = parse_input(23, (char **)argv);
	gibbs(cmd_parser);

	std::ifstream fin("./test/multinomial/inference_result.out.text");
	int nvar = 0;
	int id, e;
	double prob;
	while (fin >> id >> e >> prob) {
		EXPECT_NEAR(prob, 0.1, 0.05);
		fin >> id >> e >> prob;
		EXPECT_NEAR(prob, 0.2, 0.05);
		fin >> id >> e >> prob;
		EXPECT_NEAR(prob, 0.3, 0.05);
		fin >> id >> e >> prob;
		EXPECT_NEAR(prob, 0.4, 0.05);
		nvar++;
	}
	EXPECT_EQ(nvar, 10);

	std::ifstream fin_weight("./test/multinomial/inference_result.out.weights.text");
	double weight[4];
	fin_weight >> id >> weight[0];
	fin_weight >> id >> weight[1];
	fin_weight >> id >> weight[2];
	fin_weight >> id >> weight[3];
	double partition = 0;
	for (int i = 0; i < 4; i++) {
		partition += exp(weight[i]);
	}
	EXPECT_NEAR(exp(weight[0])/partition, 0.1, 0.04);
	EXPECT_NEAR(exp(weight[1])/partition, 0.2, 0.04);
	EXPECT_NEAR(exp(weight[2])/partition, 0.3, 0.04);
	EXPECT_NEAR(exp(weight[3])/partition, 0.4, 0.04);
}

