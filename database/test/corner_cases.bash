# a collection of SQL, PG TSV, CSV, and JSON data that can test a lot of corner case handling

# a shorthand to zip column names and types with ':' and proper escapes for shell (types can have spaces!)
namesAndTypes() {
    local TSVHeader=$1 Types=$2
    paste <(tr '\t' '\n' <<<"$TSVHeader") <(tr '\t' '\n' <<<"$Types") |
    tr '\t' : |
    sed "s/^/'/g; s/\$/'/g"
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
                   , E'\\\\\"'
                   , E'\\\\\\\\\"'
                   , E'\\\\'
                   ] AS punctuations2
            , ARRAY[ 'asdf  qwer"$'\t'"zxcv"$'\n'"1234'
                   , ''
                   , NULL
                   , 'NULL'
                   , 'null'
                   , E'\\\\N'
                   , E'\\\\\\\\N'
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
TSVHeader+=$'\t''punctuations2'    TSV+=$'\t''{.,",",.,"{{","}}",[[,]],((,)),"\\"","\\\\\\"","\\\\\\\\\\"","\\\\"}'
TSVHeader+=$'\t''torture_arr'      TSV+=$'\t''{"asdf  qwer\tzxcv\n1234"'
                                        TSV+=',""'
                                        TSV+=',NULL'
                                        TSV+=',"NULL"'
                                        TSV+=',"null"'
                                        TSV+=',"\\\\N"'
                                        TSV+=',"\\\\\\\\N"'
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
NastyColumnTypes=$(namesAndTypes "$TSVHeader" "$Types")

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
CSVHeader+=,'punctuations2'    CSV+=,'"{.,"","",.,""{{"",""}}"",[[,]],((,)),""\"""",""\\\"""",""\\\\\"""",""\\""}"'
CSVHeader+=,'torture_arr'      CSV+=,'"{""asdf  qwer'$'\t''zxcv'$'\n''1234""'
                                 CSV+=',""""'
                                 CSV+=',NULL'
                                 CSV+=',""NULL""'
                                 CSV+=',""null""'
                                 CSV+=',""\\N""'
                                 CSV+=',""\\\\N""'
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
            "\\\"",
            "\\\\\"",
            "\\"
          ],
          "torture_arr": [
            "asdf  qwer\tzxcv\n1234",
            "",
            null,
            "NULL",
            "null",
            "\\N",
            "\\\\N",
            "N",
            "\"I'\''m your father,\" said Darth Vader.",
            "{\"csv in a json\": \"a,b c,\\\",\\\",\\\"line '\''1'\''\nbogus,NULL,null,\\\\N,N,line \\\"\\\"2\\\"\\\"\",  \"foo\":123,\n\"bar\":45.678, \"null\": \"\\\\N\"}"
          ]
        }
    '

tsjFromJsonAndHeader() {
    local json=$1; shift
    local columnNames="$*" columnName=
    local jqColumns=; for columnName in $columnNames; do jqColumns+=", .$columnName"; done
    jq -r '['"${jqColumns#, }"'] | map(@json) | join("\t")' <<<"$json"
}
NastyTSJ=$(tsjFromJsonAndHeader "$NastyJSON" "$NastyTSVHeader")

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
                     , ARRAY[ NULL
                            , 0
                            , NULL
                            , 1
                            ] AS int_arr
                     , ARRAY[ NULL
                            , 0.1
                            , NULL
                            , 2.34
                            , NULL
                            , 5.678
                            ] AS float_arr
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
TSVHeader+=$'\t''int_arr'          TSV+=$'\t''{NULL'
                                        TSV+=',0'
                                        TSV+=',NULL'
                                        TSV+=',1'
                                        TSV+='}'
TSVHeader+=$'\t''float_arr'        TSV+=$'\t''{NULL'
                                        TSV+=',0.1'
                                        TSV+=',NULL'
                                        TSV+=',2.34'
                                        TSV+=',NULL'
                                        TSV+=',5.678'
                                        TSV+='}'
