package tuffy.db;

import java.util.ArrayList;

import org.apache.commons.lang3.StringEscapeUtils;
/**
 * Container of SQL related utilities.
 */
public class SQLMan {

	/**
	 * blue --> E'blue'
	 * John's hat --> E'John\'s hat'
	 * key\tdata --> E'key\tdata'
	 * @param s
	 */
	public static String escapeString(String s){
		return "E'" + StringEscapeUtils.escapeJava(s)
			.replace("'", "\\'") + "'";
	}

	public static String quoteSqlString(String s){
		return "\"" + StringEscapeUtils.escapeJava(s) + "\"";
	}
	
	public static String escapeStringNoE(String s){
		return StringEscapeUtils.escapeJava(s)
			.replace("'", "\\'");
	}
	
	public static String procTail() {
		return "\n$$ LANGUAGE 'plpgsql'";
	}
	
	public static String seqNext(String seq) {
		return "nextval('" + seq + "')";
	}

	public static String seqCurr(String seq) {
		return "currval('" + seq + "')";
	}
	
	public static String funcHead(String pname) {
		return "CREATE OR REPLACE FUNCTION " + pname +
			"() RETURNS VOID AS $$\n";
	}

	public static String funcTail() {
		return "\n$$ LANGUAGE 'plpgsql'";
	}
	
	public static String indexName(String object, String tag) {
		return "idx_" + object + "_" + tag;
	}

	public static String seqName(String object) {
		return "seq_" + object;
	}

	public static String procName(String object, String tag) {
		return "proc_" + object + "_" + tag;
	}
	
	public static String negSelCond(String cond) {
		return "NOT (" + cond + " )";
	}

	public static String andSelCond(String a, String b) {
		return "(" + a + ") AND (" + b + ")";
	}

	public static String orSelCond(String a, String b) {
		return "(" + a + ") OR (" + b + ")";
	}

	public static String andSelCond(ArrayList<String> conds) {
		StringBuilder s = new StringBuilder();
		for(int i=0; i<conds.size(); i++) {
			s.append("(" + conds.get(i) + ")");
			if(i != conds.size()-1) s.append(" AND ");
		}
		return s.toString();
	}

	public static String orSelCond(ArrayList<String> conds) {
		StringBuilder s = new StringBuilder();
		for(int i=0; i<conds.size(); i++) {
			s.append("(" + conds.get(i) + ")");
			if(i != conds.size()-1) s.append(" OR ");
		}
		return s.toString();
	}
	
	public static String sqlTypeConversions = 
		"\r\n" + 
		"CREATE OR REPLACE FUNCTION convert_to_integer(v_input anyelement)\r\n" + 
		"RETURNS INTEGER AS $$\r\n" + 
		"DECLARE v_out_value INTEGER DEFAULT NULL;\r\n" + 
		"BEGIN\r\n" + 
		"    BEGIN\r\n" + 
		"        v_out_value := v_input::INTEGER;\r\n" + 
		"    EXCEPTION WHEN OTHERS THEN\r\n" + 
		"        RETURN NULL;\r\n" + 
		"    END;\r\n" + 
		"RETURN v_out_value;\r\n" + 
		"END;\r\n" + 
		"$$ LANGUAGE plpgsql;\r\n" + 
		"\r\n" + 
		"CREATE OR REPLACE FUNCTION convert_to_float(v_input anyelement)\r\n" + 
		"RETURNS FLOAT AS $$\r\n" + 
		"DECLARE v_out_value FLOAT DEFAULT NULL;\r\n" + 
		"BEGIN\r\n" + 
		"    BEGIN\r\n" + 
		"        v_out_value := v_input::FLOAT;\r\n" + 
		"    EXCEPTION WHEN OTHERS THEN\r\n" + 
		"        RETURN NULL;\r\n" + 
		"    END;\r\n" + 
		"RETURN v_out_value;\r\n" + 
		"END;\r\n" + 
		"$$ LANGUAGE plpgsql;\r\n" + 
		"\r\n" + 
		"CREATE OR REPLACE FUNCTION convert_to_bool(v_input anyelement)\r\n" + 
		"RETURNS BOOL AS $$\r\n" + 
		"DECLARE v_out_value BOOL DEFAULT NULL;\r\n" + 
		"BEGIN\r\n" + 
		"    BEGIN\r\n" + 
		"        v_out_value := v_input::BOOL;\r\n" + 
		"    EXCEPTION WHEN OTHERS THEN\r\n" + 
		"        RETURN NULL;\r\n" + 
		"    END;\r\n" + 
		"RETURN v_out_value;\r\n" + 
		"END;\r\n" + 
		"$$ LANGUAGE plpgsql;\r\n" + 
		"CREATE OR REPLACE FUNCTION hex_to_int(hexval varchar) RETURNS integer AS $$" +  
        "DECLARE "+
        "    result  int; "+
        " BEGIN "+
        "  EXECUTE 'SELECT x''' || hexval || '''::int' INTO result;" +
        " RETURN result; "+
        " END; "+
        " $$ "+
        " LANGUAGE 'plpgsql' IMMUTABLE STRICT;\r\n ";

