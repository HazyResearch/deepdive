#!/usr/bin/env bash
# load-compute-driver.sh -- Loads the compute driver configured for the DeepDive app
# > DEEPDIVE_COMPUTER=...
# > . load-compute-driver.sh
#
# $DEEPDIVE_COMPUTER environment variable has precedence over the
# deepdive.computer value configured in computers.conf file in the DeepDive
# application.
##

if [[ -z "${DEEPDIVE_COMPUTER_TYPE:-}" ]]; then
    # parse computers.conf with fallback to built-in default if not defined already
    eval "$(
    DEEPDIVE_APP=$(find-deepdive-app)
    export DEEPDIVE_APP
    hocon2json <(cat "$DEEPDIVE_APP"/computers.conf 2>/dev/null || echo '{}') "$DEEPDIVE_HOME"/util/computers-default.conf | jq2sh \
        DEEPDIVE_COMPUTER='.deepdive.computer' \
        DEEPDIVE_COMPUTER_TYPE='.deepdive.computers[.deepdive.computer].type' \
        DEEPDIVE_COMPUTER_CONFIG='.deepdive.computers[.deepdive.computer]' \
        #
    )"

    # place the driver on PATH
    PATH="$DEEPDIVE_HOME"/util/compute-driver/"$DEEPDIVE_COMPUTER_TYPE":"$PATH"
    # make sure all operations are defined
    for op in execute
    do type compute-$op &>/dev/null || error "$DEEPDIVE_COMPUTER_TYPE compute driver does not provide compute-$op operation"
    done

    # set up the environment for using the configured computer
    export DEEPDIVE_COMPUTER{,_{TYPE,CONFIG}} PATH
fi
