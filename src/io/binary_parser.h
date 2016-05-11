#ifndef BINARY_PARSER_H
#define BINARY_PARSER_H

#include "dstruct/factor_graph/factor_graph.h"
#include <stdlib.h>

// Following gives be64toh() and htobe64() for 64-bit big <-> host endian
// conversions.
// See: http://stackoverflow.com/a/4410728/390044
#if defined(__linux__)
#include <endian.h>
#elif defined(__APPLE__)
#include <sys/_endian.h>
#define be16toh(x) ntohs(x)
#define be32toh(x) ntohl(x)
#define be64toh(x) ntohll(x)
#define htobe16(x) htons(x)
#define htobe32(x) htonl(x)
#define htobe64(x) htonll(x)
#elif defined(__FreeBSD__) || defined(__NetBSD__)
#include <sys/endian.h>
#elif defined(__OpenBSD__)
#include <sys/types.h>
#define be16toh(x) betoh16(x)
#define be32toh(x) betoh32(x)
#define be64toh(x) betoh64(x)
#define htobe16(x) htobe16(x)
#define htobe32(x) htobe32(x)
#define htobe64(x) htobe64(x)
#endif

using namespace std;

// meta data
typedef struct {
  long long num_weights;
  long long num_variables;
  long long num_factors;
  long long num_edges;

  /* Seems like the stuff below are unused */
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
 * Loads factors from the given file into the given factor graph (original mode)
 */
long long read_factors(string filename, dd::FactorGraph &);

/**
 * Loads factors from the given file into the given factor graph (incremental
 * mode)
 */
long long read_factors_inc(string filename, dd::FactorGraph &);

/**
 * Loads edges from the given file into the given factor graph (incremental
 * mode)
 */
long long read_edges_inc(string filename, dd::FactorGraph &);

// Loads domains for multinomial variables
void read_domains(string filename, dd::FactorGraph &fg);

/**
 * Resumes the computation state from the last checkpoint. It is critical
 * that we resume graphs one by one since we need to be NUMA aware.
 *
 * @param filename The file to load the checkpoint from.
 * @param n_numa_nodes The index of the factor graph to load
 * @param[out] cfg The compiled factor graph to which the state is resumed
 */
void resume(string filename, int i, dd::CompiledFactorGraph &cfg);

/**
 * Checkpoints all copies of the compiled factor graph to various files.
 */
void checkpoint(string filename, vector<dd::CompiledFactorGraph> &cfgs);

#endif
