
#ifndef _FACTOR_H_
#define _FACTOR_H_


namespace dd{

  template<bool does_change_evid>
  inline const double & get_vassign(const Variable & v);

  template<>
  inline const double & get_vassign<true>(const Variable & v){
    return v.assignment_free;
  }

  template<>
  inline const double & get_vassign<false>(const Variable & v){
    return v.assignment_evid;
  }

  class CompactFactor{
  public:
    long id;
    int func_id; 
    int n_variables;
    long n_start_i_vif;

    CompactFactor(){}

    CompactFactor(const long & _id){
      id = _id;
    }

    inline double _potential_continuousLR(const VariableInFactor * const vifs,
                                   const double * const var_values, 
                                   const long &, const double &) const;


    inline double _potential_or(const VariableInFactor * const vifs,
                                   const double * const var_values, 
                                   const long &, const double &) const;


    inline double _potential_and(const VariableInFactor * const vifs,
                                   const double * const var_values, 
                                   const long &, const double &) const;


    inline double _potential_equal(const VariableInFactor * const vifs,
                                   const double * const var_values, 
                                   const long &, const double &) const;


    inline double _potential_imply(const VariableInFactor * const vifs,
                                   const double * const var_values, 
                                   const long &, const double &) const;
    
    inline double _potential_multinomial(const VariableInFactor * const vifs,
                                   const double * const var_values, 
                                   const long &, const double &) const;

    inline double potential(const VariableInFactor * const vifs,
      const double * const var_values,
      const long & vid, const double & proposal) const{ 
      
      if(func_id == 0){
        return _potential_imply(vifs, var_values, vid, proposal);
      }else if(func_id == 4){
        return _potential_imply(vifs, var_values, vid, proposal);
      }else if(func_id == 1){ // OR
        return _potential_or(vifs, var_values, vid, proposal);
      }else if(func_id == 2){ // AND
        return _potential_and(vifs, var_values, vid, proposal);   
      }else if(func_id == 3){ // EQUAL
        return _potential_equal(vifs, var_values, vid, proposal);     
      } else if (func_id == 5) {
        return _potential_multinomial(vifs, var_values, vid, proposal);
      }else if(func_id == 10){ // SQLSELECT
        assert(false);
        return 0.0;
        //return _potential_sqlselect(vifs, var_values, vid, proposal);   
      }else if(func_id == 20){ // YUKE -- ContinuousLR
        assert(false);
        //return _potential_continuousLR(vifs, var_values, vid, proposal);  
        return 0.0;
      }else{
        std::cout << "Unsupported Factor Function ID= " << func_id << std::endl;
        assert(false);
      }
      return 0.0;
    }

  };

  class Factor {
  public:
    long id;
    long weight_id;
    int func_id;
    int n_variables;

    long n_start_i_vif;

    std::vector<VariableInFactor> tmp_variables;

    Factor(){

    }

    Factor(const long & _id,
           const int & _weight_id,
           const int & _func_id,
           const int & _n_variables){
      this->id = _id;
      this->weight_id = _weight_id;
      this->func_id = _func_id;
      this->n_variables = _n_variables;
    }
 
  };

  // potential for multinomial variable
  inline double dd::CompactFactor::_potential_multinomial(const VariableInFactor * vifs, 
    const double * var_values, const long & vid, const double & proposal) const {

    return 1.0;
  }

  inline double dd::CompactFactor::_potential_equal(
    const VariableInFactor * const vifs,
    const double * const var_values, 
    const long & vid, const double & proposal) const{

    const VariableInFactor & vif = vifs[n_start_i_vif];
    double sum;
    if(vif.vid == vid){
      sum = (vif.is_positive == true ? (proposal==vif.equal_to) : 1-(proposal==vif.equal_to));
    }else{
      sum = (vif.is_positive == true ? (var_values[vif.vid]==vif.equal_to) : 1-(var_values[vif.vid]==vif.equal_to));
    }

    for(long i_vif=n_start_i_vif+1;i_vif<n_start_i_vif+n_variables;i_vif++){
      const VariableInFactor & vif = vifs[i_vif];
      if(vif.vid == vid){
        if(sum != (vif.is_positive == true ? (proposal==vif.equal_to) : 1-(proposal==vif.equal_to))){
          return 0.0;
        }
      }else{
        if(sum != (vif.is_positive == true ? (var_values[vif.vid]==vif.equal_to) : 1-(var_values[vif.vid]==vif.equal_to))){
          return 0.0;
        }
      }
    }
    return 1.0;
  }

