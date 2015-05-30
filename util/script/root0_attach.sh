#!/bin/bash

# Initializes a new data disk (useful for Azure and EC-2).
# Before you run this script you must attach a new device to your running
# instance. You can do this through Azure Portal or AWS Management Console.

# After that, run
#
#       sudo grep SCSI /var/log/syslog
#
# to see the device name of the newly attached device.
# Set the device name below (HDD) and the target mount point (DATA_DIR).

###################################################
# PLEASE EDIT THIS SECTION 
HDD=/dev/sdc
DATA_DIR=/data
###################################################

# for more info see section "How to: Initialize a new data disk in Linux", steps 1-7 on
# http://azure.microsoft.com/en-us/documentation/articles/virtual-machines-linux-how-to-attach-disk/
# 
echo "
n
p
1


w
"|sudo fdisk $HDD

# make XFS file system on this partition
sudo apt-get install -y xfsprogs &&
sudo mkfs -t xfs "$HDD"1 &&
sudo mkdir $DATA_DIR &&
sudo mount "$HDD"1 $DATA_DIR

# enable mount on startup
echo "${HDD}1    $DATA_DIR    xfs    defaults    0    1" | sudo tee -a /etc/fstab

