#!/usr/bin/env bash
# Extract dependent libraries for sampler_dw
set -eu

# move next to the script
cd "$(dirname "$0")"

case $(uname) in
  Darwin)
    ditto -xk dw_mac.zip .  # XXX ditto handles .zip better on OS X
    # unzip dw_mac.zip
    mv -f dw_mac dw
    ln -sfnv sampler-dw-mac.sh ../util/sampler-dw
    ;;

  Linux*)
    unzip dw_linux.zip
    mv -f dw_linux dw
    ln -sfnv sampler-dw-linux.sh ../util/sampler-dw
    ;;

  *)
    echo >&2 "$(uname): Unsupported OS"
    false
esac
