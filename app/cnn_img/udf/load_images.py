#! /usr/bin/env python
#Author: Amir S 

from helper.easierlife import *
import json
import os
from PIL import Image
import cPickle as pickle
import time

image_id=0
for row in fileinput.input():
	IMGPATH, starting_id = row.split('\t')
	starting_id=int(starting_id)

	label = int(IMGPATH.split('/data')[1].split('/')[1].split('.')[0].split('n')[1])
	layer = 0 

	# print label
	# if not (label == 0
	# 	 or label == 1
		 # ): continue
	size = 235, 235
	# size = 70,70

	input_img = Image.open(IMGPATH)
	input_img = input_img.resize(size, Image.ANTIALIAS)
	roi=list(input_img.getdata())
	# input_img.save("1.JPEG", "JPEG")
	pic = []
	pic.append([])
	pic.append([])
	pic.append([])
	if(isinstance(roi[0], int)):
		continue

	for roi_i in range(0,len(roi)):
		temp_sum=0
		for i in range(0,3):
			pic[i].append(roi[roi_i][i])

	for i in range(0,3):
		pic_str=str(pic[i]).replace("[", "{").replace("]", "}")
		print "\N" + '\t' + str(starting_id) + '\t' + str(i)  +'\t' + str(size[0]) + '\t' + str(size[1]) + '\t' + pic_str + '\t' + str(layer) + '\t' + str(label) 

