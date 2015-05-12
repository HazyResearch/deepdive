#include <limits.h>
#include "dstruct/factor_graph/factor_graph.h"
#include "dstruct/factor_graph/factor.h"
#include "gtest/gtest.h"
#include <math.h>

#define EQ_TOL 0.00001

using namespace dd;

TEST(FactorTest, ONE_VAR_FACTORS) {

	VariableInFactor vifs[1];
	VariableValue values[1];
	long vid;
	VariableValue propose;

	vifs[0].vid = 0;
	vifs[0].is_positive = true;
	vifs[0].equal_to = 1;

	CompactFactor f;
	f.n_variables = 1;
	f.n_start_i_vif = 0;

	values[0] = 0;
	vid = 0;

	// CASE 1: True
	propose = 1;
	EXPECT_NEAR(f._potential_and(vifs, values, vid, propose), 1.0, EQ_TOL);
	EXPECT_NEAR(f._potential_or(vifs, values, vid, propose), 1.0, EQ_TOL);
	EXPECT_NEAR(f._potential_equal(vifs, values, vid, propose), 1.0, EQ_TOL);
	EXPECT_NEAR(f._potential_imply(vifs, values, vid, propose), 1.0, EQ_TOL);
	EXPECT_NEAR(f._potential_linear(vifs, values, vid, propose), 1.0, EQ_TOL);
	EXPECT_NEAR(f._potential_ratio(vifs, values, vid, propose), 1.0, EQ_TOL);
	EXPECT_NEAR(f._potential_logical(vifs, values, vid, propose), 1.0, EQ_TOL);
	
	// CASE 2: False
	propose = 0;
	EXPECT_NEAR(f._potential_and(vifs, values, vid, propose), 0.0, EQ_TOL);
	EXPECT_NEAR(f._potential_or(vifs, values, vid, propose), 0.0, EQ_TOL);
	EXPECT_NEAR(f._potential_equal(vifs, values, vid, propose), 1.0, EQ_TOL);
	EXPECT_NEAR(f._potential_imply(vifs, values, vid, propose), -1.0, EQ_TOL);
	EXPECT_NEAR(f._potential_linear(vifs, values, vid, propose), 0.0, EQ_TOL);
	EXPECT_NEAR(f._potential_ratio(vifs, values, vid, propose), 0.0, EQ_TOL);
	EXPECT_NEAR(f._potential_logical(vifs, values, vid, propose), 0.0, EQ_TOL);
}


TEST(FactorTest, TWO_VAR_FACTORS) {

	VariableInFactor vifs[2];
	VariableValue values[2];
	long vid;
	VariableValue propose;

	vifs[0].vid = 0;
	vifs[0].is_positive = true;
	vifs[0].equal_to = 1;

	vifs[1].vid = 1;
	vifs[1].is_positive = true;
	vifs[1].equal_to = 1;

	CompactFactor f;
	f.n_variables = 2;
	f.n_start_i_vif = 0;

	// CASE 1: True op True
	values[0] = 1;
	values[1] = 1;	
	vid = 1;
	propose = 1;
	EXPECT_NEAR(f._potential_and(vifs, values, vid, propose), 1.0, EQ_TOL);
	EXPECT_NEAR(f._potential_or(vifs, values, vid, propose), 1.0, EQ_TOL);
	EXPECT_NEAR(f._potential_equal(vifs, values, vid, propose), 1.0, EQ_TOL);
	EXPECT_NEAR(f._potential_imply(vifs, values, vid, propose), 1.0, EQ_TOL);
	EXPECT_NEAR(f._potential_linear(vifs, values, vid, propose), 1.0, EQ_TOL);
	EXPECT_NEAR(f._potential_ratio(vifs, values, vid, propose), 1.0, EQ_TOL);
	EXPECT_NEAR(f._potential_logical(vifs, values, vid, propose), 1.0, EQ_TOL);

	// CASE 2: True op False
	values[0] = 1;
	values[1] = 1;	
	vid = 1;
	propose = 0;
	EXPECT_NEAR(f._potential_and(vifs, values, vid, propose), 0.0, EQ_TOL);
	EXPECT_NEAR(f._potential_or(vifs, values, vid, propose), 1.0, EQ_TOL);
	EXPECT_NEAR(f._potential_equal(vifs, values, vid, propose), 0.0, EQ_TOL);
	EXPECT_NEAR(f._potential_imply(vifs, values, vid, propose), -1.0, EQ_TOL);
	EXPECT_NEAR(f._potential_linear(vifs, values, vid, propose), 0.0, EQ_TOL);
	EXPECT_NEAR(f._potential_ratio(vifs, values, vid, propose), 0.0, EQ_TOL);
	EXPECT_NEAR(f._potential_logical(vifs, values, vid, propose), 0.0, EQ_TOL);

	// CASE 3: False op True
	values[0] = 0;
	values[1] = 0;	
	vid = 1;
	propose = 1;
	EXPECT_NEAR(f._potential_and(vifs, values, vid, propose), 0.0, EQ_TOL);
	EXPECT_NEAR(f._potential_or(vifs, values, vid, propose), 1.0, EQ_TOL);
	EXPECT_NEAR(f._potential_equal(vifs, values, vid, propose), 0.0, EQ_TOL);
	EXPECT_NEAR(f._potential_imply(vifs, values, vid, propose), 0.0, EQ_TOL);
	EXPECT_NEAR(f._potential_linear(vifs, values, vid, propose), 1.0, EQ_TOL);
	EXPECT_NEAR(f._potential_ratio(vifs, values, vid, propose), 1.0, EQ_TOL);
	EXPECT_NEAR(f._potential_logical(vifs, values, vid, propose), 1.0, EQ_TOL);

	// CASE 4: False op False
	values[0] = 0;
	values[1] = 0;	
	vid = 1;
	propose = 0;
	EXPECT_NEAR(f._potential_and(vifs, values, vid, propose), 0.0, EQ_TOL);
	EXPECT_NEAR(f._potential_or(vifs, values, vid, propose), 0.0, EQ_TOL);
	EXPECT_NEAR(f._potential_equal(vifs, values, vid, propose), 1.0, EQ_TOL);
	EXPECT_NEAR(f._potential_imply(vifs, values, vid, propose), 0.0, EQ_TOL);
	EXPECT_NEAR(f._potential_linear(vifs, values, vid, propose), 1.0, EQ_TOL);
	EXPECT_NEAR(f._potential_ratio(vifs, values, vid, propose), 1.0, EQ_TOL);
	EXPECT_NEAR(f._potential_logical(vifs, values, vid, propose), 1.0, EQ_TOL);

}

