#! /usr/bin/env python

# Script to convert grounding files in TSV format to binary format for dimmwitted sampler
# Usage: python tobinary.py [input folder] transform_script [output folder]
# It split the specific files in the input folder and for each of them calls the C++ binary to convert the format

import sys
import re
import os

# set up parameters
CHUNKSIZE = '10000000'
INPUTFOLDER = sys.argv[1]
transform_script = sys.argv[2]
OUTPUTFOLDER = sys.argv[3]

# clean up folder
os.system('rm -rf ' + INPUTFOLDER + "/tmp")
os.system('mkdir -p ' + INPUTFOLDER + "/tmp")
os.system('rm -rf ' + INPUTFOLDER + "/nedges_")


# handle factors
for l in open(INPUTFOLDER + "/factormeta"):
  (factor_name, function_id, positives) = l.split('\t')
  positives = positives.strip().replace('true', '1').replace('false', '0').split(' ')
  nvars = '%d' % len(positives)

  print "SPLITTING", factor_name, "..."
  os.system('split -a 10 -l ' + CHUNKSIZE + ' ' + INPUTFOLDER + '/factors_' + factor_name + '_out ' + INPUTFOLDER + '/tmp/factors_' + factor_name + '_out')

  print "BINARIZE ", factor_name, "..."
  os.system('ls ' + INPUTFOLDER + '/tmp | egrep "^factors_' + factor_name + '_out"  | xargs -P 40 -I {} -n 1 sh -c \'' + transform_script + ' factor ' + INPUTFOLDER + '/tmp/{} ' + function_id + ' ' + nvars + ' ' + (' '.join(positives)) + ' \' | awk \'{s+=$1} END {print s}\' >>' + INPUTFOLDER + "/nedges_")

# handle variables
for f in os.listdir(INPUTFOLDER):
  if f.startswith('variables_'):
    print "SPLITTING", f, "..."
    os.system('split -a 10 -l ' + CHUNKSIZE + ' ' + INPUTFOLDER + '/' + f + ' ' + INPUTFOLDER + '/tmp/' + f)

    print "BINARIZE ", f, "..."
    os.system('ls ' + INPUTFOLDER + '/tmp | egrep "^' + f + '"  | xargs -P 40 -I {} -n 1 sh -c \'' + transform_script + ' variable ' + INPUTFOLDER + '/tmp/{} \'')

# handle weights
print "BINARIZE ", 'weights', "..."
os.system(transform_script + ' weight ' + INPUTFOLDER + '/weights')

# move files
os.system('rm -rf ' + INPUTFOLDER + "/factors")
os.system('mkdir -p ' + INPUTFOLDER + "/factors")
os.system('mv ' + INPUTFOLDER + '/tmp/factors*.bin ' + INPUTFOLDER + '/factors')

os.system('rm -rf ' + INPUTFOLDER + "/variables")
os.system('mkdir -p ' + INPUTFOLDER + "/variables")
os.system('mv ' + INPUTFOLDER + '/tmp/variables*.bin ' + INPUTFOLDER + '/variables')

nfactor_files = 0
nvariable_files = 0

# counting 
print "COUNTING", "variables", "..."
os.system('wc -l ' + INPUTFOLDER + "/tmp/variables_* | awk '{print $1+%d}' | tail -n 1 > " % nvariable_files + INPUTFOLDER + '/nvariables')

print "COUNTING", "factors", "..."
os.system('wc -l ' + INPUTFOLDER + "/tmp/factors_* | awk '{print $1+%d}' | tail -n 1 > " % nfactor_files + INPUTFOLDER + '/nfactors')

print "COUNTING", "weights", "..."
os.system('wc -l ' + INPUTFOLDER + "/weights | awk '{print $1}' | tail -n 1 > " + INPUTFOLDER + '/nweights')

os.system("awk '{{ sum += $1 }} END {{ print sum }}' {0}/nedges_ > {0}/nedges".format(INPUTFOLDER))

# concatenate files
print "CONCATENATING FILES..."
os.system("cat {0}/nweights {0}/nvariables {0}/nfactors {0}/nedges | tr '\n' ',' > {0}/graph.meta".format(INPUTFOLDER))
os.system("echo {0}/graph.weights,{0}/graph.variables,{0}/graph.factors,{0}/graph.edges >> {0}/graph.meta".format(INPUTFOLDER))

os.system("mv {0}/graph.meta {1}/graph.meta".format(INPUTFOLDER, OUTPUTFOLDER))
os.system("mv {0}/weights.bin {1}/graph.weights".format(INPUTFOLDER, OUTPUTFOLDER))
os.system("cat {0}/variables/* > {1}/graph.variables".format(INPUTFOLDER, OUTPUTFOLDER))
os.system("cat {0}/factors/factors*factors.bin > {1}/graph.factors".format(INPUTFOLDER, OUTPUTFOLDER))
os.system("cat {0}/factors/factors*edges.bin > {1}/graph.edges".format(INPUTFOLDER, OUTPUTFOLDER))


# clean up folder
#os.system('rm -rf {0}/nedges_'.format(INPUTFOLDER))
#os.system('rm -rf {0}/tmp'.format(INPUTFOLDER))
#os.system('rm -rf {0}/variables'.format(INPUTFOLDER))
#os.system('rm -rf {0}/factors'.format(INPUTFOLDER))
#os.system('rm -rf {0}/factors*'.format(INPUTFOLDER))
#os.system('rm -rf {0}/variables*'.format(INPUTFOLDER))
#os.system('rm -rf {0}/weights'.format(INPUTFOLDER))
