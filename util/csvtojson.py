#! /usr/bin/env python

import json, sys, csv, re

def convert_flat_type_func(column_type):
  column_type = column_type.lower()
  if column_type == "integer" or column_type == "int" or column_type == "bigint":
    return lambda x: None if x == "" else int(x)
  elif column_type == "float" or column_type == "numeric":
    return lambda x: None if x == "" else float(x)
  elif column_type == "text":
    return lambda x: None if x == "" else x
        # FIXME it's impossible to distinguish empty strings from nulls
        # In PostgreSQL's csv output, `1,,2.34` means 1, null, 2.34, whereas `1,"",2.34` means 1, empty string, 2.34.
        # csv.reader provides no formatter option to handle this.
        # See: http://stackoverflow.com/questions/11379300/python-csv-reader-behavior-with-none-and-empty-string
        # See: https://github.com/JoshClose/CsvHelper/issues/252
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
    flat_func = convert_flat_type_func(column_type)
    def convert_text_array_func(value):
      if value == "": return None
      # string unescaping
      def rep(match):
        return '""' if match.group(2) == '"' else match.group(2)
      arr = csv.reader([re.sub(r"(\\)(.)", rep, value[1:-1])], delimiter=',', quotechar='"').next()
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
  types = sys.argv[1:]
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
