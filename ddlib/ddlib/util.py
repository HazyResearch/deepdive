from collections import namedtuple,OrderedDict
import re
import sys
from inspect import isgeneratorfunction,getargspec
import csv
from io import StringIO
from datetime import datetime
import json

def print_error(err_string):
  """Function to write to stderr"""
  sys.stderr.write("ERROR[UDF]: " + str(err_string) + "\n")

# PostgreSQL COPY TO text Format parser
# See: http://www.postgresql.org/docs/9.1/static/sql-copy.html#AEN64302
escapeCodeToSpecial = {
  '\\': '\\',
  'b': '\b',
  'f': '\f',
  'r': '\r',
  't': '\t',
  'n': '\n',
  'v': '\v',
}
specialToEscapeCode = {v: k for k, v in escapeCodeToSpecial.items()}

def decode_pg_text_escapes(m):
  c = m.group(1)
  if c in escapeCodeToSpecial:
    return escapeCodeToSpecial[c]
  elif c.startswith("x"):
    return chr(int(c, base=16))
  elif c.startswith("0"):
    return chr(int(c, base=8))
  else:
    return c

def unescape_postgres_text_format(s):
  # unescape PostgreSQL text format
  return re.sub(r"\\(.|0[0-7]{1,2}|x[0-9A-Fa-f]{1,2})", decode_pg_text_escapes, s)

BOOL_PARSER = {
  't' : True,
  'f' : False,
  'NULL' : None,
  '\\N' : None
}

def timestamp(timestamp_str):
    """Given a timestamp string, return a timestamp string in ISO 8601 format to emulate
    Postgres 9.5's to_json timestamp formatting.

    This supports the `timestamp without time zone` PostgreSQL type.

    Time zone offsets are not supported. http://bugs.python.org/issue6641

    Examples:

        >>> timestamp('2016-06-17 20:10:38')
        '2016-06-17T20:10:38'

        >>> timestamp('2016-06-17 20:10:37.9293')
        '2016-06-17T20:10:37.929300'

    """

    try:
        parsed = datetime.strptime(timestamp_str, '%Y-%m-%d %H:%M:%S.%f')
    except ValueError:
        parsed = datetime.strptime(timestamp_str, '%Y-%m-%d %H:%M:%S')
    except ValueError:
        return timestamp_str
    return parsed.isoformat()

TYPE_PARSERS = {
  'text' : lambda x : str(x),
  'int' : lambda x : int(x.strip()),
  'float' : lambda x : float(x.strip()),
  'boolean' : lambda x : BOOL_PARSER[x.lower().strip()],
  'timestamp': timestamp,
}

# how to normalize type names
CANONICAL_TYPE_BY_NAME = {
  'integer'          : 'int',
  'bigint'           : 'int',
  'double'           : 'float',
  'double precision' : 'float',
  'numeric'          : 'float',
  'unknown'          : 'text',
  }
CANONICAL_TYPE_BY_REGEX = {
  re.compile(r'timestamp(\(\d\))? without time zone'): 'timestamp',
  }
def normalize_type_name(ty):
  ty = ty.lower()
  if ty.endswith('[]'):
    return normalize_type_name(ty[:-2]) + '[]'
  if ty in CANONICAL_TYPE_BY_NAME:
    return CANONICAL_TYPE_BY_NAME[ty]
  else:
    for patt,ty_canonical in CANONICAL_TYPE_BY_REGEX.items():
      if patt.match(ty):
        return ty_canonical
  return ty
def check_supported_type(nm, ty, array_nesting=0):
  if ty.endswith('[]'):
    if array_nesting == 0:
      check_supported_type(nm, ty[:-2], array_nesting=array_nesting+1)
    else: # XXX the parser cannot parse nested arrays correctly
      raise TypeError("column '%s' is of unsupported nested array type: %s" % (nm, ty + '[]'))
  elif not ty in TYPE_PARSERS:
    raise TypeError("column '%s' is of unsupported type: %s" % (nm, ty))
  return nm, ty

