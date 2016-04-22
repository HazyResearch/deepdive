do:
	cat test.json | python without_ddlib.py > a
	cat test.json | python with_ddlib.py > b
	diff a b
	rm a
	rm b
py3:
	cat test.json | python3 without_ddlib.py > a
	cat test.json | python3 with_ddlib.py > b
	diff a b
	rm a
	rm b
py2py3:
	cat test.json | python with_ddlib.py > a
	cat test.json | python3 with_ddlib.py > b
	diff a b
	rm a
	rm b