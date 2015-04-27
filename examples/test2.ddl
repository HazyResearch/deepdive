      S(a1,a2); 
      R(pk,f); 
      Q(x) :- R(x,f) weight=f; 
      Q(x) :- S(x,y),T(y); 
      T(base_attr)!; 
      R(y,x) :- U(x,y); 
      S(x,y) :- R(x,y);
