a?(k int).
b(k int, p text, q text, r int).
c(s text, n int, t text).

a(x) :- b(x, y, _, _) weight = 1, 2.