#ifndef DIMMWITTED_NUMA_AWARE_COMPUTATION_H_
#define DIMMWITTED_NUMA_AWARE_COMPUTATION_H_

namespace dd {

/**
 * Base class for variations of computations
 */
class ConcurrentComputation {
 public:
  virtual void init(int n_parts) = 0;
  virtual void start(int ith_part) = 0;
  virtual void wait() = 0;
};

/**
 * Base class for variations of computations
 */
class NumaAwareComputation : public ConcurrentComputation {
 public:
  NumaAwareComputation(ConcurrentComputation &computation_for_each_numa_node);
  virtual void init(int n_parts);
  virtual void start(int ith_part);
  virtual void wait();

 private:
  
};

}  // namespace dd

#endif  // DIMMWITTED_NUMA_AWARE_COMPUTATION_H_
