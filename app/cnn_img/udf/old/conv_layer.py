#! /usr/bin/env python
#Author: Amir S 

from helper.easierlife import *
import json
import fileinput
import numpy
import sys, getopt

FID_IN=0
FID_OUT=0
BlockSize=0
Overlap=0

try:
    myopts, args = getopt.getopt(sys.argv[1:],'i:o:s:l:')
except getopt.GetoptError as e:
    log (str(e))
    log("Usage: %s -i num_fid_in -o num_fid_out -s blocksize -l overlapping" % sys.argv[0])
    sys.exit(2)

for o, a in myopts:
    if o == '-i':
        FID_IN=int(a)
    elif o == '-o':
        FID_OUT=int(a)
    elif o == '-s':
        BlockSize=int(a)
    elif o == '-l':
        Overlap=int(a)

for row in sys.stdin:
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
		location=0
		for i in range(0,w0,for_jump):
			for j in range(0,l0,for_jump):
				if i+BlockSize<=w0 and j+BlockSize<=l0:
					fids=[]
					locations=[]
					if(Overlap==1):
						for f in range(0,FID_IN):
							fids.append(f)
							locations.append(i*l0+j)
						print json.dumps({
							"image_id":image_id,
							"fid":f_out,
							"location":location,
							"size":BlockSize,
							"center_fids": fids,
							"center_locations": locations
						})
					else:
						fids.append(f_out)
						locations.append(i*l0+j)
						print json.dumps({
							"image_id":image_id,
							"fid":f_out,
							"location":location,
							"size":BlockSize,
							"center_fids": fids,
							"center_locations": locations
						})
					location=location+1


