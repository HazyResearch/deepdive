#! /usr/bin/env python
#Author: Amir S 

from helper.easierlife import *
import json
import os
from PIL import Image
import cPickle as pickle

image_id=0
for row in fileinput.input():
	obj = json.loads(row)
	IMGPATH = obj["file_path"]
	starting_id= obj["starting_id"]

	label = int(IMGPATH.split('.')[0].split('train')[1])

	# if not (label == 0
	# 	 or label == 1
	# 	 ): continue

	input_img = Image.open(IMGPATH)

	CELL = 28
	image_limit=0
	for i in range(0, input_img.size[0], CELL):
		for j in range(0, input_img.size[1], CELL):
			# if(image_limit>0):
			# 	break

			box = (i, j, i + CELL, j + CELL)
			output_img = input_img.crop(box)
			roi=list(output_img.getdata())
			for roi_i in range(0,CELL*CELL):
				roi[roi_i]/=255.0

			print json.dumps({
				"image_id":image_id,
				"pixels":roi,
				"num_rows":CELL,
				"num_cols":CELL,
			 	"label":label
			})
			image_limit=image_limit+1
			image_id=image_id+1


