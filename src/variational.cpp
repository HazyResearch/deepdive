#include <iostream>
#include <fstream>
#include <string>
#include <vector>

#include <stdlib.h>
#include <vector>
#include <cstdio>
#include <stdio.h>
#include <sstream>
#include <iostream>
#include <fstream>
#include <cmath>

#include "sinco/sinco.hpp"


double funvalue_update(double alpha,  double K, double Wij, double  Wii, double Wjj, 
                     double Aij, double Sij,double  Sji, double update) {
  double detratio, fchange;
  /* This routine computes the change in the objective function value when a step of length\ alpha is made in the direction e_ie_j^T+e_je_i^T  */
if (update ==1) {
  detratio=(1+alpha*Wij)*(1+alpha*Wij-alpha*alpha*Wii*Wjj/(1+alpha*Wij));
  fchange=K*(log(detratio))-2*alpha*Aij-alpha*(Sij+Sji);
 }
 else {
   detratio=(1-alpha*Wij)*(1-alpha*Wij-alpha*alpha*Wii*Wjj/(1-alpha*Wij));
  fchange=K*(log(detratio))+2*alpha*Aij-alpha*(Sij+Sji);
 }
 return fchange;
}


void invupdate(Matrix& Gradp,  Matrix& Gradpp,  vector<int>& UpInd, Matrix& W, const double theta, const double K, const int p, int i, int j, int update)
{
 /* This routine computes the change in the dual matrix W (inverse of C) when a step of length\ alpha is made in the direction e_ie_j^T+e_je_i^T  */
double a,b,c,d, wij, wjj, wii;
 int ii, jj, upindind;
Matrix UpW(p,p);
wii=W(i,i);
wjj=W(j,j);
wij=W(i,j);
if (update==1){
 d=theta*theta*(wii*wjj-wij*wij)-1-2*theta*wij;
 a=-(1+theta*wij)/d;
 b=theta*wjj/d;
 c=theta*wii/d;
 for (int ii=p-1; ii>=0; ii--) {
   for (int jj=ii; jj>=0; jj--){
     upindind=ii*p+jj;
     UpW(ii,jj)=-theta*(a*W(i,ii)*W(j,jj)+ c*W(j,ii)*W(j,jj)+b*W(i,ii)*W(i,jj)+a*W(j,ii)*W(i,jj));
     if (fabs(UpW(ii,jj))>zerotol){UpInd[upindind]=1;}
     else {UpInd[ii*p+jj]=0;}
     UpW(jj,ii)=UpW(ii,jj);
     UpInd[jj*p+ii]=UpInd[upindind];
   } 
 }
}
 else{
 d=theta*theta*(-wii*wjj+wij*wij)+1-2*theta*wij;
 a=(1-theta*wij)/d;
 b=theta*wjj/d;
 c=theta*wii/d;
 
 for (int ii=p-1; ii>=0; ii--) {
   for (int jj=ii; jj>=0; jj--) {
     upindind=ii*p+jj;
     UpW(ii,jj)=theta*(a*W(i,ii)*W(j,jj)+ c*W(j,ii)*W(j,jj)+b*W(i,ii)*W(i,jj)+a*W(j,ii)*W(i,jj));
     if (fabs(UpW(ii,jj))>zerotol){UpInd[upindind]=1;}
     else {UpInd[ii*p+jj]=0;}
     UpW(jj,ii)=UpW(ii,jj);
     UpInd[jj*p+ii]=UpInd[upindind];
   }
 }
}
W.sum(UpW);
Gradp.sumwithscal(K,UpW);
Gradpp.sumwithscal(-K,UpW);
 
}



