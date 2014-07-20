#! /usr/bin/env python
#Author: Amir S 

from helper.easierlife import *
import json
import fileinput
import numpy

BlockSize=2
MAX_IMG_SIZE=28
FID_IN=4
FID_OUT=4
for row in fileinput.input():
	obj = json.loads(row)
	image_id = obj["image_id"]
	xs= obj["xs"]
	ys= obj["ys"]
	# values = obj["values"]
	fid = obj["fid"]
	max_x=max(xs)+1
	max_y=max(ys)+1

	# variables= numpy.zeros(MAX_IMG_SIZE*MAX_IMG_SIZE).reshape((MAX_IMG_SIZE, MAX_IMG_SIZE))
	for i in range(0,len(xs)):
		x=xs[i]
		y=ys[i]
		# variables[x][y] = values[i]

	for i in range(0,max_x,BlockSize):
		for j in range(0,max_y,BlockSize):
			if i+BlockSize<=max_x and j+BlockSize<=max_y:
				block=[]
				# values=[]
				for r in range(i,i+BlockSize):
					for s in range(j,j+BlockSize):
						block.append(str(image_id)+","+str(r*max_y*FID_IN+s*FID_IN+fid))

						# values.append(variables[r][s])
				print json.dumps({
					"vector_id": str(image_id)+","+str(i/BlockSize*(max_y-BlockSize+1)*FID_OUT+j/BlockSize*FID_OUT+fid),
					"image_id":image_id,
					"x":i/BlockSize,
					"y":j/BlockSize,
					"prev_id":block,
					"value":None,
					"fid":fid
					})
				
					