TSVHeader=${TSVHeader#$'\t'}       TSV=${TSV#$'\t'}  # strip the first delimiter
NullInArrayTSVHeader=$TSVHeader NullInArrayTSV=$TSV

# column types
Types=
Types+=$'\t''int'
Types+=$'\t''text[]'
Types+=$'\t''int[]'
Types+=$'\t''float[]'
Types=${Types#$'\t'}
NullInArrayTypes=$Types

# columns names with types
NullInArrayColumnTypes=$(namesAndTypes "$TSVHeader" "$Types")

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
CSVHeader+=,'int_arr'          CSV+=,'"{NULL'
                                 CSV+=',0'
                                 CSV+=',NULL'
                                 CSV+=',1'
                                 CSV+='}"'
CSVHeader+=,'float_arr'        CSV+=,'"{NULL'
                                 CSV+=',0.1'
                                 CSV+=',NULL'
                                 CSV+=',2.34'
                                 CSV+=',NULL'
                                 CSV+=',5.678'
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
        ],
        "int_arr": [
            null,
            0,
            null,
            1
        ],
        "float_arr": [
            null,
            0.1,
            null,
            2.34,
            null,
            5.678
        ] }
    '
NullInArrayTSJ=$(tsjFromJsonAndHeader "$NullInArrayJSON" "$NullInArrayTSVHeader")

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
NestedArrayColumnTypes=$(namesAndTypes "$TSVHeader" "$Types")

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
NestedArrayTSJ=$(tsjFromJsonAndHeader "$NestedArrayJSON" "$NestedArrayTSVHeader")

###############################################################################
## a case with unicode escapes

