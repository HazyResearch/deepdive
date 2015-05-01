/**
 * Integration test for learning hidden models
 * The factor graph contains a chain A-B-C, where A, C are evidence, and B is
 * partially observed. The factor between A, B and B, C is equal, respectively. 
 * There are 4 training examples with A = C = true, and only one B is observed to
 * be true. We expect the weight to be positive, and probabilities to be > 0.9.
 */

#include <limits.h>
#include "gibbs.h"
#include "gtest/gtest.h"
#include <fstream>

using namespace dd;

TEST(PartialObservationTest, INFERENCE) {

  const char* argv[23] = {
    "dw", "gibbs", "-w", "./test/partial/graph.weights", "-v", "./test/partial/graph.variables", 
    "-f", "./test/partial/graph.factors", "-e", "./test/partial/graph.edges", "-m", "./test/partial/graph.meta",
    "-o", ".", "-l", "300", "-i", "500", "-s", "1", "--alpha", "0.1", ""
  };

  dd::CmdParser cmd_parser = parse_input(23, (char **)argv);
  gibbs(cmd_parser);

  int id;
  std::ifstream fin_weight("./inference_result.out.weights.text");
  double weight;
  while(fin_weight >> id >> weight){
    EXPECT_GT(weight, 0);
  }

  std::ifstream fin("./inference_result.out.text");
  int e;
  double prob;
  while(fin >> id >> e >> prob){
    EXPECT_GT(prob, 0.9);
  }

}

