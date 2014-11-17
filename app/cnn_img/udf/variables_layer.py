#! /usr/bin/env python
#Author: Amir S 

from helper.easierlife import *
import json
import fileinput
import sys, getopt

FID_OUT=0
BlockSize=0
STRIDE=0
PADDING=0
LAYER=0

try:
    myopts, args = getopt.getopt(sys.argv[1:],'f:l:b:s:')
except getopt.GetoptError as e:
    log (str(e))
    log("Usage: %s -f num_fid_out -l layer -b blocksize -s stride" % sys.argv[0])
    sys.exit(2)

for o, a in myopts:
	if o == '-f':
		FID_OUT=int(a)
	elif o == '-l':
		LAYER=int(a)
	elif o == '-b':
		BlockSize=int(a)
	elif o == '-s':
		STRIDE=int(a)

#! /usr/bin/env python
#Author: Amir S 

from helper.easierlife import *
import json
import fileinput

for row in sys.stdin:
	obj = json.loads(row)
	num_rows=obj["num_rows"]
	num_cols=obj["num_cols"]
	values=[]
	for f in range(0,FID_OUT):
		print json.dumps({
			"image_id":-1,
			"fid":f,
			"num_rows":(num_rows-BlockSize)/STRIDE+1,
			"num_cols":(num_cols-BlockSize)/STRIDE+1,
			"values":values,
			"layer":LAYER
			})
	