# UTF-8 Sampler from http://www.columbia.edu/~fdc/utf8/
ICanEatGlass=()
ICanEatGlass+=("Sanskrit: ﻿काचं शक्नोम्यत्तुम् । नोपहिनस्ति माम् ॥")
ICanEatGlass+=("Sanskrit (standard transcription): kācaṃ śaknomyattum; nopahinasti mām.")
ICanEatGlass+=("Classical Greek: ὕαλον ϕαγεῖν δύναμαι· τοῦτο οὔ με βλάπτει.")
ICanEatGlass+=("Greek (monotonic): Μπορώ να φάω σπασμένα γυαλιά χωρίς να πάθω τίποτα.")
ICanEatGlass+=("Greek (polytonic): Μπορῶ νὰ φάω σπασμένα γυαλιὰ χωρὶς νὰ πάθω τίποτα. ")
ICanEatGlass+=("Latin: Vitrum edere possum; mihi non nocet.")
ICanEatGlass+=("Old French: Je puis mangier del voirre. Ne me nuit.")
ICanEatGlass+=("French: Je peux manger du verre, ça ne me fait pas mal.")
ICanEatGlass+=("Provençal / Occitan: Pòdi manjar de veire, me nafrariá pas.")
ICanEatGlass+=("Québécois: J'peux manger d'la vitre, ça m'fa pas mal.")
ICanEatGlass+=("Walloon: Dji pou magnî do vêre, çoula m' freut nén må. ")
ICanEatGlass+=("Picard: Ch'peux mingi du verre, cha m'foé mie n'ma. ")
ICanEatGlass+=("Kreyòl Ayisyen (Haitï): Mwen kap manje vè, li pa blese'm.")
ICanEatGlass+=("Basque: Kristala jan dezaket, ez dit minik ematen.")
ICanEatGlass+=("Catalan / Català: Puc menjar vidre, que no em fa mal.")
ICanEatGlass+=("Spanish: Puedo comer vidrio, no me hace daño.")
ICanEatGlass+=("Aragonés: Puedo minchar beire, no me'n fa mal . ")
ICanEatGlass+=("Galician: Eu podo xantar cristais e non cortarme.")
ICanEatGlass+=("European Portuguese: Posso comer vidro, não me faz mal.")
ICanEatGlass+=("Brazilian Portuguese (8): Posso comer vidro, não me machuca.")
ICanEatGlass+=("Caboverdiano/Kabuverdianu (Cape Verde): M' podê cumê vidru, ca ta maguâ-m'.")
ICanEatGlass+=("Papiamentu: Ami por kome glas anto e no ta hasimi daño.")
ICanEatGlass+=("Italian: Posso mangiare il vetro e non mi fa male.")
ICanEatGlass+=("Milanese: Sôn bôn de magnà el véder, el me fa minga mal.")
ICanEatGlass+=("Roman: Me posso magna' er vetro, e nun me fa male.")
ICanEatGlass+=("Napoletano: M' pozz magna' o'vetr, e nun m' fa mal.")
ICanEatGlass+=("Venetian: Mi posso magnare el vetro, no'l me fa mae.")
ICanEatGlass+=("Zeneise (Genovese): Pòsso mangiâ o veddro e o no me fà mâ.")
ICanEatGlass+=("Sicilian: Puotsu mangiari u vitru, nun mi fa mali. ")
ICanEatGlass+=("Romansch (Grischun): Jau sai mangiar vaider, senza che quai fa donn a mai. ")
ICanEatGlass+=("Romanian: Pot să mănânc sticlă și ea nu mă rănește.")
ICanEatGlass+=("Esperanto: Mi povas manĝi vitron, ĝi ne damaĝas min. ")
ICanEatGlass+=("Cornish: Mý a yl dybry gwéder hag éf ny wra ow ankenya.")
ICanEatGlass+=("Welsh: Dw i'n gallu bwyta gwydr, 'dyw e ddim yn gwneud dolur i mi.")
ICanEatGlass+=("Manx Gaelic: Foddym gee glonney agh cha jean eh gortaghey mee.")
ICanEatGlass+=("Old Irish (Ogham): ᚛᚛ᚉᚑᚅᚔᚉᚉᚔᚋ ᚔᚈᚔ ᚍᚂᚐᚅᚑ ᚅᚔᚋᚌᚓᚅᚐ᚜")
ICanEatGlass+=("Old Irish (Latin): Con·iccim ithi nglano. Ním·géna.")
ICanEatGlass+=("Irish: Is féidir liom gloinne a ithe. Ní dhéanann sí dochar ar bith dom.")
ICanEatGlass+=("Ulster Gaelic: Ithim-sa gloine agus ní miste damh é.")
ICanEatGlass+=("Scottish Gaelic: S urrainn dhomh gloinne ithe; cha ghoirtich i mi.")
ICanEatGlass+=("Anglo-Saxon (Runes): ᛁᚳ᛫ᛗᚨᚷ᛫ᚷᛚᚨᛋ᛫ᛖᚩᛏᚪᚾ᛫ᚩᚾᛞ᛫ᚻᛁᛏ᛫ᚾᛖ᛫ᚻᛖᚪᚱᛗᛁᚪᚧ᛫ᛗᛖ᛬")
ICanEatGlass+=("Anglo-Saxon (Latin): Ic mæg glæs eotan ond hit ne hearmiað me.")
ICanEatGlass+=("Middle English: Ich canne glas eten and hit hirtiþ me nouȝt.")
ICanEatGlass+=("English: I can eat glass and it doesn't hurt me.")
ICanEatGlass+=("English (IPA): [aɪ kæn iːt glɑːs ænd ɪt dɐz nɒt hɜːt miː] (Received Pronunciation)")
ICanEatGlass+=("English (Braille): ⠊⠀⠉⠁⠝⠀⠑⠁⠞⠀⠛⠇⠁⠎⠎⠀⠁⠝⠙⠀⠊⠞⠀⠙⠕⠑⠎⠝⠞⠀⠓⠥⠗⠞⠀⠍⠑")
ICanEatGlass+=("Jamaican: Mi kian niam glas han i neba hot mi.")
ICanEatGlass+=("Lalland Scots / Doric: Ah can eat gless, it disnae hurt us. ")
ICanEatGlass+=("Gothic (4): ЌЌЌ ЌЌЌЍ Ќ̈ЍЌЌ, ЌЌ ЌЌЍ ЍЌ ЌЌЌЌ ЌЍЌЌЌЌЌ.")
ICanEatGlass+=("Old Norse (Runes): ᛖᚴ ᚷᛖᛏ ᛖᛏᛁ ᚧ ᚷᛚᛖᚱ ᛘᚾ ᚦᛖᛋᛋ ᚨᚧ ᚡᛖ ᚱᚧᚨ ᛋᚨᚱ")
ICanEatGlass+=("Old Norse (Latin): Ek get etið gler án þess að verða sár.")
ICanEatGlass+=("Norsk / Norwegian (Nynorsk): Eg kan eta glas utan å skada meg.")
ICanEatGlass+=("Norsk / Norwegian (Bokmål): Jeg kan spise glass uten å skade meg.")
ICanEatGlass+=("Føroyskt / Faroese: Eg kann eta glas, skaðaleysur.")
ICanEatGlass+=("Íslenska / Icelandic: Ég get etið gler án þess að meiða mig.")
ICanEatGlass+=("Svenska / Swedish: Jag kan äta glas utan att skada mig.")
ICanEatGlass+=("Dansk / Danish: Jeg kan spise glas, det gør ikke ondt på mig.")
ICanEatGlass+=("Sønderjysk: Æ ka æe glass uhen at det go mæ naue.")
ICanEatGlass+=("Frysk / Frisian: Ik kin glês ite, it docht me net sear.")
ICanEatGlass+=("Nederlands / Dutch: Ik kan glas eten, het doet mĳ geen kwaad.")
ICanEatGlass+=("Kirchröadsj/Bôchesserplat: Iech ken glaas èèse, mer 't deet miech jing pieng.")
ICanEatGlass+=("Afrikaans: Ek kan glas eet, maar dit doen my nie skade nie.")
ICanEatGlass+=("Lëtzebuergescht / Luxemburgish: Ech kan Glas iessen, daat deet mir nët wei.")
ICanEatGlass+=("Deutsch / German: Ich kann Glas essen, ohne mir zu schaden.")
ICanEatGlass+=("Ruhrdeutsch: Ich kann Glas verkasematuckeln, ohne dattet mich wat jucken tut.")
ICanEatGlass+=("Langenfelder Platt: Isch kann Jlaas kimmeln, uuhne datt mich datt weh dääd.")
ICanEatGlass+=("Lausitzer Mundart ("Lusatian"): Ich koann Gloos assn und doas dudd merr ni wii.")
ICanEatGlass+=("Odenwälderisch: Iech konn glaasch voschbachteln ohne dass es mir ebbs daun doun dud.")
ICanEatGlass+=("Sächsisch / Saxon: 'sch kann Glos essn, ohne dass'sch mer wehtue.")
ICanEatGlass+=("Pfälzisch: Isch konn Glass fresse ohne dasses mer ebbes ausmache dud.")
ICanEatGlass+=("Schwäbisch / Swabian: I kå Glas frässa, ond des macht mr nix!")
ICanEatGlass+=("Deutsch (Voralberg): I ka glas eassa, ohne dass mar weh tuat.")
ICanEatGlass+=("Bayrisch / Bavarian: I koh Glos esa, und es duard ma ned wei.")
ICanEatGlass+=("Allemannisch: I kaun Gloos essen, es tuat ma ned weh.")
ICanEatGlass+=("Schwyzerdütsch (Zürich): Ich chan Glaas ässe, das schadt mir nöd.")
ICanEatGlass+=("Schwyzerdütsch (Luzern): Ech cha Glâs ässe, das schadt mer ned. ")
ICanEatGlass+=("Hungarian: Meg tudom enni az üveget, nem lesz tőle bajom.")
ICanEatGlass+=("Suomi / Finnish: Voin syödä lasia, se ei vahingoita minua.")
ICanEatGlass+=("Sami (Northern): Sáhtán borrat lása, dat ii leat bávččas.")
ICanEatGlass+=("Erzian: Мон ярсан суликадо, ды зыян эйстэнзэ а ули.")
ICanEatGlass+=("Northern Karelian: Mie voin syvvä lasie ta minla ei ole kipie.")
ICanEatGlass+=("Southern Karelian: Minä voin syvvä st'oklua dai minule ei ole kibie. ")
ICanEatGlass+=("Estonian: Ma võin klaasi süüa, see ei tee mulle midagi.")
ICanEatGlass+=("Latvian: Es varu ēst stiklu, tas man nekaitē.")
ICanEatGlass+=("Lithuanian: Aš galiu valgyti stiklą ir jis manęs nežeidžia ")
ICanEatGlass+=("Czech: Mohu jíst sklo, neublíží mi.")
ICanEatGlass+=("Slovak: Môžem jesť sklo. Nezraní ma.")
ICanEatGlass+=("Polska / Polish: Mogę jeść szkło i mi nie szkodzi.")
ICanEatGlass+=("Slovenian: Lahko jem steklo, ne da bi mi škodovalo.")
ICanEatGlass+=("Bosnian, Croatian, Montenegrin and Serbian (Latin): Ja mogu jesti staklo, i to mi ne šteti.")
ICanEatGlass+=("Bosnian, Montenegrin and Serbian (Cyrillic): Ја могу јести стакло, и то ми не штети.")
ICanEatGlass+=("Macedonian: Можам да јадам стакло, а не ме штета.")
ICanEatGlass+=("Russian: Я могу есть стекло, оно мне не вредит.")
ICanEatGlass+=("Belarusian (Cyrillic): Я магу есці шкло, яно мне не шкодзіць.")
ICanEatGlass+=("Belarusian (Lacinka): Ja mahu jeści škło, jano mne ne škodzić.")
ICanEatGlass+=("Ukrainian: Я можу їсти скло, і воно мені не зашкодить.")
ICanEatGlass+=("Bulgarian: Мога да ям стъкло, то не ми вреди.")
ICanEatGlass+=("Georgian: მინას ვჭამ და არა მტკივა.")
ICanEatGlass+=("Armenian: Կրնամ ապակի ուտել և ինծի անհանգիստ չըներ։")
ICanEatGlass+=("Albanian: Unë mund të ha qelq dhe nuk më gjen gjë.")
ICanEatGlass+=("Turkish: Cam yiyebilirim, bana zararı dokunmaz.")
ICanEatGlass+=("Turkish (Ottoman): جام ييه بلورم بڭا ضررى طوقونمز")
ICanEatGlass+=("Bangla / Bengali: আমি কাঁচ খেতে পারি, তাতে আমার কোনো ক্ষতি হয় না।")
ICanEatGlass+=("Marathi: मी काच खाऊ शकतो, मला ते दुखत नाही.")
ICanEatGlass+=("Kannada: ನನಗೆ ಹಾನಿ ಆಗದೆ, ನಾನು ಗಜನ್ನು ತಿನಬಹುದು")
ICanEatGlass+=("Hindi: मैं काँच खा सकता हूँ और मुझे उससे कोई चोट नहीं पहुंचती.")
ICanEatGlass+=("Tamil: நான் கண்ணாடி சாப்பிடுவேன், அதனால் எனக்கு ஒரு கேடும் வராது.")
ICanEatGlass+=("Telugu: నేను గాజు తినగలను మరియు అలా చేసినా నాకు ఏమి ఇబ్బంది లేదు")
ICanEatGlass+=("Sinhalese: මට වීදුරු කෑමට හැකියි. එයින් මට කිසි හානියක් සිදු නොවේ.")
ICanEatGlass+=("Urdu(3): میں کانچ کھا سکتا ہوں اور مجھے تکلیف نہیں ہوتی ۔")
ICanEatGlass+=("Pashto(3): زه شيشه خوړلې شم، هغه ما نه خوږوي")
ICanEatGlass+=("Farsi / Persian(3): .من می توانم بدونِ احساس درد شيشه بخورم")
ICanEatGlass+=("Arabic(3): أنا قادر على أكل الزجاج و هذا لا يؤلمني. ")
ICanEatGlass+=("Maltese: Nista' niekol il-ħġieġ u ma jagħmilli xejn.")
ICanEatGlass+=("Hebrew(3): אני יכול לאכול זכוכית וזה לא מזיק לי.")
ICanEatGlass+=("Yiddish(3): איך קען עסן גלאָז און עס טוט מיר נישט װײ. ")
ICanEatGlass+=("Twi: Metumi awe tumpan, ɜnyɜ me hwee.")
ICanEatGlass+=("Hausa (Latin): Inā iya taunar gilāshi kuma in gamā lāfiyā.")
ICanEatGlass+=("Hausa (Ajami) (2): إِنا إِىَ تَونَر غِلَاشِ كُمَ إِن غَمَا لَافِىَا")
ICanEatGlass+=("Yoruba(4): Mo lè je̩ dígí, kò ní pa mí lára.")
ICanEatGlass+=("Lingala: Nakokí kolíya biténi bya milungi, ekosála ngáí mabé tɛ́.")
ICanEatGlass+=("(Ki)Swahili: Naweza kula bilauri na sikunyui.")
ICanEatGlass+=("Malay: Saya boleh makan kaca dan ia tidak mencederakan saya.")
ICanEatGlass+=("Tagalog: Kaya kong kumain nang bubog at hindi ako masaktan.")
ICanEatGlass+=("Chamorro: Siña yo' chumocho krestat, ti ha na'lalamen yo'.")
ICanEatGlass+=("Fijian: Au rawa ni kana iloilo, ia au sega ni vakacacani kina.")
ICanEatGlass+=("Javanese: Aku isa mangan beling tanpa lara.")
ICanEatGlass+=("Burmese: က္ယ္ဝန္‌တော္‌၊က္ယ္ဝန္‌မ မ္ယက္‌စားနုိင္‌သည္‌။ ၎က္ရောင္‌့ ထိခုိက္‌မ္ဟု မရ္ဟိပာ။ (9)")
ICanEatGlass+=("Vietnamese (quốc ngữ): Tôi có thể ăn thủy tinh mà không hại gì.")
ICanEatGlass+=("Vietnamese (nôm) (4): 些 ࣎ 世 咹 水 晶 ও 空 ࣎ 害 咦")
ICanEatGlass+=("Khmer: ខ្ញុំអាចញុំកញ្ចក់បាន ដោយគ្មានបញ្ហារ")
ICanEatGlass+=("Lao: ຂອ້ຍກິນແກ້ວໄດ້ໂດຍທີ່ມັນບໍ່ໄດ້ເຮັດໃຫ້ຂອ້ຍເຈັບ.")
ICanEatGlass+=("Thai: ฉันกินกระจกได้ แต่มันไม่ทำให้ฉันเจ็บ")
ICanEatGlass+=("Mongolian (Cyrillic): Би шил идэй чадна, надад хортой биш")
ICanEatGlass+=("Mongolian (Classic) (5): ᠪᠢ ᠰᠢᠯᠢ ᠢᠳᠡᠶᠦ ᠴᠢᠳᠠᠨᠠ ᠂ ᠨᠠᠳᠤᠷ ᠬᠣᠤᠷᠠᠳᠠᠢ ᠪᠢᠰᠢ ")
ICanEatGlass+=("Nepali: ﻿म काँच खान सक्छू र मलाई केहि नी हुन्‍न् ।")
ICanEatGlass+=("Tibetan: ཤེལ་སྒོ་ཟ་ནས་ང་ན་གི་མ་རེད།")
ICanEatGlass+=("Chinese: 我能吞下玻璃而不伤身体。")
ICanEatGlass+=("Chinese (Traditional): 我能吞下玻璃而不傷身體。")
ICanEatGlass+=("Taiwanese(6): Góa ē-tàng chia̍h po-lê, mā bē tio̍h-siong.")
ICanEatGlass+=("Japanese: 私はガラスを食べられます。それは私を傷つけません。")
ICanEatGlass+=("Korean: 나는 유리를 먹을 수 있어요. 그래도 아프지 않아요")
ICanEatGlass+=("Bislama: Mi save kakae glas, hemi no save katem mi.")
ICanEatGlass+=("Hawaiian: Hiki iaʻu ke ʻai i ke aniani; ʻaʻole nō lā au e ʻeha.")
ICanEatGlass+=("Marquesan: E koʻana e kai i te karahi, mea ʻā, ʻaʻe hauhau.")
ICanEatGlass+=("Inuktitut (10): ᐊᓕᒍᖅ ᓂᕆᔭᕌᖓᒃᑯ ᓱᕋᙱᑦᑐᓐᓇᖅᑐᖓ")
ICanEatGlass+=("Chinook Jargon: Naika məkmək kakshət labutay, pi weyk ukuk munk-sik nay.")
ICanEatGlass+=("Navajo: Tsésǫʼ yishą́ągo bííníshghah dóó doo shił neezgai da. ")
ICanEatGlass+=("Lojban: mi kakne le nu citka le blaci .iku'i le se go'i na xrani mi")
ICanEatGlass+=("Nórdicg: Ljœr ye caudran créneþ ý jor cẃran.")

