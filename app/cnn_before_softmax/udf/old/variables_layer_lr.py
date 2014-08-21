#! /usr/bin/env python
#Author: Amir S 

from helper.easierlife import *
import json
import fileinput
import numpy

N_CLASS=1
for row in fileinput.input():
	obj = json.loads(row)
	image_id = obj["image_id"]
	fid= obj["fid"]
	label = obj["label"]
	values=[]
	for c in range(0,N_CLASS):
		values.append(label)
	print json.dumps({
		"image_id":image_id,
		"fid":fid,
		"num_rows":1,
		"num_cols":1,
		"value":values,
		})