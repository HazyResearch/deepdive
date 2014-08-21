#! /usr/bin/env python
#Author: Amir S 

from helper.easierlife import *
import json
import os
from PIL import Image
import cPickle as pickle

# BASE_FOLDER = "/Users/Amir/Desktop/Spring2014/DeepDive/app/cnn"
BASE_FOLDER = "/lfs/local/0/amir/deepdive/app/cnn"

image_id=0
for bigimage in os.listdir(BASE_FOLDER + '/data'):
	if bigimage.startswith('.'): continue

	IMGPATH = BASE_FOLDER + "/data/" + bigimage
	input_img = Image.open(IMGPATH)
	
	CELL = 28

	print json.dumps({
		"file_path":IMGPATH,
		"starting_id":image_id,
	})
	image_id+=(input_img.size[0]/CELL+1)*(input_img.size[1]/CELL+1)
