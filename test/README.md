DDLog tests
===========

`test.sh` runs all tests.

[Bats](https://github.com/sstephenson/bats.git) is used for most end-to-end tests.
Before running Bats, `.bats` files will be prepared for each `*.bats.template` if they have a directory with the same name (without `.bats.template`) contaning test spec subdirectories.
For each test spec, a `.bats` symlink pointing to the `.bats.template` is created, which is in turn run with Bats.
This way, creating new tests for different inputs is it's very easy as we can simply clone existing test specs.