ICanEatGlass+=("Euro Symbol: €.")
ICanEatGlass+=("Greek: Μπορώ να φάω σπασμένα γυαλιά χωρίς να πάθω τίποτα.")
ICanEatGlass+=("Íslenska / Icelandic: Ég get etið gler án þess að meiða mig.")
ICanEatGlass+=("Polish: Mogę jeść szkło, i mi nie szkodzi.")
ICanEatGlass+=("Romanian: Pot să mănânc sticlă și ea nu mă rănește.")
ICanEatGlass+=("Ukrainian: Я можу їсти шкло, й воно мені не пошкодить.")
ICanEatGlass+=("Armenian: Կրնամ ապակի ուտել և ինծի անհանգիստ չըներ։")
ICanEatGlass+=("Georgian: მინას ვჭამ და არა მტკივა.")
ICanEatGlass+=("Hindi: मैं काँच खा सकता हूँ, मुझे उस से कोई पीडा नहीं होती.")
ICanEatGlass+=("Hebrew(2): אני יכול לאכול זכוכית וזה לא מזיק לי.")
ICanEatGlass+=("Yiddish(2): איך קען עסן גלאָז און עס טוט מיר נישט װײ.")
ICanEatGlass+=("Arabic(2): أنا قادر على أكل الزجاج و هذا لا يؤلمني.")
ICanEatGlass+=("Japanese: 私はガラスを食べられます。それは私を傷つけません。")
ICanEatGlass+=("Thai: ฉันกินกระจกได้ แต่มันไม่ทำให้ฉันเจ็บ")

