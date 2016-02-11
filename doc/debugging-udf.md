---
layout: default
title: Debugging UDFs
---

# Debugging UDFs

## Overview

Since UDFs are user defined and can be implemented in a wide range of executable types, it is really up to the user to write the code and verify that it works as expected.  Here are some tips for how to print to logs and running the UDFs in limited ways to help work through issues without needing to run the entire DeepDive process.

## Printing to log

Remember that the output of a UDF is tsv formatted data that gets loaded into a table.  Therefore anything printed to `standard out` for debugging will mangle the tsv structure and ultimately cause the UDF execution to fail.  The correct way to print log statements is to print to `standard error`.  Below is an example in python.

```
import sys
print >>sys.stderr, 'This prints some_object to logs :', some_object
```

During execution of the script, anything written to `standard error` will be printed to the console as well as to the run.log which can be found in the `run/LATEST/` directory.

## Exectuing UDFs with DeepDive

To assist with debugging any issues that may be related to the environment in which the UDF is executed, DeepDive provides a command to execute the UDFs.  For example, for a python UDF in the `udf` directory named `fn.py`, it can be executed in DeepDive with the following command.

```
deepdive env python udf/fn.py
```
<!--
<todo>write</todo>
<br><todo>

- `deepdive check testfire udf/fn.py` will be added here?

</todo>
-->
