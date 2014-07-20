#! /usr/bin/env python
#Author: Amir S 

from helper.easierlife import *
import json
import fileinput

FID_IN=1
for row in fileinput.input():
	obj = json.loads(row)
	image_id = obj["image_id"]
	pixels= deserialize(obj["pixels"])

	width, height, depth = pixels.shape
	for i in range(0, width):
		for j in range(0, height):
			for k in range(0,depth):
				color = float(pixels[i][j][k])
			color=color/3
			f=0
			print json.dumps({
				"vector_id": i*height+j,
				"image_id":image_id,
				"x":i,
				"y":j,
				"value":color,
				"fid":f
				})