import re
import sys


class TableCell:
  ROW_REGEX = re.compile("(.+)\t(.+)\t(.+)")
  LOC_REGEX = re.compile("table (\d+) page (\d+) row (\d+) col (\d+) to (\d+)")
  
  def __init__(self, table_id, page, row, column_from, column_to, content, coordinates):
    self.table_id = table_id
    self.page = page
    self.row = row
    self.column_from = column_from
    self.column_to = column_to
    self.content = content
    self.coordinates = coordinates
  
  @staticmethod
  def parse(rowstr):
    if TableCell.ROW_REGEX.match(rowstr):
      loc_desc, content, coordinates = TableCell.ROW_REGEX.match(rowstr).groups()
      table_id, page, row, column_from, column_to = map(lambda x: int(x), TableCell.LOC_REGEX.match(loc_desc).groups() )
      return TableCell(table_id, page, row, column_from, column_to, content, coordinates)
    else:
      return None
  
  def __repr__(self):
     return "(Page %d Table %d [%d, %d-%d]): %s" %(self.page, self.table_id, self.row, self.column_to, self.column_to, self.content)