#! /usr/bin/env python
#Author: Amir S 

from helper.easierlife import *
import json
import fileinput

for row in fileinput.input():
	obj = json.loads(row)
	image_id = obj["image_id"]
	pixels= obj["pixels"]
	values=[]
	num_rows=obj["num_rows"]
	num_cols=obj["num_cols"]
	layer=obj["layer"]
	f=0
	print json.dumps({
		"image_id":image_id,
		"fid":f,
		"num_rows":num_rows,
		"num_cols":num_cols,
		"values":pixels,
		"layer":layer
		})
