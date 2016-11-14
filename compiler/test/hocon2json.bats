#!/usr/bin/env bats
# Tests for hocon2json
load test_environ

@test "hocon2json works" {
    # check if it outputs proper JSON
    set -o pipefail
    for hoconfile in {"$DEEPDIVE_SOURCE_ROOT","$DEEPDIVE_TEST_ROOT"}/postgresql/*/deepdive.conf; do
        [[ -e "$hoconfile" ]] || continue
        echo "hocon2json $hoconfile"
        json=$(hocon2json "$hoconfile")
        jq empty <<<"$json"
    done

    # check if it respects the order (later definitions get overridden)
    json=$(hocon2json <(echo 'foo: 123';                    echo 'baz: hello world'  ) \
                      <(echo 'foo: 45' ; echo 'bar: true'                            ) \
                      <(                 echo 'bar: false'; echo 'qux: happy hacking')
        )
    echo "$json"
    test "$(jq -r .foo <<<"$json")" = 123
    test "$(jq -r .bar <<<"$json")" = true
    test "$(jq -r .baz <<<"$json")" = "hello world"
    test "$(jq -r .qux <<<"$json")" = "happy hacking"
}
