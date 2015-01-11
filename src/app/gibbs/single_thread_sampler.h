
#include "dstruct/factor_graph/factor_graph.h"
#include "timer.h"
#include "common.h"

#ifndef _SINGLE_THREAD_SAMPLER_H
#define _SINGLE_THREAD_SAMPLER_H

namespace dd{

  class SingleThreadSampler{

  public:
    FactorGraph * const p_fg;

    double potential_pos;
    double potential_neg;

    double potential_pos_freeevid;
    double potential_neg_freeevid;

    double proposal_freevid;
    double proposal_fixevid;

    double r;
    unsigned short p_rand_seed[3];
    double * const p_rand_obj_buf;

    std::vector<double> varlen_potential_buffer;

    double sum;
    double acc;
    double multi_proposal;

    SingleThreadSampler(FactorGraph * _p_fg) :
      p_fg (_p_fg), p_rand_obj_buf(new double){
      p_rand_seed[0] = rand();
      p_rand_seed[1] = rand();
      p_rand_seed[2] = rand();
    }

    void sample(const int & i_sharding, const int & n_sharding){
      long nvar = p_fg->n_var;
      long start = ((long)(nvar/n_sharding)+1) * i_sharding;
      long end = ((long)(nvar/n_sharding)+1) * (i_sharding+1);
      end = end > nvar ? nvar : end;
      //for(int i=0;i<100;i++){
      for(long i=start; i<end; i++){
        this->sample_single_variable(i);
      }
      //}
    }

    void sample_sgd(const int & i_sharding, const int & n_sharding){
      long nvar = p_fg->n_var;
      long start = ((long)(nvar/n_sharding)+1) * i_sharding;
      long end = ((long)(nvar/n_sharding)+1) * (i_sharding+1);
      end = end > nvar ? nvar : end;
      for(long i=start; i<end; i++){
        this->sample_sgd_single_variable(i);
      }
    }

  private:

    void sample_sgd_single_variable(long vid){
      
      Variable & variable = p_fg->variables[vid];

      if(variable.is_evid == false){
	      return;
      }

      if(variable.domain_type == DTYPE_BOOLEAN){

          if(variable.is_evid == false){
	    //return;
            potential_pos = p_fg->template potential<false>(variable, 1);
            potential_neg = p_fg->template potential<false>(variable, 0);
            *this->p_rand_obj_buf = erand48(this->p_rand_seed);
            if((*this->p_rand_obj_buf) * (1.0 + exp(potential_neg-potential_pos)) < 1.0){
              p_fg->template update<false>(variable, 1.0);
            }else{
              p_fg->template update<false>(variable, 0.0);
            }
          }

          potential_pos_freeevid = p_fg->template potential<true>(variable, 1);
          potential_neg_freeevid = p_fg->template potential<true>(variable, 0);

          *this->p_rand_obj_buf = erand48(this->p_rand_seed);
          if((*this->p_rand_obj_buf) * (1.0 + exp(potential_neg_freeevid-potential_pos_freeevid)) < 1.0){
            p_fg->template update<true>(variable, 1.0);
          }else{
            p_fg->template update<true>(variable, 0.0);
          }

          this->p_fg->update_weight(variable);
          
      }else if(variable.domain_type == DTYPE_MULTINOMIAL){

        while(variable.upper_bound >= varlen_potential_buffer.size()){
          varlen_potential_buffer.push_back(0.0);
        }

        if(variable.is_evid == false){
          sum = -100000.0;
          acc = 0.0;
          multi_proposal = -1;
          for(int propose=0;propose <= variable.upper_bound; propose++){
            varlen_potential_buffer[propose] = p_fg->template potential<false>(variable, propose);
            sum = logadd(sum, varlen_potential_buffer[propose]);
          }

          *this->p_rand_obj_buf = erand48(this->p_rand_seed);
          for(int propose=0;propose <= variable.upper_bound; propose++){
            acc += exp(varlen_potential_buffer[propose]-sum);
            if(*this->p_rand_obj_buf <= acc){
              multi_proposal = propose;
              break;
            }
          }
          assert(multi_proposal != -1.0);
          p_fg->template update<false>(variable, multi_proposal);
        }

        sum = -100000.0;
        acc = 0.0;
        multi_proposal = -1;
        for(int propose=0;propose <= variable.upper_bound; propose++){
          varlen_potential_buffer[propose] = p_fg->template potential<true>(variable, propose);
          sum = logadd(sum, varlen_potential_buffer[propose]);
        }

        *this->p_rand_obj_buf = erand48(this->p_rand_seed);
        for(int propose=0;propose <= variable.upper_bound; propose++){
          acc += exp(varlen_potential_buffer[propose]-sum);
          if(*this->p_rand_obj_buf <= acc){
            multi_proposal = propose;
            break;
          }
        }
        assert(multi_proposal != -1.0);
        p_fg->template update<true>(variable, multi_proposal);

        this->p_fg->update_weight(variable);

      }else{
        //std::cout << "[ERROR] Only Boolean and Multinomial variables are supported now!" << std::endl;
        //assert(false);
        return;
      }
      
    }

    void sample_single_variable(long vid){

      Variable & variable = this->p_fg->variables[vid];

      if(variable.domain_type == DTYPE_BOOLEAN){

        if(variable.is_evid == false){

          potential_pos = p_fg->template potential<false>(variable, 1);
          potential_neg = p_fg->template potential<false>(variable, 0);

          *this->p_rand_obj_buf = erand48(this->p_rand_seed);
          if((*this->p_rand_obj_buf) * (1.0 + exp(potential_neg-potential_pos)) < 1.0){
            p_fg->template update<false>(variable, 1.0);
          }else{
            p_fg->template update<false>(variable, 0.0);
          }

        }

      }else if(variable.domain_type == DTYPE_MULTINOMIAL){

        while(variable.upper_bound >= varlen_potential_buffer.size()){
          varlen_potential_buffer.push_back(0.0);
        }

        if(variable.is_evid == false){
          sum = -100000.0;
          acc = 0.0;
          multi_proposal = -1;
          for(int propose=0;propose <= variable.upper_bound; propose++){
            varlen_potential_buffer[propose] = p_fg->template potential<false>(variable, propose);
            sum = logadd(sum, varlen_potential_buffer[propose]);
          }

          *this->p_rand_obj_buf = erand48(this->p_rand_seed);
          for(int propose=0;propose <= variable.upper_bound; propose++){
            acc += exp(varlen_potential_buffer[propose]-sum);
            if(*this->p_rand_obj_buf <= acc){
              multi_proposal = propose;
              break;
            }
          }
          assert(multi_proposal != -1.0);
          p_fg->template update<false>(variable, multi_proposal);
        }

      }else{
        //std::cout << "[ERROR] Only Boolean and Multinomial variables are supported now!" << std::endl;
        //assert(false);
        return;
      }

    }

  };

}


#endif

