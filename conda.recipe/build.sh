if [ "$(uname)" == "Darwin" ]; then
  # C++11 finagling for Mac OSX
  export CC=clang
  export CXX=clang++
  export MACOSX_VERSION_MIN="10.9"
  CXXFLAGS="${CXXFLAGS} -mmacosx-version-min=${MACOSX_VERSION_MIN}"
  CXXFLAGS="${CXXFLAGS} -Wno-error=unused-command-line-argument" 
  export LDFLAGS="${LDFLAGS} -mmacosx-version-min=${MACOSX_VERSION_MIN}"
  export LINKFLAGS="${LDFLAGS}"
  export MACOSX_DEPLOYMENT_TARGET=10.9

  # make sure clang-format is installed
  # e.g. brew install clang-format

  # make sure autoreconf can be found
  # e.g. brew install autoconf

  # for graphviz, also brew install autogen libtool
fi

make install PREFIX=$PREFIX
