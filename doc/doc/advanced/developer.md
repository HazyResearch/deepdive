---
layout: default
---

# DeepDive Developer's Guide

This document describes useful information for those who want to make modifications to the DeepDive infrastructure itself and contribute new code.
Most of the content here are irrelevant for DeepDive users who just want to build a DeepDive application.

### DeepDive Project at GitHub

Nearly all DeepDive development activities happen over GitHub.

#### Branches and Releases of DeepDive

* The `master` branch points to the latest code.
* We use [Semantic Versioning](http://semver.org/).
* Every MAJOR.MINOR version has a maintenance branch that starts with `v` and ends with `.x`, e.g., `v0.6.x` (since 0.6).
* Every release is pointed by a tag, e.g., `v0.6.0`, `0.05-RELEASE`.
   Since 0.6, release tag names start with `v`, followed by MAJOR.MINOR.PATCH versions.
   They usually point to a commit in the release maintenance branch.
* Any other branch points to someone's work in progress.

#### Contributing Code to DeepDive

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


### DeepDive Code

DeepDive is written in several programming languages.

* Scala is the main language for generating SQL queries and shell scripts that run the actual data pipeline, defined by the user's extractors and inference rules.
* C++ is used for writing the [high performance Gibbs sampler](https://github.com/HazyResearch/sampler) that takes care of learning and inference of the model defined by user's inference rules.
* Bash and Python scripts are used for other parts.
* Python is the main language we use for the udfs in our examples.

#### DeepDive Code Structure

* `src/main/scala/` contains the main Scala code.
* `util/` contains utility scripts and binaries.
* `lib/` contains libraries required by some DeepDive executables in `util/`.
* `ddlib/` contains the ddlib Python library that helps users write feature extractors.
* `src/test/`, `test/`, and `test.sh` contains the test code.
* `doc/` contains the Markdown/Jekyll source for the DeepDive website and documentation.
* `examples/` contains the DeepDive examples.

DeepDive build is controlled by several files:

* `Makefile` takes care of the overall build process.
* `build.sbt`, `sbt/` and `project/` contains the build tool and configuration for Scala.
* `.travis.yml` enables our continuous integration builds and tests at [Travis CI](https://travis-ci.org/HazyResearch/deepdive), which are triggered every time a new commit is pushed to our GitHub repository.
* `Dockerfile` defines how a new Docker image for DeepDive is generated.

DeepDive source tree includes several git submodules and ports:

* [`sampler/`](https://github.com/HazyResearch/sampler) is the DimmWitted Gibbs sampler.
* [`mindbender/`](https://github.com/HazyResearch/mindbender) is the collection of tools supporting development, such as Mindtagger.
* [`ddlog/`](https://github.com/HazyResearch/ddlog) is the DDlog compiler.
* `mln/` contains a [Tuffy](http://i.stanford.edu/hazy/hazy/tuffy/) port.


#### Building and Testing DeepDive

* To install all dependencies and build what's under DeepDive's source tree, run:

    ```bash
    make
    ```

* To run all tests, from the top of the source tree, run:

    ```bash
    make test
    ```

    Note that at least one of PostgreSQL, MySQL, or Greenplum database must be running to run the tests.


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


#### Modifying DeepDive Documentation

DeepDive documentation is written in [Markdown](http://daringfireball.net/projects/markdown/) under `doc/`, and the website is compiled using [Jekyll](http://jekyllrb.com).

To preview your changes to the documentation locally, run:

```bash
make -C doc/ test
```

To deploy changes to the main website, run:

```bash
make -C doc/ deploy
```


#### Scala Style Guide

Please follow the [Scala Style Guide](http://docs.scala-lang.org/style/) when contributing.

