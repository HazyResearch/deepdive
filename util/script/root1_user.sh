#! /bin/bash

##################################################
DD_USER=dduser       # deepdive user account
MNT_DIR=/mnt         # mountpoint of local disks
DATA_DIR=/data       # mountpoint of persistent volume
##################################################

sudo adduser $DD_USER

sudo chown ccuser:ccuser $MNT_DIR

sudo chown ccuser:ccuser $DATA_DIR