def parse_pgtsv_element(s, t, array_nesting_depth=0):
  """
  Parse an element in psql-compatible tsv format, i.e. {-format arrays
  based on provided type and type-parser dictionary
  """
  if s is None:
    return s

  if array_nesting_depth == 0:
    if s == '\\N':
      # NULLs outside arrays are represented as \N
      # unless specified otherwise in the SQL statement (COPY ... NULL ...)
      return None
    elif not t.endswith('[]'):
      # Need to handle PG TSV escape sequences for primitive types here,
      # escapes for array elements are handled during array parsing
      s = unescape_postgres_text_format(s)

  if t.endswith('[]'): # Handle lists recursively
    if s[0] == '{' and s[-1] == '}':
      s_orig = s
      s = s[1:-1] # to strip curly braces
      def unescapeTSVBackslashes(matches):
        c = matches.group(1)
        return escapeCodeToSpecial[c] if c in escapeCodeToSpecial else c
      s = re.sub(r'\\(.)', unescapeTSVBackslashes, s)
      s = re.sub(r'\\(.)', lambda m : '""' if m.group(1) == '"' else m.group(1), s) # XXX quotes and backslashes in arrays are escaped another time
      values = []
      v = None
      is_quoted = False
      def null_or_itself(v): return None if not is_quoted and v == 'NULL' else v
      while len(s) > 0:
        if s[0] == ',':  # found the end of a value
          values.append(null_or_itself(v))
          v = None
          is_quoted = False
          s = s[1:]
        elif s[0] == '"': # found a quote
          # e.g.: 1,this"is an error",2,3
          if v is None:  # this is a new value
            v = ""
          else:  # this an escaped quote, append to the current value
            v += '"'
          # find the other end of the quote and consume
          m = re.match(r'^"([^"]*)"', s)
          if m:
            v += m.group(1)
            is_quoted = True # TODO error if quoting mixed
            s = s[len(m.group(0)):]
          else:
            raise Exception("Unterminated quote in '%s'" % s_orig)
        else:
          m = re.match(r'^([^,]*)', s)
          if m: # find the next comma to consume up to it
            v = m.group(1)
          else: # or consume the rest of the string as the value
            v = s
          s = s[len(v):]
      values.append(null_or_itself(v))
      split = values
    else:
      raise Exception("Surrounding curly braces ({...}) expected for array type %(type)s but found: '%(value)s'" % dict(
        type=t,
        value=s,
        ))
    return [parse_pgtsv_element(ss, t[:-2], array_nesting_depth=array_nesting_depth+1) for ss in split]

  else: # parse correct value using the parser corresponding to the type
    try:
      parser = TYPE_PARSERS[t]
    except KeyError:
      raise Exception("Unsupported type: %s" % t)
    return parser(s)


class Row:
  def __str__(self):
    return '<Row(' + ', '.join("%s=%s" % x for x in self.__dict__.items()) + ')>'

  def __repr__(self):
    return str(self)

  def _asdict(self):
    return self.__dict__


class PGTSVParser:
  """
  Initialized with a list of duples (field_name, field_type)
  Is a factory for simple Row class
  Parsed from Postgres-style TSV input lines
  """
  def __init__(self, fields):
    self.fields = [check_supported_type(nm,normalize_type_name(ty)) for nm,ty in fields]

  def parse_line(self, line):
    row = Row()
    attribs = line.rstrip().split('\t')
    if len(attribs) != len(self.fields):
      raise ValueError("Expected %(num_rows_declared)d attributes, but found %(num_rows_found)d in input row:\n%(row)s" % dict(
        num_rows_declared=len(self.fields), num_rows_found=len(attribs), row=row,
      ))
    for i,attrib in enumerate(attribs):
      field_name, field_type = self.fields[i]
      setattr(row, field_name, parse_pgtsv_element(attrib, field_type))
    return row

  def parse_stdin(self):
    for line in sys.stdin:
      yield self.parse_line(line)


