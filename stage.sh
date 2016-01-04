#!/usr/bin/env bash
# stage.sh -- a script that stages code and data for distribution
set -eu

###############################################################################
STAGE_DIR=${1:-dist/stage}
stage() {
    local src=$1 dst=$2
    [[ -e "$src" ]] || {
        echo >&2 "$src: No such file to stage"
        false
    }
    dst="$STAGE_DIR/$dst"
    dstdir=$(dirname "$dst.")
    [[ -d "$dstdir" ]] || mkdir -p "$dstdir"
    if [[ -d "$src" ]]; then
        # use rsync(1) for staging directories
        set -x; rsync -aH --delete "$src" "$dst"
    else
        # use install(1) for staging files when changed
        dstfile=$dst
        case $dst in */) dstfile=$dst${src#*/}; esac
        if [[ "$src" -nt "$dstfile" ]]; then
            if [[ -x "$src" ]]; then
                set -x; install -m a=rx "$src" "$dst"
            else
                # preserve flags of non-executable data since install(1) always marks as executable
                set -x; install -m a=r  "$src" "$dst"
            fi
        fi
    fi
    { set +x; } &>/dev/null
}
###############################################################################

# DeepDive shell
stage shell/deepdive                                              bin/
stage shell/deepdive-version                                      util/
stage shell/deepdive-help                                         util/
stage shell/deepdive-env                                          util/
stage shell/deepdive-whereis                                      util/
stage shell/find-deepdive-app                                     util/
stage shell/parse-url.sh                                          util/
stage shell/error                                                 util/
stage shell/usage                                                 util/
stage shell/escape4sh                                             util/
stage compiler/jq2sh                                              util/
stage shell/jq-f                                                  util/
stage shell/logging-with-ts                                       util/
stage shell/deepdive_bash_completion.sh                           etc/

stage compiler/deepdive-compile                                   util/
stage compiler/deepdive-check                                     util/
stage compiler/app-has-been-compiled                              util/
stage .build/submodule/compiler/hocon2json/hocon2json.sh                                util/hocon2json
stage .build/submodule/compiler/hocon2json/target/scala-2.10/hocon2json-assembly-*.jar  util/hocon2json.jar
stage src/main/resources/application.conf                         etc/deepdive-default.conf
stage compiler/compile-config                                     util/
stage compiler/compile-check                                      util/
stage compiler/compile-code                                       util/
stage compiler/compile-codegen                                    util/

stage runner/deepdive-plan                                        util/
stage runner/deepdive-do                                          util/
stage runner/deepdive-redo                                        util/
stage runner/deepdive-mark                                        util/
stage runner/deepdive-done                                        util/
stage runner/format_timestamp                                     util/
stage runner/mark_done                                            util/
stage runner/resolve-args-to-do.sh                                util/
stage runner/show_progress                                        util/
stage runner/deepdive-run                                         util/

stage runner/deepdive-compute                                     util/
stage runner/load-compute-driver.sh                               util/
stage runner/compute-driver/local                                 util/compute-driver/
stage runner/computers-default.conf                               util/
stage .build/submodule/runner/mkmimo/mkmimo                       util/

stage database/deepdive-db                                        util/
stage database/deepdive-initdb                                    util/
stage database/deepdive-sql                                       util/
stage database/deepdive-load                                      util/
stage database/load-db-driver.sh                                  util/
stage database/db-driver/postgresql                               util/db-driver/
stage database/db-driver/greenplum                                util/db-driver/
stage database/db-driver/postgresql-xl                            util/db-driver/
stage database/db-driver/mysql                                    util/db-driver/
stage database/db-driver/mysqlcluster                             util/db-driver/
stage database/partition_id_range                                 util/
stage database/pgtsv_to_json                                      util/

# DeepDive core
stage target/scala-2.10/deepdive-assembly-*.jar                   lib/deepdive.jar || true  # when testing, .jar may be missing

# DeepDive utilities
stage util/tobinary.py                                            util/
stage util/active.sh                                              util/
stage util/draw_calibration_plot                                  util/
stage util/calibration.py                                         util/
stage util/calibration.plg                                        util/

# DDlog compiler
stage compiler/ddlog-wrapper.sh                                   bin/ddlog
stage .build/submodule/compiler/ddlog/target/scala-2.10/ddlog-assembly-0.1-SNAPSHOT.jar  lib/ddlog.jar

# DDlib
stage ddlib/ddlib                                                 lib/python/

# DimmWitted sampler
case $(uname) in
Linux)
    stage util/ndbloader/ndbloader-linux                          util/ndbloader
    # copy shared libraries required by the dimmwitted sampler
    ldd .build/submodule/inference/dimmwitted/dw | grep '=>' |
    awk '{print $3}' | sort -u | grep -v '^(' |
    grep -v '^/lib/' |
    xargs cp -vt "$STAGE_DIR"/lib/
    stage .build/submodule/inference/dimmwitted/dw                util/sampler-dw.bin
    stage inference/dimmwitted-wrapper.linux.sh                   util/sampler-dw
    ;;
Darwin)
    stage util/ndbloader/ndbloader-mac                            util/ndbloader
    # copy shared libraries required by the dimmwitted sampler
    otool -L .build/submodule/inference/dimmwitted/dw |
    grep 'dylib' | sed 's/(.*)//' | awk '{print $1}' | sort -u |
    grep -v '^/usr/lib/' |
    xargs cp -vt "$STAGE_DIR"/lib/
    stage .build/submodule/inference/dimmwitted/dw                util/sampler-dw.bin
    stage inference/dimmwitted-wrapper.mac.sh                     util/sampler-dw
    ;;
*)
    echo >&2 "$(uname): Unsupported OS"; false
    ;;
esac
stage inference/format_converter                                  util/format_converter

# piggy extractor helper
stage util/piggy_prepare.py                                       util/
# plpy extractor helpers
stage util/ddext.py                                               util/
stage util/ddext_input_sql_translator.py                          util/

# Mindbender
stage .build/submodule/util/mindbender/mindbender-LATEST.sh       bin/mindbender || true  # keeping it optional for now

# runtime dependencies after building them from source
stage depends/.build/bundled                                      lib/
