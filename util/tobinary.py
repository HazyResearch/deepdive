#! /usr/bin/env python

import sys
import re
import os

CHUNKSIZE = '10000000'
INPUTFOLDER = sys.argv[1]
transform_script = sys.argv[2]
OUTPUTFOLDER = sys.argv[3]

os.system('rm -rf ' + INPUTFOLDER + "/tmp")
os.system('mkdir -p ' + INPUTFOLDER + "/tmp")
# print('rm -rf ' + INPUTFOLDER + "/nedges_")


for f in os.listdir(INPUTFOLDER):
  if f.startswith('edges'):
    print "SPLITTING", "edges", "..."
    os.system('split -a 10 -l ' + CHUNKSIZE + ' ' + INPUTFOLDER + '/' + f + ' ' + INPUTFOLDER + '/tmp/' + f)

    print "BINARIZE ", "edges", "..."
    os.system('ls ' + INPUTFOLDER + '/tmp | egrep edges' + '  | xargs -P 1 -I {} -n 1 sh -c \'' + transform_script + ' edges ' + INPUTFOLDER + '/tmp/{} \'' )

for f in os.listdir(INPUTFOLDER):
  if f.startswith('variables_'):
    print "SPLITTING", f, "..."
    os.system('split -a 10 -l ' + CHUNKSIZE + ' ' + INPUTFOLDER + '/' + f + ' ' + INPUTFOLDER + '/tmp/' + f)

    print "BINARIZE ", f, "..."
    os.system('ls ' + INPUTFOLDER + '/tmp | egrep "^' + f + '"  | xargs -P 40 -I {} -n 1 sh -c \'' + transform_script + ' variable ' + INPUTFOLDER + '/tmp/{} \'')

print "BINARIZE ", 'weights', "..."
os.system(transform_script + ' weight ' + INPUTFOLDER + '/weights')

# print('rm -rf ' + INPUTFOLDER + "/factors")
# print('mkdir -p ' + INPUTFOLDER + "/factors")
# print('mv ' + INPUTFOLDER + '/tmp/factors*.bin ' + INPUTFOLDER + '/factors')

os.system('rm -rf ' + INPUTFOLDER + "/variables")
os.system('rm -rf ' + INPUTFOLDER + "/fedges")

os.system('mkdir -p ' + INPUTFOLDER + "/variables")
os.system('mkdir -p ' + INPUTFOLDER + "/fedges")

os.system('mv ' + INPUTFOLDER + '/tmp/variables*.bin ' + INPUTFOLDER + '/variables')
os.system('mv ' + INPUTFOLDER + '/tmp/edges*.bin ' + INPUTFOLDER + '/fedges')


nfactor_files = 0
nvariable_files = 0
nedge_files = 0


print "COUNTING", "variables", "..."
os.system('wc -l ' + INPUTFOLDER + "/tmp/variables_* | awk '{print $1+%d}' | tail -n 1 > " % nvariable_files + INPUTFOLDER + '/nvariables')

# print "COUNTING", "factors", "..."
# os.system('wc -l ' + INPUTFOLDER + "/tmp/factors_* | awk '{print $1+%d}' | tail -n 1 > " % nfactor_files + INPUTFOLDER + '/nfactors')

print "COUNTING", "weights", "..."
os.system('wc -l ' + INPUTFOLDER + "/weights | awk '{print $1}' | tail -n 1 > " + INPUTFOLDER + '/nweights')


print "COUNTING", "hyper edges", "..."
os.system('wc -l ' + INPUTFOLDER + "/tmp/edges* | awk '{print $1+%d}' | tail -n 1 > " % nedge_files + INPUTFOLDER + '/nedges')


# os.system("awk '{{ sum += $1 }} END {{ print sum }}' {0}/nedges_ > {0}/nedges".format(INPUTFOLDER))


print "CATENATING FILES..."
os.system("cat {0}/nweights {0}/nvariables {0}/nedges| tr '\n' ',' > {0}/graph.meta".format(INPUTFOLDER))
os.system("echo {0}/graph.weights,{0}/graph.variables,{0}/graph.edges >> {0}/graph.meta".format(INPUTFOLDER))



os.system("mv {0}/graph.meta {1}/graph.meta".format(INPUTFOLDER, OUTPUTFOLDER))
os.system("mv {0}/weights.bin {1}/graph.weights".format(INPUTFOLDER, OUTPUTFOLDER))
os.system("cat {0}/variables/* > {1}/graph.variables".format(INPUTFOLDER, OUTPUTFOLDER))
os.system("cat {0}/fedges/* > {1}/graph.edges".format(INPUTFOLDER, OUTPUTFOLDER))
# os.system("cat {0}/factors/factors*factors.bin > {1}/graph.factors".format(INPUTFOLDER, OUTPUTFOLDER))
# os.system("cat {0}/factors/factors*edges.bin > {1}/graph.edges".format(INPUTFOLDER, OUTPUTFOLDER))

# os.system('rm -rf {0}/nedges_'.format(INPUTFOLDER))
os.system('rm -rf {0}/tmp'.format(INPUTFOLDER))
os.system('rm -rf {0}/variables'.format(INPUTFOLDER))
os.system('rm -rf {0}/fedges'.format(INPUTFOLDER))

# os.system('rm -rf {0}/factors'.format(INPUTFOLDER))
# os.system('rm -rf {0}/factors*'.format(INPUTFOLDER))
# os.system('rm -rf {0}/variables*'.format(INPUTFOLDER))
# os.system('rm -rf {0}/weights'.format(INPUTFOLDER))