double findposstep(double K, double Wij, double Wii, double Wjj, 
                   double Aij, double Sij, double Sji, int update)  {
 /* This routine computes the optimal length of  a step of length in the direction e_ie_j^T+e_je_i^T for the positive component of C */
  double aux1, aux2, aux3, aux4, aux5;
  double a, b, c, D, alpha, alpha1, alpha2;

  if ( update ==1 ) {
    aux1=2*(K*Wij-Aij)-Sji-Sij;
    aux2=(Wii*Wjj-Wij*Wij);
    aux3=2*Wij;
    aux4=-2*K*(Wii*Wjj+Wij*Wij);
    aux5=2*K*Wij*aux2;
  }
  else {
    aux1=2*(-K*Wij+Aij)-Sij-Sji;  
    aux2=(-Wii*Wjj+Wij*Wij);
    aux3=2*Wij;
    aux4=2*K*(Wii*Wjj+Wij*Wij);
    aux5=-2*K*Wij*aux2;
  }
  a=aux2*aux1-aux5;
  b=-aux3*aux1-aux4;
  c=-update*aux1;
  if (fabs(a)>zerotol) {
    D=b*b-4*a*c;
    if (D<0) { 
      printf ( "negative discriminant, \n");
      abort;
    }
    alpha1=fmin((-b-sqrt(D))/(2*a),(-b+sqrt(D))/(2*a));
    alpha2=fmax((-b-sqrt(D))/(2*a),(-b+sqrt(D))/(2*a));
    if (alpha1>=0){ alpha=alpha1;}
    else  {
      if (alpha2>=0){ alpha=alpha2;}
       else {
        printf ( "unbounded direction!!!, \n");
        abort;
      }
    }
  }
  else if (-c/b>0)
    { alpha=-c/b;}
  else {
    printf ( "unbounded direction!!!,\n");
    abort;
  }

  /* Check correctness
double theta=alpha;
 double fprime;
 if ( update ==1 ) {
    fprime=(K*Wij+K*Wij-Aij-Aij-Sij-Sji)- 
     K*theta/(theta*theta*(Wii*Wjj-Wij*Wij)-1-2*theta*Wij)*(-2*(Wii*Wjj+Wij*Wij) 
   +2*theta*Wij*(Wii*Wjj-Wij*Wij));
 }
 else {
   fprime=(-K*Wij-K*Wij+Aij+Aij-Sij-Sji)-K*theta/(theta*theta*(-Wii*Wjj+Wij*Wij) 
           +1-2*theta*Wij)*(2*(Wii*Wjj+Wij*Wij)
           +2*theta*Wij*(Wii*Wjj-Wij*Wij));
 }
 if (fabs(fprime) > 10e-6){
   printf("\n bad gradient");
   fflush(stdout);
    abort;
    } */
  return alpha;
}


double findnegstep(double K, bool diag, double Wij, double Wii, double Wjj, 
                   double Aij, double Sij, double Sji, double Cpij, double Cppij,  int update)  {
/* This routine computes the optimal length of  a step of length in the direction e_ie_j^T+e_je_i^T for the negative component of C */
  double aux1, aux2, aux3, aux4, aux5;
  double a, b, c, D, maxstep, alpha, alpha1, alpha2;

  if (update ==1) {
   aux1=2*(K*Wij-Aij)-Sji-Sij;
   aux2=(Wii*Wjj-Wij*Wij);
   aux3=2*Wij;
   aux4=-2*K*(Wii*Wjj+Wij*Wij);
   aux5=2*K*Wij*aux2;
   maxstep=Cpij;}
  else {
   aux1=2*(-K*Wij+Aij)-Sij-Sji;  
   aux2=(-Wii*Wjj+Wij*Wij);
   aux3=2*Wij;
   aux4=2*K*(Wii*Wjj+Wij*Wij);
   aux5=-2*K*Wij*aux2;
   maxstep=Cppij;
}
if (diag) {
 maxstep=maxstep/2;
}
a=aux2*aux1-aux5;
b=-aux3*aux1-aux4;
c=-update*aux1;
 if (fabs(a)>zerotol) { 
  D=b*b-4*a*c;
  if (D<0) { 
      printf ( "negative discriminant, \n");
      abort;
    }
  double sqrootD=sqrt(D);
  alpha1=fmin(((-b-sqrootD)/(2*a)),((-b+sqrootD)/(2*a)));
  alpha2=fmax(((-b-sqrootD)/(2*a)),((-b+sqrootD)/(2*a)));
  if ( alpha2<0 ){
    alpha=fmax(alpha2, -maxstep);}
  else if ( alpha1<0 ){
    alpha=fmax(alpha1, -maxstep);}
  else {
    alpha=-maxstep; 
  }
 }
 else if (-c/b<0){
   alpha=fmax(-c/b, -maxstep);}
 else {
   alpha=-maxstep;
 }
 /* Check correctness 

double theta=alpha;
 double fprime;
 if ( update ==1 ) {
    fprime=(K*Wij+K*Wij-Aij-Aij-Sij-Sji)- 
     K*theta/(theta*theta*(Wii*Wjj-Wij*Wij)-1-2*theta*Wij)*(-2*(Wii*Wjj+Wij*Wij) 
   +2*theta*Wij*(Wii*Wjj-Wij*Wij));
 }
 else {
   fprime=(-K*Wij-K*Wij+Aij+Aij-Sij-Sji)-K*theta/(theta*theta*(-Wii*Wjj+Wij*Wij) 
           +1-2*theta*Wij)*(2*(Wii*Wjj+Wij*Wij)
           +2*theta*Wij*(Wii*Wjj-Wij*Wij));
 }
 if ((abs(theta)<maxstep) && (fabs(fprime) > 10e-6)){
   printf("\n bad gradient");
   fflush(stdout);
    abort;
    } */
  return alpha;
} 


