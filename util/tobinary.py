#! /usr/bin/env python

import sys
import re
import os

CHUNKSIZE = '10000000'
INPUTFOLDER = sys.argv[1]
transform_script = sys.argv[2]
transform_script2 = sys.argv[3]

os.system('rm -rf ' + INPUTFOLDER + "/tmp")
os.system('mkdir -p ' + INPUTFOLDER + "/tmp")
os.system('rm -rf ' + INPUTFOLDER + "/nedges")
for l in open(INPUTFOLDER + "/factormeta"):
	(factor_name, function_id, positives) = l.split('\t')
	positives = positives.strip().replace('true', '1').replace('false', '0').split(' ')
	nvars = '%d' % len(positives)
	#print factor_name, function_name, positives, nvars
	#os.system('rm -rf ' + INPUTFOLDER + "/tmp/*")

	print "SPLITTING", factor_name, "..."
	os.system('split -a 10 -l ' + CHUNKSIZE + ' ' + INPUTFOLDER + '/factors_' + factor_name + '_out ' + INPUTFOLDER + '/tmp/factors_' + factor_name + '_out')

	print "BINARIZE ", factor_name, "..."
	os.system('ls ' + INPUTFOLDER + '/tmp | egrep "^factors_' + factor_name + '_out"  | xargs -P 40 -I {} -n 1 sh -c \'' + transform_script + ' factor ' + INPUTFOLDER + '/tmp/{} ' + function_id + ' ' + nvars + ' ' + (' '.join(positives)) + ' \' | awk \'{s+=$1} END {print s}\' >>' + INPUTFOLDER + "/nedges")
	#break

for f in os.listdir(INPUTFOLDER):
	if f.startswith('variables_'):
		print "SPLITTING", f, "..."
		os.system('split -a 10 -l ' + CHUNKSIZE + ' ' + INPUTFOLDER + '/' + f + ' ' + INPUTFOLDER + '/tmp/' + f)

		print "BINARIZE ", f, "..."
		os.system('ls ' + INPUTFOLDER + '/tmp | egrep "^' + f + '"  | xargs -P 40 -I {} -n 1 sh -c \'' + transform_script + ' variable ' + INPUTFOLDER + '/tmp/{} \'')

print "BINARIZE ", 'weights', "..."
os.system(transform_script + ' weight ' + INPUTFOLDER + '/weights')

os.system('rm -rf ' + INPUTFOLDER + "/factors")
os.system('mkdir -p ' + INPUTFOLDER + "/factors")
os.system('mv ' + INPUTFOLDER + '/tmp/factors*.bin ' + INPUTFOLDER + '/factors')

os.system('rm -rf ' + INPUTFOLDER + "/variables")
os.system('mkdir -p ' + INPUTFOLDER + "/variables")
os.system('mv ' + INPUTFOLDER + '/tmp/variables*.bin ' + INPUTFOLDER + '/variables')


nfactor_files = 0
nvariable_files = 0
#for f in os.listdir(INPUTFOLDER + '/tmp'):
#	if f.startswith('variables_'):
#		nvariable_files = nvariable_files + 1
#	if f.startswith('factors_'):
#		nfactor_files = nfactor_files + 1

print "COUNTING", "variables", "..."
os.system('wc -l ' + INPUTFOLDER + "/tmp/variables_* | awk '{print $1+%d}' | tail -n 1 > " % nvariable_files + INPUTFOLDER + '/nvariables')

print "COUNTING", "factors", "..."
os.system('wc -l ' + INPUTFOLDER + "/tmp/factors_* | awk '{print $1+%d}' | tail -n 1 > " % nfactor_files + INPUTFOLDER + '/nfactors')

print "COUNTING", "weights", "..."
os.system('wc -l ' + INPUTFOLDER + "/weights | awk '{print $1}' | tail -n 1 > " + INPUTFOLDER + '/nweights')

# transform these binary files to our old format...
print "Converting format..."
print '{} {}'.format(transform_script2, INPUTFOLDER)
os.system('{} {}'.format(transform_script2, INPUTFOLDER))
