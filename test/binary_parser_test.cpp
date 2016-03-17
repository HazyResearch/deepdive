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

// the factor graph used for test is from biased coin, which contains 18
// variables,
// 1 weight, 18 factors, and 18 edges. Variables of id 0-8 are evidence: id 0-7
// positive and id 8 negative.

// test read_variables
TEST(BinaryParserTest, read_variables) {
  system(
      "./text2bin variable test/coin/variables.tsv test/coin/graph.variables");
  dd::FactorGraph fg(18, 1, 1, 1);
  long nvars = read_variables("./test/coin/graph.variables", fg);
  EXPECT_EQ(nvars, 18);
  EXPECT_EQ(fg.c_nvar, 18);
  EXPECT_EQ(fg.n_evid, 9);
  EXPECT_EQ(fg.n_query, 9);
  EXPECT_EQ(fg.variables[1].id, 1);
  EXPECT_EQ(fg.variables[1].domain_type, DTYPE_BOOLEAN);
  EXPECT_EQ(fg.variables[1].is_evid, true);
  EXPECT_EQ(fg.variables[1].cardinality, 2);
  EXPECT_EQ(fg.variables[1].assignment_evid, 1);
  EXPECT_EQ(fg.variables[1].assignment_free, 1);
}

// test read_factors
TEST(BinaryParserTest, read_factors) {
  system(
      "./text2bin factor test/coin/factors.tsv test/coin/graph.factors 4 1 0 "
      "1");
  dd::FactorGraph fg(18, 18, 1, 18);
  int nfactors = read_factors("./test/coin/graph.factors", fg);
  EXPECT_EQ(nfactors, 18);
  EXPECT_EQ(fg.c_nfactor, 18);
  EXPECT_EQ(fg.factors[0].id, 0);
  EXPECT_EQ(fg.factors[0].weight_id, 0);
  EXPECT_EQ(fg.factors[0].func_id, FUNC_ISTRUE);
  EXPECT_EQ(fg.factors[0].n_variables, 1);
  EXPECT_EQ(fg.factors[1].tmp_variables[0].vid, 1);
  EXPECT_EQ(fg.factors[1].tmp_variables[0].n_position, 0);
  EXPECT_EQ(fg.factors[1].tmp_variables[0].is_positive, true);
}

// test read_weights
TEST(BinaryParserTest, read_weights) {
  system("./text2bin weight test/coin/weights.tsv test/coin/graph.weights");
  dd::FactorGraph fg(1, 1, 1, 1);
  int nweights = read_weights("./test/coin/graph.weights", fg);
  EXPECT_EQ(nweights, 1);
  EXPECT_EQ(fg.c_nweight, 1);
  EXPECT_EQ(fg.weights[0].id, 0);
  EXPECT_EQ(fg.weights[0].isfixed, false);
  EXPECT_EQ(fg.weights[0].weight, 0.0);
}

// test read domains
TEST(BinaryParserTest, read_domains) {
  system(
      "./text2bin domain test/domains/domains.tsv test/domains/graph.domains");
  int num_variables = 3;
  int domain_sizes[] = {1, 2, 3};
  dd::FactorGraph fg(num_variables, 1, 1, 1);
  // add variables
  for (int i = 0; i < num_variables; i++) {
    fg.variables[i] = dd::Variable(i, 0, 0, domain_sizes[i], 0, 0, 0, 0);
  }
  read_domains("./test/domains/graph.domains", fg);

  for (int i = 0; i < num_variables; i++) {
    EXPECT_EQ(fg.variables[i].domain.size(), domain_sizes[i]);
    EXPECT_EQ(fg.variables[i].domain_map.size(), domain_sizes[i]);
  }

  EXPECT_EQ(fg.variables[2].domain[0], 1);
  EXPECT_EQ(fg.variables[2].domain[1], 3);
  EXPECT_EQ(fg.variables[2].domain[2], 5);

  EXPECT_EQ(fg.variables[2].domain_map[1], 0);
  EXPECT_EQ(fg.variables[2].domain_map[3], 1);
  EXPECT_EQ(fg.variables[2].domain_map[5], 2);
}
