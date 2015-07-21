a?(k int).
c?(k int).
b(k int, p int, q text).

a(x) :- b(x,y,_)
weight = x + y.

c(x) :- b(x,y,_)
weight = "string".