#!/usr/bin/env bats
# DeepDive Scala Tests
# Generated: 2015-07-17T00:33:13

. "$BATS_TEST_DIRNAME"/../env.sh >&2

setup() {
    db-init
}

@test "$DBVARIANT ScalaTest org.deepdive.test.unit.SettingsParserSpec" {
    java org.scalatest.tools.Runner -oDF -s org.deepdive.test.unit.SettingsParserSpec
}
