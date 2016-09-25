/**
 * Unit tests for binary format
 *
 * Author: Feiran Wang
 */

#include "binary_format.h"
#include "dimmwitted.h"
#include "factor.h"
#include "factor_graph.h"
#include <fstream>
#include <gtest/gtest.h>

namespace dd {

// the factor graph used for test is from biased coin, which contains 18
// variables,
// 1 weight, 18 factors, and 18 edges. Variables of id 0-8 are evidence: id 0-7
// positive and id 8 negative.

// test read_variables
TEST(BinaryFormatTest, read_variables) {
  FactorGraph fg({18, 1, 1, 1});
  fg.load_variables("./test/biased_coin/graph.variables");
  EXPECT_EQ(fg.size.num_variables, 18U);
  EXPECT_EQ(fg.size.num_variables_evidence, 9U);
  EXPECT_EQ(fg.size.num_variables_query, 9U);
  EXPECT_EQ(fg.variables[1].id, 1U);
  EXPECT_EQ(fg.variables[1].domain_type, DTYPE_BOOLEAN);
  EXPECT_EQ(fg.variables[1].is_evid, true);
  EXPECT_EQ(fg.variables[1].cardinality, 2U);
  EXPECT_EQ(fg.variables[1].assignment_dense, 1U);
}

// test read_factors
TEST(BinaryFormatTest, read_factors) {
  FactorGraph fg({18, 18, 1, 18});
  fg.load_variables("./test/biased_coin/graph.variables");
  fg.load_factors("./test/biased_coin/graph.factors");
  EXPECT_EQ(fg.size.num_factors, 18U);
  EXPECT_EQ(fg.factors[0].id, 0U);
  EXPECT_EQ(fg.factors[0].weight_id, 0U);
  EXPECT_EQ(fg.factors[0].func_id, FUNC_ISTRUE);
  EXPECT_EQ(fg.factors[0].num_vars, 1U);
  EXPECT_EQ(fg.get_factor_vif_at(fg.factors[1], 0).vid, 1U);
}

// test read_weights
TEST(BinaryFormatTest, read_weights) {
  FactorGraph fg({1, 1, 1, 1});
  fg.load_weights("./test/biased_coin/graph.weights");
  EXPECT_EQ(fg.size.num_weights, 1U);
  EXPECT_EQ(fg.weights[0].id, 0U);
  EXPECT_EQ(fg.weights[0].isfixed, false);
  EXPECT_EQ(fg.weights[0].weight, 0.0);
}

// test read domains
TEST(BinaryFormatTest, read_domains) {
  size_t num_variables = 3;
  size_t domain_sizes[] = {1, 2, 3};
  FactorGraph fg({num_variables, 1, 1, 1});

  // add variables
  for (size_t i = 0; i < num_variables; ++i) {
    fg.variables[i] = Variable(i, DTYPE_CATEGORICAL, false, domain_sizes[i], 0);
  }
  fg.load_domains("./test/domains/graph.domains");

  for (size_t i = 0; i < num_variables; ++i) {
    EXPECT_EQ(fg.variables[i].domain_map->size(), domain_sizes[i]);
  }

  EXPECT_EQ(fg.variables[2].get_domain_index(1), 0U);
  EXPECT_EQ(fg.variables[2].get_domain_index(3), 1U);
  EXPECT_EQ(fg.variables[2].get_domain_index(5), 2U);
}

}  // namespace dd
