#ifndef DIMMWITTED_BINARY_FORMAT_H_
#define DIMMWITTED_BINARY_FORMAT_H_

#include "factor_graph.h"

#include <cstdlib>

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
FactorGraphDescriptor read_meta(const std::string& meta_file);

/**
 * Loads weights from the given file into the given factor graph
 */
size_t read_weights(const std::string& filename, dd::FactorGraph&);

/**
 * Loads variables from the given file into the given factor graph
 */
size_t read_variables(const std::string& filename, dd::FactorGraph&);

/**
 * Loads factors from the given file into the given factor graph (original mode)
 */
size_t read_factors(const std::string& filename, dd::FactorGraph&);

/**
 * Loads factors from the given file into the given factor graph (incremental
 * mode)
 */
size_t read_factors_inc(const std::string& filename, dd::FactorGraph&);

/**
 * Loads edges from the given file into the given factor graph (incremental
 * mode)
 */
size_t read_edges_inc(const std::string& filename, dd::FactorGraph&);

// Loads domains for multinomial variables
void read_domains(const std::string& filename, dd::FactorGraph& fg);

}  // namespace dd

#endif  // DIMMWITTED_BINARY_FORMAT_H_
