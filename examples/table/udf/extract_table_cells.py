#! /usr/bin/env python

import fileinput
import json
import os
import glob
import sys
import json
from table_cell import TableCell

def debug(str):
  sys.stderr.write("%s\n" % str)

BASE_FOLDER = os.path.split(os.path.realpath(__file__))[0]
BASE_FOLDER = BASE_FOLDER + "/.."
OUTPUT_DIR = BASE_FOLDER + "/pdf_rawtables"
TMP_DIR = BASE_FOLDER + "/tables_tmp"

# Create the temporary output directory if it does not exist
if not os.path.exists(OUTPUT_DIR):
  os.makedirs(OUTPUT_DIR)
if not os.path.exists(TMP_DIR):
  os.makedirs(TMP_DIR)

# Process each file
for row in fileinput.input():
  for filename in [os.path.abspath(f) for f in glob.glob("%s/data/*.pdf" % BASE_FOLDER)]:
    debug("Processing %s" % filename)
    OUTPUT_FILE = "%s/%s.xml" %(OUTPUT_DIR, os.path.basename(filename))
    os.system("java -jar %s/lib/totable.jar %s %s >| %s" %(BASE_FOLDER, filename, TMP_DIR, OUTPUT_FILE))
    # Read the table cells
    with open(OUTPUT_FILE) as f:
      cells = [TableCell.parse(x) for x in f.readlines()]
    # Output one tuple for each table cell
    for tc in [c for c in cells if c != None]:
      print json.dumps({
        "filename": filename,
        "page": tc.page,
        "table_id": tc.table_id,
        "row": tc.row,
        "column_start": tc.column_from,
        "column_end": tc.column_to,
        "content": tc.content,
        "pdf_coordinates": tc.coordinates
      })

