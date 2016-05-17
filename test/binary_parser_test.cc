/**
 * Unit tests for binary format
 *
 * Author: Feiran Wang
 */

#include "factor.h"
#include "factor_graph.h"
#include "dimmwitted.h"
#include "binary_format.h"
#include <gtest/gtest.h>
#include <fstream>

using namespace dd;

// the factor graph used for test is from biased coin, which contains 18
// variables,
// 1 weight, 18 factors, and 18 edges. Variables of id 0-8 are evidence: id 0-7
// positive and id 8 negative.

// test read_variables
TEST(BinaryFormatTest, read_variables) {
  system(
      "./text2bin variable test/biased_coin/variables.tsv "
      "test/biased_coin/graph.variables");
  dd::FactorGraph fg(18, 1, 1, 1);
  fg.load_variables("./test/biased_coin/graph.variables");
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
TEST(BinaryFormatTest, read_factors) {
  system(
      "./text2bin factor test/biased_coin/factors.tsv "
      "test/biased_coin/graph.factors 4 1 0 "
      "1");
  dd::FactorGraph fg(18, 18, 1, 18);
  fg.load_factors("./test/biased_coin/graph.factors");
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
TEST(BinaryFormatTest, read_weights) {
  system(
      "./text2bin weight test/biased_coin/weights.tsv "
      "test/biased_coin/graph.weights");
  dd::FactorGraph fg(1, 1, 1, 1);
  fg.load_weights("./test/biased_coin/graph.weights");
  EXPECT_EQ(fg.c_nweight, 1);
  EXPECT_EQ(fg.weights[0].id, 0);
  EXPECT_EQ(fg.weights[0].isfixed, false);
  EXPECT_EQ(fg.weights[0].weight, 0.0);
}

// test read domains
TEST(BinaryFormatTest, read_domains) {
  system(
      "./text2bin domain test/domains/domains.tsv "
      "test/domains/graph.domains");
  int num_variables = 3;
  int domain_sizes[] = {1, 2, 3};
  dd::FactorGraph fg(num_variables, 1, 1, 1);

  // add variables
  for (int i = 0; i < num_variables; i++) {
    fg.variables[i] = dd::RawVariable(i, 0, 0, domain_sizes[i], 0, 0, 0, 0);
  }
  fg.load_domains("./test/domains/graph.domains");

  for (int i = 0; i < num_variables; i++) {
    EXPECT_EQ(fg.variables[i].domain_map->size(), domain_sizes[i]);
  }

  EXPECT_EQ(fg.variables[2].get_domain_index(1), 0);
  EXPECT_EQ(fg.variables[2].get_domain_index(3), 1);
  EXPECT_EQ(fg.variables[2].get_domain_index(5), 2);
}
