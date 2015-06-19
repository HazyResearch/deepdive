#! /usr/bin/env python

# Usage: calibration.py [target/calibration_data_file.csv] [output_file.png]

import sys
import numpy as np
import matplotlib.pyplot as plt
import matplotlib.gridspec as gridspec

CALIBRATION_FILE = sys.argv[1]
OUT_IMG_FILE  = sys.argv[2]

labels = []
counts = []
prec = []
counts_train = []
for l in open(CALIBRATION_FILE):
        (a,b,c,d,e) = l.rstrip().split('\t')
        labels.append((float(a) + float(b))/2)
        counts.append(int(c))
        if float(d) + float(e) == 0:
                prec.append(0.0)
        else:
                prec.append(float(d)/(float(d) + float(e)))
        counts_train.append(float(d)+float(e))

fig, ax = plt.subplots(figsize=(12,3))

MARGIN = 1
fig.subplots_adjust(right=0.99, left=0.05, top=0.9, bottom=0.25)

gs = gridspec.GridSpec(1, 3, width_ratios=[1,1,1])

plt.subplot(gs[0])
width = 0.1
labels_nz = []
prec_nz = []
for i in range(0, len(labels)):
        if counts_train[i] != 0:
                labels_nz.append(labels[i])
                prec_nz.append(prec[i])
plt.plot(labels_nz, prec_nz, 'ro-')
plt.plot([0,1],[0,1],'b--')
plt.title("(a) Accuracy (Testing Set)")
plt.ylabel("Accuracy")
plt.xlabel("Probability")
plt.ylim(0,1)
plt.xlim(0,1.1)

plt.text(0, -0.35 , "* (a) and (b) are produced using 50% held-out on evidence variables; (c) also includes all non-evidence variables of the same relation.", fontsize=10, style='italic')

plt.subplot(gs[1])
width = 0.1
plt.bar(labels, counts_train, width, color='b')
plt.title("(b) # Predictions (Testing Set)")
plt.ylabel("# Predictions")
plt.xlabel("Probability")
plt.xlim(0,1.1)

plt.subplot(gs[2])
width = 0.1
plt.bar(labels, counts, width, color='b')
plt.title("(c) # Predictions (Whole Set)")
plt.ylabel("# Predictions")
plt.xlabel("Probability")
plt.xlim(0,1.1)

plt.savefig(OUT_IMG_FILE)


