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
	x= obj["x"]
	y= obj["y"]
	label = obj["label"]
	f = obj["fid"]
	for c in range(0,N_CLASS):
		print json.dumps({
			"vector_id": [image_id,x,y,f],
			"image_id":image_id,
			"x":x,
			"y":y,
			"class":c,
			"value":label,
			"fid":f
			})