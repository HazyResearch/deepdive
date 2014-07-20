#! /usr/bin/env python
#Author: Amir S 

from helper.easierlife import *
import json
import fileinput
import numpy

BlockSize=4
MAX_IMG_SIZE=28
FID_IN=6
FID_OUT=20
for row in fileinput.input():
	obj = json.loads(row)
	image_id = obj["image_id"]
	xs= obj["xs"]
	ys= obj["ys"]
	# values = obj["values"]
	fids = obj["fids"]
	max_x=max(xs)+1
	max_y=max(ys)+1

	# variables= numpy.zeros(MAX_IMG_SIZE*MAX_IMG_SIZE).reshape((MAX_IMG_SIZE, MAX_IMG_SIZE))
	for i in range(0,len(xs)):
		x=xs[i]
		y=ys[i]
		# variables[x][y] = values[i]

	for i in range(0,max_x):
		for j in range(0,max_y):
			if i+BlockSize<=max_x and j+BlockSize<=max_y:
				block=[]
				# values=[]
				for f in range(0,FID_IN):
					block.append(f*max_x*max_y+i*max_y+j)
					# values.append(variables[r][s])
				for f in range(0,FID_OUT):
					print json.dumps({
						"vector_id": f*(max_y-BlockSize+1)*(max_x-BlockSize+1)+i*(max_y-BlockSize+1)+j,
						"image_id":image_id,
						"x":i,
						"y":j,
						"num_blocks": FID_IN,
						"lenght_blocks": BlockSize,
						"center_ids": block,						
						"value":None,
						"fid":f
						})
				
					