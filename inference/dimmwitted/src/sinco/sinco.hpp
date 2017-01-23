

#include <stdlib.h>
#include <vector>
#include <cstdio>
#include <stdio.h>
#include <sstream>
#include <iostream>
#include <fstream>
#include <cmath>

#define zerotol 1e-10
using namespace std;


class Matrix{
public: 
  Matrix(){}
    Matrix(int d1, int d2, double elm=0){ 
	M.resize(d1*d2, elm); 
        dim1=d1;
        dim2=d2;
    }
    Matrix(const Matrix& A){ 
	M=A.M; 
        dim1=A.dim1;
        dim2=A.dim2;
    }
    Matrix& operator=(const Matrix& A){
        if (this !=&A){
          M=A.M;
          dim1=A.dim1;
          dim2=A.dim2;
        }
        return *this;
    }

    inline double operator()(int i, int j) const {
        return M[i*dim2+j];
    }
    inline double& operator()(int i, int j){
        return M[i*dim2+j];
    }

      double dotmultiply (Matrix A){
      double S=0;
	for (int i=dim1*dim2-1; i>=0; i--) {
              S+=A.M[i]*M[i];
        }
	return S;
      }
      void importMat(double* A, int d1, int d2) {
        M.resize(d1*d2);
        for (int i=d1*d2-1; i>=0; i--) { 
	    M[i]=A[i];
        }
	dim1=d1;
        dim2=d2;
      }

      void exportMat(double* A) {
        for (int i=dim1*dim2-1; i>=0; i--) {
           A[i]=M[i];
        }
      }


       
  /*   double dotmultiply (Matrix& A){
      double S=0;
	for (int i=0; i<dim1*dim2; i++) {
              S+=A.M[i]*M[i];
        }
	return S; 
    } */
    void sum (Matrix& A){
        for (int i=dim1*dim2-1; i>=0; i--) {
	   M[i]+=A.M[i];
        }
    }
    void sumwithscal(double N, Matrix& A){
	for (int i=dim1*dim2-1; i>=0; i--) {
              M[i]+=N*A.M[i];
        }
    }
    void addidentity(double N){
	for (int i=0; i<dim1; i++) {
              M[i*dim1+i]+=N;
        }
    }
     double normS (Matrix& A){
        double S=0;
	for (int i=dim1*dim2-1; i>=0; i--) {
            S+=fabs(M[i]*A.M[i]);
        }
        return S;
	} 
  /*   double normS (Matrix& A){
        double S=0;
	for (int i=0; i<dim1*dim2; i++) {
            S+=fabs(M[i]*A.M[i]);
        }
        return S;
	} */
 
    vector< double>  M;
    int dim1;
    int dim2;
};

class Step{
public:
     double alpha;
     double fchange;
     int   update;
};

  
class Steps{
public:  
    Steps(int d1, int d2){ 
	Stepmat.resize(d1*d2); 
        dim1=d1;
        dim2=d2;
	}
    inline Step& operator()(int i, int j){
        return Stepmat[i*dim2+j];
    }
    vector< Step >  Stepmat;
    int dim1;
    int dim2;
};

class SOL{
public:
    Matrix C;
    Matrix W;
    double f;
};

/* Class ICS is a class of "Inverse Covariance Selection" problems, it has the optimization function, the data A,  lambda, S, p and N. */

class ICS{
public:
    void sinco(SOL& ICS_sol, const SOL& ICS_start, double tol );
    Matrix A;
    Matrix S;
    double lambda;
    int p;
    int N;
    
};


