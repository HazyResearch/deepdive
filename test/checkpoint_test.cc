/**
 * Unit tests for checkpointing
 *
 * Author: Ivan Gozali
 */

#include "factor_graph.h"
#include "binary_format.h"
#include "dimmwitted.h"
#include <gtest/gtest.h>
#include <fstream>

namespace dd {

const std::string get_copy_filename(const std::string &, int);

// test fixture
// the factor graph used for test is from biased coin, which contains 18
// variables,
// 1 weight, 18 factors, and 18 edges. Variables of id 0-8 are evidence: id 0-7
// positive and id 8 negative.
class CheckpointTest : public testing::Test {
 protected:
  dd::CompiledFactorGraph cfg;
  std::string checkpoint_filename;

  CheckpointTest()
      : cfg(dd::CompiledFactorGraph(18, 18, 1, 18)),
        checkpoint_filename("graph.checkpoint") {}

  virtual void SetUp() {
    system(
        "dw text2bin variable test/biased_coin/variables.tsv "
        "test/biased_coin/graph.variables");
    system(
        "dw text2bin factor test/biased_coin/factors.tsv "
        "test/biased_coin/graph.factors 4 1 0 1");
    system(
        "dw text2bin weight test/biased_coin/weights.tsv "
        "test/biased_coin/graph.weights");
    const char *argv[] = {
        "dw",      "gibbs",
        "-w",      "./test/biased_coin/graph.weights",
        "-v",      "./test/biased_coin/graph.variables",
        "-f",      "./test/biased_coin/graph.factors",
        "-m",      "./test/biased_coin/graph.meta",
        "-o",      ".",
        "-l",      "100",
        "-i",      "100",
        "-s",      "1",
        "--alpha", "0.1",
    };
    dd::CmdParser cmd_parser(sizeof(argv) / sizeof(*argv), argv);

    dd::FactorGraph fg(18, 18, 1, 18);
    fg.load_variables(cmd_parser.variable_file);
    fg.load_weights(cmd_parser.weight_file);
    fg.load_domains(cmd_parser.domain_file);
    fg.load_factors(cmd_parser.factor_file);
    fg.safety_check();

    fg.compile(cfg);
  }

