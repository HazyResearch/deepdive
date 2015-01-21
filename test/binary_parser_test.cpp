/**
 * Unit tests for binary parser
 *
 * Author: Feiran Wang
 */

#include "gibbs.h"
#include "gtest/gtest.h"
#include "io/binary_parser.h"
#include "dstruct/factor_graph/factor_graph.h"
#include "dstruct/factor_graph/factor.h"
#include <fstream>

using namespace dd;

// the factor graph used for test is from biased coin, which contains 18 variables,
// 1 weight, 18 factors, and 18 edges. Variables of id 0-8 are evidence: id 0-7 
// positive and id 8 negative.

// test read_variables
TEST(BinaryParserTest, read_variables) {
	dd::FactorGraph fg(18, 1, 1, 1);
	long nvars = read_variables("./test/coin/graph.variables", fg);
	EXPECT_EQ(nvars, 18);
	EXPECT_EQ(fg.c_nvar, 18);
	EXPECT_EQ(fg.n_evid, 9);
	EXPECT_EQ(fg.n_query, 9);
	EXPECT_EQ(fg.variables[1].id, 1);
	EXPECT_EQ(fg.variables[1].domain_type, DTYPE_BOOLEAN);
	EXPECT_EQ(fg.variables[1].is_evid, true);
	EXPECT_EQ(fg.variables[1].lower_bound, 0);
	EXPECT_EQ(fg.variables[1].upper_bound, 1);
	EXPECT_EQ(fg.variables[1].assignment_evid, 1);
	EXPECT_EQ(fg.variables[1].assignment_free, 1);
}

// test read_factors
TEST(BinaryParserTest, read_factors) {
	dd::FactorGraph fg(1, 18, 1, 1);
	int nfactors = read_factors("./test/coin/graph.factors", fg);
	EXPECT_EQ(nfactors, 18);
	EXPECT_EQ(fg.c_nfactor, 18);
	EXPECT_EQ(fg.factors[0].id, 0);
	EXPECT_EQ(fg.factors[0].weight_id, 0);
	EXPECT_EQ(fg.factors[0].func_id, FUNC_ISTRUE);
	EXPECT_EQ(fg.factors[0].n_variables, 1);
}

// test read_weights
TEST(BinaryParserTest, read_weights) {
	dd::FactorGraph fg(1, 1, 1, 1);
	int nweights = read_weights("./test/coin/graph.weights", fg);
	EXPECT_EQ(nweights, 1);
	EXPECT_EQ(fg.c_nweight, 1);
	EXPECT_EQ(fg.weights[0].id, 0);
	EXPECT_EQ(fg.weights[0].isfixed, false);
	EXPECT_EQ(fg.weights[0].weight, 0.0);
}

// test read_edges
TEST(BinaryParserTest, read_edges) {
	dd::FactorGraph fg(18, 18, 1, 18);
	int nedges = read_edges("./test/coin/graph.edges", fg);
	EXPECT_EQ(nedges, 18);
	EXPECT_EQ(fg.factors[1].tmp_variables[0].vid, 1);
	EXPECT_EQ(fg.factors[1].tmp_variables[0].n_position, 0);
	EXPECT_EQ(fg.factors[1].tmp_variables[0].is_positive, true);
}