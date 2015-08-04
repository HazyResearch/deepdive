#! /usr/bin/env python

import json, sys, csv, os, pipes, subprocess

def convert_type(value, column_type):
  if value == "": return None
  column_type = column_type.lower()
  if column_type == "integer" or column_type == "int" or column_type == "bigint":
    return int(value)
  elif column_type == "float" or column_type == "numeric":
    return float(value)
  elif column_type == "text":
    return value
  elif column_type == "boolean":
    return value == "t"
  elif column_type.endswith("[]"):
    # csv array starts and ends with curly braces
    value = value[1:-1]
    column_type = column_type[:-2]
    # string unescaping
    # note this is an array as a csv field, we first unescape csv escaping,
    # and then do array unescaping, apply csv escaping, and parse the arry
    # elements as csv
    # csv escapes double quote with two double quotes
    # array escapes double quote using backslashes
    if column_type == "text":
      value = value.replace('""', '"').replace('\\"', '""')
    arr = csv.reader([value], delimiter=',', quotechar='"').next()
    return [convert_type(x, column_type) for x in arr]
  else:
    raise ValueError("Unsupported data type %s" %column_type)


def main():
  # get column types
  sql = "CREATE TEMP TABLE __to_json_temp AS (%s) LIMIT 0; \
  COPY (SELECT format_type(atttypid, atttypmod) AS type \
  FROM pg_attribute \
  WHERE attrelid = '__to_json_temp'::regclass \
  AND attnum > 0 \
  AND NOT attisdropped \
  ORDER BY attnum) TO STDOUT" %(sys.argv[1].rstrip(";"))
  types = subprocess.check_output("deepdive sql %s" %pipes.quote(sql), shell=True).strip().split("\n")

  # read the contents
  reader = csv.reader(sys.stdin, delimiter = ',', quotechar = '"')
  fieldnames = reader.next()
  if len(types) != len(fieldnames):
    raise ValueError("Number of columns does not match schema")
  for line in reader:
    if len(line) != len(fieldnames):
      raise ValueError("Number of columns does not match schema")
    obj = {}
    for i in range(len(fieldnames)):
      obj[fieldnames[i]] = convert_type(line[i], types[i])
    print json.dumps(obj)

if __name__ == "__main__":
  main()
