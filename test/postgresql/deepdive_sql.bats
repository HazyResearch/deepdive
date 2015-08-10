#!/usr/bin/env bats
# Tests for `deepdive sql` command

. "$BATS_TEST_DIRNAME"/env.sh >&2
PATH="$DEEPDIVE_SOURCE_ROOT/util/test:$PATH"

setup() {
    db-execute "SELECT 1" &>/dev/null || db-init
}

@test "$DBVARIANT deepdive sql works" {
    result=$(deepdive sql "
        CREATE TEMP TABLE foo(a INT);
        INSERT INTO foo VALUES (1), (2), (3), (4);
        COPY(SELECT SUM(a) AS sum FROM foo) TO STDOUT
        ")
    [[ $result = 10 ]]
}

@test "$DBVARIANT deepdive sql eval works" {
    [[ $(deepdive sql eval "SELECT 1") = 1 ]]
}

@test "$DBVARIANT deepdive sql fails on bad SQL" {
    ! deepdive sql "
        CREATE;
        INSERT;
        SELECT 1
        "
}

@test "$DBVARIANT deepdive sql stops on error" {
    ! deepdive sql "
        CREATE TEMP TABLE foo(id INT);
        INSERT INTO foo SELECT id FROM __non_existent_table__$$;
        SELECT 1
        "
}

@test "$DBVARIANT deepdive sql eval fails on empty SQL" {
    ! deepdive sql eval ""
}

@test "$DBVARIANT deepdive sql eval fails on bad SQL" {
    ! deepdive sql eval "SELECT FROM WHERE"
}

@test "$DBVARIANT deepdive sql eval fails on non-SELECT SQL" {
    ! deepdive sql eval "CREATE TEMP TABLE foo(id INT)"
}

@test "$DBVARIANT deepdive sql eval fails on multiple SQL" {
    ! deepdive sql eval "CREATE TEMP TABLE foo(id INT); SELECT 1"
}

@test "$DBVARIANT deepdive sql eval fails on trailing semicolon" {
    ! deepdive sql eval "SELECT 1;"
}


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
TSVHeader+=$'\t''float_arr'        TSV+=$'\t''{1.2,3.45,67.890}'
TSVHeader+=$'\t''text_arr'         TSV+=$'\t''{easy,123,abc,"two words"}'
TSVHeader+=$'\t''nonprintable'     TSV+=$'\t''{\b,"\f","\n","\r","\t",'$'\x1c'','$'\x1d'',"'$'\x1e'' '$'\x1f''",'$'\x7f''}'
TSVHeader+=$'\t''punctuations'     TSV+=$'\t''{.,",",.,"{","}",[,],(,),"\\"","\\\\"}'
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
CSVHeader+=,'float_arr'        CSV+=,'"{1.2,3.45,67.890}"'
CSVHeader+=,'text_arr'         CSV+=,'"{easy,123,abc,""two words""}"'
CSVHeader+=,'nonprintable'     CSV+=,'"{'$'\b'',""'$'\f''"",""'$'\n''"",""'$'\r''"",""'$'\t''"",'$'\x1c'','$'\x1d'',""'$'\x1e'' '$'\x1f''"",'$'\x7f''}"'
CSVHeader+=,'punctuations'     CSV+=,'"{.,"","",.,""{"",""}"",[,],(,),""\"""",""\\""}"'
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

@test "$DBVARIANT deepdive sql eval format=tsv works" {
    actual=$(deepdive sql eval "$NastySQL" format=tsv)
    diff -u <(echo "$NastyTSV")                         <(echo "$actual")
}

@test "$DBVARIANT deepdive sql eval format=tsv header=1 works" {
    actual=$(deepdive sql eval "$NastySQL" format=tsv header=1)
    diff -u <(echo "$NastyTSVHeader"; echo "$NastyTSV") <(echo "$actual")
}

@test "$DBVARIANT deepdive sql eval format=csv works" {
    actual=$(deepdive sql eval "$NastySQL" format=csv)
    diff -u <(echo "$NastyCSV")                         <(echo "$actual")
}

@test "$DBVARIANT deepdive sql eval format=csv header=1 works" {
    actual=$(deepdive sql eval "$NastySQL" format=csv header=1)
    diff -u <(echo "$NastyCSVHeader"; echo "$NastyCSV") <(echo "$actual")
}

@test "$DBVARIANT deepdive sql eval format=json works" {
    actual=$(deepdive sql eval "$NastySQL" format=json)
    compare_json "$NastyJSON" "$actual"
}


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

@test "$DBVARIANT deepdive sql eval (with null in arrays) format=tsv works" {
    actual=$(deepdive sql eval "$NullInArraySQL" format=tsv)
    diff -u                               <(echo "$NullInArrayTSV") <(echo "$actual")
}

@test "$DBVARIANT deepdive sql eval (with null in arrays) format=tsv header=1 works" {
    actual=$(deepdive sql eval "$NullInArraySQL" format=tsv header=1)
    diff -u <(echo "$NullInArrayTSVHeader"; echo "$NullInArrayTSV") <(echo "$actual")
}

@test "$DBVARIANT deepdive sql eval (with null in arrays) format=csv works" {
    actual=$(deepdive sql eval "$NullInArraySQL" format=csv)
    diff -u                               <(echo "$NullInArrayCSV") <(echo "$actual")
}

@test "$DBVARIANT deepdive sql eval (with null in arrays) format=csv header=1 works" {
    actual=$(deepdive sql eval "$NullInArraySQL" format=csv header=1)
    diff -u <(echo "$NullInArrayCSVHeader"; echo "$NullInArrayCSV") <(echo "$actual")
}

@test "$DBVARIANT deepdive sql eval (with null in arrays) format=json works" {
    actual=$(deepdive sql eval "$NullInArraySQL" format=json)
    compare_json "$NullInArrayJSON" "$actual"   || skip # XXX not supported by pgtsv_to_json
}


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

@test "$DBVARIANT deepdive sql eval (with nested arrays) format=tsv works" {
    actual=$(deepdive sql eval "$NestedArraySQL" format=tsv)
    diff -u                               <(echo "$NestedArrayTSV") <(echo "$actual")
}

@test "$DBVARIANT deepdive sql eval (with nested arrays) format=tsv header=1 works" {
    skip  # XXX psql does not support HEADER for FORMAT text
    actual=$(deepdive sql eval "$NestedArraySQL" format=tsv header=1)
    diff -u <(echo "$NestedArrayTSVHeader"; echo "$NestedArrayTSV") <(echo "$actual")
}

@test "$DBVARIANT deepdive sql eval (with nested arrays) format=csv works" {
    actual=$(deepdive sql eval "$NestedArraySQL" format=csv)
    diff -u                               <(echo "$NestedArrayCSV") <(echo "$actual")
}

@test "$DBVARIANT deepdive sql eval (with nested arrays) format=csv header=1 works" {
    actual=$(deepdive sql eval "$NestedArraySQL" format=csv header=1)
    diff -u <(echo "$NestedArrayCSVHeader"; echo "$NestedArrayCSV") <(echo "$actual")
}

@test "$DBVARIANT deepdive sql eval (with nested arrays) format=json works" {
    actual=$(deepdive sql eval "$NestedArraySQL" format=json)   || skip # XXX not supported by driver.postgresql/db-query
    compare_json "$NestedArrayJSON" "$actual"
}
