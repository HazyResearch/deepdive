#!/usr/bin/env bash
# Extract dependent libraries for sampler_dw
set -eu

# move next to the script
cd "$(dirname "$0")"

case $(uname) in
  Darwin)
    [ -d dw_mac ] ||
      ditto -xk dw_mac.zip .  # XXX ditto handles .zip better on OS X
      # unzip dw_mac.zip
    ln -sfnv sampler-dw-mac.sh ../util/sampler-dw
    ;;

  Linux*)
    [ -d dw_linux ] ||
      unzip dw_linux.zip
    ln -sfnv sampler-dw-linux.sh ../util/sampler-dw
    ;;

  *)
    echo >&2 "$(uname): Unsupported OS"
    false
esac
