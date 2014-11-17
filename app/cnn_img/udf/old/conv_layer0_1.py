#! /usr/bin/env python
#Author: Amir S 

from helper.easierlife import *
import json
import fileinput
import numpy

FID_IN=1
FID_OUT=4
BlockSize=5
Overlap=1
for row in fileinput.input():
	obj = json.loads(row)
	image_id=obj["image_id"]
	w0 = obj["w0"]
	w0=w0[0]
	l0= obj["l0"]
	l0=l0[0]
	for_jump=BlockSize
	if(Overlap==1):
		for_jump=1
	for f_out in range(0,FID_OUT):
		for i in range(0,w0,for_jump):
			for j in range(0,l0,for_jump):
				if i+BlockSize<=w0 and j+BlockSize<=l0:
					fids=[]
					locations=[]
					for f in range(0,FID_IN):
						fids.append(f)
						locations.append(i*l0+j)
					print json.dumps({
						"image_id":image_id,
						"fid":f_out,
						"location":(i/for_jump)*(l0-BlockSize+1)+j/for_jump,
						"size":BlockSize,
						"center_fids": fids,
						"center_locations": locations
					})