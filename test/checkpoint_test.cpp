/**
 * Unit tests for checkpointing
 *
 * Author: Ivan Gozali
 */

#include "dstruct/factor_graph/factor_graph.h"
#include "io/binary_parser.h"
#include "gibbs.h"
#include "gtest/gtest.h"
#include <fstream>

using namespace dd;

// test fixture
// the factor graph used for test is from biased coin, which contains 18
// variables,
// 1 weight, 18 factors, and 18 edges. Variables of id 0-8 are evidence: id 0-7
// positive and id 8 negative.
class CheckpointTest : public testing::Test {
 protected:
  dd::CompiledFactorGraph cfg;

  CheckpointTest() : cfg(dd::CompiledFactorGraph(18, 18, 1, 18)) {}

  virtual void SetUp() {
    system(
        "./text2bin variable test/biased_coin/variables.tsv "
        "test/biased_coin/graph.variables");
    system(
        "./text2bin factor test/biased_coin/factors.tsv "
        "test/biased_coin/graph.factors 4 1 0 1");
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

    dd::FactorGraph fg(18, 18, 1, 18);
    fg.load(cmd_parser, false, 0);

    fg.compile(cfg);
  }
};

/*
 * Test that resume and checkpoint works properly for one graph.
 *
 * Well, this test just got long.
 */
TEST_F(CheckpointTest, checkpoint_and_resume) {
  std::string filename("graph.checkpoint");

  /* Sort of gives you an idea how to call checkpoint() */
  std::vector<CompiledFactorGraph> cfgs;
  cfgs.push_back(cfg);
  checkpoint(filename, cfgs);

  /* And an idea on how to call resume() */
  CompiledFactorGraph resumed_cfg(18, 18, 1, 18);
  resume(filename, 0, resumed_cfg);

  ASSERT_EQ(resumed_cfg.n_var, cfg.n_var);
  ASSERT_EQ(resumed_cfg.n_factor, cfg.n_factor);
  ASSERT_EQ(resumed_cfg.n_weight, cfg.n_weight);
  ASSERT_EQ(resumed_cfg.n_edge, cfg.n_edge);

  /*
   * Check all arrays of variables, factors, compiled factors, compiled
   * variables, all that good stuff.
   */
  for (auto i = 0; i < cfg.n_var; i++) {
    ASSERT_EQ(resumed_cfg.variables[i].id, cfg.variables[i].id);
    ASSERT_EQ(resumed_cfg.variables[i].domain_type,
              cfg.variables[i].domain_type);

    ASSERT_EQ(resumed_cfg.variables[i].is_evid, cfg.variables[i].is_evid);
    ASSERT_EQ(resumed_cfg.variables[i].is_observation,
              cfg.variables[i].is_observation);
    ASSERT_EQ(resumed_cfg.variables[i].cardinality,
              cfg.variables[i].cardinality);

    ASSERT_EQ(resumed_cfg.variables[i].assignment_evid,
              cfg.variables[i].assignment_evid);
    ASSERT_EQ(resumed_cfg.variables[i].assignment_free,
              cfg.variables[i].assignment_free);

    ASSERT_EQ(resumed_cfg.variables[i].n_factors, cfg.variables[i].n_factors);
    ASSERT_EQ(resumed_cfg.variables[i].n_start_i_factors,
              cfg.variables[i].n_start_i_factors);

    ASSERT_EQ(resumed_cfg.variables[i].n_start_i_tally,
              cfg.variables[i].n_start_i_tally);
    ASSERT_EQ(resumed_cfg.variables[i].isactive, cfg.variables[i].isactive);

    /* TODO: What to do with domain_map */
  }

  for (auto i = 0; i < cfg.n_factor; i++) {
    ASSERT_EQ(resumed_cfg.factors[i].id, cfg.factors[i].id);
    ASSERT_EQ(resumed_cfg.factors[i].weight_id, cfg.factors[i].weight_id);
    ASSERT_EQ(resumed_cfg.factors[i].func_id, cfg.factors[i].func_id);
    ASSERT_EQ(resumed_cfg.factors[i].n_variables, cfg.factors[i].n_variables);
    ASSERT_EQ(resumed_cfg.factors[i].n_start_i_vif,
              cfg.factors[i].n_start_i_vif);

    /* TODO: What to do with weight_ids? */
  }

  /* Check vifs, compact_factor_weightids, compact_factors, and factor_ids */
  for (auto i = 0; i < cfg.n_edge; i++) {
    ASSERT_EQ(resumed_cfg.vifs[i].vid, cfg.vifs[i].vid);
    ASSERT_EQ(resumed_cfg.vifs[i].n_position, cfg.vifs[i].n_position);
    ASSERT_EQ(resumed_cfg.vifs[i].is_positive, cfg.vifs[i].is_positive);
    ASSERT_EQ(resumed_cfg.vifs[i].equal_to, cfg.vifs[i].equal_to);

    ASSERT_EQ(resumed_cfg.compact_factors[i].id, cfg.compact_factors[i].id);
    ASSERT_EQ(resumed_cfg.compact_factors[i].func_id,
              cfg.compact_factors[i].func_id);
    ASSERT_EQ(resumed_cfg.compact_factors[i].n_variables,
              cfg.compact_factors[i].n_variables);
    ASSERT_EQ(resumed_cfg.compact_factors[i].n_start_i_vif,
              cfg.compact_factors[i].n_start_i_vif);

    ASSERT_EQ(resumed_cfg.compact_factors_weightids[i],
              cfg.compact_factors_weightids[i]);

    ASSERT_EQ(resumed_cfg.factor_ids[i], cfg.factor_ids[i]);
  }

  /* More importantly, ensure that the inference results are equal */
  for (auto i = 0; i < cfg.n_var; i++) {
    ASSERT_EQ(resumed_cfg.infrs->agg_means[i], cfg.infrs->agg_means[i]);
    ASSERT_EQ(resumed_cfg.infrs->agg_nsamples[i], cfg.infrs->agg_nsamples[i]);
    ASSERT_EQ(resumed_cfg.infrs->assignments_free[i],
              cfg.infrs->assignments_free[i]);
    ASSERT_EQ(resumed_cfg.infrs->assignments_evid[i],
              cfg.infrs->assignments_evid[i]);
  }

  ASSERT_EQ(resumed_cfg.infrs->ntallies, cfg.infrs->ntallies);
  for (auto i = 0; i < cfg.infrs->ntallies; i++) {
    ASSERT_EQ(resumed_cfg.infrs->multinomial_tallies[i],
              cfg.infrs->multinomial_tallies[i]);
  }

  for (auto i = 0; i < cfg.n_weight; i++) {
    ASSERT_EQ(resumed_cfg.infrs->weight_values[i], cfg.infrs->weight_values[i]);
    ASSERT_EQ(resumed_cfg.infrs->weights_isfixed[i],
              cfg.infrs->weights_isfixed[i]);
  }
}
