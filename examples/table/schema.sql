DROP TABLE IF EXISTS table_cells CASCADE;
CREATE TABLE table_cells(
  id bigint,
  filename text,
  page int,
  table_id int,
  row int,
  column_start int,
  column_end int,
  content text,
  pdf_coordinates text
)