NastySQL="SELECT 123::bigint as i
            , 45.678 as float
            , TRUE as t
            , FALSE as f
            , 'foo bar baz'::text AS s
            , ''::text AS empty_str
            , NULL::text AS n
            , 'NULL'   AS n1
            , 'null'   AS n2
            , E'\\\\N' AS n3
            , 'N'      AS n4
            , ARRAY[1,2,3] AS num_arr
            , ARRAY[1.2,3.45,67.890] AS float_arr
            , ARRAY[ 'easy'
                   , '123'
                   , 'abc'
                   , 'two words'
                   ] AS text_arr
            , ARRAY[ E'\b'
                   , E'\f'
                   , E'\n'
                   , E'\r'
                   , E'\t'
                   , E'\x1c'
                   , E'\x1d'
                   , E'\x1e \x1f'
                   , E'\x7f'
                   ] AS nonprintable
            , ARRAY[ '.'
                   , ','
                   , '.'
                   , '{'
                   , '}'
                   , '['
                   , ']'
                   , '('
                   , ')'
                   , '\"'
                   , E'\\\\' -- XXX Greenplum doesn't like the simpler '\\'
                   ] AS punctuations
            , ARRAY[ 'asdf  qwer"$'\t'"zxcv"$'\n'"1234'
                   , ''
                   , 'NULL'
                   , 'null'
                   , E'\\\\N'
                   , 'N'
                   , '\"I''m your father,\" said Darth Vader.'
                   , E'"'{"csv in a json": "a,b c,\\",\\",\\"line '\'\''1'\'\'$'\n''bogus,NULL,null,\\\\N,N,line \\"\\"2\\"\\"",  "foo":123,'$'\n''"bar":45.678, "null": "\\\\N"}'"'
                     -- XXX Greenplum (or older PostgreSQL 8.x) treats backslashes as escapes in strings '...'
                     -- and E'...' is a consistent way to write backslashes in string literal across versions
                   ] AS torture_arr"
deepdive sql eval "$NastySQL" format=tsv
