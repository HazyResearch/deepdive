#!/bin/bash

LSB=$(lsb_release -r 2> /dev/null) 

if [[ ! $LSB =~ "14.04" && ! $LSB =~ "15.04" ]]
then
  echo "This installer only works with Ubuntu 14.04 and 15.04."
  exit 1
fi

CONF_DIR=~/.deepdive
if [ -d "$CONF_DIR" ]; then
  echo "You have an existing directory $CONF_DIR. Please remove it and then try again."
  exit 1
fi

mkdir -p $CONF_DIR

DEFAULT_REPLY=Y
read -p "Would you like to install XL? [Yn]: " REPLY
REPLY=${REPLY:-$DEFAULT_REPLY}
if [[ $REPLY =~ ^[Yy]$ ]]
then
    curl -s https://raw.githubusercontent.com/hazyresearch/deepdive/raphael-script/util/script/pgxl_install.sh > $CONF_DIR/pgxl_install.sh
    chmod +x $CONF_DIR/pgxl_install.sh

    DEFAULT_NUM_DATA_NODES=2
    read -p "Enter number of data nodes [$DEFAULT_NUM_DATA_NODES]: " NUM_DATA_NODES
    NUM_DATA_NODES=${NUM_DATA_NODES:-$DEFAULT_NUM_DATA_NODES}
    DEFAULT_DATA_DIR=/mnt/pgxl/nodes
    read -p "Enter data dir to hold segment data [$DEFAULT_DATA_DIR]: " DATA_DIR
    DATA_DIR=${DATA_DIR:-$DEFAULT_DATA_DIR}
    DEFAULT_USER_DB_PORT=5432
    read -p "Enter port number [$DEFAULT_USER_DB_PORT]: " USER_DB_PORT
    USER_DB_PORT=${USER_DB_PORT:-$DEFAULT_USER_DB_PORT}
    . $CONF_DIR/pgxl_install.sh 

    echo -e "You can run 'psql postgres' to log in."
fi

read -p "Would you like to install DeepDive? [Yn]: " REPLY 
REPLY=${REPLY:-$DEFAULT_REPLY}
if [[ $REPLY =~ ^[Yy]$ ]]
then
    curl -s https://raw.githubusercontent.com/hazyresearch/deepdive/raphael-script/util/script/pgxl_install.sh > $CONF_DIR/dd_install.sh
    chmod +x $CONF_DIR/dd_install.sh
    . $CONF_DIR/dd_install.sh
fi

echo "All done here."
