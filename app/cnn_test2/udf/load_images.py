#! /usr/bin/env python
#Author: Amir S 

from helper.easierlife import *
import json
import cv2
import os
from PIL import Image
import cPickle as pickle

BASE_FOLDER = "/Users/Amir/Desktop/Spring2014/DeepDive/app/cnn"

width=4;height=4;depth = 1
CELL = 4

for image_id in range(0,10):
	roi = [[image_id,image_id,image_id,image_id],[image_id,image_id,image_id,image_id],
			[image_id,image_id,image_id,image_id],[image_id,image_id,image_id,image_id]]
	label=0
	if(image_id > 7):
		label=1
	print json.dumps({
		"image_id":image_id,
		"pixels":serialize(roi),
	 	"label":label
	})
