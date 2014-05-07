#! /usr/bin/env python

import sys, re

_libraries = [] # (X,Y,Z): from Y import X as Z
_input_names = []
_input_types = []

_return_names = []
_return_types = []

_run_func_content = ''
_init_func_content = ''
_ext_name = ''

# Add a library (should be installed on machines within GP)
# Sample Usage:
#   import_lib(X, Y, Z): from Z import X as Y
#   import_lib(X, Y): from Y import X
#   import_lib(X, as_name=Z): import X as Z
#   import_lib(X): import X
def import_lib(libname, from_package=None, as_name=None):
  global _libraries
  # (X,Y,Z): from Z import X as Y
  _libraries.append((libname, from_package, as_name))

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

  # This code seems not working....
  # # Do not allow 'x = 5' for input argument x's
  # for var in _input_names:
  #   badmatch = re.search(r'\b' + var + r'\b\s*=\s*\b', _run_func_content)
  #   if badmatch != None:
  #     print >>sys.stderr, 'WARNING: PlPy do not allow assignments to input arguments.'
  #     print >>sys.stderr, 'CONTEXT:', _run_func_content[badmatch.start():badmatch.start() + 30] + '...'



def _make_SD():
  init_func_exec = re.sub(r'[ \t]*ddext\.', '', _init_func_content)
  init_func_exec = re.sub(r'\n[ \t]*', '\n', init_func_exec)

  # print >>sys.stderr, init_func_exec
  try:
    exec(init_func_exec)
  except:
    print >>sys.stderr, "ERROR: cannot parse init function. Try to remove comments and extra lines in function init()."
    print >>sys.stderr, init_func_exec
    sys.exit(1)

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
    + _return_types[i] 
    # + ' []'   # previous version
    for i in range(len(_return_names))]) \
    + ');\n'

  
  # Create the function.
  ret += 'CREATE OR REPLACE FUNCTION ' + func_name + '''(
    ''' + ', '.join([ _input_names[i] + ' ' \
    + _input_types[i] for i in range(len(_input_names))]) \
    + ''') RETURNS SETOF ''' + ret_type_name + ' AS\n$$\n';

  # Import Libraries
  for lib in _libraries:
    libname, from_package, as_name = lib
    as_str = ''
    from_str = ''
    varname = libname
    if as_name != None:
      as_str = ' as ' + as_name
      varname = as_name
    if from_package != None:
      from_str = 'from ' + from_package + ' '


    ret += """
  if '"""+ varname +"""' in SD: 
    """+ varname +""" = SD['"""+ varname +"""']
  else: 
    """+ from_str + """import """+ libname + as_str +"""
    SD['"""+ varname +"""'] = """+ varname + '\n'

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