UnicodeSQL="SELECT CAST('I Can Eat Glass' AS TEXT) AS t $(i=1; for s in "${ICanEatGlass[@]}"; do s=${s//\'/\'\'}; echo ", CAST('$s' AS TEXT) AS l$i"; let ++i; done)"

TSVHeader=$({ echo t; seq ${#ICanEatGlass[@]} | sed 's/^/l/'; } | tr '\n' '\t')
TSVHeader=${TSVHeader%$'\t'}
UnicodeTSVHeader=$TSVHeader
UnicodeTSV=$(echo -n "I Can Eat Glass"; for s in "${ICanEatGlass[@]}"; do echo -n $'\t'"$s"; done)

Types=$(echo "text"; yes "text" | head -n ${#ICanEatGlass[@]})
UnicodeTypes=$Types
UnicodeColumnTypes=$(namesAndTypes "$TSVHeader" "$Types")

UnicodeCSVHeader=${UnicodeTSVHeader//$'\t'/,}
UnicodeCSV=$(
  echo -n "I Can Eat Glass"
  for s in "${ICanEatGlass[@]}"; do
      case $s in (*,*) echo -n ',"'"$s"'"' ;; (*) echo -n ",$s" ;; esac
  done
)

UnicodeJSON='{ "t": "I Can Eat Glass" '"$(
  i=1
  for s in "${ICanEatGlass[@]}"; do
    s=${s//\"/\\\"}
    echo ", \"l$i\": \"$s\""
    let ++i
  done
)}"
UnicodeTSJ=$(tsjFromJsonAndHeader "$UnicodeJSON" "$UnicodeTSVHeader")


###############################################################################
## a case with timestamps
ts="2016-06-17 20:10:37"
tsus="$ts.092939"
tsISO="2016-06-17T20:10:37"
tsISOus="$tsISO.092939"
TimestampSQL="
    SELECT CAST('$tsus' AS TIMESTAMP) AS ts
         , CAST('$tsus' AS TIMESTAMP WITHOUT TIME ZONE) AS tswotz
         , CAST('$tsus' AS TIMESTAMP(6) WITHOUT TIME ZONE) AS tswotz6
         , CAST('$tsus' AS TIMESTAMP(0) WITHOUT TIME ZONE) AS tswotz0
    "

# expected TSV output
TSVHeader=                          TSV=
TSVHeader+=$'\t''ts'                TSV+=$'\t'"$tsus"
TSVHeader+=$'\t''tswotz'            TSV+=$'\t'"$tsus"
TSVHeader+=$'\t''tswotz6'           TSV+=$'\t'"$tsus"
TSVHeader+=$'\t''tswotz0'           TSV+=$'\t'"$ts"
TSVHeader=${TSVHeader#$'\t'}        TSV=${TSV#$'\t'}  # strip the first delimiter
TimestampTSVHeader=$TSVHeader TimestampTSV=$TSV

# column types
Types=
Types+=$'\t''timestamp'
Types+=$'\t''timestamp without time zone'
Types+=$'\t''timestamp(6) without time zone'
Types+=$'\t''timestamp(0) without time zone'
Types=${Types#$'\t'}
TimestampTypes=$Types

# columns names with types
TimestampColumnTypes=$(namesAndTypes "$TSVHeader" "$Types")

# expected CSV output and header
CSVHeader=                     CSV=
CSVHeader+=,'ts'               CSV+=,"$tsus"
CSVHeader+=,'tswotz'           CSV+=,"$tsus"
CSVHeader+=,'tswotz6'          CSV+=,"$tsus"
CSVHeader+=,'tswotz0'          CSV+=,"$ts"
CSVHeader=${CSVHeader#,}       CSV=${CSV#,}  # strip the first delimiter
TimestampCSVHeader=$CSVHeader TimestampCSV=$CSV

# expected JSON output
TimestampJSON='
        {
          "ts": "'"$tsISOus"'",
          "tswotz": "'"$tsISOus"'",
          "tswotz6": "'"$tsISOus"'",
          "tswotz0": "'"$tsISO"'"
        }
    '
TimestampTSJ=$(tsjFromJsonAndHeader "$TimestampJSON" "$TimestampTSVHeader")
