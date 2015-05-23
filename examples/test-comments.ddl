# This is a comment
// This is also a comment

R(a int).
S( a int   # this is an integer field
 , b text  // seond one is a text field
 , c /* third one is a float field */ float
).

R(x) :- S(x, y, z).

/*
Multi
-line
comments
    /* unfortunately cannot be nested */
