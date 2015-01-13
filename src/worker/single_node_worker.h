
#include <vector>
#include <thread>
#include "common.h"


#ifndef _SINGLE_NODE_WORKER_H
#define _SINGLE_NODE_WORKER_H

/**
 * Class for a single node worker
 * WORKAREA work area where the task is performed
 * worker the worker function
 */
template<class WORKAREA, void (*worker) (WORKAREA * const, int, int)>
class SingeNodeWorker{
public:

  int nthread;  // number of threads
  int nodeid;   // node id

  // workarea where the task is performed in, e.g., a FactorGraph class
  WORKAREA * const p_workarea;
  // threads
  std::vector<std::thread> threads;
  double * tmp;

  /**
   * Constructs a SingleNodeWorker with given workarea, number of threads,
   * and node id.
   */
  SingeNodeWorker(WORKAREA * const _p_workarea, const int & _nthread,
      const int & _nodeid) : p_workarea(_p_workarea){
    this->nthread = _nthread;
    this->nodeid = _nodeid;
  }

  /**
   * Performs work on the NUMA node with id given by nodeid.
   * Each work is perfromed using nthread threads
   */
  void execute(){
    numa_run_on_node(this->nodeid);
    //numa_set_localalloc();

    this->threads.clear();

    for(int i=0;i<this->nthread;i++){
      this->threads.push_back(std::thread(worker, this->p_workarea, i, this->nthread));
    }
  }

  /**
   * Waits until all threads finish.
   */
  void wait(){
    for(int i=0;i<this->nthread;i++){
      this->threads[i].join();
    }
  }

};



#endif