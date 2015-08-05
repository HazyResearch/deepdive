#! /usr/bin/env python

import json, sys, csv

def convert_flat_type_func(column_type):
  column_type = column_type.lower()
  if column_type == "integer" or column_type == "int" or column_type == "bigint":
    return lambda x: None if x == "" else int(x)
  elif column_type == "float" or column_type == "numeric":
    return lambda x: None if x == "" else float(x)
  elif column_type == "text":
    return lambda x: None if x == "" else x
  elif column_type == "boolean":
    return lambda x: None if x == "" else x == "t"
  else:
    raise ValueError("Unsupported data type %s" %column_type)

# given a column type, returns a function that takes a string input
# and output with the correct type
def convert_type_func(column_type):
  column_type = column_type.lower()
  if column_type.endswith("[]"):
    # csv array starts and ends with curly braces
    column_type = column_type[:-2]
    # string unescaping
    # note this is an array as a csv field, we first unescape csv escaping,
    # and then do array unescaping, apply csv escaping, and parse the arry
    # elements as csv
    # csv escapes double quote with two double quotes
    # array escapes double quote using backslashes
    flat_func = convert_flat_type_func(column_type)
    def convert_text_array_func(value):
      if value == "": return None
      arr = csv.reader([value[1:-1].replace('""', '"').replace('\\"', '""')], delimiter=',', quotechar='"').next()
      return [flat_func(x) for x in arr]
    def convert_other_array_func(value):
      if value == "": return None
      arr = csv.reader([value[1:-1]], delimiter=',', quotechar='"').next()
      return [flat_func(x) for x in arr]
    if column_type == "text":
      return convert_text_array_func
    else:
      return convert_other_array_func
  else:
    return convert_flat_type_func(column_type)

def main():
  # get column types
  types = sys.argv[1].split(",")
  convert_funcs = [convert_type_func(x) for x in types]

  # read the contents
  reader     = csv.reader(sys.stdin, delimiter = ',', quotechar = '"')
  fieldnames = reader.next()
  if len(types) != len(fieldnames):
    raise ValueError("Number of columns does not match schema")
  for line in reader:
    obj = {}
    for name, field, func in zip(fieldnames, line, convert_funcs):
      obj[name] = func(field)
    print json.dumps(obj)

if __name__ == "__main__":
  main()
