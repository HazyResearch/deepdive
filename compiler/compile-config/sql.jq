###############################################################################
## jq helper functions for SQL generation
###############################################################################

include "util";

# a handy way to quote SQL identifiers
# See: http://www.postgresql.org/docs/current/static/sql-syntax-lexical.html#SQL-SYNTAX-IDENTIFIERS
def asSqlIdent: "\"\( tostring  # anything can be turned into a SQL identifier if they are inside double quotes
                    | gsub("\""; "\"\"") # even double quote themselves (in fact, PostgreSQL says \0 cannot be included)
                    )\"";

# a handy way to turn things into SQL string literals (as it is hard to write single quote within single quote in this Bash script)
def asSqlLiteral:
    # map null to NULL
    if type == "null" then "NULL"
    # shouldn't turn primitive data types into string literals
    elif type | in({number:1, boolean:1}) then tostring
    # surround strings with single quotes with appropriate escaping
    # and turn any array or object into JSON format
    else "'\(tostring | gsub("'"; "''"))'"
    end;

# a handy way to generate a string from an array with prefix/suffix and delimiters
# where an empty string is desired for empty input
# to simplify the following SQL SELECT query generation
def mapJoinString(prefix; eachItem; delimiter; suffix):
    "\(prefix)\(map(eachItem) | join(delimiter))\(suffix)";
def mapJoinOrEmptyString(prefix; eachItem; delimiter; suffix):
    if type == null then null
    elif length == 0 then ""
    else
        if type == "array" then . else [.] end |
        mapJoinString(prefix; eachItem; delimiter; suffix)
    end;
# some short hands
def mapJoinString(prefix; eachItem; delimiter)        : mapJoinString(prefix; eachItem; delimiter; "");
def mapJoinString(prefix; eachItem)                   : mapJoinString(prefix; eachItem; "");
def mapJoinString(prefix)                             : mapJoinString(prefix; .);
def mapJoinOrEmptyString(prefix; eachItem; delimiter) : mapJoinOrEmptyString(prefix; eachItem; delimiter; "");
def mapJoinOrEmptyString(prefix; eachItem)            : mapJoinOrEmptyString(prefix; eachItem; "");
def mapJoinOrEmptyString(prefix)                      : mapJoinOrEmptyString(prefix; .);

# compile SQL expression: table.column or generic expression
def asSqlExpr:
    if has("expr")                       then .expr | tostring  # TODO support more structured expressions, e.g., binary operators and asSqlCondition
    elif has("table") and has("column") then "\(.table | asSqlIdent).\(.column | asSqlIdent)"
    elif has("column")                   then .column | asSqlIdent
    else error("Neither keys .expr or .column found for SQL expression in \(tostring)")
    end;
# compile `expression AS alias` in the field list of SELECT clauses
def asSqlExprAlias:
    (asSqlExpr) + (.alias | mapJoinOrEmptyString(" AS "; asSqlIdent));
# compile `table alias` mainly for the FROM or JOIN clauses
def asSqlTableAlias(asSql):
    if has("table") then
        (.table | asSqlIdent) +
        (.alias | mapJoinOrEmptyString(" "; asSqlIdent))
    elif has("sql") then
        "(\(.sql | asSql)) " +
        (.alias // error(".alias must be set for subquery \(.sql | tostring)") | asSqlIdent)
    else error("Neither keys .table or .sql found for FROM clause in \(tostring)")
    end;
def asSqlJoinTypeTableAlias(asSql):
    # five types of joins in ANSI SQL standard: https://en.wikipedia.org/wiki/Join_(SQL)
    (.LEFT_OUTER  | mapJoinOrEmptyString("LEFT OUTER JOIN "  ; asSqlTableAlias(asSql))) //
    (.RIGHT_OUTER | mapJoinOrEmptyString("RIGHT OUTER JOIN " ; asSqlTableAlias(asSql))) //
    (.FULL_OUTER  | mapJoinOrEmptyString("FULL OUTER JOIN "  ; asSqlTableAlias(asSql))) //
    (.INNER       | mapJoinOrEmptyString("INNER JOIN "       ; asSqlTableAlias(asSql))) //
    (.CROSS       | mapJoinOrEmptyString("CROSS JOIN "       ; asSqlTableAlias(asSql))) //
    error("Join table must be specified under one of these keys: LEFT_OUTER, RIGHT_OUTER, FULL_OUTER, INNER, CROSS, but found: \(tostring)");
# compile SQL conditional expressions for WHILE, HAVING, and JOIN ON clauses
# TODO maybe these comparisons should be folded into asSqlExpr
def asSqlCondition:
    def asSqlBinaryComparison(op): nullOr(
        if type == "array" and length == 2 then
            "\(.[0] | asSqlExpr) \(op) \(.[1] | asSqlExpr)"
        else error("Comparison '\(op)' expects exactly two expressions as an array, but found: \(tostring)")
        end);
    (.eq | asSqlBinaryComparison("=" )) //
    (.gt | asSqlBinaryComparison(">" )) //
    (.ge | asSqlBinaryComparison(">=")) //
    (.le | asSqlBinaryComparison("<=")) //
    (.lt | asSqlBinaryComparison("<" )) //
    (.isNull   | nullOr("\(asSqlExpr) IS NULL")) //
    (.isntNull | nullOr("\(asSqlExpr) IS NOT NULL")) //
    (.and | nullOr(    mapJoinString(""; asSqlCondition; " AND "))   ) //
    (.or  | nullOr("(\(mapJoinString(""; asSqlCondition;  " OR ")))")) //
    error("Unrecognized SQL condition \(tostring)");
# a more structured way to generate a SQL (Structured! Query Language) SELECT query than assembling strings
# which turns an object in a particular format into SQL, taking care of many escaping issues
def asSql:
    [ (.SELECT  |mapJoinOrEmptyString("SELECT "   ; asSqlExprAlias                                      ; "\n     , "))
    , (.FROM    |mapJoinOrEmptyString("FROM "     ; asSqlTableAlias(asSql)                              ; ", "))
    , (.JOIN    |mapJoinOrEmptyString(""; "\(asSqlJoinTypeTableAlias(asSql)) ON \(.ON | asSqlCondition)"; " " ))
    , (.WHERE   |mapJoinOrEmptyString("WHERE "    ; asSqlCondition                                      ; " AND " ))
    , (.GROUP_BY|mapJoinOrEmptyString("GROUP BY " ; asSqlExpr                                           ; ", "    ))
    , (.HAVING  |mapJoinOrEmptyString("HAVING "   ; asSqlCondition                                      ; " AND " ))
    , (.ORDER_BY|mapJoinOrEmptyString("ORDER BY " ; "\(.expr | asSqlExpr) \(.order // "ASC")"           ; ", "    ))
    ] | join("\n") | trimWhitespace;

## finally, a test case
#if
#    { SELECT:
#        [ { column: "id" }
#        , { expr: "CASE WHEN isfixed THEN 1 ELSE 0 END" }
#        , { expr: "COALESCE(initvalue, 0)" }
#        ]
#    , FROM:
#        [ { table:"dd_weights_foo" }
#        ]
#    } | asSql | debug |
#false then . else . end |
###############################################################################
