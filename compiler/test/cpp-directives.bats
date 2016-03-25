#!/usr/bin/env bats
# Tests for cpp directives in DDlog, such as #include, #define and macro expansions
. "$BATS_TEST_DIRNAME"/env.sh >&2

without_comments() {
    sed 's/[[:space:]]*#.*$//' "$@" |
    grep -v '^[[:space:]]*$'
}

@test "compiler supports #include directives" {
    cd "${BATS_TEST_FILENAME%.bats}"/include
    deepdive compile
    diff -u <(cat a.ddlog app.ddlog b.ddlog | without_comments) <(cat run/compiled/app.ddlog | without_comments)
}

@test "compiler supports #define directives" {
    cd "${BATS_TEST_FILENAME%.bats}"/define
    deepdive compile
    diff -u <(without_comments expected.ddlog) <(without_comments run/compiled/app.ddlog)
}

@test "compiler supports #ifdef directives" {
    cd "${BATS_TEST_FILENAME%.bats}"/ifdef
    deepdive compile
    diff -u <(without_comments expected.ddlog) <(without_comments run/compiled/app.ddlog)
}

@test "compiler supports CPPFLAGS environment" {
    cd "${BATS_TEST_FILENAME%.bats}"/ifdef
    CPPFLAGS="-DSOME_MACRO=100" deepdive compile
    diff -u <(without_comments expected_SOME_MACRO.ddlog) <(without_comments run/compiled/app.ddlog)
}
