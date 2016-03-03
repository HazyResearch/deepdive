from collections import namedtuple,OrderedDict
import re
import sys
from inspect import isgeneratorfunction,getargspec
from StringIO import StringIO

def print_error(err_string):
  """Function to write to stderr"""
  sys.stderr.write("ERROR[UDF]: " + str(err_string) + "\n")

escapeCodeToSpecial = {
  '\\': '\\',
  'b': '\b',
  'f': '\f',
  'r': '\r',
  't': '\t',
  'n': '\n',
}
specialToEscapeCode = {v: k for k, v in escapeCodeToSpecial.items()}

BOOL_PARSER = {
  't' : True,
  'f' : False,
  'NULL' : None,
  '\\N' : None
}

TYPE_PARSERS = {
  'text' : lambda x : str(x),
  'int' : lambda x : int(x.strip()),
  'float' : lambda x : float(x.strip()),
  'boolean' : lambda x : BOOL_PARSER[x.lower().strip()]
}


def parse_pgtsv_element(s, t, array_nesting_depth=0):
  """
  Parse an element in psql-compatible tsv format, i.e. {-format arrays
  based on provided type and type-parser dictionary
  """
  if s is None:
    return s

  if array_nesting_depth > 0:
    # Quoting only will occur within a psql array with strings
    quoted = (len(s) > 1 and s[0] == '"' and s[-1] == '"')
    if quoted:
      if t == 'text':
        s = s[1:-1]
      else:
        raise Exception("Type mismatch with quoted array element:\n%s" % s)

  if s == '\\N' and array_nesting_depth == 0:
    # Interpret nulls correctly according to postgres convention
    # Note for arrays: {,} --> NULLS, {"",""} --> empty strings
    return None

  elif '[]' in t: # Handle lists recursively
    if s[0] == '{' and s[-1] == '}':
      s_orig = s
      s = s[1:-1] # to strip curly braces
      def unescapeTSVBackslashes(matches):
        c = matches.group(1)
        return escapeCodeToSpecial[c] if c in escapeCodeToSpecial else c
      s = re.sub(r'\\(.)', unescapeTSVBackslashes, s)
      s = re.sub(r'\\(.)', lambda(m): '""' if m.group(1) == '"' else m.group(1), s) # XXX quotes and backslashes in arrays are escaped another time
      values = []
      v = None
      while len(s) > 0:
        if s[0] == ',':  # found the end of a value
          values.append(v)
          v = None
          s = s[1:]
        elif s[0] == '"': # found a quote
          # TODO is_quoted = True and error checking if quoting mixed
          # e.g.: 1,this"is an error",2,3
          if v is None:  # this is a new value
            v = ""
          else:  # this an escaped quote, append to the current value
            v += '"'
          # find the other end of the quote and consume
          m = re.match(r'^"([^"]*)"', s)
          if m:
            v += m.group(1)
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
      values.append(v)
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

def print_pgtsv_element(x, n, t, array_nesting_depth=0):
  """Checks element x against type string t, then prints in PG-TSV format if a match"""
  # Handle NULLs first
  if x is None:
    if array_nesting_depth == 0:
      return '\N'
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
    if array_nesting_depth == 0:
      return x
    else:
      def escapeWithTSVBackslashes(x):
        return re.sub(r'[\b\f\n\r\t\\]', lambda(m): "\\" + specialToEscapeCode[m.group(0)], x)
      if re.search(r'^[a-zA-Z0-9_.\x1c\x1d\x1e\x1f\x7f\[\]()]+$|^[\b]$', x) \
          and x not in ["", "NULL", "null"]:
        # we don't need to quote the value in some special cases
        return escapeWithTSVBackslashes(x)
      else: # otherwise, surround value with quotes
        x = re.sub(r'[\\"]', lambda(m): '\\' +  m.group(0), x) # XXX quotes and backslashes in arrays are escaped another time
        return '"%s"' % escapeWithTSVBackslashes(x) # then, the TSV escaping
  elif t == 'boolean':
    return 't' if x else 'f'
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