void ICS::sinco(SOL& ICS_sol, const SOL& ICS_start, double tol ){
  /*This is the main routine for computing the sparse inverse covariance.
   ICS class contains the problem data, that is A, K, p, lambda and S. 
   The input for this routine is a starting point ICS_start which contains 
   intial C, initial W=C^{-1} and initial objective function value. The output
of the routine is final C, W and function value. Tolerance is set in tol and
if set too high may significantly slow down the solver, since convergence
 is slow in the end.  */
  vector<int> UpInd;
  double alphamax, funmax, K, fnew;
  int imax, jmax, updatemax,  iter=0;
  UpInd.resize(p*p, 1); 
  bool stop=false;
  double f=ICS_start.f;
  Steps AllSteps(p,p);
  K=N/2;
  ICS_sol.W=ICS_start.W;
  ICS_sol.C=ICS_start.C;
  Matrix Cp(p,p,0);
  Matrix Cpp(p,p,0);
  Matrix Gradp(p,p,0);
  Matrix Gradpp(p,p,0);
  for (int i=p-1; i>=0; i--) {
    for (int j=i; j>=0; j--) {
            if (ICS_start.C(i,j)>0) { 
               Cp(i,j)=ICS_start.C(i,j);
               Cp(j,i)=ICS_start.C(j,i); }
            else {
               Cpp(i,j)=-ICS_start.C(i,j);
               Cpp(j,i)=-ICS_start.C(j,i);
            }
     }
  }
  Gradp.sumwithscal(-1*lambda, S);
  Gradp.sumwithscal(-1, A);
  Gradp.sumwithscal(K,ICS_sol.W);
  Gradpp.sumwithscal(-1*lambda, S);
  Gradpp.sum(A);
  Gradpp.sumwithscal(-1*K,ICS_sol.W);
  

  while (!stop && (iter<100000)) {
    // %find the largest positive derivative 
      alphamax=0;
      funmax=f;
      iter+=1;
      //      printf("\n iteration [%4d] [%6f]", iter, f);
      //    fflush(stdout);
     
      for (int i=p-1; i>=0; i--) {
        for (int j=i; j>=0; j--) {
          if (UpInd[i*p+j] || UpInd[i*p+i] || UpInd[j*p+j]){
            AllSteps(i,j).alpha=0;
            if ((Gradp(i,j)+Gradp(j,i) > tol) && (Cpp(i,j)<=tol)){
              AllSteps(i,j).update=1;
              AllSteps(i,j).alpha=findposstep(K, ICS_sol.W(i,j),ICS_sol.W(i,i),
                ICS_sol.W(j,j), A(i,j), lambda*S(i,j),
                                                lambda*S(j,i), AllSteps(i,j).update);  
            }  
            else if ((Gradpp(i,j)+Gradpp(j,i) > tol) && (Cp(i,j)<=tol)) {
              AllSteps(i,j).update=-1;
              AllSteps(i,j).alpha=findposstep(K, ICS_sol.W(i,j),ICS_sol.W(i,i),
                ICS_sol.W(j,j), A(i,j), lambda*S(i,j),
                                                lambda*S(j,i), AllSteps(i,j).update);  
            }
            else if ((Gradp(i,j)+Gradp(j,i)<-tol) && (Cp(i,j)> tol )) {
              AllSteps(i,j).update=1;
              AllSteps(i,j).alpha=findnegstep(K, (i==j), ICS_sol.W(i,j),ICS_sol.W(i,i),
                ICS_sol.W(j,j), A(i,j), lambda*S(i,j),
                                              lambda*S(j,i), Cp(i,j), Cpp(i,j), 
                                              AllSteps(i,j).update); 
            }
            else if ((Gradpp(i,j)+Gradpp(j,i)<-tol) && (Cpp(i,j)> tol)) {
              AllSteps(i,j).update=-1;
              AllSteps(i,j).alpha=findnegstep(K, (i==j), ICS_sol.W(i,j),ICS_sol.W(i,i),
              ICS_sol.W(j,j), A(i,j), lambda*S(i,j),
                                       lambda*S(j,i), Cp(i,j), Cpp(i,j), AllSteps(i,j).update); 
            }
            if ( fabs(AllSteps(i,j).alpha)> tol) {
               AllSteps(i,j).fchange=funvalue_update(AllSteps(i,j).alpha,
                                              K,ICS_sol.W(i,j),ICS_sol.W(i,i),
                ICS_sol.W(j,j), A(i,j), lambda*S(i,j),
                                              lambda*S(j,i),  AllSteps(i,j).update); 
            }
            else {
               AllSteps(i,j).fchange=0;
            }
          }
          fnew=f+AllSteps(i,j).fchange;
          if (fnew > funmax ) {
            funmax=fnew;
            imax=i;
            jmax=j;
            alphamax=AllSteps(i,j).alpha;
            updatemax=AllSteps(i,j).update;
    }
  }
      }
      /*      for (int i=0; i<p; i++) { 
      for (int j=0; j<p; j++) {
        printf("\n [%6f] [%6f] [%6d] [%6d]", AllSteps(i,j).fchange, AllSteps(i,j).alpha, i, j);
       }
       } */
    if ( (alphamax==0) || ((funmax-f)/(fabs(f)+1))<tol ) { stop=true;}
    else {
      if (updatemax==1) {
        Cp(imax,jmax)=Cp(imax,jmax)+alphamax;
        Cp(jmax,imax)=Cp(jmax,imax)+alphamax; 
      }
      else {
        Cpp(imax,jmax)=Cpp(imax,jmax)+alphamax;
        Cpp(jmax,imax)=Cpp(jmax,imax)+alphamax;
      }
      if ((Cp(imax,jmax)<0) || (Cpp(imax, jmax)<0)) {
  printf("\n illegal step");
    }
      f=funmax;
      invupdate(Gradp, Gradpp,  UpInd, ICS_sol.W, alphamax, K, p, imax, jmax, updatemax);
      /*    for (int i=0; i<p; i++) { 
       for (int j=0; j<p; j++) {
   printf("\n [%6f] [%6f] [%6d] [%6d]", Gradp(i,j), Gradpp(i,j), i, j);
        }
  } */
    }
  }

  ICS_sol.C=Cp;
  ICS_sol.C.sumwithscal(-1, Cpp);
  ICS_sol.f=f;
}  // end of the sinco routine.



