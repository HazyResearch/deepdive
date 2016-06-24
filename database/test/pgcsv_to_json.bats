#!/usr/bin/env bats
# Tests for pgcsv_to_json
. "$BATS_TEST_DIRNAME"/env.sh >&2
PATH="$DEEPDIVE_SOURCE_ROOT/util/build/test:$PATH"

load corner_cases

@test "pgcsv_to_json works" {
    actual=$(pgcsv_to_json $NastyColumnTypes <<<"$NastyCSV")
    compare_json "$NastyJSON" "$actual"
}

@test "pgcsv_to_json works (with null in arrays)" {
    actual=$(pgcsv_to_json $NullInArrayColumnTypes <<<"$NullInArrayCSV")
    compare_json "$NullInArrayJSON" "$actual"
}

@test "pgcsv_to_json works (with nested arrays)" {
    actual=$(pgcsv_to_json $NestedArrayColumnTypes <<<"$NestedArrayCSV")
    compare_json "$NestedArrayJSON" "$actual"
}
