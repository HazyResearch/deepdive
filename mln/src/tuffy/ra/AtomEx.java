package tuffy.ra;

import java.util.ArrayList;
import java.util.HashSet;

import tuffy.mln.Predicate;

/**
 * STILL IN DEVELOPMENT.
 * An atomic formula with possibly functional arguments.
 * 
 */
public class AtomEx{
	private Predicate pred;
	private ArrayList<Expression> args = new ArrayList<Expression>();
	private HashSet<String> vars = new HashSet<String>();

	public ArrayList<Expression> getArguments(){
		return args;
	}
	
	public boolean isBuiltIn(){
		return pred.isBuiltIn();
	}
	
	public AtomEx(Predicate predicate){
		this.pred = predicate;
	}
	
	/**
	 * Returns the set of variable names in this literal.
	 */
	public HashSet<String> getVars(){
		return vars;
	}

	/**
	 * Returns the predicate of this literal.
	 */
	public Predicate getPred() {
		return pred;
	}

	public String toSQL(){
		// cast argument into correct types
		return null;
	}
	

	/**
	 * Appends a new term.
	 * 
	 * @param t the term to be appended
	 */
	public void appendTerm(Expression t){
		args.add(t);
		vars.addAll(t.getVars());
	}
	
	
	
}
