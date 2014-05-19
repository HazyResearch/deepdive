do:
	cat test.json | python ./without_ddlib.py > a
	cat test.json | python ./with_ddlib.py > b
	diff a b	
	rm a
	rm b
