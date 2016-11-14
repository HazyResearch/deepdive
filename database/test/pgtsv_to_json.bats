#!/usr/bin/env bats
# Tests for pgtsv_to_json
load test_environ
PATH="$DEEPDIVE_SOURCE_ROOT/util/build/test:$PATH"

load corner_cases

# a handy wrapper
pgtsv_to_json() {
    keeping_output_of deepdive env pgtsv_to_json "$@"
}

@test "pgtsv_to_json works" {
    actual=$(eval pgtsv_to_json $NastyColumnTypes <<<"$NastyTSV")
    compare_json "$NastyJSON" "$actual"
}

@test "pgtsv_to_json works (with null in arrays)" {
    actual=$(eval pgtsv_to_json $NullInArrayColumnTypes <<<"$NullInArrayTSV")
    compare_json "$NullInArrayJSON" "$actual"
}

@test "pgtsv_to_json works (with nested arrays)" {
    actual=$(eval pgtsv_to_json $NestedArrayColumnTypes <<<"$NestedArrayTSV") || skip "nested array unsupported"
    compare_json "$NestedArrayJSON" "$actual"
}

@test "pgtsv_to_json works (with unicode)" {
    actual=$(eval pgtsv_to_json $UnicodeColumnTypes <<<"$UnicodeTSV")
    compare_json "$UnicodeJSON" "$actual"
}

@test "pgtsv_to_json works (with timestamps)" {
    actual=$(eval pgtsv_to_json $TimestampColumnTypes <<<"$TimestampTSV")
    compare_json "$TimestampJSON" "$actual"
}
