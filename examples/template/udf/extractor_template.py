#!/usr/bin/env python
from deepdive import *

@tsv_extractor
@returns(lambda
        column1 = "text",
        column2 = "int",
        column3 = "float",
    :[])
def my_udf(
        column1 = "text",
        column2 = "int",
        column3 = "float",
    ):
  yield [
      column1,
      column2,
      column3,
    ]
