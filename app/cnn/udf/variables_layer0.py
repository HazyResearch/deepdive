#! /usr/bin/env python
#Author: Amir S 

from helper.easierlife import *
import json
import fileinput

for row in fileinput.input():
	obj = json.loads(row)
	image_id = obj["image_id"]
	pixels= deserialize(obj["pixels"])
	values=[]
	width, height, depth = pixels.shape
	for i in range(0, width):
		for j in range(0, height):
			for k in range(0,depth):
				color = float(pixels[i][j][k])
			color=color/3
			values.append(color)			
			f=0
	print json.dumps({
		"image_id":image_id,
		"fid":f,
		"width":width,
		"length":height,
		"values":values
		})
