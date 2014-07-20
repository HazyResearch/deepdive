#! /usr/bin/env python
#Author: Amir S 

import json
import cv2
import os
from PIL import Image

BASE_FOLDER = "/Users/Amir/Desktop/Spring2014/DeepDive/app/cnn"

for bigimage in os.listdir(BASE_FOLDER + '/data'):
	if bigimage.startswith('.'): continue

	label = int(bigimage.split('.')[0].split('train')[1])

	if not (label == 0
		# or label == 1
		 ): continue

	IMGPATH = BASE_FOLDER + "/data/" + bigimage
	mat = cv2.imread(IMGPATH)
	width, height, depth = mat.shape
	'''
	print "width: " , width
	print "height: " , height
	print "depth: " , depth
	'''
	
	CELL = 28
	blockid = 0

	input_img = Image.open(IMGPATH)
	image_id=0
	for i in range(0, width, CELL):
		for j in range(0, height, CELL):
			blockid = blockid + 1
			if image_id >1000:
				break;
			
			roi = mat[i:i+CELL,j:j+CELL]


			'''
			box = (i, j, i + CELL, j + CELL)
			output_img = input_img.crop(box)
			'''
			
			for r in range(0,CELL):
				for c in range(0,CELL):
					print '\t'.join([str(_) for _ in 
						['\N',
						image_id,
						r,
						c,
						str(roi[r,c]),
					 	label]
						])
						
					# print json.dumps({
					# 	"image_id":image_id,
					# 	"x":r,
					# 	"y":c,
					# 	"value":str(roi[r,c]),
					#  	"label":label
					# })
			image_id=image_id+1
