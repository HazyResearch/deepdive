# DeepDive Table Examples

This example demonstrates how to extract tabular data from PDF documents. The `lib/totable.jar` file uses Computer Vision techniques to recognize tables and outputs XML markup. The DeepDive extractor parses the XML markup and populates a database relation with table data. The schema of the relation is as follows:

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

The `TableCell` in `udf/table_cell.py` can be reused in your own applications to deal with tabular data.

## Running

1. Edit the `env.sh` file according to your sytem and databse configuration.
2. Run `run.sh`

## Notes

This example application does not perform any inference. It's purpose is to show how to use extractors to extract tables from PDF data.