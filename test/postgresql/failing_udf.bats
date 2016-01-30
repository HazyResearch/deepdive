#!/usr/bin/env bats
# Tests for broken example

. "$BATS_TEST_DIRNAME"/env.sh >&2

@test "$DBVARIANT UDFs with error should not be silenced" {
    cd "${BATS_TEST_FILENAME%.bats}" || skip "app directory not found"
    deepdive compile
    deepdive redo process/init/app
    deepdive redo process/fine_extractor
    ! deepdive redo process/failing_cmd_extractor || false
    ! deepdive redo process/failing_tsv_extractor || false
    ! deepdive redo process/failing_sql_extractor || false
}