TYPE_CHECKERS = {
  'text' : lambda x : type(x) == str,
  'int' : lambda x : type(x) == int,
  'float' : lambda x : type(x) == float,
  'boolean' : lambda x : type(x) == bool,
  # TODO timestamp
}

def print_pgtsv_element(x, n, t, array_nesting_depth=0):
  """Checks element x against type string t, then prints in PG-TSV format if a match"""
  # Handle NULLs first
  if x is None:
    if array_nesting_depth == 0:
      return r'\N'
    elif t == 'text':
      return 'NULL'
    else:
      return ''

  # Handle lists recursively
  if '[]' in t:
    if not hasattr(x, '__iter__'):
      raise ValueError("Mismatch between array type and non-iterable in output row:\n%s" % x)
    else:
      return '{%s}' % ','.join(print_pgtsv_element(e, n, t[:-2], array_nesting_depth=array_nesting_depth+1) for e in x)

  # Else check type & print, hanlding special case of string in array
  try:
    checker = TYPE_CHECKERS[t]
  except KeyError:
    raise Exception("Unsupported type: %s" % t)
  if not checker(x):
    raise Exception("Output column '%(name)s' of type %(declared_type)s has incorrect value of %(value_type)s: '%(value)s'" % dict(
        name=n, declared_type=t, value_type=type(x), value=x,
    ))
  if t == 'text':
    x = str(x)
    def escapeWithTSVBackslashes(x):
      return re.sub(r'[\b\f\n\r\t\\]', lambda m : "\\" + specialToEscapeCode[m.group(0)], x)
    if array_nesting_depth == 0:
      # primitive types just need TSV escaping
      return escapeWithTSVBackslashes(x)
    else:
      if re.search(r'^[a-zA-Z0-9_.\b\x1c\x1d\x1e\x1f\x7f\[\]()]+$', x) \
          and x not in ["", "NULL", "null"]:
        # we don't need to quote the value in some special cases
        return escapeWithTSVBackslashes(x)
      else: # otherwise, surround value with quotes
        x = re.sub(r'[\\"]', lambda m : '\\' +  m.group(0), x) # XXX quotes and backslashes in arrays are escaped another time
        return '"%s"' % escapeWithTSVBackslashes(x) # then, the TSV escaping
  elif t == 'boolean':
    return 't' if x else 'f'
  # TODO timestamp
  else:
    return str(x)


class PGTSVPrinter:
  """
  Initialized with a list of type strings
  Prints out Postgres-format TSV output lines
  """
  def __init__(self, fields):
    self.fields = fields

  def write(self, out):
    if len(out) != len(self.fields):
      raise ValueError("Expected %(num_rows_declared)d attributes, but found %(num_rows_found)d in output row:\n%(row)s" % dict(
        num_rows_declared=len(self.fields), num_rows_found=len(out), row=out,
      ))
    else:
      print('\t'.join(print_pgtsv_element(x, n, t) for x,(n,t) in zip(out, self.fields)))


# how to get types specified as default values of a function
def format_from_args_defaults_of(aFunctionOrFormat):
  if hasattr(aFunctionOrFormat, '__call__'):
    # TODO in Python3, support types in function annotations (PEP 3107: https://www.python.org/dev/peps/pep-3107/)
    spec = getargspec(aFunctionOrFormat)
    return list(zip(spec.args, spec.defaults))
  else:
    return aFunctionOrFormat


## function decorators to be used directly in UDF implementations

