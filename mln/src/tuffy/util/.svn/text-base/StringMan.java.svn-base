package tuffy.util;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringEscapeUtils;

import tuffy.db.SQLMan;

/**
 * Container of string related utilities.
 */
public class StringMan {

	private static int uniqVar = 1;
	
	public static String getUniqVarName(){
		return "uniqvar" + (uniqVar++);
	}

	public static String escapeJavaString(String s){
		return StringEscapeUtils.escapeJava(s);
	}

	public static String quoteJavaString(String s){
		return "\"" + StringEscapeUtils.escapeJava(s) + "\"";
	}

	/**
	 * Gets a string with all zeros.
	 * @param length
	 * @return
	 */
	public static String zeros(int length){
		StringBuilder sb = new StringBuilder();
		for(int i=0;i<length;i++){
			sb.append("0");
		}
		return sb.toString();
	}
	
	/**
	 * Concatenates multiple strings with a given separator.
	 * 
	 * @param sep the separator
	 * @param parts substrings to be concatenated
	 * @return the resulting string
	 */
	public static String join(String sep, ArrayList<String> parts) {
		StringBuilder sb = new StringBuilder("");
		for(int i=0; i<parts.size(); i++) {
			sb.append(parts.get(i));
			if(i != parts.size()-1) sb.append(sep);
		}
		return sb.toString();
	}
	
	public static String joinAndEscape(String sep, ArrayList<String> parts) {
		StringBuilder sb = new StringBuilder("");
		for(int i=0; i<parts.size(); i++) {
			sb.append(SQLMan.escapeStringNoE(parts.get(i)));
			if(i != parts.size()-1) sb.append(sep);
		}
		return sb.toString();
	}
	
	public static String join(String sep, List<String> parts) {
		StringBuilder sb = new StringBuilder("");
		for(int i=0; i<parts.size(); i++) {
			sb.append(parts.get(i));
			if(i != parts.size()-1) sb.append(sep);
		}
		return sb.toString();
	}

	/**
	 * Concatenates multiple strings with commas.
	 * 
	 * @param parts list substrings to be concatenated
	 * @return the resulting string
	 */
	public static String commaList(ArrayList<String> parts) {
		return join(", ", parts);
	}
	
	public static String commaList(List<String> parts) {
		return join(", ", parts);
	}


	/**
	 * Concatenates multiple strings with commas, and then
	 * surrounds the result with a pair of parentheses.
	 * 
	 * @param ts substrings to be concatenated
	 * @return the resulting string
	 */
	public static String commaListParen(ArrayList<String> ts) {
		return "(" + commaList(ts) + ")";
	}

    public static String repeat(String str, int repeat) {
        if (str == null) {
            return null;
        }
        if (repeat <= 0) {
            return "";
        }
        int inputLength = str.length();
        if (repeat == 1 || inputLength == 0) {
            return str;
        }
        int outputLength = inputLength * repeat;
        switch (inputLength) {
            case 1 :
                char ch = str.charAt(0);
                char[] output1 = new char[outputLength];
                for (int i = repeat - 1; i >= 0; i--) {
                    output1[i] = ch;
                }
                return new String(output1);
            case 2 :
                char ch0 = str.charAt(0);
                char ch1 = str.charAt(1);
                char[] output2 = new char[outputLength];
                for (int i = repeat * 2 - 2; i >= 0; i--, i--) {
                    output2[i] = ch0;
                    output2[i + 1] = ch1;
                }
                return new String(output2);
            default :
                StringBuilder buf = new StringBuilder(outputLength);
                for (int i = 0; i < repeat; i++) {
                    buf.append(str);
                }
                return buf.toString();
        }
    }

}
