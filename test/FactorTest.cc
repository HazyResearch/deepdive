#include <limits.h>
#include "dstruct/factor_graph/factor_graph.h"
#include "dstruct/factor_graph/factor.h"
#include "gtest/gtest.h"

#define EQ_TOL 0.00001

using namespace dd;

TEST(FactorTest, Imply) {
	
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

	// second test case: True /\ x => True, x propose to True, Expect 1
	propose = 1;
	EXPECT_NEAR(f._potential_imply(vifs, values, vid, propose), 1.0, EQ_TOL);

	// third test case: True /\ True => x, x propose to False, Expect -1
	vid = 2;
	propose = 0;
	EXPECT_NEAR(f._potential_imply(vifs, values, vid, propose), -1.0, EQ_TOL);

	// forth test case: True /\ True => x, x propose to True, Expect 1
	vid = 2;
	propose = 1;
	EXPECT_NEAR(f._potential_imply(vifs, values, vid, propose), 1.0, EQ_TOL);

}

int main(int argc, char **argv) {
	testing::InitGoogleTest(&argc, argv);
	return RUN_ALL_TESTS();
}
