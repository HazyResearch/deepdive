A(a1 int,
  a2 int).
B(a1 int,
  a2 int).
C(a1 int,
  a2 int,
  a3 int).
Q(a1) :- A(a1, x); B(y, a1); C(a, b, a1).
