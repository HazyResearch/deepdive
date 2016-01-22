from collections import namedtuple
import re
import sys
from inspect import isgeneratorfunction

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
  quoted = (s[0] == '"' and s[-1] == '"')
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
      split = s[1:-1].split(',')
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
      raise ValueError("Wrong number of attributes for input row:\n%s" % line)
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
    raise Exception("Output column '%(name)s' of type %(declared_type)s has incorrect value of %(value_type)s: '%(value)s'" % {
        name:n, declared_type:t, value_type:type(x), value:x,
    })
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
      raise ValueError("Wrong number of attributes for output row:\n%s" % out_row)
    else:
      print '\t'.join(print_pgtsv_element(x, n, t) for x,(n,t) in zip(out, self.fields))


def tsv_extractor(input_format, output_format, generator):
  """
  This function parses Postgres-style TSV (PGTSV) input rows,
  applies a function which must be a generator object to each row,
  and then checks that each line of this generator is in the output format before
  printing back as PGTSV rows
  """
  # Check generator function
  if not isgeneratorfunction(generator):
    raise ValueError("The generator function must be a *generator* i.e. use yield not return")

  # Create the input parser
  parser = PGTSVParser(input_format)

  # Create the output parser
  printer = PGTSVPrinter(output_format)

  for row in parser.parse_stdin():
    for out_row in generator(**row._asdict()):
      printer.write(out_row)
