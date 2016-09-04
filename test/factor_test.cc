#include "factor.h"
#include "factor_graph.h"
#include <gtest/gtest.h>
#include <limits.h>
#include <math.h>

#define EQ_TOL 0.00001

namespace dd {

TEST(FactorTest, ONE_VAR_FACTORS) {
  FactorToVariable vifs[1];
  size_t values[1];
  size_t vid;
  size_t propose;

  vifs[0].vid = 0;
  vifs[0].dense_equal_to = 1;

  Factor f;
  f.num_vars = 1;
  f.vif_base = 0;

  values[0] = 0;
  vid = 0;

  // CASE 1: True
  propose = 1;
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_AND)(vifs, values, vid, propose), 1.0,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_OR)(vifs, values, vid, propose), 1.0,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_EQUAL)(vifs, values, vid, propose), 1.0,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_IMPLY_NATURAL)(vifs, values, vid, propose),
              1.0, EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_LINEAR)(vifs, values, vid, propose), 1.0,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_RATIO)(vifs, values, vid, propose), 1.0,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_LOGICAL)(vifs, values, vid, propose), 1.0,
              EQ_TOL);

  // CASE 2: False
  propose = 0;
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_AND)(vifs, values, vid, propose), -1,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_OR)(vifs, values, vid, propose), -1,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_EQUAL)(vifs, values, vid, propose), 1.0,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_IMPLY_NATURAL)(vifs, values, vid, propose),
              -1.0, EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_LINEAR)(vifs, values, vid, propose), 0.0,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_RATIO)(vifs, values, vid, propose), 0.0,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_LOGICAL)(vifs, values, vid, propose), 0.0,
              EQ_TOL);
}

TEST(FactorTest, TWO_VAR_FACTORS) {
  FactorToVariable vifs[2];
  size_t values[2];
  size_t vid;
  size_t propose;

  vifs[0].vid = 0;
  vifs[0].dense_equal_to = 1;

  vifs[1].vid = 1;
  vifs[1].dense_equal_to = 1;

  Factor f;
  f.num_vars = 2;
  f.vif_base = 0;

  // CASE 1: True op True
  values[0] = 1;
  values[1] = 1;
  vid = 1;
  propose = 1;
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_AND)(vifs, values, vid, propose), 1.0,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_OR)(vifs, values, vid, propose), 1.0,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_EQUAL)(vifs, values, vid, propose), 1.0,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_IMPLY_NATURAL)(vifs, values, vid, propose),
              1.0, EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_LINEAR)(vifs, values, vid, propose), 1.0,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_RATIO)(vifs, values, vid, propose), 1.0,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_LOGICAL)(vifs, values, vid, propose), 1.0,
              EQ_TOL);

  // CASE 2: True op False
  values[0] = 1;
  values[1] = 1;
  vid = 1;
  propose = 0;
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_AND)(vifs, values, vid, propose), -1,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_OR)(vifs, values, vid, propose), 1.0,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_EQUAL)(vifs, values, vid, propose), -1,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_IMPLY_NATURAL)(vifs, values, vid, propose),
              -1.0, EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_LINEAR)(vifs, values, vid, propose), 0.0,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_RATIO)(vifs, values, vid, propose), 0.0,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_LOGICAL)(vifs, values, vid, propose), 0.0,
              EQ_TOL);

  // CASE 3: False op True
  values[0] = 0;
  values[1] = 0;
  vid = 1;
  propose = 1;
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_AND)(vifs, values, vid, propose), -1,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_OR)(vifs, values, vid, propose), 1.0,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_EQUAL)(vifs, values, vid, propose), -1,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_IMPLY_NATURAL)(vifs, values, vid, propose),
              0.0, EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_LINEAR)(vifs, values, vid, propose), 1.0,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_RATIO)(vifs, values, vid, propose), 1.0,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_LOGICAL)(vifs, values, vid, propose), 1.0,
              EQ_TOL);

  // CASE 4: False op False
  values[0] = 0;
  values[1] = 0;
  vid = 1;
  propose = 0;
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_AND)(vifs, values, vid, propose), -1,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_OR)(vifs, values, vid, propose), -1,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_EQUAL)(vifs, values, vid, propose), 1.0,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_IMPLY_NATURAL)(vifs, values, vid, propose),
              0.0, EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_LINEAR)(vifs, values, vid, propose), 1.0,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_RATIO)(vifs, values, vid, propose), 1.0,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_LOGICAL)(vifs, values, vid, propose), 1.0,
              EQ_TOL);
}

TEST(FactorTest, THREE_VAR_IMPLY) {
  FactorToVariable vifs[3];
  size_t values[3];
  size_t vid;
  size_t propose;

  // first test case: True /\ x => True, x propose to False, Expect 0
  vifs[0].vid = 0;
  vifs[0].dense_equal_to = 1;

  vifs[1].vid = 1;
  vifs[1].dense_equal_to = 1;

  vifs[2].vid = 2;
  vifs[2].dense_equal_to = 1;

  values[0] = 1;
  values[1] = 1;
  values[2] = 1;

  vid = 1;
  propose = 0;

  Factor f;
  f.num_vars = 3;
  f.vif_base = 0;

  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_IMPLY_NATURAL)(vifs, values, vid, propose),
              0.0, EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_IMPLY_MLN)(vifs, values, vid, propose), 1.0,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_LINEAR)(vifs, values, vid, propose), 2.0,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_RATIO)(vifs, values, vid, propose),
              log2(3.0), EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_LOGICAL)(vifs, values, vid, propose), 1.0,
              EQ_TOL);

  // second test case: True /\ x => True, x propose to True, Expect 1
  propose = 1;
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_IMPLY_NATURAL)(vifs, values, vid, propose),
              1.0, EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_IMPLY_MLN)(vifs, values, vid, propose), 1.0,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_LINEAR)(vifs, values, vid, propose), 2.0,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_RATIO)(vifs, values, vid, propose),
              log2(3.0), EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_LOGICAL)(vifs, values, vid, propose), 1.0,
              EQ_TOL);

  // third test case: True /\ True => x, x propose to False, Expect -1
  vid = 2;
  propose = 0;
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_IMPLY_NATURAL)(vifs, values, vid, propose),
              -1.0, EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_IMPLY_MLN)(vifs, values, vid, propose), 0.0,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_LINEAR)(vifs, values, vid, propose), 0.0,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_RATIO)(vifs, values, vid, propose), 0.0,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_LOGICAL)(vifs, values, vid, propose), 0.0,
              EQ_TOL);

  // forth test case: True /\ True => x, x propose to True, Expect 1
  vid = 2;
  propose = 1;
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_IMPLY_NATURAL)(vifs, values, vid, propose),
              1.0, EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_IMPLY_MLN)(vifs, values, vid, propose), 1.0,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_LINEAR)(vifs, values, vid, propose), 2.0,
              EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_RATIO)(vifs, values, vid, propose),
              log2(3.0), EQ_TOL);
  EXPECT_NEAR(f.POTENTIAL_SIGN(FUNC_LOGICAL)(vifs, values, vid, propose), 1.0,
              EQ_TOL);
}

}  // namespace dd
