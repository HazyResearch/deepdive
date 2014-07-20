#!/usr/bin/python2.7

import sys
sys.path.reverse()

import cPickle as pickle


import fileinput
import json
import math
import zlib,base64

IMG_SIZE = 28

def dump_input(OUTFILE):
	fo = open(OUTFILE, 'w')
	for line in fileinput.input():
		fo.write(line)
	fo.close()

def log(str):	
	sys.stderr.write("-------------------------------------------------------")
	sys.stderr.write(str.__repr__() + "\n")

def asciiCompress(data, level=9):
    """ compress data to printable ascii-code """

    code = zlib.compress(data,level)
    csum = zlib.crc32(code)
    code = base64.encodestring(code)
    return code.replace('\n', ' ')

def asciiDecompress(code):
    """ decompress result of asciiCompress """

    code = base64.decodestring(code.replace(' ', '\n'))
    csum = zlib.crc32(code)
    data = zlib.decompress(code)
    return data

def serialize(obj):
	#return zlib.compress(pickle.dumps(obj))
	return asciiCompress(pickle.dumps(obj))

def deserialize(obj):
	#return pickle.loads(str(unicode(obj)))
	return pickle.loads(asciiDecompress(obj.encode("utf-8")))

def get_inputs():
	for line in fileinput.input():
		line = line.rstrip()
		try:
			yield json.loads(line)
		except:
			log("ERROR  :  " + line)

def dump_input(OUTFILE):
	fo = open(OUTFILE, 'w')
	for line in fileinput.input():
		fo.write(line)
	fo.close()
