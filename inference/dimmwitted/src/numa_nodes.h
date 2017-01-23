#ifndef DIMMWITTED_NUMA_NODES_H_
#define DIMMWITTED_NUMA_NODES_H_

#include <string>
#include <vector>
#include <ostream>

struct bitmask;  // just need a forward declaration since we only point to it

namespace dd {

/**
 * A class for easily setting up NUMA/CPU constraints.
 */
class NumaNodes {
 private:
  /**
   * A libnuma nodestring describing which NUMA nodes this instance covers.
   * See: man numa(3)
   */
  std::string nodestring_;
  friend std::ostream& operator<<(std::ostream&, const NumaNodes&);

  /**
   * The bitmask that corresponds to the NUMA nodestring.
   */
  struct bitmask* numa_nodemask_;
  struct bitmask* numa_nodemask();

  /**
   * Whether libnuma API is available is checked first and this flag is set.
   * Other members will try to behave reasonably if this flag is false.
   */
  bool numa_available_;

 public:
  NumaNodes(const std::string& nodestring);
  ~NumaNodes();

  NumaNodes(const NumaNodes& other);
  NumaNodes& operator=(NumaNodes other);
  void swap(NumaNodes& other);

  /**
   * Returns the nodestring used by libnuma that represents the NUMA nodes this
   * instance covers.
   */
  std::string nodestring() const;

  /**
   * Binds memory allocation and CPU affinity of the calling task to the NUMA
   * nodes of this instance.
   */
  void bind();
  /**
   * Unbinds any previously bound memory allocation and CPU affinity of the
   * calling task.
   */
  void unbind();

  /**
   * Returns how many NUMA nodes are configured on the system.
   */
  static size_t num_configured();
  /**
   * Returns the i-th partition when all NUMA nodes are grouped into a given
   * number of partitions.
   */
  static NumaNodes partition(size_t ith_part, size_t num_parts);
  /**
   * Returns a vector of instances that cover all NUMA nodes given a number of
   * partitions.
   */
  static std::vector<NumaNodes> partition(size_t num_parts);
};

std::ostream& operator<<(std::ostream&, const NumaNodes&);

}  // namespace dd

#endif  // DIMMWITTED_NUMA_NODES_H_
