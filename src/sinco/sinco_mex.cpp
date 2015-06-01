// MEX wrapper for the function computing sparse inv. covariance 

// function [Csol, Wsol, fsol]=sinco(Cstart, fstart, Wstart, A, S, lambda, p, K);

// Calling sequence: void ICS::sinco(SOL& ICS_sol, const SOL& ICS_start, double tol ){

// Last Modified: Katya Scheinber Feb 2009
//


#include "sinco.hpp" 
#include "mex.h"

void mexFunction(int nlhs, mxArray *plhs[], int nrhs, const mxArray *prhs[])
{
  double *Cstart, *Wstart,  *Csol, *Wsol, *A, *S, *fsol;  
  double fstart,  K, lambda, tol;
  int p;
  ICS prob;
  SOL Init_Sol;
  SOL  Opt_Sol;
 
  Cstart = mxGetPr(prhs[0]);
  p = mxGetN(prhs[0]);
  Wstart = mxGetPr(prhs[2]);
  A = mxGetPr(prhs[3]);
  S = mxGetPr(prhs[4]);
  fstart = mxGetScalar(prhs[1]);
  lambda = mxGetScalar(prhs[5]);
  K = mxGetScalar(prhs[6]);
  tol = mxGetScalar(prhs[7]);

  plhs[0] = mxCreateDoubleMatrix(p, p, mxREAL);
  plhs[2] = mxCreateDoubleMatrix(1, 1, mxREAL);
  plhs[1] = mxCreateDoubleMatrix(p, p, mxREAL);
  Csol = mxGetPr(plhs[0]);
  fsol = mxGetPr(plhs[2]);
  Wsol = mxGetPr(plhs[1]);

  prob.p=p;
  prob.S.importMat(S, p, p);
  prob.A.importMat(A, p, p);
  prob.lambda=lambda;
  prob.N=K;
  Init_Sol.C.importMat(Cstart, p, p);
  Init_Sol.W.importMat(Wstart, p, p);
  Init_Sol.f=fstart;
 
  prob.sinco(Opt_Sol, Init_Sol, tol );

  Opt_Sol.C.exportMat(Csol);
  Opt_Sol.W.exportMat(Wsol);
  *fsol=Opt_Sol.f;
  
}

