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

# add the /util directory to the PATH inside this conda env
# http://conda.pydata.org/docs/using/envs.html#saved-environment-variables
cat << EOF > ${PKG_NAME}-env-activate.sh
#!/usr/bin/env bash

export PRE_${PKG_NAME}_PATH=\$PATH
export PATH=\$CONDA_PREFIX/util:\$PATH
EOF

cat << EOF > ${PKG_NAME}-env-deactivate.sh
#!/usr/bin/env bash

export PATH=\$PRE_${PKG_NAME}_PATH
unset PRE_${PKG_NAME}_PATH
EOF

mkdir -p $PREFIX/etc/conda/activate.d
mkdir -p $PREFIX/etc/conda/deactivate.d

mv ${PKG_NAME}-env-activate.sh $PREFIX/etc/conda/activate.d
mv ${PKG_NAME}-env-deactivate.sh $PREFIX/etc/conda/deactivate.d