TEST(FactorTest, THREE_VAR_IMPLY) {

	VariableInFactor vifs[3];
	VariableValue values[3];
	long vid;
	VariableValue propose;

	// first test case: True /\ x => True, x propose to False, Expect 0 
	vifs[0].vid = 0;
	vifs[0].is_positive = true;
	vifs[0].equal_to = 1;

	vifs[1].vid = 1;
	vifs[1].is_positive = true;
	vifs[1].equal_to = 1;

	vifs[2].vid = 2;
	vifs[2].is_positive = true;
	vifs[2].equal_to = 1;

	values[0] = 1;
	values[1] = 1;
	values[2] = 1;
	
	vid = 1;
	propose = 0;

	CompactFactor f;
	f.n_variables = 3;
	f.n_start_i_vif = 0;

	EXPECT_NEAR(f._potential_imply(vifs, values, vid, propose), 0.0, EQ_TOL);
	EXPECT_NEAR(f._potential_imply_mln(vifs, values, vid, propose), 1.0, EQ_TOL);
	EXPECT_NEAR(f._potential_linear(vifs, values, vid, propose), 2.0, EQ_TOL);
	EXPECT_NEAR(f._potential_ratio(vifs, values, vid, propose), log2(3.0), EQ_TOL);
	EXPECT_NEAR(f._potential_logical(vifs, values, vid, propose), 1.0, EQ_TOL);

	// second test case: True /\ x => True, x propose to True, Expect 1
	propose = 1;
	EXPECT_NEAR(f._potential_imply(vifs, values, vid, propose), 1.0, EQ_TOL);
	EXPECT_NEAR(f._potential_imply_mln(vifs, values, vid, propose), 1.0, EQ_TOL);
	EXPECT_NEAR(f._potential_linear(vifs, values, vid, propose), 2.0, EQ_TOL);
	EXPECT_NEAR(f._potential_ratio(vifs, values, vid, propose), log2(3.0), EQ_TOL);
	EXPECT_NEAR(f._potential_logical(vifs, values, vid, propose), 1.0, EQ_TOL);


	// third test case: True /\ True => x, x propose to False, Expect -1
	vid = 2;
	propose = 0;
	EXPECT_NEAR(f._potential_imply(vifs, values, vid, propose), -1.0, EQ_TOL);
	EXPECT_NEAR(f._potential_imply_mln(vifs, values, vid, propose), 0.0, EQ_TOL);
	EXPECT_NEAR(f._potential_linear(vifs, values, vid, propose), 0.0, EQ_TOL);
	EXPECT_NEAR(f._potential_ratio(vifs, values, vid, propose), 0.0, EQ_TOL);
	EXPECT_NEAR(f._potential_logical(vifs, values, vid, propose), 0.0, EQ_TOL);

	// forth test case: True /\ True => x, x propose to True, Expect 1
	vid = 2;
	propose = 1;
	EXPECT_NEAR(f._potential_imply(vifs, values, vid, propose), 1.0, EQ_TOL);
	EXPECT_NEAR(f._potential_imply_mln(vifs, values, vid, propose), 1.0, EQ_TOL);
	EXPECT_NEAR(f._potential_linear(vifs, values, vid, propose), 2.0, EQ_TOL);
	EXPECT_NEAR(f._potential_ratio(vifs, values, vid, propose), log2(3.0), EQ_TOL);
	EXPECT_NEAR(f._potential_logical(vifs, values, vid, propose), 1.0, EQ_TOL);

}
