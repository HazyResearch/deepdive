#!/usr/bin/env bats
# Tests for json exporter

. "$BATS_TEST_DIRNAME"/env.sh >&2

@test "$DBVARIANT json exporter" {
    q="SELECT 123::bigint as i, 45.678 as float, TRUE as t, FALSE as f, 'foo bar baz'::text as s, NULL::text as n, ARRAY['asdf qwer"$'\t'"zxcv"$'\n'"1234', '\"I''m your father,\" said Darth Vader.'] as a, ARRAY[1,2,3] as b"
    expected="{\"i\":123,\"float\":45.678,\"t\":true,\"f\":false,\"s\":\"foo bar baz\",\"n\":null,\"a\":[\"asdf qwer\\tzxcv\\n1234\",\"\\\"I'm your father,\\\" said Darth Vader.\"],\"b\":[1,2,3]}"
    [[ $(db-query "$q" json 0) = "$expected" ]]
}
