#!/usr/bin/env bats
. "$BATS_TEST_DIRNAME"/env.sh

@test "jq2sh works as expected" {
    unset DEEPDIVE_COMPUTER DEEPDIVE_COMPUTER_CONFIG
    eval "$(
    jq2sh <<<'{"deepdive":{"computer":"local", "computers":{"local":{"type":"local"}, "foo": "bar"}, "foo": "bar"}}' \
        DEEPDIVE_COMPUTER='.deepdive.computer' \
        DEEPDIVE_COMPUTER_CONFIG='.deepdive.computers[.deepdive.computer]' \
        #
    )"
    [[ $DEEPDIVE_COMPUTER == 'local' ]]
    [[ $DEEPDIVE_COMPUTER_CONFIG == '{"type":"local"}' ]]
}

@test "jq2sh respects environment variables" {
    unset DEEPDIVE_COMPUTER DEEPDIVE_COMPUTER_CONFIG
    DEEPDIVE_COMPUTER=foo
    DEEPDIVE_COMPUTER_CONFIG='{"type": "bar"}'
    eval "$(
    jq2sh <<<'{"deepdive":{"computer":"local", "computers":{"local":{"type":"local"}, "foo": "bar"}, "foo": "bar"}}' \
        DEEPDIVE_COMPUTER='.deepdive.computer' \
        DEEPDIVE_COMPUTER_CONFIG='.deepdive.computers[.deepdive.computer]' \
        #
    )"
    [[ $DEEPDIVE_COMPUTER == 'foo' ]]
    [[ $DEEPDIVE_COMPUTER_CONFIG == '{"type": "bar"}' ]]
}