  virtual void TearDown() {
    std::string cmd = "rm " + get_copy_filename(checkpoint_filename, 0);
    system(cmd.c_str());
  }
};

/*
 * Test that resume and checkpoint works properly for one graph.
 *
 * Well, this test just got long.
 */
TEST_F(CheckpointTest, checkpoint_and_resume) {
  /* Sort of gives you an idea how to call checkpoint() */
  std::vector<CompiledFactorGraph> cfgs;
  cfgs.push_back(cfg);
  checkpoint(checkpoint_filename, cfgs);

  /* And an idea on how to call resume() */
  CompiledFactorGraph resumed_cfg(18, 18, 1, 18);
  resume(checkpoint_filename, 0, resumed_cfg);

  ASSERT_EQ(cfg.n_var, resumed_cfg.n_var);
  ASSERT_EQ(cfg.n_factor, resumed_cfg.n_factor);
  ASSERT_EQ(cfg.n_weight, resumed_cfg.n_weight);
  ASSERT_EQ(cfg.n_edge, resumed_cfg.n_edge);

  /*
   * Check all arrays of variables, factors, compiled factors, compiled
   * variables, all that good stuff.
   */
  for (auto i = 0; i < cfg.n_var; i++) {
    ASSERT_EQ(cfg.variables[i].id, resumed_cfg.variables[i].id);
    ASSERT_EQ(cfg.variables[i].domain_type,
              resumed_cfg.variables[i].domain_type);

    ASSERT_EQ(cfg.variables[i].is_evid, resumed_cfg.variables[i].is_evid);
    ASSERT_EQ(cfg.variables[i].is_observation,
              resumed_cfg.variables[i].is_observation);
    ASSERT_EQ(cfg.variables[i].cardinality,
              resumed_cfg.variables[i].cardinality);

    ASSERT_EQ(cfg.variables[i].assignment_evid,
              resumed_cfg.variables[i].assignment_evid);
    ASSERT_EQ(cfg.variables[i].assignment_free,
              resumed_cfg.variables[i].assignment_free);

    ASSERT_EQ(cfg.variables[i].n_factors, resumed_cfg.variables[i].n_factors);
    ASSERT_EQ(cfg.variables[i].n_start_i_factors,
              resumed_cfg.variables[i].n_start_i_factors);

    ASSERT_EQ(cfg.variables[i].n_start_i_tally,
              resumed_cfg.variables[i].n_start_i_tally);

    /* TODO: What to do with domain_map */
  }

  for (auto i = 0; i < cfg.n_factor; i++) {
    ASSERT_EQ(cfg.factors[i].id, resumed_cfg.factors[i].id);
    ASSERT_EQ(cfg.factors[i].weight_id, resumed_cfg.factors[i].weight_id);
    ASSERT_EQ(cfg.factors[i].func_id, resumed_cfg.factors[i].func_id);
    ASSERT_EQ(cfg.factors[i].n_variables, resumed_cfg.factors[i].n_variables);
    ASSERT_EQ(cfg.factors[i].n_start_i_vif,
              resumed_cfg.factors[i].n_start_i_vif);

    /* TODO: What to do with weight_ids? */
  }

  /* Check vifs, compact_factor_weightids, compact_factors, and factor_ids */
  for (auto i = 0; i < cfg.n_edge; i++) {
    ASSERT_EQ(cfg.vifs[i].vid, resumed_cfg.vifs[i].vid);
    ASSERT_EQ(cfg.vifs[i].n_position, resumed_cfg.vifs[i].n_position);
    ASSERT_EQ(cfg.vifs[i].is_positive, resumed_cfg.vifs[i].is_positive);
    ASSERT_EQ(cfg.vifs[i].equal_to, resumed_cfg.vifs[i].equal_to);

    ASSERT_EQ(cfg.compact_factors[i].id, resumed_cfg.compact_factors[i].id);
    ASSERT_EQ(cfg.compact_factors[i].func_id,
              resumed_cfg.compact_factors[i].func_id);
    ASSERT_EQ(cfg.compact_factors[i].n_variables,
              resumed_cfg.compact_factors[i].n_variables);
    ASSERT_EQ(cfg.compact_factors[i].n_start_i_vif,
              resumed_cfg.compact_factors[i].n_start_i_vif);

    ASSERT_EQ(cfg.compact_factors_weightids[i],
              resumed_cfg.compact_factors_weightids[i]);

    ASSERT_EQ(cfg.factor_ids[i], resumed_cfg.factor_ids[i]);
  }

  /* More importantly, ensure that the inference results are equal */
  for (auto i = 0; i < cfg.n_var; i++) {
    ASSERT_EQ(cfg.infrs->agg_means[i], resumed_cfg.infrs->agg_means[i]);
    ASSERT_EQ(cfg.infrs->agg_nsamples[i], resumed_cfg.infrs->agg_nsamples[i]);
    ASSERT_EQ(cfg.infrs->assignments_free[i],
              resumed_cfg.infrs->assignments_free[i]);
    ASSERT_EQ(cfg.infrs->assignments_evid[i],
              resumed_cfg.infrs->assignments_evid[i]);
  }

  ASSERT_EQ(cfg.infrs->ntallies, resumed_cfg.infrs->ntallies);
  for (auto i = 0; i < cfg.infrs->ntallies; i++) {
    ASSERT_EQ(cfg.infrs->multinomial_tallies[i],
              resumed_cfg.infrs->multinomial_tallies[i]);
  }

  /*
   * If you're wondering why weights aren't checked, then you probably haven't
   * read the code (which is fine!). Read binary_parser.cpp:checkpoint()
   */
}

}  // namespace dd
