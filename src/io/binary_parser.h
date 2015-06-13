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

/**
 * Reads meta data from the given file.
 * For reference of factor graph file formats, refer to 
 * deepdive.stanford.edu
 */
Meta read_meta(string meta_file);

/**
 * Loads weights from the given file into the given factor graph
 */
long long read_weights(string filename, dd::FactorGraph &);

/**
 * Loads variables from the given file into the given factor graph
 */
long long read_variables(string filename, dd::FactorGraph &);

/**
 * Loads factors from the given file into the given factor graph
 */
long long read_factors(string filename, dd::FactorGraph &);

/**
 * Loads edges from the given file into the given factor graph
 */
long long read_edges(string filename, dd::FactorGraph &);

#endif