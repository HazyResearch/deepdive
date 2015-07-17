#!/usr/bin/env bash
# A script for regnerating scalatests/*.bats
set -eu

cd "$(dirname "$0")"

# generate a corresponding bats file for every test in Scala
mkdir -p scalatests
for t in $(cd ../.. && sbt coverage "export printTests" | grep ^org.deepdive.test); do
    bats=scalatests/${t}.bats
    echo >&2 $bats
    cat >$bats <<EOF
#!/usr/bin/env bats
# DeepDive Scala Tests
# Generated: $(date +%FT%T)

. "\$BATS_TEST_DIRNAME"/../env.sh >&2

setup() {
    db-init
}

@test "\$DBVARIANT ScalaTest $t" {
    java org.scalatest.tools.Runner -oDF -s $t
}
EOF
done
