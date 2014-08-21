

#! /usr/bin/env python
#Author: Amir S 

from helper.easierlife import *
import json
import fileinput
import numpy

BASE_FOLDER = "/Users/Amir/Desktop/Spring2014/DeepDive/app/cnn"

for row in fileinput.input():
	obj = json.loads(row)
	image_id = obj["image_id"]
	bx= obj["bx"]
	by= obj["by"]
	label = obj["label"]
	f = obj["fid"]
	vector_id="("+str(image_id)+","+str(bx)+","+str(by)+","+str(f)+")"

	print json.dumps({
		"vector_id": vector_id,
		"image_id":image_id,
		"x":bx,
		"y":by,
		"value":label,
		"fid":f
		})