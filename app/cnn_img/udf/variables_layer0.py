#! /usr/bin/env python
#Author: Amir S 

from helper.easierlife import *
import json
import fileinput

for row in fileinput.input():
	obj = json.loads(row)
	f=obj["fid"]
	pixels= obj["pixels"]
	values=[]
	num_rows=obj["num_rows"]
	num_cols=obj["num_cols"]
	layer=obj["layer"]
	print json.dumps({
		"image_id":-1,
		"fid":f,
		"num_rows":num_rows,
		"num_cols":num_cols,
		"values":pixels,
		"layer":layer
		})
