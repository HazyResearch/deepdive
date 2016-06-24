# a collection of SQL, PG TSV, CSV, and JSON data that can test a lot of corner case handling

###############################################################################
## a nasty SQL input to test output formatters
NastySQL="
       SELECT 123::bigint as i
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
            , ARRAY[1.2,3.45,67.89] AS float_arr
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
            , ARRAY[ E'\b\b'
                   , E'\f\f'
                   , E'\n\n'
                   , E'\r\r'
                   , E'\t\t'
                   , E'\x1c\x1c'
                   , E'\x1d\x1d'
                   , E'\x1e\x1e'
                   , E'\x1f\x1f'
                   , E'\x7f\x7f'
                   ] AS nonprintable2
            , ARRAY[ E'abc\bdef\bghi'
                   , E'abc\fdef\fghi'
                   , E'\n\n'
                   , E'\r\r'
                   , E'\t\t'
                   , E'\x1c\x1c'
                   , E'\x1d\x1d'
                   , E'\x1e\x1e'
                   , E'\x1f\x1f'
                   , E'\x7f\x7f'
                   ] AS nonprintable3
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
            , ARRAY[ '.'
                   , ','
                   , '.'
                   , '{{'
                   , '}}'
                   , '[['
                   , ']]'
                   , '(('
                   , '))'
                   , E'\\\"'
                   , E'\\\\'
                   ] AS punctuations2
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
                   ] AS torture_arr
    "

# expected TSV output
TSVHeader=                         TSV=
TSVHeader+=$'\t''i'                TSV+=$'\t''123'
TSVHeader+=$'\t''float'            TSV+=$'\t''45.678'
TSVHeader+=$'\t''t'                TSV+=$'\t''t'
TSVHeader+=$'\t''f'                TSV+=$'\t''f'
TSVHeader+=$'\t''s'                TSV+=$'\t''foo bar baz'
TSVHeader+=$'\t''empty_str'        TSV+=$'\t'''
TSVHeader+=$'\t''n'                TSV+=$'\t''\N'
TSVHeader+=$'\t''n1'               TSV+=$'\t''NULL'
TSVHeader+=$'\t''n2'               TSV+=$'\t''null'
TSVHeader+=$'\t''n3'               TSV+=$'\t''\\N'
TSVHeader+=$'\t''n4'               TSV+=$'\t''N'
TSVHeader+=$'\t''num_arr'          TSV+=$'\t''{1,2,3}'
TSVHeader+=$'\t''float_arr'        TSV+=$'\t''{1.2,3.45,67.89}'
TSVHeader+=$'\t''text_arr'         TSV+=$'\t''{easy,123,abc,"two words"}'
TSVHeader+=$'\t''nonprintable'     TSV+=$'\t''{\b,"\f","\n","\r","\t",'$'\x1c'','$'\x1d'',"'$'\x1e'' '$'\x1f''",'$'\x7f''}'
TSVHeader+=$'\t''nonprintable2'    TSV+=$'\t''{\b\b,"\f\f","\n\n","\r\r","\t\t",'$'\x1c'$'\x1c'','$'\x1d'$'\x1d'','$'\x1e'$'\x1e'','$'\x1f'$'\x1f'','$'\x7f'$'\x7f''}'
TSVHeader+=$'\t''nonprintable3'    TSV+=$'\t''{abc\bdef\bghi,"abc\fdef\fghi","\n\n","\r\r","\t\t",'$'\x1c'$'\x1c'','$'\x1d'$'\x1d'','$'\x1e'$'\x1e'','$'\x1f'$'\x1f'','$'\x7f'$'\x7f''}'
TSVHeader+=$'\t''punctuations'     TSV+=$'\t''{.,",",.,"{","}",[,],(,),"\\"","\\\\"}'
TSVHeader+=$'\t''punctuations2'    TSV+=$'\t''{.,",",.,"{{","}}",[[,]],((,)),"\\"","\\\\"}'
TSVHeader+=$'\t''torture_arr'      TSV+=$'\t''{"asdf  qwer\tzxcv\n1234"'
                                        TSV+=',""'
                                        TSV+=',"NULL"'
                                        TSV+=',"null"'
                                        TSV+=',"\\\\N"'
                                        TSV+=',N'
                                        TSV+=',"\\"I'\''m your father,\\" said Darth Vader."'
                                        TSV+=',"{\\"csv in a json\\": \\"a,b c,\\\\\\",\\\\\\",\\\\\\"line '\''1'\''\nbogus,NULL,null,\\\\\\\\N,N,line \\\\\\"\\\\\\"2\\\\\\"\\\\\\"\\",  \\"foo\\":123,\n\\"bar\\":45.678, \\"null\\": \\"\\\\\\\\N\\"}"'
                                        TSV+='}'