# decorators for input and output formats
def format_decorator(attrName):
  def decorator(*name_type_pairs, **name_type_dict):
    """
    When a function is decorated with this (e.g., @returns(...) or @over(...)
    preceding the def line), the pairs of column name and type given as
    arguments are kept as the function's attribute to supply other decorators,
    such as @tsv_extractor, with information for deciding how to parse the
    input lines or format the output lines.
    """
    # check single argument case with a function or dict
    if len(name_type_pairs) == 1:
      if hasattr(name_type_pairs[0], '__call__'):
        name_type_pairs = format_from_args_defaults_of(name_type_pairs[0])
      elif type(name_type_pairs[0]) in [dict, OrderedDict]:
        name_type_pairs = name_type_pairs[0]
        # XXX @over(collection.OrderedDict(foo="type", bar="type", ...)) doesn't work
        # as Python forgets the order when calling with keyword argument binding.
    # merge dictionaries
    name_type_pairs = list(name_type_pairs) + list(name_type_dict.items())
    def decorate(f):
      setattr(f, attrName, name_type_pairs)
      return f
    return decorate
  return decorator
over    = format_decorator("input_format")
returns = format_decorator("output_format")

def get_generator_format(generator):
  # Expects the input and output formats to have been decorated with @over and @returns
  try:
    # @over has precedence over default values of function arguments
    input_format = generator.input_format
  except AttributeError:
    input_format = format_from_args_defaults_of(generator)
  try:
    output_format = generator.output_format
    # also support function argument defaults for output_format for symmetry
    output_format = format_from_args_defaults_of(output_format)
  except AttributeError:
    raise ValueError("The function must be decorated with @returns")
    # TODO or maybe just skip type checking if @returns isn't present?
  # Check generator function
  if not isgeneratorfunction(generator):
    raise ValueError("The function must be a *generator*, i.e., use yield not return")

  return input_format, output_format

# decorators that initiate the main extractor loop
def tsv_extractor(generator):
  """
  When a generator function is decorated with this (i.e., @tsv_extractor
  preceding the def line), standard input is parsed as Postgres-style TSV
  (PGTSV) input rows, the function is applied to generate output rows, and then
  checks that each line of this generator is in the output format before
  printing back as PGTSV rows.
  """
  input_format, output_format = get_generator_format(generator)

  # Create the input parser
  parser = PGTSVParser(input_format)

  # Create the output parser
  printer = PGTSVPrinter(output_format)

  for row in parser.parse_stdin():
    for out_row in generator(**row._asdict()):
      printer.write(out_row)


def tsj_extractor(generator):
  """
  When a generator function is decorated with this (i.e., @tsj_extractor
  preceding the def line), each standard input line is parsed as
  tab-separated JSON (TSJ) values, then the function is applied to the parsed
  array to generate output rows, and each output row expected to be an array
  is formatted as TSJ.
  """
  try: # For Python 2, set default encoding to UTF-8
      reload(sys).setdefaultencoding("utf8") # to avoid UnicodeEncodeError of JSON values during conversion by str()
  except:
      pass # Python 3 raises an exception as reload() is not available

  input_format, output_format = get_generator_format(generator)
  input_names  = [name for name,t in input_format]
  num_input_values = len(input_format)
  num_input_splits = num_input_values - 1
  num_output_values = len(output_format)

  def parse_json(column_index, json_value):
    try:
      return json.loads(json_value)
    except ValueError as exc:
      raise ValueError("JSON parse error in column %d (%s):\n  %s\n" % (column_index, exc, json_value))

  for line in sys.stdin:
    try:
      columns = line.rstrip("\n").split("\t", num_input_splits)
      assert len(columns) == num_input_values
      values_in = (parse_json(i,v) for i,v in enumerate(columns))
      input_dict = dict(zip(input_names, values_in))
    except ValueError as exc:
      raise ValueError("could not parse TSJ line:\n  %s\ndue to %s" % (line, exc))
    for values_out in generator(**input_dict):
      if len(values_out) == num_output_values:
        for i,v in enumerate(values_out):
          if i > 0: sys.stdout.write("\t")
          sys.stdout.write(json.dumps(v))
        sys.stdout.write("\n")
      else:
        raise ValueError("Expected %d values but got %d\n  input: %s\n output: %s" % (
          num_output_values, len(values_out),
          json.dumps(values_in), json.dumps(values_out)))
