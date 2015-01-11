
#include <vector>
#include <thread>
#include "common.h"


#ifndef _SINGLE_NODE_WORKER_H
#define _SINGLE_NODE_WORKER_H

template<class WORKAREA, void (*worker) (WORKAREA * const, int, int)>
class SingeNodeWorker{
public:

  int nthread;
  int nodeid;

  WORKAREA * const p_workarea;
  std::vector<std::thread> threads;
  double * tmp;

  SingeNodeWorker(WORKAREA * const _p_workarea, const int & _nthread,
      const int & _nodeid) : p_workarea(_p_workarea){
    this->nthread = _nthread;
    this->nodeid = _nodeid;
  }

  void execute(){
    numa_run_on_node(this->nodeid);
    //numa_set_localalloc();

    this->threads.clear();
    for(int i=0;i<this->nthread;i++){
      this->threads.push_back(std::thread(worker, this->p_workarea, i, this->nthread));
    }
  }

  void wait(){
    for(int i=0;i<this->nthread;i++){
      this->threads[i].join();
    }
  }

};



#endif