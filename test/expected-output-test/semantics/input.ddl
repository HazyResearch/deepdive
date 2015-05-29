R(a int, b int, c int).
S(a int, b int, c int).
T(a int, b int, c int).
Q?(x int).

Q(x) :- R(x, y, z); R(x, y, z), S(y, z, w); S(y, x, w), T(x, z, w)
weight = y
semantics = imply.
