#ifndef DIMMWITTED_BINARY_FORMAT_H_
#define DIMMWITTED_BINARY_FORMAT_H_

#include "factor_graph.h"
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

namespace dd {

/**
 * Reads meta data from the given file.
 * For reference of factor graph file formats, refer to
 * deepdive.stanford.edu
 */
FactorGraphDescriptor read_meta(std::string meta_file);

/**
 * Loads weights from the given file into the given factor graph
 */
long long read_weights(std::string filename, dd::FactorGraph &);

/**
 * Loads variables from the given file into the given factor graph
 */
long long read_variables(std::string filename, dd::FactorGraph &);

/**
 * Loads factors from the given file into the given factor graph (original mode)
 */
long long read_factors(std::string filename, dd::FactorGraph &);

/**
 * Loads factors from the given file into the given factor graph (incremental
 * mode)
 */
long long read_factors_inc(std::string filename, dd::FactorGraph &);

/**
 * Loads edges from the given file into the given factor graph (incremental
 * mode)
 */
long long read_edges_inc(std::string filename, dd::FactorGraph &);

// Loads domains for multinomial variables
void read_domains(std::string filename, dd::FactorGraph &fg);

/**
 * Resumes the computation state from the last checkpoint. It is critical
 * that we resume graphs one by one since we need to be NUMA aware.
 *
 * @param filename
 *   The base filename to load the checkpoint from. ".part$i" will be appended
 *   to this parameter to generate the full checkpoint filename.
 * @param i Indicates the NUMA node to which the factor graph should be loaded
 * @param[out] cfg The compiled factor graph to which the state is resumed
 */
void resume(std::string filename, int i, dd::CompactFactorGraph &cfg);

/**
 * Checkpoints all copies of the compiled factor graph to various files.
 *
 * @param filename
 *   The base filename to checkpoint to. ".part$i" will be appended into this
 *   parameter to generate the full checkpoint filename.
 * @param cfgs A list of CompiledFactorGraphs to write to file.
 */
void checkpoint(std::string filename,
                std::vector<dd::CompactFactorGraph> &cfgs);

}  // namespace dd

#endif  // DIMMWITTED_BINARY_FORMAT_H_
