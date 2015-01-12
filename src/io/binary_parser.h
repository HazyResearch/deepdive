#ifndef BINARY_PARSER_H
#define BINARY_PARSER_H

#include <stdlib.h>
#include "dstruct/factor_graph/factor_graph.h"
using namespace std;

// meta data
typedef struct {
	long long num_weights;
	long long num_variables;
	long long num_factors;
	long long num_edges;
	string weights_file; 
	string variables_file;
	string factors_file;
	string edges_file;
} Meta;


Meta read_meta(string meta_file);
long long read_weights(string filename, dd::FactorGraph &);
long long read_variables(string filename, dd::FactorGraph &);
long long read_factors(string filename, dd::FactorGraph &);
long long read_edges(string filename, dd::FactorGraph &);

#endif