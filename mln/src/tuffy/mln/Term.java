package tuffy.mln;

import tuffy.util.StringMan;

/**
 * A term in first-order logic; either a variable or a constant.
 */
public class Term {
	
	/**
	 * Whether this term is a variable.
	 */
	private boolean isVariable;
	

	/**
	 * The name of this term. $var$ is not null iff.
	 * this term is a variable.
	 */
	private String var = null;

	/**
	 * The ID of this term. $constantID$ is not null iff.
	 * this term is a constant.
	 */
	private Integer constantID = null;
	private String constantString = null;
	
	
	/**
	 * Constructor of Term (variable version).
	 * 
	 * @param var name of the variable
	 */
	public Term(String var){
		isVariable = true;
		this.var = var;
	}
	
	public Term(String s, boolean isConstant) {
		if (isConstant) {
			constantString = s;
			isVariable = false;
		} else {
			var = s;
			isVariable = true;
		}
	}
	
	/**
	 * Constructor a Term (constant version).
	 * 
	 * @param cid the constant in the form of its integer ID
	 */
	public Term(Integer cid){
		isVariable = false;
		constantID = cid;
		constantString = cid.toString();
	}
	
	/**
	 * Return whether this term is a variable.
	 */
	public boolean isVariable(){
		return isVariable;
	}
	
	/**
	 * 
	 * @return Whether this term is a constant.
	 */
	public boolean isConstant(){
		return !isVariable;
	}
	
	/**
	 * @return The name of this term, which is null when it is
	 * a constant.
	 */
	public String var(){
		return var;
	}
	
	/**
	 * 
	 * @return The name of this term, which is null when it is 
	 * a variable.
	 */
	public int constant(){
		return constantID;
	}
	
	public String constantString() {
		return constantString;
	}
	
	/**
	 * @return This term's human-friendly representation.
	 */
	public String toString() {
		if(isVariable) {
			return var;
		}else {
			return StringMan.quoteJavaString(constantString);
		}
	}
	
}
