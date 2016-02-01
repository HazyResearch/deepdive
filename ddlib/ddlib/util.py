from collections import namedtuple,OrderedDict
import re
import sys
from inspect import isgeneratorfunction,getargspec
import csv
from StringIO import StringIO

def print_error(err_string):
  """Function to write to stderr"""
  sys.stderr.write("ERROR[UDF]: " + str(err_string) + "\n")


BOOL_PARSER = {
  't' : True,
  'f' : False,
  'NULL' : None,
  '\\N' : None
}

TYPE_PARSERS = {
  'text' : lambda x : str(x.replace('\n', ' ')),
  'int' : lambda x : int(x.strip()),
  'float' : lambda x : float(x.strip()),
  'boolean' : lambda x : BOOL_PARSER(x.lower().strip())
}


def parse_pgtsv_element(s, t, sep='|^|', sep2='|~|', d=0):
  """
  Parse an element in psql-compatible tsv format, i.e. {-format arrays
  based on provided type and type-parser dictionary
  """
  # Quoting only will occur within a psql array with strings
  quoted = (len(s) > 1 and s[0] == '"' and s[-1] == '"')
  if quoted and d > 0:
    if t == 'text':
      s = s[1:-1]
    else:
      raise Exception("Type mismatch with quoted array element:\n%s" % s)
  elif quoted and t != 'text':
    raise Exception("Type mismatch with quoted array element:\n%s" % s)

  # Interpret nulls correctly according to postgres convention
  # Note for arrays: {,} --> NULLS, {"",""} --> empty strings
  if s == '\\N':
    return None
  elif len(s) == 0 and (t != 'text' or (d > 0 and not quoted)):
    return None

  # Handle lists recursively
  elif '[]' in t:
    if s[0] == '{' and s[-1] == '}':
      split = list(csv.reader(StringIO(s[1:-1])))[0]
    else:
      split = s.split(sep)
    return [parse_pgtsv_element(ss, t[:-2], sep=sep2, d=d+1) for ss in split]

  # Else parse using parser
  else:
    try:
      parser = TYPE_PARSERS[t]
    except KeyError:
      raise Exception("Unsupported type: %s" % t)
    return parser(s)


class Row:
  def __str__(self):
    return '<Row(' + ', '.join("%s=%s" % x for x in self.__dict__.iteritems()) + ')>'

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
    self.fields = fields

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
  'boolean' : lambda x : type(x) == bool
}

def print_pgtsv_element(x, n, t, d=0):
  """Checks element x against type string t, then prints in PG-TSV format if a match"""
  # Handle NULLs first
  if x is None:
    if d == 0:
      return '\N'
    else:
      return ''

  # Handle lists recursively
  if '[]' in t:
    if not hasattr(x, '__iter__'):
      raise ValueError("Mismatch between array type and non-iterable in output row:\n%s" % x)
    else:
      return '{%s}' % ','.join(print_pgtsv_element(e, n, t[:-2], d=d+1) for e in x)

  # Else check type & print, hanlding special case of string in array
  try:
    checker = TYPE_CHECKERS[t]
  except KeyError:
    raise Exception("Unsupported type: %s" % t)
  if not checker(x):
    raise Exception("Output column '%(name)s' of type %(declared_type)s has incorrect value of %(value_type)s: '%(value)s'" % dict(
        name=n, declared_type=t, value_type=type(x), value=x,
    ))
  if d > 0 and t == 'text':
    return '"%s"' % str(tok).replace('\\', '\\\\').replace('"', '\\\\"')
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
      print '\t'.join(print_pgtsv_element(x, n, t) for x,(n,t) in zip(out, self.fields))


# how to get types specified as default values of a function
def format_from_args_defaults_of(aFunctionOrFormat):
  if hasattr(aFunctionOrFormat, '__call__'):
    # TODO in Python3, support types in function annotations (PEP 3107: https://www.python.org/dev/peps/pep-3107/)
    spec = getargspec(aFunctionOrFormat)
    return zip(spec.args, spec.defaults)
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
    name_type_pairs = list(name_type_pairs) + name_type_dict.items()
    def decorate(f):
      setattr(f, attrName, name_type_pairs)
      return f
    return decorate
  return decorator
over    = format_decorator("input_format")
returns = format_decorator("output_format")

# decorators that initiate the main extractor loop
def tsv_extractor(generator):
  """
  When a generator function is decorated with this (i.e., @tsv_extractor
  preceding the def line), standard input is parsed as Postgres-style TSV
  (PGTSV) input rows, the function is applied to generate output rows, and then
  checks that each line of this generator is in the output format before
  printing back as PGTSV rows.
  """
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

  # Create the input parser
  parser = PGTSVParser(input_format)

  # Create the output parser
  printer = PGTSVPrinter(output_format)

  for row in parser.parse_stdin():
    for out_row in generator(**row._asdict()):
      printer.write(out_row)
