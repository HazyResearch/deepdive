#include "numa_nodes.h"

#include <sstream>
#include <cassert>

#ifdef __linux__

// libnuma on Linux
#include <numa.h>
#include <numaif.h>

#else  // __linux__

// portability shims for platforms without libnuma support, e.g., Mac
#define numa_available() -1
#define numa_num_configured_nodes() 1
#define numa_bind(bmp)
#define numa_parse_nodestring(str) nullptr
#define numa_bitmask_alloc(n) nullptr
#define numa_bitmask_free(bmp)

void numa_warn(int number, char* where, ...) {
  // ignore
}

void numa_error(char* where) {
  // ignore
}

#endif  // __linux__

namespace dd {

NumaNodes::NumaNodes(const std::string& nodestring)
    : nodestring_(nodestring),
      numa_nodemask_(nullptr),
      numa_available_(numa_available() != -1 &&
                      numa_num_configured_nodes() > 0) {}

NumaNodes::~NumaNodes() {
  if (numa_nodemask_) numa_bitmask_free(numa_nodemask_);
}

NumaNodes::NumaNodes(const NumaNodes& other) : NumaNodes(other.nodestring_) {}
NumaNodes& NumaNodes::operator=(NumaNodes other) {
  swap(other);
  return *this;
}
void NumaNodes::swap(NumaNodes& other) {
  std::swap(nodestring_, other.nodestring_);
  std::swap(numa_nodemask_, other.numa_nodemask_);
}

struct bitmask* NumaNodes::numa_nodemask() {
  if (!numa_nodemask_ && numa_available_)
    numa_nodemask_ = numa_parse_nodestring(&nodestring_[0]);
  return numa_nodemask_;
}
void NumaNodes::bind() {
  if (numa_available_) numa_bind(numa_nodemask());
}
void NumaNodes::unbind() {
  if (numa_available_) numa_bind(numa_nodemask());
}

size_t NumaNodes::num_configured() {
  return std::max(numa_available() != -1 ? numa_num_configured_nodes() : 1, 1);
}

NumaNodes NumaNodes::partition(size_t ith_part, size_t num_parts) {
  size_t num_numa_nodes = num_configured();
  assert(num_numa_nodes % num_parts == 0);

  size_t num_numa_nodes_per_part = num_numa_nodes / num_parts;
  size_t begin = ith_part * num_numa_nodes_per_part;
  size_t end = begin + num_numa_nodes_per_part - 1;

  std::ostringstream nodestringstream;
  nodestringstream << begin << "-" << end;
  return NumaNodes(nodestringstream.str());
}

std::vector<NumaNodes> NumaNodes::partition(size_t num_parts) {
  std::vector<NumaNodes> parts;
  for (size_t i = 0; i < num_parts; ++i) {
    parts.push_back(partition(i, num_parts));
  }
  return parts;
}

std::ostream& operator<<(std::ostream& output, const NumaNodes& numa_nodes) {
  return output << numa_nodes.nodestring_;
}

}  // namespace dd
