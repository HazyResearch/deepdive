R(a int, b int).

A(x, x + y) :- R(x, y).
B(x, z) :- A(x, y), R(y, z).