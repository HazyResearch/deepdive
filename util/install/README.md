# DeepDive Installer Scripts

## Layout of an Installer Script

Shell functions whose name starts with `install_`.

```bash
install_deepdive_runtime_deps() {
    ...
}

install_postgres() {
    ...
}

...
```

## Utilities
Several utility functions are available to the installer scripts.

* `error "Cannot install dependencies without Homebrew (http://brew.sh)"`
* `source_script install.Ubuntu.sh`
* `source_os_script pgxl`
* `enter NUM_DATA_NODES "number of data nodes"`

