R?(x text).
S?(x text).
T(x text).

@weight(3)
@semantics("linear")
R(x) => S(x) :- T(x).