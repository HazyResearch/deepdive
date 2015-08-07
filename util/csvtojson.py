#! /usr/bin/env python

import json, sys, csv, re

def convert_flat_type_func(column_type):
  column_type = column_type.lower()
  if column_type == "integer" or column_type == "int" or column_type == "bigint":
    return lambda x: None if x == "" else int(x)
  elif column_type == "float" or column_type == "numeric":
    return lambda x: None if x == "" else float(x)
  elif column_type == "boolean":
    return lambda x: None if x == "" else x == "t"
  elif column_type == "text" or column_type == "unknown":
    return lambda x: x
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
    def backslashes_to_csv_escapes(s):
        return re.sub(r"\\(.)", lambda m: '""' if m.group(1) is '"' else m.group(1), s)
    def convert_text_array_func(value):
      if value == "": return None
      arr = csv.reader([backslashes_to_csv_escapes(value[1:-1])], delimiter=',', quotechar='"').next()
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

# PostgreSQL COPY TO text Format
# See: http://www.postgresql.org/docs/9.1/static/sql-copy.html#AEN64302
pgTextEscapeSeqs = {
    "b": "\b",
    "f": "\f",
    "n": "\n",
    "r": "\r",
    "t": "\t",
    "v": "\v"
    }
def decode_pg_text_escapes(m):
  c = m.group(1)
  if c in pgTextEscapeSeqs:
    return pgTextEscapeSeqs[c]
  elif c.startswith("x"):
    return chr(int(c, base=16))
  elif c.startswith("0"):
    return chr(int(c, base=8))
  else:
    return c
def unescape_postgres_text_format(s):
  # unescape PostgreSQL text format
  return re.sub(r"\\(.|0[0-7]{1,2}|x[0-9A-Fa-f]{1,2})", decode_pg_text_escapes, s)

def main():
  # get column types
  def parseArg(arg):
      field_name, field_type = re.match(r"(.+):([^:]+)", arg).groups()
      return field_name, convert_type_func(field_type)
  names_converters = map(parseArg, sys.argv[1:])

  # read the PostgreSQL COPY TO text format
  # XXX With Python's csv.reader, it's impossible to distinguish empty strings from nulls in PostgreSQL's csv output.
  # In PostgreSQL's csv output, `1,,2.34` means 1, null, 2.34, whereas `1,"",2.34` means 1, empty string, 2.34.
  # csv.reader provides no formatter option to handle this.
  # See: http://grokbase.com/t/python/python-ideas/131b0eaykx/csv-dialect-enhancement
  # See: http://stackoverflow.com/questions/11379300/python-csv-reader-behavior-with-none-and-empty-string
  # See: https://github.com/JoshClose/CsvHelper/issues/252
  reader = csv.reader(sys.stdin, delimiter='\t', quotechar=None, quoting=csv.QUOTE_NONE)
  for line in reader:
    obj = {}
    for (name, convert), field in zip(names_converters, line):
        obj[name] = None if field == "\\N" \
                         else convert(unescape_postgres_text_format(field))
    print json.dumps(obj)

if __name__ == "__main__":
  main()
