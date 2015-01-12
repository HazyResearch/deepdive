
#ifndef _WEIGHT_H_
#define _WEIGHT_H_

namespace dd{

  typedef long WeightIndex;

  class Weight {
  public:
    long id;
    double weight;
    bool isfixed;

    Weight(const long & _id,
           const double & _weight,
           const bool & _isfixed);

    Weight();

  }; 


}

#endif