  inline double dd::CompactFactor::_potential_and(
    const VariableInFactor * const vifs,
    const double * const var_values, 
    const long & vid, const double & proposal) const{

    double sum = 0.0;
    for(long i_vif=n_start_i_vif;i_vif<n_start_i_vif+n_variables;i_vif++){
      const VariableInFactor & vif = vifs[i_vif];
      if(vif.vid == vid){
        sum += (vif.is_positive == false ? (proposal==vif.equal_to) : 1-(proposal==vif.equal_to));
      }else{
        sum += (vif.is_positive == false ? (var_values[vif.vid]==vif.equal_to)
          : 1-(var_values[vif.vid]==vif.equal_to));
      }
    }
    if(sum != 0){
      return 1.0 - 1.0;
    }else{
      return 1.0 - 0.0;
    }
  }

  inline double dd::CompactFactor::_potential_or(
    const VariableInFactor * const vifs,
    const double * const var_values, 
    const long & vid, const double & proposal) const{

    double sum = 0.0;
    for(long i_vif=n_start_i_vif;i_vif<n_start_i_vif+n_variables;i_vif++){
      const VariableInFactor & vif = vifs[i_vif];
      if(vif.vid == vid){
        sum += (vif.is_positive == true ? (proposal==vif.equal_to) : 1-(proposal==vif.equal_to));
      }else{
        sum += (vif.is_positive == true ? (var_values[vif.vid]==vif.equal_to)
          : 1-(var_values[vif.vid]==vif.equal_to));
      }
    }
    if(sum != 0){
      return 1.0;
    }else{
      return 0.0;
    }
  }


  inline double dd::CompactFactor::_potential_imply(
      const VariableInFactor * const vifs,
      const double * const var_values, 
      const long & vid, const double & proposal) const{

      double sum = 0.0;

      for(long i_vif=n_start_i_vif;i_vif<n_start_i_vif+n_variables;i_vif++){
        const VariableInFactor & vif = vifs[i_vif];
        
        if(vif.n_position == n_variables - 1){
          if(vif.vid == vid){
            //if(vid == 548590 || vid == 2531610) std::cout << "  head  " << vif.vid << " , " << proposal << " , " << vif.is_positive << " , " << (proposal==vif.equal_to) << std::endl;
            sum += (vif.is_positive == true ? (proposal==vif.equal_to) : 1-(proposal==vif.equal_to));
            //std::cout << proposal << " --> " << (proposal==vif.equal_to) << " " << vif.equal_to << std::endl; 
          }else{
            //if(vid == 548590 || vid == 2531610) std::cout << "  head  " << vif.vid << " , " << var_values[vif.vid] << " , " << vif.is_positive << " , " << (var_values[vif.vid]==vif.equal_to) << std::endl;
            sum += (vif.is_positive == true ? (var_values[vif.vid]==vif.equal_to)
              : 1-(var_values[vif.vid]==vif.equal_to));
          }
        }else{
          if(vif.vid == vid){
            //if(vid == 548590 || vid == 2531610) std::cout << "  body  " << vif.vid << " , " << proposal << " , " << vif.is_positive << " , " << (proposal==vif.equal_to) << std::endl;
            sum += (vif.is_positive == false ? (proposal==vif.equal_to) : 1-(proposal==vif.equal_to));
          }else{
            //if(vid == 548590 || vid == 2531610) std::cout << "  body  " << vif.vid << " , " << var_values[vif.vid] << " , " << vif.is_positive << " , " << (var_values[vif.vid]==vif.equal_to) << std::endl;
            sum += (vif.is_positive == false ? (var_values[vif.vid]==vif.equal_to)
              : (var_values[vif.vid]==vif.equal_to));
          }
        }
      }

      //if(vid == 548590 || vid == 2531610){
      //  std::cout << "@" << n_variables << "  v" << vid << "    " << proposal << "    " << sum << std::endl;  
      //}


      if(sum != 0){
        //std::cout << "f" << id << " " << proposal << " -> 1.0" << "    " << vifs[0].equal_to <<std::endl;
        return 1.0;
      }else{
        //std::cout << "f" << id << " " << proposal << " -> 0.0" << "    " << vifs[0].equal_to <<std::endl;
        return 0.0;
      }
  }
}

#endif




