DDLog tests
===========

`test.sh` runs all tests.

[Bats](https://github.com/sstephenson/bats.git) is used for most end-to-end tests.
Every `*-example.bats` will be run for each .ddl example under `../examples/`, with the path to the .ddl file set in the `EXAMPLE` environment variable.
Other .bats tests will run once.
