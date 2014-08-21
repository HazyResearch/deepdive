#! /usr/bin/env python
#Author: Amir S 

from helper.easierlife import *
import json
import os
from PIL import Image
import cPickle as pickle

image_id=0
for row in fileinput.input():
	starting_id= 0

	CELL = 3
	for k in range(1,10,6):
		roi=list([k,k+1,k+2,k,k+1,k+2,k,k+1,k+2])
		# roi=list([k,k,k,k,k,k,k,k,k])

		if(k<2):
			label=0
		else:
			label =1
		print json.dumps({
			"image_id":image_id,
			"pixels":roi,
			"num_rows":CELL,
			"num_cols":CELL,
		 	"label":label
		})
		image_id=image_id+1