class Component {
public:
  std::vector<long> vids;
};

std::vector<Component> components;

int main(int argc, char** argv){

  std::string dir(argv[1]);
  int nsample = atoi(argv[2]);

  /** first, read all components **/
  std::string file_compoments = dir + "/mat_components_hasevids";
  std::ifstream fin(file_compoments.c_str());
  long vid, has_evid;
  while(fin >> vid >> has_evid){
    components.push_back(Component());
  }
  std::cout << "Created Components: " << components.size() << std::endl;

  /** second, read variables into components **/
  std::string file_var_comps = dir + "/mat_active_components";
  std::ifstream fin2(file_var_comps.c_str());
  long compid;
  long nvars = 0;
  while(fin2 >> vid >> compid){
    components[compid].vids.push_back(vid);
    nvars ++;
  }
  std::cout << "Created Variables: " << nvars << std::endl;

  std::string file_output_rs = dir + "/mat_compact_factorgraphs";
  std::ofstream fout(file_output_rs.c_str());

  float * tally_values = new float[nvars];
  for(long i=0;i<nvars;i++){
    tally_values[i] = 0;
  }
  for(int i=0;i<nsample;i++){
    std::ifstream fsample((dir + "/mat_samples.epoch_" + std::to_string(i) + "_numa_0.text").c_str());
    long tmp;
    long ct = 0;
    while(fsample >> tmp){
      tally_values[ct++] += tmp;
    }
  }
  float todivide = 1.0/nsample;
  for(long i=0;i<nvars;i++){
    tally_values[i] *= todivide;
  }
  std::cout << "Calculated Variables Mean." << std::endl;

  /** third, for each components, construct the invariance matrix **/
  /** This could be faster if we introduce more structures in the components,
      but this is not a bottleneck now **/
  for(long cid=0;cid<components.size();cid++){
    const Component & component = components[cid];
    double v1, v2;
    ICS test;
    test.p = component.vids.size();
    int p=test.p;

    Matrix A(p,p,1);
    A.dim1=p;
    A.dim2=p;
    test.S=A;

    /**
      TODO: FILL IN CONV
    **/

    Matrix Zeros(p,p,0);
    Zeros.dim1=p;
    Zeros.dim2=p;

    SOL Init_Sol;
    SOL Opt_Sol;

    Init_Sol.C=Zeros;
    Init_Sol.W=Zeros;
    Opt_Sol.C=Zeros;
    Opt_Sol.W=Zeros;
    test.A=Zeros;

    test.lambda=200;
    test.N=50;

    Init_Sol.W.addidentity(1);
    Init_Sol.C.addidentity(1);
    v1=Init_Sol.C.dotmultiply(test.A);
    v2=Init_Sol.C.normS(test.S);

    Init_Sol.f=-v1-test.lambda*v2;
    double tol=1e-6;
    for (int iter=1; iter<=20; iter++){
      test.sinco(Opt_Sol, Init_Sol, tol );
      test.lambda-=10;
      Init_Sol=Opt_Sol;
      v2=Init_Sol.C.normS(test.S);
      Init_Sol.f-=10*v2;
    }

    for (int i=0; i<p; i++) { 
      for (int j=0; j<p; j++) {
        if(i == j){
          if(tally_values[component.vids[i]] > 0.99){
            fout << component.vids[i] << " " << component.vids[j] << " " << 10 << std::endl; 
          }else if(tally_values[component.vids[i]] < 0.01){
            fout << component.vids[i] << " " << component.vids[j] << " " << -10 << std::endl; 
          }else{
            float a = log(tally_values[component.vids[i]]/(1-tally_values[component.vids[i]]));
            fout << component.vids[i] << " " << component.vids[j] << " " << a << std::endl; 
          }
        }else if ( Opt_Sol.C(i,j) !=0) {
          fout << component.vids[i] << " " << component.vids[j] << " " << Opt_Sol.C(i,j) << std::endl;
        }
      }
    }

  }

  delete [] tally_values;
  fout.close();

  return 0;
}