	public static String sqlFuncMisc = 
	"create or replace function isnum(text) returns boolean as '\r\n" + 
	"select $1 ~ ''^(-)?[0-9]+(.[0-9]+)?$'' as result\r\n" + 
	"' language sql;";
	
	public static String sqlRandomAgg = 
		"CREATE OR REPLACE FUNCTION _random_element(anyarray)           \r\n" + 
			" RETURNS anyelement AS\r\n" + 
			"$BODY$\r\n" + 
			" SELECT $1[array_lower($1,1) + floor((1 + array_upper($1, 1) - array_lower($1, 1))*random())];\r\n" + 
			"$BODY$\r\n" + 
			"LANGUAGE 'sql' IMMUTABLE;\r\n" + 
			" \r\n" + 
			"CREATE AGGREGATE random(anyelement) (\r\n" + 
			"  SFUNC=array_append, --Function to call for each row. Just builds the array\r\n" + 
			"  STYPE=anyarray,\r\n" + 
			"  FINALFUNC=_random_element, --Function to call after everything has been added to array\r\n" + 
			"  INITCOND='{}' --Initialize an empty array when starting\r\n" + 
			");";
	
	public static String sqlIntArrayFuncReg = "\r\n" + 
			"-- Create the user-defined type for the 1-D integer arrays (_int4)\r\n" + 
			"--\r\n" + 
			"\r\n" + 
			"-- Query type\r\n" + 
			"CREATE OR REPLACE FUNCTION bqarr_in(cstring)\r\n" + 
			"RETURNS query_int\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C STRICT IMMUTABLE;\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION bqarr_out(query_int)\r\n" + 
			"RETURNS cstring\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C STRICT IMMUTABLE;\r\n" + 
			"\r\n" + 
			"CREATE TYPE query_int (\r\n" + 
			"	INTERNALLENGTH = -1,\r\n" + 
			"	INPUT = bqarr_in,\r\n" + 
			"	OUTPUT = bqarr_out\r\n" + 
			");\r\n" + 
			"\r\n" + 
			"--only for debug\r\n" + 
			"CREATE OR REPLACE FUNCTION querytree(query_int)\r\n" + 
			"RETURNS text\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C STRICT IMMUTABLE;\r\n" + 
			"\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION boolop(_int4, query_int)\r\n" + 
			"RETURNS bool\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C STRICT IMMUTABLE;\r\n" + 
			"\r\n" + 
			"COMMENT ON FUNCTION boolop(_int4, query_int) IS 'boolean operation with array';\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION rboolop(query_int, _int4)\r\n" + 
			"RETURNS bool\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C STRICT IMMUTABLE;\r\n" + 
			"\r\n" + 
			"COMMENT ON FUNCTION rboolop(query_int, _int4) IS 'boolean operation with array';\r\n" + 
			"\r\n" + 
			"CREATE OPERATOR @@ (\r\n" + 
			"	LEFTARG = _int4,\r\n" + 
			"	RIGHTARG = query_int,\r\n" + 
			"	PROCEDURE = boolop,\r\n" + 
			"	COMMUTATOR = '~~',\r\n" + 
			"	RESTRICT = contsel,\r\n" + 
			"	JOIN = contjoinsel\r\n" + 
			");\r\n" + 
			"\r\n" + 
			"CREATE OPERATOR ~~ (\r\n" + 
			"	LEFTARG = query_int,\r\n" + 
			"	RIGHTARG = _int4,\r\n" + 
			"	PROCEDURE = rboolop,\r\n" + 
			"	COMMUTATOR = '@@',\r\n" + 
			"	RESTRICT = contsel,\r\n" + 
			"	JOIN = contjoinsel\r\n" + 
			");\r\n" + 
			"\r\n" + 
			"\r\n" + 
			"--\r\n" + 
			"-- External C-functions for R-tree methods\r\n" + 
			"--\r\n" + 
			"\r\n" + 
			"-- Comparison methods\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION _int_contains(_int4, _int4)\r\n" + 
			"RETURNS bool\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C STRICT IMMUTABLE;\r\n" + 
			"\r\n" + 
			"COMMENT ON FUNCTION _int_contains(_int4, _int4) IS 'contains';\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION _int_contained(_int4, _int4)\r\n" + 
			"RETURNS bool\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C STRICT IMMUTABLE;\r\n" + 
			"\r\n" + 
			"COMMENT ON FUNCTION _int_contained(_int4, _int4) IS 'contained in';\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION _int_overlap(_int4, _int4)\r\n" + 
			"RETURNS bool\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C STRICT IMMUTABLE;\r\n" + 
			"\r\n" + 
			"COMMENT ON FUNCTION _int_overlap(_int4, _int4) IS 'overlaps';\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION _int_same(_int4, _int4)\r\n" + 
			"RETURNS bool\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C STRICT IMMUTABLE;\r\n" + 
			"\r\n" + 
			"COMMENT ON FUNCTION _int_same(_int4, _int4) IS 'same as';\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION _int_different(_int4, _int4)\r\n" + 
			"RETURNS bool\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C STRICT IMMUTABLE;\r\n" + 
			"\r\n" + 
			"COMMENT ON FUNCTION _int_different(_int4, _int4) IS 'different';\r\n" + 
			"\r\n" + 
			"-- support routines for indexing\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION _int_union(_int4, _int4)\r\n" + 
			"RETURNS _int4\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C STRICT IMMUTABLE;\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION _int_inter(_int4, _int4)\r\n" + 
			"RETURNS _int4\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C STRICT IMMUTABLE;\r\n" + 
			"\r\n" + 
			"--\r\n" + 
			"-- OPERATORS\r\n" + 
			"--\r\n" + 
			"\r\n" + 
			"CREATE OPERATOR && (\r\n" + 
			"	LEFTARG = _int4,\r\n" + 
			"	RIGHTARG = _int4,\r\n" + 
			"	PROCEDURE = _int_overlap,\r\n" + 
			"	COMMUTATOR = '&&',\r\n" + 
			"	RESTRICT = contsel,\r\n" + 
			"	JOIN = contjoinsel\r\n" + 
			");\r\n" + 
			"\r\n" + 
			"--CREATE OPERATOR = (\r\n" + 
			"--	LEFTARG = _int4,\r\n" + 
			"--	RIGHTARG = _int4,\r\n" + 
			"--	PROCEDURE = _int_same,\r\n" + 
			"--	COMMUTATOR = '=',\r\n" + 
			"--	NEGATOR = '<>',\r\n" + 
			"--	RESTRICT = eqsel,\r\n" + 
			"--	JOIN = eqjoinsel,\r\n" + 
			"--	SORT1 = '<',\r\n" + 
			"--	SORT2 = '<'\r\n" + 
			"--);\r\n" + 
			"\r\n" + 
			"--CREATE OPERATOR <> (\r\n" + 
			"--	LEFTARG = _int4,\r\n" + 
			"--	RIGHTARG = _int4,\r\n" + 
			"--	PROCEDURE = _int_different,\r\n" + 
			"--	COMMUTATOR = '<>',\r\n" + 
			"--	NEGATOR = '=',\r\n" + 
			"--	RESTRICT = neqsel,\r\n" + 
			"--	JOIN = neqjoinsel\r\n" + 
			"--);\r\n" + 
			"\r\n" + 
			"CREATE OPERATOR @> (\r\n" + 
			"	LEFTARG = _int4,\r\n" + 
			"	RIGHTARG = _int4,\r\n" + 
			"	PROCEDURE = _int_contains,\r\n" + 
			"	COMMUTATOR = '<@',\r\n" + 
			"	RESTRICT = contsel,\r\n" + 
			"	JOIN = contjoinsel\r\n" + 
			");\r\n" + 
			"\r\n" + 
			"CREATE OPERATOR <@ (\r\n" + 
			"	LEFTARG = _int4,\r\n" + 
			"	RIGHTARG = _int4,\r\n" + 
			"	PROCEDURE = _int_contained,\r\n" + 
			"	COMMUTATOR = '@>',\r\n" + 
			"	RESTRICT = contsel,\r\n" + 
			"	JOIN = contjoinsel\r\n" + 
			");\r\n" + 
			"\r\n" + 
			"-- obsolete:\r\n" + 
			"CREATE OPERATOR @ (\r\n" + 
			"	LEFTARG = _int4,\r\n" + 
			"	RIGHTARG = _int4,\r\n" + 
			"	PROCEDURE = _int_contains,\r\n" + 
			"	COMMUTATOR = '~',\r\n" + 
			"	RESTRICT = contsel,\r\n" + 
			"	JOIN = contjoinsel\r\n" + 
			");\r\n" + 
			"\r\n" + 
			"CREATE OPERATOR ~ (\r\n" + 
			"	LEFTARG = _int4,\r\n" + 
			"	RIGHTARG = _int4,\r\n" + 
			"	PROCEDURE = _int_contained,\r\n" + 
			"	COMMUTATOR = '@',\r\n" + 
			"	RESTRICT = contsel,\r\n" + 
			"	JOIN = contjoinsel\r\n" + 
			");\r\n" + 
			"\r\n" + 
			"--------------\r\n" + 
			"CREATE OR REPLACE FUNCTION intset(int4)\r\n" + 
			"RETURNS _int4\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C STRICT IMMUTABLE;\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION icount(_int4)\r\n" + 
			"RETURNS int4\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C STRICT IMMUTABLE;\r\n" + 
			"\r\n" + 
			"CREATE OPERATOR # (\r\n" + 
			"	RIGHTARG = _int4,\r\n" + 
			"	PROCEDURE = icount\r\n" + 
			");\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION sort(_int4, text)\r\n" + 
			"RETURNS _int4\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C STRICT IMMUTABLE;\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION sort(_int4)\r\n" + 
			"RETURNS _int4\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C STRICT IMMUTABLE;\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION sort_asc(_int4)\r\n" + 
			"RETURNS _int4\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C STRICT IMMUTABLE;\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION sort_desc(_int4)\r\n" + 
			"RETURNS _int4\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C STRICT IMMUTABLE;\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION uniq(_int4)\r\n" + 
			"RETURNS _int4\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C STRICT IMMUTABLE;\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION idx(_int4, int4)\r\n" + 
			"RETURNS int4\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C STRICT IMMUTABLE;\r\n" + 
			"\r\n" + 
			"CREATE OPERATOR # (\r\n" + 
			"	LEFTARG = _int4,\r\n" + 
			"	RIGHTARG = int4,\r\n" + 
			"	PROCEDURE = idx\r\n" + 
			");\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION subarray(_int4, int4, int4)\r\n" + 
			"RETURNS _int4\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C STRICT IMMUTABLE;\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION subarray(_int4, int4)\r\n" + 
			"RETURNS _int4\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C STRICT IMMUTABLE;\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION intarray_push_elem(_int4, int4)\r\n" + 
			"RETURNS _int4\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C STRICT IMMUTABLE;\r\n" + 
			"\r\n" + 
			"CREATE OPERATOR + (\r\n" + 
			"	LEFTARG = _int4,\r\n" + 
			"	RIGHTARG = int4,\r\n" + 
			"	PROCEDURE = intarray_push_elem\r\n" + 
			");\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION intarray_push_array(_int4, _int4)\r\n" + 
			"RETURNS _int4\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C STRICT IMMUTABLE;\r\n" + 
			"\r\n" + 
			"CREATE OPERATOR + (\r\n" + 
			"	LEFTARG = _int4,\r\n" + 
			"	RIGHTARG = _int4,\r\n" + 
			"	COMMUTATOR = +,\r\n" + 
			"	PROCEDURE = intarray_push_array\r\n" + 
			");\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION intarray_del_elem(_int4, int4)\r\n" + 
			"RETURNS _int4\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C STRICT IMMUTABLE;\r\n" + 
			"\r\n" + 
			"CREATE OPERATOR - (\r\n" + 
			"	LEFTARG = _int4,\r\n" + 
			"	RIGHTARG = int4,\r\n" + 
			"	PROCEDURE = intarray_del_elem\r\n" + 
			");\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION intset_union_elem(_int4, int4)\r\n" + 
			"RETURNS _int4\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C STRICT IMMUTABLE;\r\n" + 
			"\r\n" + 
			"CREATE OPERATOR | (\r\n" + 
			"	LEFTARG = _int4,\r\n" + 
			"	RIGHTARG = int4,\r\n" + 
			"	PROCEDURE = intset_union_elem\r\n" + 
			");\r\n" + 
			"\r\n" + 
			"CREATE OPERATOR | (\r\n" + 
			"	LEFTARG = _int4,\r\n" + 
			"	RIGHTARG = _int4,\r\n" + 
			"	COMMUTATOR = |,\r\n" + 
			"	PROCEDURE = _int_union\r\n" + 
			");\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION intset_subtract(_int4, _int4)\r\n" + 
			"RETURNS _int4\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C STRICT IMMUTABLE;\r\n" + 
			"\r\n" + 
			"CREATE OPERATOR - (\r\n" + 
			"	LEFTARG = _int4,\r\n" + 
			"	RIGHTARG = _int4,\r\n" + 
			"	PROCEDURE = intset_subtract\r\n" + 
			");\r\n" + 
			"\r\n" + 
			"CREATE OPERATOR & (\r\n" + 
			"	LEFTARG = _int4,\r\n" + 
			"	RIGHTARG = _int4,\r\n" + 
			"	COMMUTATOR = &,\r\n" + 
			"	PROCEDURE = _int_inter\r\n" + 
			");\r\n" + 
			"--------------\r\n" + 
			"\r\n" + 
			"-- define the GiST support methods\r\n" + 
			"CREATE OR REPLACE FUNCTION g_int_consistent(internal,_int4,int,oid,internal)\r\n" + 
			"RETURNS bool\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C IMMUTABLE STRICT;\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION g_int_compress(internal)\r\n" + 
			"RETURNS internal\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C IMMUTABLE STRICT;\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION g_int_decompress(internal)\r\n" + 
			"RETURNS internal\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C IMMUTABLE STRICT;\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION g_int_penalty(internal,internal,internal)\r\n" + 
			"RETURNS internal\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C IMMUTABLE STRICT;\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION g_int_picksplit(internal, internal)\r\n" + 
			"RETURNS internal\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C IMMUTABLE STRICT;\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION g_int_union(internal, internal)\r\n" + 
			"RETURNS _int4\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C IMMUTABLE STRICT;\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION g_int_same(_int4, _int4, internal)\r\n" + 
			"RETURNS internal\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C IMMUTABLE STRICT;\r\n" + 
			"\r\n" + 
			"\r\n" + 
			"-- Create the operator class for indexing\r\n" + 
			"\r\n" + 
			"\r\n" + 
			"---------------------------------------------\r\n" + 
			"-- intbig\r\n" + 
			"---------------------------------------------\r\n" + 
			"-- define the GiST support methods\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION _intbig_in(cstring)\r\n" + 
			"RETURNS intbig_gkey\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C STRICT IMMUTABLE;\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION _intbig_out(intbig_gkey)\r\n" + 
			"RETURNS cstring\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C STRICT IMMUTABLE;\r\n" + 
			"\r\n" + 
			"CREATE TYPE intbig_gkey (\r\n" + 
			"        INTERNALLENGTH = -1,\r\n" + 
			"        INPUT = _intbig_in,\r\n" + 
			"        OUTPUT = _intbig_out\r\n" + 
			");\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION g_intbig_consistent(internal,internal,int,oid,internal)\r\n" + 
			"RETURNS bool\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C IMMUTABLE STRICT;\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION g_intbig_compress(internal)\r\n" + 
			"RETURNS internal\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C IMMUTABLE STRICT;\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION g_intbig_decompress(internal)\r\n" + 
			"RETURNS internal\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C IMMUTABLE STRICT;\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION g_intbig_penalty(internal,internal,internal)\r\n" + 
			"RETURNS internal\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C IMMUTABLE STRICT;\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION g_intbig_picksplit(internal, internal)\r\n" + 
			"RETURNS internal\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C IMMUTABLE STRICT;\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION g_intbig_union(internal, internal)\r\n" + 
			"RETURNS _int4\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C IMMUTABLE STRICT;\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION g_intbig_same(internal, internal, internal)\r\n" + 
			"RETURNS internal\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C IMMUTABLE STRICT;\r\n" + 
			"\r\n" + 
			"-- register the opclass for indexing (not as default)\r\n" + 
			"\r\n" + 
			"CREATE OPERATOR CLASS gist__intbig_ops\r\n" + 
			"FOR TYPE _int4 USING gist\r\n" + 
			"AS\r\n" + 
			"	OPERATOR	3	&&,\r\n" + 
			"	OPERATOR	6	= (anyarray, anyarray),\r\n" + 
			"	OPERATOR	7	@>,\r\n" + 
			"	OPERATOR	8	<@,\r\n" + 
			"	OPERATOR	13	@,\r\n" + 
			"	OPERATOR	14	~,\r\n" + 
			"	OPERATOR	20	@@ (_int4, query_int),\r\n" + 
			"	FUNCTION	1	g_intbig_consistent (internal, internal, int, oid, internal),\r\n" + 
			"	FUNCTION	2	g_intbig_union (internal, internal),\r\n" + 
			"	FUNCTION	3	g_intbig_compress (internal),\r\n" + 
			"	FUNCTION	4	g_intbig_decompress (internal),\r\n" + 
			"	FUNCTION	5	g_intbig_penalty (internal, internal, internal),\r\n" + 
			"	FUNCTION	6	g_intbig_picksplit (internal, internal),\r\n" + 
			"	FUNCTION	7	g_intbig_same (internal, internal, internal),\r\n" + 
			"	STORAGE		intbig_gkey;\r\n" + 
			"\r\n" + 
			"--GIN\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION ginint4_queryextract(internal, internal, int2, internal, internal)\r\n" + 
			"RETURNS internal\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C IMMUTABLE STRICT;\r\n" + 
			"\r\n" + 
			"CREATE OR REPLACE FUNCTION ginint4_consistent(internal, int2, internal, int4, internal, internal)\r\n" + 
			"RETURNS bool\r\n" + 
			"AS '$libdir/_int'\r\n" + 
			"LANGUAGE C IMMUTABLE STRICT;\r\n" + 
			"\r\n" + 
			"CREATE OPERATOR CLASS gin__int_ops\r\n" + 
			"FOR TYPE _int4 USING gin\r\n" + 
			"AS\r\n" + 
			"	OPERATOR	3	&&,\r\n" + 
			"	OPERATOR	6	= (anyarray, anyarray),\r\n" + 
			"	OPERATOR	7	@>,\r\n" + 
			"	OPERATOR	8	<@,\r\n" + 
			"	OPERATOR	13	@,\r\n" + 
			"	OPERATOR	14	~,\r\n" + 
			"	OPERATOR	20	@@ (_int4, query_int),\r\n" + 
			"	FUNCTION	1	btint4cmp (int4, int4),\r\n" + 
			"	FUNCTION	2	ginarrayextract (anyarray, internal),\r\n" + 
			"	FUNCTION	3	ginint4_queryextract (internal, internal, int2, internal, internal),\r\n" + 
			"	FUNCTION	4	ginint4_consistent (internal, int2, internal, int4, internal, internal),\r\n" + 
			"	STORAGE		int4;\r\n" + 
			"";

}
