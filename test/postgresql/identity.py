#!/usr/bin/env python

from deepdive import *

@tsv_extractor
@returns(lambda
v1 = "text",
v2 = "float",
#v3 = "boolean",
#v4 = "boolean",
v3 = "text",
v4 = "text",
v5 = "text",
v6 = "text",
v7 = "text",
v8 = "text",
v9 = "text",
v10 = "text",
v11 = "text",
v12 = "int[]",
v13 = "float[]",
v14 = "text[]",
v15 = "text[]",
v16 = "text[]",
v17 = "text[]",
v18 = "text[]",
v19 = "text[]",
v20 = "text[]",
:[])

def identity_func(
v1 = "text",
v2 = "float",
#v3 = "boolean",
#v4 = "boolean",
v3 = "text",
v4 = "text",
v5 = "text",
v6 = "text",
v7 = "text",
v8 = "text",
v9 = "text",
v10 = "text",
v11 = "text",
v12 = "int[]",
v13 = "float[]",
v14 = "text[]",
v15 = "text[]",
v16 = "text[]",
v17 = "text[]",
v18 = "text[]",
v19 = "text[]",
v20 = "text[]",
 ):
 import sys
 for i, x in enumerate([v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20]):
  print >>sys.stderr, "v%d =\t" % i,
  print >>sys.stderr, x
 yield [v1, v2, v3, v4, v5, v6, v7, v8, v9, v10, v11, v12, v13, v14, v15, v16, v17, v18, v19, v20]

