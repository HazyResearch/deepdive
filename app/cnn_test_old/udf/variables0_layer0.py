#! /usr/bin/env python
#Author: Amir S 

from helper.easierlife import *
import json
import fileinput

BASE_FOLDER = "/Users/Amir/Desktop/Spring2014/DeepDive/app/cnn"


for row in fileinput.input():
	obj = json.loads(row)
	image_id = obj["image_id"]
	pixels= deserialize(obj["pixels"])

	width=3;height=3
	for i in range(0, width):
		for j in range(0, height):
			color = float(pixels[i][j])
			f=0
			print json.dumps({
				"vector_id": "("+str(image_id)+","+str(i)+","+str(j)+","+str(f)+")",
				"image_id":image_id,
				"x":i,
				"y":j,
				"value":color,
				"fid":f
				})