
#ifndef _WEIGHT_H_
#define _WEIGHT_H_

namespace dd{

  class Weight {
  public:
    long id;
    double weight;
    bool isfixed;

    Weight(const long & _id,
           const double & _weight,
           const bool & _isfixed):
      id(_id),
      weight(_weight),
      isfixed(_isfixed)
    {}

    Weight(){
      
    }
  }; 


}

#endif