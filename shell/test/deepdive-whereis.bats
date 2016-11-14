#!/usr/bin/env bats
load test_environ

@test "deepdive whereis usage" {
    ! deepdive whereis || false
    ! deepdive whereis app || false
    ! deepdive whereis installed || false
    deepdive whereis bin/deepdive
}

# example app to test against
app="$DEEPDIVE_SOURCE_ROOT"/examples/smoke
app_paths=(
    app.ddlog
    deepdive.conf
    db.url
    input/person.tsv
    input/friends.tsv
)

@test "deepdive whereis app works" {
    cd "$app"
    # each path under the app
    for f in "${app_paths[@]}"; do
        [ x"$(deepdive whereis app "$f")" = x"$app/$f" ]
    done
    # multiple paths
    cmp <(deepdive whereis app "${app_paths[@]}") <(printf "$app/%s\n" "${app_paths[@]}")
}

@test "deepdive whereis app should return error properly" {
    # on non-existent paths
    cd "$app"
    ! deepdive whereis app non-existent/path || false
    ! deepdive whereis app deepdive.conf non-existent/path/between-existing-ones app.ddlog || false
    # outside an app
    cd /
    ! deepdive whereis app deepdive.conf || false
}

installed_paths=(
    bin/deepdive
    etc/deepdive_bash_completion.sh
    etc/deepdive-default.conf
)

@test "deepdive whereis installed works" {
    test_deepdive_installed() {
        # each path
        for f in "${installed_paths[@]}"; do
            [ x"$(deepdive whereis installed "$f")" = x"$DEEPDIVE_HOME/$f" ]
        done
        # multiple paths
        cmp <(deepdive whereis installed "${installed_paths[@]}") <(printf "$DEEPDIVE_HOME/%s\n" "${installed_paths[@]}")
    }
    # outside an app
    cd /
    test_deepdive_installed
    # each path inside an app
    cd "$app"
    test_deepdive_installed
}

@test "deepdive whereis installed should return error properly" {
    # on non-existent paths
    cd /
    ! deepdive whereis installed non-existent/path || false
    ! deepdive whereis installed bin/deepdive non-existent/path/between-existing-ones etc/deepdive-default.conf || false
}

@test "deepdive whereis works with mixed app and installed" {
    cd "$app"
    # each path under the app
    for f in "${app_paths[@]}"; do
        [ x"$(deepdive whereis "$f")" = x"$app/$f" ]
    done
    # each path under the app
    for f in "${installed_paths[@]}"; do
        [ x"$(deepdive whereis "$f")" = x"$DEEPDIVE_HOME/$f" ]
    done
    # multiple paths
    cmp <(deepdive whereis "${app_paths[@]}" "${installed_paths[@]}") <(printf "$app/%s\n" "${app_paths[@]}"; printf "$DEEPDIVE_HOME/%s\n" "${installed_paths[@]}")

    # aborts on non-existent paths
    ! deepdive whereis non-existent/path || false
    ! deepdive whereis bin/deepdive non-existent/path/between-existing-ones deepdive.conf || false
}
