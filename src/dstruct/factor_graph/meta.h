#ifndef META_H
#define META_H

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

#endif