a(k int).
b(k int, p text, q text, r int).
c(s text, n int, t text).

Q("test", 123, id) :- a(id), b(id, x,y,z), c("foo", 10, t), z>100.