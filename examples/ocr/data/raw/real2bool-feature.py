import os,sys   # needed by most
import random   # random 
import yaml     # yaml parsing
import pprint   # pretty print


if __name__ == "__main__": 
	inpath = 'f52-c3-m1011/'
	outpath = ''
  # ...
  if len(sys.argv) == 3:
    inpath = sys.argv[1]
    num = int(sys.argv[2])
  else:
    print 'Usage:',sys.argv[0],'<path> <num>'
    sys.exit(1)
