# Tests for DeepDive Compiler

Each test case is a DeepDive app whose name begins with one of the following prefixes, followed by a hyphen (`-`) and a descriptive name of what it tests.

* `should_reject-*/`
    These apps should fail `deepdive compile`.

* `should_accept-*/`
    These apps should pass `deepdive compile`.


## Running Tests

Simply run the .bats file if you have [bats(1)][bats] installed on your `$PATH`.

```bash
./should_reject-empty_app.bats
```

Or use the bats command and specify which tests to run.

```bash
bats *.bats
```

[bats]: https://github.com/sstephenson/bats#readme


## Adding New Test Cases

1. Create a new app directory for the test following the naming convention.

2. Create symbolic links to the `db.url`, `schema.json`, `schema.sql` in `../stub-app` to qualify it as a proper DeepDive app.

    ```bash
    cd should_reject-new_test/
    ln -sfnv ../stub-app/db.url
    ln -sfnv ../stub-app/schema.sql
    vim deepdive.conf
    ```

3. Run the script for creating the corresponding .bats symlink to the .bats.template.

    ```bash
    ./update-bats-symlinks
    ```

4. For `should_accept-*` tests, an optional executable for validating the compiled result can be added:

    * `validate-compiled-config` which runs against `run/compiled/config.json`
    * `validate-compiled-code` which runs against `run/compiled/code-*.json`
