
#ifndef _WEIGHT_H_
#define _WEIGHT_H_

namespace dd{

  typedef long WeightIndex;

  /**
   * Encapsulates a weight for factors. 
   */
  class Weight {
  public:
    long id;        // weight id
    double weight;  // weight value
    bool isfixed;   // whether the weight is fixed

    Weight(const long & _id,
           const double & _weight,
           const bool & _isfixed);

    Weight();

  }; 


}

#endif