Here are test cases for running the "expected-output-test".

Each test case is a directory that contains the following files:

* `input.ddlog` is the input DDLog program.
    * Parser is tested against every input program.
    * If the input is supposed to cause a parse error, `parse.error.expected` containing an expected error message should be there.
* `print.expected` is the expected output of running the `print` command.  Test is skipped if not available.
* `compile.expected` is the expected output of running the `compile` command.  Test is skipped if not available.
* `print-incremental.expected` is the expected output of running the `print --incremental` command.  Test is skipped if not available.
* `compile-incremental.expected` is the expected output of running the `compile --incremental` command.  Test is skipped if not available.

The tests will create `.actual` files corresponding to each `.expected` file.
