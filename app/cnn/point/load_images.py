#! /usr/bin/env python
#Author: Amir S 

from helper.easierlife import *
import json
import cv2
import os
from PIL import Image
import cPickle as pickle

BASE_FOLDER = "/Users/Amir/Desktop/Spring2014/DeepDive/app/cnn"

image_id=0
for bigimage in os.listdir(BASE_FOLDER + '/data'):
	if bigimage.startswith('.'): continue

	label = int(bigimage.split('.')[0].split('train')[1])

	# if not (label == 0
	# 	 # or label == 1
	# 	 ): continue

	IMGPATH = BASE_FOLDER + "/data/" + bigimage
	mat = cv2.imread(IMGPATH)
	width, height, depth = mat.shape
	'''
	print "width: " , width
	print "height: " , height
	print "depth: " , depth
	'''
	
	CELL = 28
	image_limit=0
	input_img = Image.open(IMGPATH)
	for i in range(0, width, CELL):
		for j in range(0, height, CELL):
			if(image_limit>60):
				break
			roi = mat[i:i+CELL,j:j+CELL]

			'''
			box = (i, j, i + CELL, j + CELL)
			output_img = input_img.crop(box)
			'''

			print json.dumps({
				"image_id":image_id,
				"pixels":serialize(roi),
			 	"label":label
			})
			image_limit=image_limit+1
			image_id=image_id+1
