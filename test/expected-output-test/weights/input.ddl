a?(k int).
c?(k int).
d?(k int).
e?(k int).
f?(k int).
b(k int, p int, q text).

a(x) :- b(x,y,_)
weight = x + y.

c(x) :- b(x,y,_)
weight = "string".

d(x) :- b(x,y,_)
weight = x, y.

e(x) :- b(x,y,_)
weight = -10.

f(x) :- b(x,y,_)
weight = -0.3.