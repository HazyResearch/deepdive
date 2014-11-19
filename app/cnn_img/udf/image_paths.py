#! /usr/bin/env python
#Author: Amir S 

from helper.easierlife import *
import json
import os
from PIL import Image
import cPickle as pickle

# BASE_FOLDER = "/Users/Amir/Desktop/Spring2014/DeepDive/app/cnn"
BASE_FOLDER = "/lfs/local/0/amir/deepdive/app/cnn_img"

image_id=0 #image_id!=-1 --> reserved
folder_name="data2"
for image_folder in os.listdir(BASE_FOLDER + '/' +folder_name+ '/'):
	if image_folder.startswith('n') == False: continue
	for bigimage in os.listdir(BASE_FOLDER + '/' +folder_name+ '/' + image_folder +"/"):
		IMGPATH = BASE_FOLDER + '/' +folder_name+ '/' + image_folder +"/" + bigimage
		input_img = Image.open(IMGPATH)		
		print json.dumps({
			"file_path":IMGPATH,
			"starting_id":image_id,
		})
		image_id=image_id+1