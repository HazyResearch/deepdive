#! /usr/bin/env python
import numpy as np
import mnist
import Image
import scipy.misc
import os

os.system('mkdir -p data')

f = open('images.tsv', 'w')
images, labels = mnist.load_mnist('training')

for i in xrange(len(labels)):
  filename = '%s.jpg' % i
  scipy.misc.imsave('data/' + filename, images[i])
  f.write("%s\t%s\t%s\n" % (filename, labels[i][0], 't') )

n_train = len(labels)
images, labels = mnist.load_mnist('testing')

for i in xrange(len(labels)):
  filename = '%s.jpg' % (n_train + i)
  scipy.misc.imsave('data/' + filename, images[i])
  f.write("%s\t%s\t%s\n" % (filename, labels[i][0], 'f') )
f.close()

