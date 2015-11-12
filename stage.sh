#!/usr/bin/env bash
# stage.sh -- a script that stages code and data for distribution
set -eu

###############################################################################
STAGE_DIR=${1:-dist/stage}
stage() {
    local src=$1 dst=$2
    [[ -e "$src" ]]
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
stage shell/deepdive_bash_completion.sh                           etc/

stage shell/deepdive-initdb                                       util/
stage shell/deepdive-sql                                          util/
stage shell/deepdive-load                                         util/

# runtime dependencies
stage depends/jq                                                  util/
stage depends/ts                                                  util/
stage depends/dot                                                 util/

stage compiler/deepdive-compile                                   util/
stage compiler/hocon2json                                         util/
stage src/main/resources/application.conf                         etc/deepdive-default.conf
stage compiler/configExtended                                     util/
stage compiler/configNormalized                                   util/
stage compiler/configCompleted                                    util/
stage compiler/config2dependencies                                util/
stage compiler/dependencies2make                                  util/
stage compiler/dependencies2dot                                   util/
stage compiler/config2compilation-plan                            util/
stage compiler/compile-tsv_extractor                              util/
stage compiler/compile-sql_extractor                              util/
stage compiler/compile-cmd_extractor                              util/
stage compiler/compile-json_extractor                             util/
stage compiler/compile-plpy_extractor                             util/
stage compiler/compile-piggy_extractor                            util/
stage compiler/compile-sql_factor                                 util/

stage runner/deepdive-plan                                        util/
stage runner/deepdive-do                                          util/
stage runner/deepdive-redo                                        util/
stage runner/deepdive-mark                                        util/
stage runner/format_timestamp                                     util/
stage runner/mark_done                                            util/
stage runner/resolve-args-to-do.sh                                util/
stage runner/show_progress                                        util/
stage runner/deepdive-run                                         util/

stage shell/load-db-driver.sh                                     util/
stage shell/db-driver/postgresql                                  util/db-driver/
stage shell/db-driver/greenplum                                   util/db-driver/
stage shell/db-driver/postgresql-xl                               util/db-driver/
stage shell/db-driver/mysql                                       util/db-driver/
stage shell/db-driver/mysqlcluster                                util/db-driver/

# DeepDive core
stage target/scala-2.10/deepdive-assembly-*.jar                   lib/deepdive.jar || true  # when testing, .jar may be missing

# DeepDive utilities
stage util/tobinary.py                                            util/
stage util/active.sh                                              util/
stage util/calibration.py                                         util/
stage util/calibration.plg                                        util/
stage util/pgtsv_to_json                                          util/

# DDlog compiler
stage util/ddlog                                                  bin/
stage util/ddlog.jar                                              lib/
#stage ddlog/ddlog.jar                                            lib/ # TODO

# DDlib
stage ddlib/ddlib                                                 lib/python/

# DimmWitted sampler
if ! [[ -e "$STAGE_DIR"/util/sampler-dw ]]; then
case $(uname) in
Linux)
    stage util/ndbloader/ndbloader-linux                          util/ndbloader
    stage util/format_converter_linux                             util/format_converter
    unzip -o util/sampler-dw-linux-lib.zip                     -d "$STAGE_DIR"/lib/
    #stage sampler/dw                                             util/sampler-dw # TODO
    stage util/sampler-dw-linux                                   util/
    stage util/sampler-dw-linux.sh                                util/
    ln -sfn sampler-dw-linux.sh                                   "$STAGE_DIR"/util/sampler-dw
    ;;
Darwin)
    stage util/ndbloader/ndbloader-mac                            util/ndbloader
    stage util/format_converter_mac                               util/format_converter
    ditto -xk util/sampler-dw-mac-lib.zip                         "$STAGE_DIR"/lib/
    stage util/sampler-dw-mac                                     util/
    stage util/sampler-dw-mac.sh                                  util/
    ln -sfn sampler-dw-mac.sh                                     "$STAGE_DIR"/util/sampler-dw
    ;;
*)
    echo >&2 "$(uname): Unsupported OS"; false
    ;;
esac
fi

# piggy extractor helper
stage util/piggy_prepare.py                                       util/
# plpy extractor helpers
stage util/ddext.py                                               util/
stage util/ddext_input_sql_translator.py                          util/

# Mindbender
stage mindbender/mindbender-LATEST-$(uname)-x86_64.sh             bin/mindbender || true  # keeping it optional for now
