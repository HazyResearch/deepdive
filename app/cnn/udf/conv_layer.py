#! /usr/bin/env python
#Author: Amir S 

from helper.easierlife import *
import json
import fileinput
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
	image_id, w0, l0 = row.split('\t')
	w0=int(w0)
	l0=int(l0)
	image_id=int(image_id)

	for_jump=BlockSize
	w1=w0/for_jump;
	l1=l0/for_jump;
	if(Overlap==1):
		for_jump=1
		w1=w0-BlockSize+1;
		l1=l0-BlockSize+1;


	for f_out in range(0,FID_OUT):
		location=0
		for i in range(0,w0,for_jump):
			for j in range(0,l0,for_jump):
				if i+BlockSize<=w0 and j+BlockSize<=l0:
					fids=[]
					locations_x=[]
					locations_y=[]
					if(Overlap==1):
						for f in range(0,FID_IN):
							fids.append(f)
							locations_x.append(i)
							locations_y.append(j)
					else:
						fids.append(f_out)
						locations_x.append(i)
						locations_y.append(j)

					fids_str=str(fids).replace("[", "{").replace("]", "}")
					locations_x_str=str(locations_x).replace("[", "{").replace("]", "}")
					locations_y_str=str(locations_y).replace("[", "{").replace("]", "}")

					location_x=location/l1
					location_y=location%l1
					location=location+1


					print "\N" + '\t' + str(image_id) + '\t' + str(f_out) + '\t' + str(location_x) + '\t' + str(location_y) + '\t' + str(BlockSize) + '\t' + fids_str + '\t' + locations_x_str + '\t' + locations_y_str


