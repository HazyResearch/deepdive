# DeepDive execution plan compiler

The compiler basically transforms a DeepDive application's deepdive.conf object into a collection of shell scripts and Makefile, which can be later used for actual execution.
`deepdive-compile` is the user-facing facade command, an entry point to the compilation, and the rest are implementations for a specific step or transformation in the overall compilation.
Here's a brief summary of how the compilation is done.

1. DDlog rules and declarations in `app.ddlog` are compiled and combined with user's `deepdive.conf` and `schema.json` first.
    The HOCON syntax used by `deepdive.conf` is interpreted by `hocon2json` and everything is converted into a single JSON config object that holds everything under the key "deepdive".

2. The config object is first extended with some implied extractors, such as initializing the database and loading input tables.
    Then, the dependencies of extractors are normalized, and their names are qualified with corresponding prefixes (by `compile-config_normalized`) to make it easier and clearer to produce the final code for execution.
    DeepDive's built-in processes for variables and factors, such as grounding, learning, inference, and calibration, are added to the config object after the normalization.
    User's original config is kept intact under "deepdive" while the normalized one is created under a different key, "deepdive_".

3. The dependency information in config object is captured in a `Makefile` that is later used by the execution engine to produce an execution plan for any data product defined in the application, and to keep track of when each process or data has been done.

4. The actual computation that needs to be performed for each process, e.g., running a SQL query or a Python UDF, is compiled as a shell script (by `compile-code-*`).
    Each compiler component takes as input the normalized config object and generates a code fragment for the part it is responsible for, e.g., `compile-code-tsv_extractor` handles the `tsv_extractor`s.
    Compiled code fragments are represented again as JSON objects that map contents of files by their path names.
    These objects are merged, then passed to a code generator that actually materializes the code as executable shell scripts.

Everything compiled is kept under `run/` directory of the application.
The runner uses the compiled `Makefile` and executable files to plan and execute various data processing and computation tasks for the DeepDive application.
