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
    ;;

  Linux*)
    [ -d dw_linux ] ||
      unzip dw_linux.zip
    ;;

  *)
    echo >&2 "$(uname): Unsupported OS"
    false
esac
