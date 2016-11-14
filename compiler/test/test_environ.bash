#!/usr/bin/env bash
# Unit tests for DeepDive compiler
# Usage: load test_environ # from .bats files typically on the same directory

load ../../test/test_environ

setup() {
    # speed up deepdive-compile by disabling some unnecessary compilation steps
    export \
        DEEPDIVE_COMPILE_SKIP_DATAFLOW=true \
        DEEPDIVE_COMPILE_SKIP_CODEGEN=true \
        DEEPDIVE_COMPILE_INPUT_JSON=run/LATEST.COMPILE/deepdive.conf.json \
        #
    if [[ deepdive.conf -nt $DEEPDIVE_COMPILE_INPUT_JSON ||
          app.ddlog     -nt $DEEPDIVE_COMPILE_INPUT_JSON ||
          schema.json   -nt $DEEPDIVE_COMPILE_INPUT_JSON ]]; then
        # invalidate deepdive.conf.json if any source were updated
        rm -f $DEEPDIVE_COMPILE_INPUT_JSON
    fi
}
teardown() {
    if [[ ! -e $DEEPDIVE_COMPILE_INPUT_JSON &&
            -e run/LATEST.COMPILE/deepdive.conf.json &&
          ! $DEEPDIVE_COMPILE_INPUT_JSON -ef run/LATEST.COMPILE/deepdive.conf.json ]]; then
        # cache the compiled deepdive.conf.json to speed up future runs
        cp -pf run/LATEST.COMPILE/deepdive.conf.json $DEEPDIVE_COMPILE_INPUT_JSON
    fi
}
