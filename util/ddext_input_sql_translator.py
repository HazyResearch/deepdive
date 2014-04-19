#! /usr/bin/env python

import sys, re

_init_called = False

_libraries = []
_input_names = []
_input_types = []

_return_names = []
_return_types = []

_run_func_content = ''
_init_func_content = ''
_ext_name = ''

# Add a library (should be installed on machines within GP)
def import_lib(libname):
  global _libraries
  _libraries.append(libname)


# Add an input variable and its data type
def input(name, datatype):
  global _input_names, _input_types
  if name in _input_names:
    raise RuntimeError('Input variable',name,'is added multiple times!')
    
  _input_names.append(name)
  _input_types.append(datatype)

def returns(name, datatype):
  global _return_names, _return_types
  if name in _return_names:
    raise RuntimeError('Return variable',name,'is added multiple times!')
    
  _return_names.append(name)
  _return_types.append(datatype)

# def debug_init(init):
#   init()

# Match a single function with certain name
def _match_function(code, function_name):
  match = re.search(r'def\s+'+function_name+'.*\n.*', code, flags=re.DOTALL | re.M)
  func_full = match.group()
  start_index = re.search(r':|\n', func_full).end() + 1
  end_index = re.search(r'\ndef|$', func_full).start()  # TODO Up to the next function or to the end.
  func_content = func_full[start_index : end_index]

  if re.search(r'\bprint\b', func_content) != None:
    print >>sys.stderr, 'WARNING: cannot use print function in extractors. Commented out print sentences.'
    func_content = re.sub(r'\bprint\b', '# print', func_content)

  return func_content


# Heuristics:
# 1. def must be in a single line
def parse(ext_code, ext_name):
  # Match till the end
  
  global _run_func_content, _init_func_content
  global _ext_name
  _run_func_content = _match_function(ext_code, 'run')
  _init_func_content = _match_function(ext_code, 'init')
  _ext_name = ext_name

  match = re.search(r'def\s+init.*\n.*', ext_code, flags=re.DOTALL | re.M)
  func_full = match.group()
  start_index = re.search(r':|\n', func_full).end() + 1
  end_index = re.search(r'\ndef|$', func_full).start()
  func_content = func_full[start_index : end_index]

def _make_SD():
  init_func_exec = re.sub(r'[ \t]*ddext\.', '', _init_func_content)
  exec(init_func_exec)

  # TODO make RUN function ddext.LIBNAME -> SD['LIBNAME']!!

def make_insert_func(query, funcname, outputrel):

  ret = ''
  if len(_return_names) == 0:
    raise RuntimeError('Cannot have empty return_names!')
  ret += "INSERT INTO " + outputrel + "(" + \
    ', '.join(_return_names) + """)\nSELECT """
  ret += ', '.join(["""(__X.__COLUMN).""" + name for name in _return_names])
  
  # HEURISTIC: First select--Last from is the select content we need
  query = query.lstrip().rstrip(' \t\n;').lower()
  parts = query.rsplit('from', 2)
  if len(parts) != 2: # Only select part
    rest_part = ''
  else: 
    rest_part = 'from ' + parts[1]
  subparts = parts[0].split('select', 2)
  if len(subparts) != 2:
    raise RuntimeError('NO SELECT IN INPUT SQL!')
  select_content = subparts[1]
  
  if '"' in select_content:
    print >>sys.stderr, 'WARNING: quotation marks "" in input query might cause errors.'

  if ' as ' in select_content:
    print >>sys.stderr, 'WARNING: "AS" in input query might cause errors.'

  ret += "\nFROM (SELECT "+funcname + "(" + select_content + ") AS __COLUMN \n" + rest_part + ") as __X;"


  return ret


if __name__ == '__main__':
  if len(sys.argv) == 6:
    udfcodepath = sys.argv[1]
    querypath = sys.argv[2]
    outputrel = sys.argv[3]
    funcname = sys.argv[4]
    outpath = sys.argv[5]
  else:
    print >>sys.stderr, 'Usage:',sys.argv[0],'inputQueryFile udfFile outputRel funcName sqlInsertFile'
    sys.exit(1)

  code = open(udfcodepath).read()
  query = open(querypath).read().strip().rstrip(';')
  print '-----------QUERY:----------'
  print query

  parse(code, funcname)
  _make_SD()

  # print '============== PG_FUNC: ================'
  parsedcode = make_insert_func(query, funcname, outputrel)

  # print parsedcode

  fout = open(outpath, 'w')
  print >>fout, parsedcode
  fout.close()