TSVHeader=${TSVHeader#$'\t'}       TSV=${TSV#$'\t'}  # strip the first delimiter
NastyTSVHeader=$TSVHeader NastyTSV=$TSV

# column types
Types=
Types+=$'\t''int'
Types+=$'\t''float'
Types+=$'\t''boolean'
Types+=$'\t''boolean'
Types+=$'\t''text'
Types+=$'\t''text'
Types+=$'\t''text'
Types+=$'\t''text'
Types+=$'\t''text'
Types+=$'\t''text'
Types+=$'\t''text'
Types+=$'\t''int[]'
Types+=$'\t''float[]'
Types+=$'\t''text[]'
Types+=$'\t''text[]'
Types+=$'\t''text[]'
Types+=$'\t''text[]'
Types+=$'\t''text[]'
Types+=$'\t''text[]'
Types+=$'\t''text[]'
Types=${Types#$'\t'}
NastyTypes=$Types

# columns names with types
NastyColumnTypes=$(paste <(tr '\t' '\n' <<<"$TSVHeader") <(tr '\t' '\n' <<<"$Types") | tr '\t' :)

# expected CSV output and header
CSVHeader=                     CSV=
CSVHeader+=,'i'                CSV+=,'123'
CSVHeader+=,'float'            CSV+=,'45.678'
CSVHeader+=,'t'                CSV+=,'t'
CSVHeader+=,'f'                CSV+=,'f'
CSVHeader+=,'s'                CSV+=,'foo bar baz'
CSVHeader+=,'empty_str'        CSV+=,'""'
CSVHeader+=,'n'                CSV+=,''
CSVHeader+=,'n1'               CSV+=,'NULL'
CSVHeader+=,'n2'               CSV+=,'null'
CSVHeader+=,'n3'               CSV+=,'\N'
CSVHeader+=,'n4'               CSV+=,'N'
CSVHeader+=,'num_arr'          CSV+=,'"{1,2,3}"'
CSVHeader+=,'float_arr'        CSV+=,'"{1.2,3.45,67.89}"'
CSVHeader+=,'text_arr'         CSV+=,'"{easy,123,abc,""two words""}"'
CSVHeader+=,'nonprintable'     CSV+=,'"{'$'\b'',""'$'\f''"",""'$'\n''"",""'$'\r''"",""'$'\t''"",'$'\x1c'','$'\x1d'',""'$'\x1e'' '$'\x1f''"",'$'\x7f''}"'
CSVHeader+=,'nonprintable2'    CSV+=,'"{'$'\b'$'\b'',""'$'\f'$'\f''"",""'$'\n'$'\n''"",""'$'\r'$'\r''"",""'$'\t'$'\t''"",'$'\x1c'$'\x1c'','$'\x1d'$'\x1d'','$'\x1e'$'\x1e'','$'\x1f'$'\x1f'','$'\x7f'$'\x7f''}"'
CSVHeader+=,'nonprintable3'    CSV+=,'"{abc'$'\b''def'$'\b''ghi,""abc'$'\f''def'$'\f''ghi"",""'$'\n'$'\n''"",""'$'\r'$'\r''"",""'$'\t'$'\t''"",'$'\x1c'$'\x1c'','$'\x1d'$'\x1d'','$'\x1e'$'\x1e'','$'\x1f'$'\x1f'','$'\x7f'$'\x7f''}"'
CSVHeader+=,'punctuations'     CSV+=,'"{.,"","",.,""{"",""}"",[,],(,),""\"""",""\\""}"'
CSVHeader+=,'punctuations2'    CSV+=,'"{.,"","",.,""{{"",""}}"",[[,]],((,)),""\"""",""\\""}"'
CSVHeader+=,'torture_arr'      CSV+=,'"{""asdf  qwer'$'\t''zxcv'$'\n''1234""'
                                 CSV+=',""""'
                                 CSV+=',""NULL""'
                                 CSV+=',""null""'
                                 CSV+=',""\\N""'
                                 CSV+=',N'
                                 CSV+=',""\""I'\''m your father,\"" said Darth Vader.""'
                                 CSV+=',""{\""csv in a json\"": \""a,b c,\\\"",\\\"",\\\""line '\''1'\'$'\n''bogus,NULL,null,\\\\N,N,line \\\""\\\""2\\\""\\\""\"",  \""foo\"":123,'$'\n''\""bar\"":45.678, \""null\"": \""\\\\N\""}""'
                                 CSV+='}"'
CSVHeader=${CSVHeader#,}       CSV=${CSV#,}  # strip the first delimiter
NastyCSVHeader=$CSVHeader NastyCSV=$CSV

# expected JSON output
NastyJSON='
        {
          "i": 123,
          "float": 45.678,
          "t": true,
          "f": false,
          "s": "foo bar baz",
          "empty_str": "",
          "n": null,
          "n1": "NULL",
          "n2": "null",
          "n3": "\\N",
          "n4": "N",
          "num_arr": [
            1,
            2,
            3
          ],
          "float_arr": [
            1.2,
            3.45,
            67.89
          ],
          "text_arr": [
            "easy",
            "123",
            "abc",
            "two words"
          ],
          "nonprintable": [
            "\b",
            "\f",
            "\n",
            "\r",
            "\t",
            "\u001c",
            "\u001d",
            "\u001e \u001f",
            "\u007f"
          ],
          "nonprintable2": [
            "\b\b",
            "\f\f",
            "\n\n",
            "\r\r",
            "\t\t",
            "\u001c\u001c",
            "\u001d\u001d",
            "\u001e\u001e",
            "\u001f\u001f",
            "\u007f\u007f"
          ],
          "nonprintable3": [
            "abc\bdef\bghi",
            "abc\fdef\fghi",
            "\n\n",
            "\r\r",
            "\t\t",
            "\u001c\u001c",
            "\u001d\u001d",
            "\u001e\u001e",
            "\u001f\u001f",
            "\u007f\u007f"
          ],
          "punctuations": [
            ".",
            ",",
            ".",
            "{",
            "}",
            "[",
            "]",
            "(",
            ")",
            "\"",
            "\\"
          ],
          "punctuations2": [
            ".",
            ",",
            ".",
            "{{",
            "}}",
            "[[",
            "]]",
            "((",
            "))",
            "\"",
            "\\"
          ],
          "torture_arr": [
            "asdf  qwer\tzxcv\n1234",
            "",
            "NULL",
            "null",
            "\\N",
            "N",
            "\"I'\''m your father,\" said Darth Vader.",
            "{\"csv in a json\": \"a,b c,\\\",\\\",\\\"line '\''1'\''\nbogus,NULL,null,\\\\N,N,line \\\"\\\"2\\\"\\\"\",  \"foo\":123,\n\"bar\":45.678, \"null\": \"\\\\N\"}"
          ]
        }
    '

###############################################################################
## a case where NULL is in an array
NullInArraySQL="SELECT 1 AS i
                     , ARRAY[''
                            , NULL
                            , 'NULL'
                            , 'null'
                            , E'\\\\N'
                            , 'N'
                            ] AS arr
    "

# expected TSV output for NULL in arrays
TSVHeader=                         TSV=
TSVHeader+=$'\t''i'                TSV+=$'\t''1'
TSVHeader+=$'\t''arr'              TSV+=$'\t''{""'
                                        TSV+=',NULL'
                                        TSV+=',"NULL"'
                                        TSV+=',"null"'
                                        TSV+=',"\\\\N"'
                                        TSV+=',N'
                                        TSV+='}'
TSVHeader=${TSVHeader#$'\t'}       TSV=${TSV#$'\t'}  # strip the first delimiter
NullInArrayTSVHeader=$TSVHeader NullInArrayTSV=$TSV

# column types
Types=
Types+=$'\t''int'
Types+=$'\t''text[]'
Types=${Types#$'\t'}
NullInArrayTypes=$Types

# columns names with types
NullInArrayColumnTypes=$(paste <(tr '\t' '\n' <<<"$TSVHeader") <(tr '\t' '\n' <<<"$Types") | tr '\t' :)

# expected CSV output for NULL in arrays
CSVHeader=                     CSV=
CSVHeader+=,'i'                CSV+=,'1'
CSVHeader+=,'arr'              CSV+=,'"{""""'
                                 CSV+=',NULL'
                                 CSV+=',""NULL""'
                                 CSV+=',""null""'
                                 CSV+=',""\\N""'
                                 CSV+=',N'
                                 CSV+='}"'
CSVHeader=${CSVHeader#,}       CSV=${CSV#,}  # strip the first delimiter
NullInArrayCSVHeader=$CSVHeader NullInArrayCSV=$CSV

NullInArrayJSON='
        { "i": 1,
        "arr": [
            "",
            null,
            "NULL",
            "null",
            "\\N",
            "N"
        ] }
    '

###############################################################################
## a case with nested array
NestedArraySQL="
       SELECT 123::bigint as i
            , ARRAY[ ARRAY[ 'not so easy', '123 45', '789 10' ]
                   , ARRAY[       E'\b\f',  E'\n\r',    E'\t' ]
                   , ARRAY[        '.,\"', '{}[]()',  E'\\\\' ]
                   , ARRAY[            '',     NULL,   'NULL' ]
                   , ARRAY[        'null', E'\\\\N',      'N' ]
                   , ARRAY[ 'asdf  qwer"$'\t'"zxcv"$'\n'"1234'
                          , '\"I''m your father,\" said Darth Vader.'
                          , E'"'{"csv in a json": "a,b c,\\",\\",\\"line '\'\''1'\'\'$'\n''bogus,NULL,null,\\\\N,N,line \\"\\"2\\"\\"",  "foo":123,'$'\n''"bar":45.678, "null": "\\\\N"}'"'
                          ]
                   ] AS text_arr_arr
    "

# expected TSV output
TSVHeader=                         TSV=
TSVHeader+=$'\t''i'                TSV+=$'\t''123'
TSVHeader+=$'\t''text_arr_arr'     TSV+=$'\t''{{"not so easy","123 45","789 10"}'
                                        TSV+=',{"\b\f","\n\r","\t"}'
                                        TSV+=',{".,\\"","{}[]()","\\\\"}'
                                        TSV+=',{"",NULL,"NULL"}'
                                        TSV+=',{"null","\\\\N",N}'
                                        TSV+=',{"asdf  qwer\tzxcv\n1234"'
                                         TSV+=',"\\"I'\''m your father,\\" said Darth Vader."'
                                         TSV+=',"{\\"csv in a json\\": \\"a,b c,\\\\\\",\\\\\\",\\\\\\"line '\''1'\''\nbogus,NULL,null,\\\\\\\\N,N,line \\\\\\"\\\\\\"2\\\\\\"\\\\\\"\\",  \\"foo\\":123,\n\\"bar\\":45.678, \\"null\\": \\"\\\\\\\\N\\"}"'
                                         TSV+='}'
                                        TSV+='}'
TSVHeader=${TSVHeader#$'\t'}       TSV=${TSV#$'\t'}  # strip the first delimiter
NestedArrayTSVHeader=$TSVHeader NestedArrayTSV=$TSV

# column types
Types=
Types+=$'\t''int'
Types+=$'\t''text[][]'
Types=${Types#$'\t'}
NestedArrayTypes=$Types

# columns names with types
NestedArrayTypes=$(paste <(tr '\t' '\n' <<<"$TSVHeader") <(tr '\t' '\n' <<<"$Types") | tr '\t' :)

# expected CSV output and header
CSVHeader=                     CSV=
CSVHeader+=,'i'                CSV+=,'123'
CSVHeader+=,'text_arr_arr'     CSV+=,'"{{""not so easy"",""123 45"",""789 10""}'
                                 CSV+=',{""'$'\b'$'\f''"",""'$'\n'$'\r''"",""'$'\t''""}'
                                 CSV+=',{"".,\"""",""{}[]()"",""\\""}'
                                 CSV+=',{"""",NULL,""NULL""}'
                                 CSV+=',{""null"",""\\N"",N}'
                                 CSV+=',{""asdf  qwer'$'\t''zxcv'$'\n''1234""'
                                  CSV+=',""\""I'\''m your father,\"" said Darth Vader.""'
                                  CSV+=',""{\""csv in a json\"": \""a,b c,\\\"",\\\"",\\\""line '\''1'\'''$'\n''bogus,NULL,null,\\\\N,N,line \\\""\\\""2\\\""\\\""\"",  \""foo\"":123,'$'\n''\""bar\"":45.678, \""null\"": \""\\\\N\""}""'
                                  CSV+='}'
                                 CSV+='}"'
CSVHeader=${CSVHeader#,}       CSV=${CSV#,}  # strip the first delimiter
NestedArrayCSVHeader=$CSVHeader NestedArrayCSV=$CSV

# expected JSON output
NestedArrayJSON='
        {
          "i": 123,
          "text_arr_arr": [ [ "not so easy", "123 45", "789 10" ]
                          , [        "\b\f",   "\n\r",     "\t" ]
                          , [        ".,\"", "{}[]()",     "\\" ]
                          , [            "",     null,   "NULL" ]
                          , [        "null",    "\\N",      "N" ]
                          , [ "asdf  qwer\tzxcv\n1234"
                            , "\"I'\''m your father,\" said Darth Vader."
                            , "{\"csv in a json\": \"a,b c,\\\",\\\",\\\"line '\''1'\''\nbogus,NULL,null,\\\\N,N,line \\\"\\\"2\\\"\\\"\",  \"foo\":123,\n\"bar\":45.678, \"null\": \"\\\\N\"}"
                            ]
          ]
        }
    '

