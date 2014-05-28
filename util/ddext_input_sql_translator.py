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


  # TODO make RUN function ddext.LIBNAME -> SD['LIBNAME']!!

def make_insert_func(query, funcname, outputrel):

  ret = ''
  if len(_return_names) == 0:
    raise RuntimeError('Cannot have empty return_names!')

  viewname = 'dd_tmp_view_' + funcname
  # remember to drop view
  ret += 'DROP VIEW IF EXISTS '+ viewname + ' CASCADE;\n'
  ret += 'CREATE VIEW ' + viewname + ' AS ' + query + ';\n'

  ret += "INSERT INTO " + outputrel + "(" + \
    ', '.join(_return_names) + """)\nSELECT """
  ret += ', '.join([
    # 'unnest(' + 
    """(__X.__COLUMN).""" + name
     # + ')' 
    for name in _return_names])
  
  # # HEURISTIC: First select--FIRST from is the select content we need
  # # MIGHT BE WRONG!!!
  # query = query.lstrip().rstrip(' \t\n;').lower()
  # # # LAST from
  # # parts = query.rsplit('from', 2)
  # # FIRST from
  # # Python bug: canot accept maxsplit
  # parts = query.split('from', 2) 
  # if len(parts) < 2: # Only select part
  #   print 'WARNING: No FROM clause. Parts:',len(parts)
  #   print parts
  #   rest_part = ''
  # else: 
  #   rest_part = 'from'.join([''] + parts[1:])
  #   # print rest_part
  # subparts = parts[0].split('select', 2)

  # if len(subparts) < 2:
  #   print 'ERROR: SUBPARTS:', '\n\n'.join(subparts), len(subparts)
  #   raise RuntimeError('NO SELECT IN INPUT SQL!')
  # select_content = 'select'.join( subparts[1:] )
  
  # if '"' in select_content:
  #   print >>sys.stderr, 'WARNING: quotation marks "" in input query might cause errors.'

  # if ' as ' in select_content:
  #   print >>sys.stderr, 'WARNING: "AS" in input query might cause errors.'

  # ret += "\nFROM (SELECT "+funcname + "(" + select_content + ") AS __COLUMN \n" + rest_part + ") as __X;"

  # NEW REQUIREMENT: Must alias names as the same to ddext.input of extrctors!!!
  ret += "\nFROM (SELECT "+funcname + "(" + ', '.join(_input_names) + ") AS __COLUMN \n FROM " + viewname + ") as __X;\n"
  ret += 'DROP VIEW IF EXISTS '+ viewname + ' CASCADE;\n'

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
  # print '-----------QUERY:----------'
  # print query

  parse(code, funcname)
  _make_SD()

  # print '============== PG_FUNC: ================'
  parsedcode = make_insert_func(query, funcname, outputrel)

  # print parsedcode

  fout = open(outpath, 'w')
  print >>fout, parsedcode
  fout.close()
