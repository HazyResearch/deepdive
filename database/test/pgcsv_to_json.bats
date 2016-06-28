#!/usr/bin/env bats
# Tests for pgtsv_to_json
. "$BATS_TEST_DIRNAME"/env.sh >&2
PATH="$DEEPDIVE_SOURCE_ROOT/util/build/test:$PATH"

load corner_cases

@test "pgtsv_to_json works" {
    actual=$(keeping_output_of pgtsv_to_json $NastyColumnTypes <<<"$NastyCSV")
    compare_json "$NastyJSON" "$actual"
}

@test "pgtsv_to_json works (with null in arrays)" {
    actual=$(keeping_output_of pgtsv_to_json $NullInArrayColumnTypes <<<"$NullInArrayCSV")
    compare_json "$NullInArrayJSON" "$actual"
}

@test "pgtsv_to_json works (with nested arrays)" {
    actual=$(keeping_output_of pgtsv_to_json $NestedArrayColumnTypes <<<"$NestedArrayCSV")
    compare_json "$NestedArrayJSON" "$actual"
}

@test "pgtsv_to_json works (with unicode)" {
    actual=$(keeping_output_of pgtsv_to_json $UnicodeColumnTypes <<<"$UnicodeCSV")
    compare_json "$UnicodeJSON" "$actual"
}
