---
layout: default
title: Debugging user-defined functions
---

# Debugging user-defined functions

Many things can go wrong in user-defined functions (UDFs), so debugging support is important for the user to write the code and easily verify that it works as expected.
UDFs can be implemented in any programming language as long as they take the form of an executable that reads from *standard input* and writes to *standard output*.
Here are some general tips for printing information to the log and running the UDFs in limited ways to help work through issues without needing to run the entire data flow of the DeepDive application.


## Printing to the log

Remember that the *standard output* of a UDF is already reserved for TSJ or TSV formatted data that gets loaded into the database.
Therefore when a typical print statement is used for debugging, it won't appear anywhere in the log but just mangle the TSJ output stream and ultimately fail the UDF execution or corrupt its output.
The correct way to print log statements is to print to the *standard error*.
Below is an example in Python.

```python
#!/usr/bin/env python
from deepdive import *
import sys

@tsj_extractor
@returns( ... )
def extract( ... ):
    ...
    print >>sys.stderr, 'This prints some_object to logs :', some_object
    ...
```

During execution of the script, anything written to *standard error* appears in the console as well as in the file named `run.log` under the `run/LATEST/` directory.


## Executing UDFs within DeepDive's environment

To assist with debugging issues in UDFs, DeepDive provides a wrapper command to directly execute it within the same environment it uses for the actual execution.

Suppose a Python UDF at `udf/fn.py` imports `deepdive` and [`ddlib`](gen_feats.md) as suggested in the [guide for writing UDFs](writing-udf-python.md).
When run normally as a Python script, it will give an error that looks like this:

```bash
python udf/fn.py
```
```
Traceback (most recent call last):
  File "udf/fn.py", line 2, in <module>
    from deepdive import *
ImportError: No module named deepdive
```

Instead, by prefixing the command with `deepdive env`, they can be executed as if they were executed in the middle of DeepDive's data flow.

```bash
deepdive env python udf/fn.py
```

This will take TSJ rows from standard input and print TSJ rows to standard output as well as debug logs to standard error. It can therefore be debugged just like a normal Python program.

<!--
<todo>write</todo>
<br><todo>

- `deepdive check testfire udf/fn.py` will be added here?

</todo>
-->
