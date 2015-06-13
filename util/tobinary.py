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
os.system('rm -rf ' + INPUTFOLDER + "/dd_tmp")
os.system('mkdir -p ' + INPUTFOLDER + "/dd_tmp")
os.system('rm -rf ' + INPUTFOLDER + "/nedges_")


# handle factors
for l in open(INPUTFOLDER + "/dd_factormeta"):
  (factor_name, function_id, positives) = l.split('\t')
  positives = positives.strip().replace('true', '1').replace('false', '0').split(' ')
  nvars = '%d' % len(positives)

  print "SPLITTING", factor_name, "..."
  os.system('split -a 10 -l ' + CHUNKSIZE + ' ' + INPUTFOLDER + '/dd_factors_' + factor_name + '_out ' + INPUTFOLDER + '/dd_tmp/dd_factors_' + factor_name + '_out')

  print "BINARIZE ", factor_name, "..."
  os.system('ls ' + INPUTFOLDER + '/dd_tmp | egrep "^dd_factors_' + factor_name + '_out"  | xargs -P 40 -I {} -n 1 sh -c \'' + transform_script + ' factor ' + INPUTFOLDER + '/dd_tmp/{} ' + function_id + ' ' + nvars + ' ' + (' '.join(positives)) + ' \' | awk \'{s+=$1} END {printf \"%.0f\\n\", s}\' >>' + INPUTFOLDER + "/dd_nedges_")

# handle variables
for f in os.listdir(INPUTFOLDER):
  if f.startswith('dd_variables_'):
    print "SPLITTING", f, "..."
    os.system('split -a 10 -l ' + CHUNKSIZE + ' ' + INPUTFOLDER + '/' + f + ' ' + INPUTFOLDER + '/dd_tmp/' + f)

    print "BINARIZE ", f, "..."
    os.system('ls ' + INPUTFOLDER + '/dd_tmp | egrep "^' + f + '"  | xargs -P 40 -I {} -n 1 sh -c \'' + transform_script + ' variable ' + INPUTFOLDER + '/dd_tmp/{} \'')

# handle weights
print "BINARIZE ", 'weights', "..."
os.system(transform_script + ' weight ' + INPUTFOLDER + '/dd_weights')

# move files
os.system('rm -rf ' + INPUTFOLDER + "/dd_factors")
os.system('mkdir -p ' + INPUTFOLDER + "/dd_factors")
os.system('mv ' + INPUTFOLDER + '/dd_tmp/dd_factors*.bin ' + INPUTFOLDER + '/dd_factors')

os.system('rm -rf ' + INPUTFOLDER + "/dd_variables")
os.system('mkdir -p ' + INPUTFOLDER + "/dd_variables")
os.system('mv ' + INPUTFOLDER + '/dd_tmp/dd_variables*.bin ' + INPUTFOLDER + '/dd_variables')

nfactor_files = 0
nvariable_files = 0

# counting 
print "COUNTING", "variables", "..."
os.system('wc -l ' + INPUTFOLDER + "/dd_tmp/dd_variables_* | tail -n 1 | sed -e 's/^[ \t]*//g' | cut -d ' ' -f 1 > " + INPUTFOLDER + '/dd_nvariables_wc')
os.system('export dd_nvar=`cat ' + INPUTFOLDER + '/dd_nvariables_wc`; echo $dd_nvar + %d | bc > ' % nvariable_files + INPUTFOLDER + '/dd_nvariables; unset dd_nvar')

print "COUNTING", "factors", "..."
os.system('wc -l ' + INPUTFOLDER + "/dd_tmp/dd_factors_* | tail -n 1 | sed -e 's/^[ \t]*//g' | cut -d ' ' -f 1 > " + INPUTFOLDER + '/dd_nfactors_wc')
os.system('export dd_nfact=`cat ' + INPUTFOLDER + '/dd_nfactors_wc`; echo $dd_nfact + %d | bc > ' % nfactor_files + INPUTFOLDER + '/dd_nfactors; unset dd_nfact')

print "COUNTING", "weights", "..."
os.system('wc -l ' + INPUTFOLDER + "/dd_weights | tail -n 1 | sed -e 's/^[ \t]*//g' | cut -d ' ' -f 1 > " + INPUTFOLDER + '/dd_nweights')

os.system("awk '{{ sum += $1 }} END {{ printf \"%.0f\\n\", sum }}' {0}/dd_nedges_ > {0}/dd_nedges".format(INPUTFOLDER))

# concatenate files
print "CONCATENATING FILES..."
os.system("cat {0}/dd_nweights {0}/dd_nvariables {0}/dd_nfactors {0}/dd_nedges | tr '\n' ',' > {0}/graph.meta".format(INPUTFOLDER))
os.system("echo {0}/graph.weights,{0}/graph.variables,{0}/graph.factors,{0}/graph.edges >> {1}/graph.meta".format(OUTPUTFOLDER, INPUTFOLDER))

if INPUTFOLDER != OUTPUTFOLDER:
    os.system("mv {0}/graph.meta {1}/graph.meta".format(INPUTFOLDER, OUTPUTFOLDER))
os.system("mv {0}/dd_weights.bin {1}/graph.weights".format(INPUTFOLDER, OUTPUTFOLDER))
os.system("cat {0}/dd_variables/* > {1}/graph.variables".format(INPUTFOLDER, OUTPUTFOLDER))
os.system("cat {0}/dd_factors/dd_factors*factors.bin > {1}/graph.factors".format(INPUTFOLDER, OUTPUTFOLDER))
os.system("cat {0}/dd_factors/dd_factors*edges.bin > {1}/graph.edges".format(INPUTFOLDER, OUTPUTFOLDER))


# clean up folder
print "Cleaning up files"
os.system('rm -rf {0}/dd_*'.format(INPUTFOLDER))
