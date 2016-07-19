---
layout: default
title: DeepDive Developer's Guide
---

# DeepDive developer's guide

This document describes useful information for those who want to make modifications to the DeepDive infrastructure itself and contribute new code.
Most of the content here are irrelevant for DeepDive users who just want to build a DeepDive application.

### DeepDive project at GitHub

Nearly all DeepDive development activities happen over GitHub.

#### Branches and releases of DeepDive

* The `master` branch points to the latest code.
* We use [Semantic Versioning](http://semver.org/).
* Every MAJOR.MINOR version has a maintenance branch that starts with `v` and ends with `.x`, e.g., `v0.6.x` (since 0.6).
* Every release is pointed by a tag, e.g., `v0.6.0`, `0.05-RELEASE`.
   Since 0.6, release tag names start with `v`, followed by MAJOR.MINOR.PATCH versions.
   They usually point to a commit in the release maintenance branch.
* Any other branch points to someone's work in progress.

#### Contributing code to DeepDive

1. If you are part of the [Hazy Research group](https://github.com/HazyResearch), you can push your commits to a new branch, then [create a Pull Request](https://github.com/HazyResearch/deepdive/compare/) to `master`.
   Otherwise, you need to first fork our repository, then push your code to that fork to create a Pull Request.
2. If you already know who can review your code, assign that member to the Pull Request.
3. The reviewer leaves comments about the code, then lets you know to fix them.
4. You improve the code and push more commits to the branch for the Pull Request, then tell the reviewer to have another look.
   Remember that GitHub doesn't send out notifications (emails) unless you leave an actual comment on the Pull Request.
   The reviewer assumes the Pull Request is not ready for another look until you explicitly say so.
5. Steps 3-4 repeat until the reviewer says everything looks good.
6. The reviewer could merge your code to the master branch him/herself or ask you to do so (if you have permission).
7. Your branch should be deleted after the Pull Request is merged or closed.


### DeepDive code

DeepDive is written in several programming languages.

* [Bash](https://en.wikibooks.org/wiki/Bash_Shell_Scripting) and [jq](https://stedolan.github.io/jq/) are the main programming languages for generating SQL queries and shell scripts that run the actual data pipeline, defined by the user's extractors and inference rules.
* C++ is used for writing the [high performance Gibbs sampler](https://github.com/HazyResearch/sampler) that takes care of learning and inference of the model defined by user's inference rules.
* C is used for the [high performance data router, mkmimo](https://github.com/netj/mkmimo) that enables executing many UDF processes in parallel efficiently.
* Python is the main language we use for the udfs in our examples.
* Scala and other mini languages are used for other minor parts.

#### DeepDive code structure

* `compiler/` contains the code that compiles DeepDive application configuration into an execution plan.
* `database/` contains database drivers as well as code implementing other database operations.
* `ddlib/` contains the ddlib Python library that helps users write their applications.
* `doc/` contains the Markdown/Jekyll source for the DeepDive website and documentation.
* `examples/` contains the DeepDive examples.
* `extern/` contains scripts for building and bundling runtime dependencies from external 3rd parties.
* `inference/` contains the engine and necessary utilities for statistical learning and inference.
* `runner/` contains the engine for running the execution plan compiled by the compiler.
* `shell/` contains the code for the general `deepdive` command-line interface.
* `test/` at the top as well as `*/test/` under each subdirectory contain the test code.
* `util/` contains other utilities for installation, build, and development.

DeepDive build is controlled by several files:

* `Makefile` takes care of the overall build process.
* `stage.sh` contains the commands that stages built code under `dist/`, which is the default location where the built executables and runtime data will be staged.
* `test/bats.mk` contains the Make recipes for running tests written in [BATS](https://github.com/sstephenson/bats) under `test/`.
* `test/enumerate-tests.sh` and `test/*/should-work.sh` determines the .bats files to run for `make test`.
* `.travis.yml` enables our continuous integration builds and tests at [Travis CI](https://travis-ci.org/HazyResearch/deepdive), which are triggered every time a new commit is pushed to our GitHub repository.

DeepDive source tree includes several git submodules and ports:

* [`compiler/ddlog/`](https://github.com/HazyResearch/ddlog) is the DDlog compiler.
* [`inference/dimmwitted/`](https://github.com/HazyResearch/sampler) is the DimmWitted Gibbs sampler.
* [`runner/mkmimo/`](https://github.com/netj/mkmimo) is a data routing component that is used for executing parallel UDF processes and efficiently streaming data through them.
* [`util/mindbender/`](https://github.com/HazyResearch/mindbender) is the collection of tools supporting development, such as Mindtagger.


#### <a name="build-test"></a> Building and Testing DeepDive

* First, get DeepDive's source tree and move into it, by running:

    ```bash
    git clone https://github.com/HazyResearch/deepdive.git
    cd deepdive
    ```

##### <a name="build-test-docker"></a> Containerized builds and tests

DeepDive build and tests can be done using [Docker](https://www.docker.com), which can simplify the development environment setup dramatically.

* To build the source tree inside a container and create a new Docker image, run:

    ```bash
    make build--in-container
    ```

    Or, if you don't even have `make`, just run:

    ```bash
    ./DockerBuild/build-in-container
    ```

    This pulls the master image from Docker Hub ([netj/deepdive-build](https://hub.docker.com/r/netj/deepdive-build/)), then inside a fresh container, runs the build after applying the changes made to the current source tree.
    This is the default for `make` (without any target argument) when Docker is available on your system.

    CAVEAT: Note that only files that are tracked by git is reflected in the build inside containers.
    Use `git add` to make sure any new files are also considered when transfering changes to containers.


* To test the latest build, run:

    ```bash
    make test--in-container
    ```

    You can pass the `ONLY=` and `EXCEPT=` filters as you do for the normal builds (described below).


    Or, the equivalent without `make` is:

    ```bash
    ./DockerBuild/test-in-container-postgres
    ```

    You can in fact override the entire test command with this:

    ```bash
    ./DockerBuild/test-in-container-postgres  make test ONLY=test/postgresql/*.bats
    ```

* To make the latest build the new master image (on your local machine), run:

    ```bash
    ./DockerBuild/update-master-image
    ```

    Until you run this command, new builds will always start from the master image, not from the latest build.
    If your source tree has diverged a lot from it, it's a good idea to update the master image once the initial long build finishes and passes the tests.
    That way each subsequent build won't have to repeat the same long build.

    If you have permission, you can push your master image to DockerHub and have others start build from there by running:

    ```bash
    docker push netj/deepdive-build:master
    ```

* To inspect the build, run:

    ```bash
    ./DockerBuild/inspect-build
    ```

    You can pass a command to run as arguments:

    ```bash
    ./DockerBuild/inspect-build  make test
    ```

##### <a name="build-test-docker"></a> Normal builds and tests

Running containerized builds and tests in Docker is the recommended way, but you are welcome to run normal builds directly on the host in the old way.
Everything described here about normal builds in fact applies to the source tree inside the container.
Moreover, normal build is the only way to produce releases for Mac and environments other than the one used in the master image.

* To install all build and runtime dependencies, run:

    ```bash
    make depends
    ```

    Or, if you don't have even `make` installed:

    ```bash
    util/install.sh _deepdive_build_deps _deepdive_runtime_deps
    ```

    Basically, DeepDive requires C/C++ compiler, JDK, Python, GNU coreutils and several libraries with headers to build from source.
    [`install__deepdive_build_deps` in `util/install/install.Ubuntu.sh`](../util/install/install.Ubuntu.sh) script enumerates most of the build dependencies as APT packages.
    You may easily find corresponding packages for your platform and install them.
    On the other hand, most of the runtime dependencies will be built and bundled (see: `depends/bundled/`), so eventually users will just grab a DeepDive binary and run it without having to waste time on installing the correct software packages.


* To build most of what's under DeepDive's source tree and install at `~/local/`, run:

    ```bash
    make install
    ```

    Overriding the `PREFIX` variable allows the installation destination to be changed.  For example:

    ```bash
    make install PREFIX=/opt/deepdive
    ```

* To run all tests, from the top of the source tree, run:

    ```bash
    make test
    ```

    Note that at least one of PostgreSQL, MySQL, or Greenplum database must be running to run the tests.

    By setting `TEST_DBHOST` environment to a `user:password@hostname`, it is possible to specify against which database the tests should run.
    For specifying non-default ports for different database types, there are more specific variables: `TEST_POSTGRES_DBHOST`, `TEST_GREENPLUM_DBHOST`, and `TEST_MYSQL_DBHOST`.

* To run tests selectively, use `ONLY` and `EXCEPT` Make variables for `make test`.

    For example, to run only the test with spouse example against PostgreSQL:

    ```bash
    make test ONLY=test/postgresql/spouse_example.bats
    ```

    Or, to skip the tests against MySQL:

    ```bash
    make test EXCEPT=test/mysql/*.bats
    ```

* To create a tarball package from the built and staged code, run:

    ```bash
    make package
    ```

    The tarball is created at `dist/deepdive.tar.gz`.

* To build the DDlog compiler from source and place the jar under `util/`, run:

    ```bash
    make build-ddlog
    ```

* To build the sampler from source and replace the binaries, run:

    ```bash
    make build-sampler
    ```

* To build the Mindbender toolchain from source and place the binary under `util/`, run:

    ```bash
    make build-mindbender
    ```

All commands shown above should be run from the top of the source tree.


#### Modifying DeepDive documentation

DeepDive documentation is written in [Markdown](http://daringfireball.net/projects/markdown/) under `doc/`, and the website is compiled using [Jekyll](http://jekyllrb.com).

To preview your changes to the documentation locally, run:

```bash
make -C doc/ test
```

To deploy changes to the main website, run:

```bash
make -C doc/ deploy
```


