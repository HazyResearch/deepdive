#ifndef DIMMWITTED_BINARY_FORMAT_H_
#define DIMMWITTED_BINARY_FORMAT_H_

#include "factor_graph.h"

#include <cstdlib>
#include <iostream>

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
 * a few specialized functions for writing big endian values
 */
template <int num_bytes>
inline void write_be(std::ostream &output, void *value);
template <>
inline void write_be<1>(std::ostream &output, void *value) {
  output.write((char *)value, 1);
}
template <>
inline void write_be<2>(std::ostream &output, void *value) {
  uint16_t tmp = htobe16(*(uint16_t *)value);
  output.write((char *)&tmp, sizeof(tmp));
}
template <>
inline void write_be<4>(std::ostream &output, void *value) {
  uint32_t tmp = htobe32(*(uint32_t *)value);
  output.write((char *)&tmp, sizeof(tmp));
}
template <>
inline void write_be<8>(std::ostream &output, void *value) {
  uint64_t tmp = htobe64(*(uint64_t *)value);
  output.write((char *)&tmp, sizeof(tmp));
}

/**
 * a handy way to serialize values of certain type in big endian
 */
template <typename T>
inline void write_be(std::ostream &output, T value) {
  write_be<sizeof(T)>(output, &value);
}

/**
 * Reads meta data from the given file.
 * For reference of factor graph file formats, refer to
 * deepdive.stanford.edu
 */
FactorGraphDescriptor read_meta(const std::string &meta_file);

/**
 * Loads weights from the given file into the given factor graph
 */
num_weights_t read_weights(const std::string &filename, FactorGraph &);

/**
 * Loads variables from the given file into the given factor graph
 */
num_variables_t read_variables(const std::string &filename, FactorGraph &);

/**
 * Loads factors from the given file into the given factor graph (original mode)
 */
num_factors_t read_factors(const std::string &filename, FactorGraph &);

/**
 * Loads domains for categorical variables
 */
num_variables_t read_domains(const std::string &filename, FactorGraph &fg);

}  // namespace dd

#endif  // DIMMWITTED_BINARY_FORMAT_H_
