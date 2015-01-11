#ifndef BINARY_PARSER_H
#define BINARY_PARSER_H

#include <stdlib.h>
#include "dstruct/factor_graph/factor_graph.h"
#include "dstruct/factor_graph/meta.h"
using namespace std;

Meta read_meta(string meta_file);
long long read_weights(string filename, dd::FactorGraph &);
long long read_variables(string filename, dd::FactorGraph &);
long long read_factors(string filename, dd::FactorGraph &);
long long read_edges(string filename, dd::FactorGraph &);

#endif