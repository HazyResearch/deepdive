a(k int).
b(k int, p text, q text, r int).
c(s text, n int, t text).

# comprehensive
Q("test" :: TEXT, 123, id, unnest(y)) * :- a(id), b(id, x,y,z), c(x || y,10,"foo"), [z>100; !z < 20, z < 50].

# group by
P(y, z, MAX(w)) :- a(x), b(x,y,z,w).

# expressions
A( x + (w * (x + w)) ) :- b(x,y,z,w).

B( func(x + w + x) ) :- b(x,y,z,w).

C( func(func(func(x))) ) :- b(x,y,z,w).

D( func(x * func2(w + x)) + x ) :- b(x,y,z,w).

E( x :: INT) :- b(x,y,z,w).

F( (x :: INT) + (w :: INT) ) :- b(x,y,z,w).

# conditions
G(x) :- b(x,y,z,w), x + w = 100.

H(x) :- b(x,y,z,w), x + w = 100, x > 50.

I(x) :- b(x,y,z,w), [x + w = 100; [x > 50, w < 10]].

J(x) :- b(x,y,z,w), [x + w = 100; !x > 50].

K(x) :- b(x,y,z,w), [x + w = 100, [!x > 50; x = 40]].