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

def make_pg_func():

  ret_type_name = 'ret_' + _ext_name
  func_name = _ext_name

  ret = ''

  if len(_return_names) == 0:
    raise RuntimeError('Cannot have empty return_names!')

  # Create a return type. e.g.:
  # CREATE TYPE greeting AS (how text, who text);
  ret += 'DROP TYPE IF EXISTS ' + ret_type_name + ' CASCADE;\n'
  ret += 'CREATE TYPE ' + ret_type_name + ' AS (' \
    + ', '.join([ _return_names[i] + ' ' \
    + _return_types[i] for i in range(len(_return_names))]) \
    + ');\n'

  
  # Create the function.
  ret += 'CREATE OR REPLACE FUNCTION ' + func_name + '''(
    ''' + ', '.join([ _input_names[i] + ' ' \
    + _input_types[i] for i in range(len(_input_names))]) \
    + ''') RETURNS SETOF ''' + ret_type_name + ' AS\n$$\n';

  # Import Libraries
  for lib in _libraries:
    ret += """
  if '"""+ lib +"""' in SD: 
    """+ lib +""" = SD['"""+ lib +"""']
  else: 
    import """+ lib +"""
    SD['"""+ lib +"""'] = """+ lib + '\n'

  # TODO now we do not need library name...
  # _run_func_content = re.sub(r'ddext\.', '', _run_func_content)

  ret += '\n' + _run_func_content \
    + '\n$$ LANGUAGE plpythonu ;'

  return ret

if __name__ == '__main__':
  if len(sys.argv) == 4:
    path = sys.argv[1]
    outpath = sys.argv[2]
    funcname = sys.argv[3]
  else:
    print >>sys.stderr, 'Usage:',sys.argv[0],'<codePath> <outPath> <funcName>'
    sys.exit(1)

  code = open(path).read()

  parse(code, funcname)
  # print >>sys.stderr, '============== RUN: ================'
  # print >>sys.stderr, _run_func_content

  _make_SD()

  # print '============== PG_FUNC: ================'
  parsedcode = make_pg_func()
  fout = open(outpath, 'w')
  print >>fout, parsedcode
  fout.close()
