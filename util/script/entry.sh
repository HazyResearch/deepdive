#!/bin/bash
URL=https://raw.githubusercontent.com/hazyresearch/deepdive/raphael-script/util/script/install.sh
function download {
  scratch=$(mktemp -d -t tmp.XXXXXXXXXX) || exit
  script_file=$scratch/install.sh
  curl -s $URL > $script_file || exit
  chmod 755 $script_file
  echo "Running DeepDive installer from: $script_file"
  $script_file
}
download < /dev/tty

