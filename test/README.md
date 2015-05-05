DDLog tests
===========

`test.sh` runs all tests.

[Bats](https://github.com/sstephenson/bats.git) is used for most end-to-end tests.
Every `*-example.bats` will be run for each `.ddl` example under `../examples/`, with the `EXAMPLE` environment variable set to the path to the example.
Rest of the `.bats` tests will run once.
