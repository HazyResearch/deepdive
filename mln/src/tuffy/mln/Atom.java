package tuffy.mln;
import java.util.*;

import tuffy.util.StringMan;


/**
 * An atomic formula. It's designed as a light-weight construct, hence
 * all fields are transparent (public).
 */
public class Atom {
	
	/**
	 * Enumerated type of Atoms. 1) NONE; 2) EVIDENCE: atom in evidence;
	 * 3) QUERY: atom in query; 4) QUEVID: atom in query as evidence.
	 */
	public static enum AtomType {EVIDENCE, QUERY, NONE, QUEVID};
	
	/**
	 * The predicate of this Atom.
	 */
	
	public Predicate pred;
	/**
	 * The argument list represented as a tuple of integers:
	 * constant as positive number and variable as negative number.
	 */
	public Tuple args = null;
	
	public ArrayList<String> sargs = null;
	
	/**
	 * Truth value of this atom.
	 */
	public Boolean truth = null;
	
	/**
	 * Probability of "soft evidence".
	 */
	public Double prior = null;
	
	/**
	 * Type of this atom. Values are enumerated by {@link AtomType}.
	 */
	public AtomType type = AtomType.NONE;

	/**
	 * Map the {@link AtomType} value of this atom into an integer,
	 * which is used internally by the DB.
	 */
	public int club() {
		switch(type) {
		case NONE: return 0;
		case QUERY: return 1;
		case EVIDENCE: return 2;
		case QUEVID: return 3; // evidence in query
		default: return 0;
		}
	}
	
	/**
	 * Test if this atom is soft evidence.
	 */
	public boolean isSoftEvidence(){
		return prior != null;
	}
	
	/**
	 * Return the number of grounded atoms when grounding this atom.
	 * This number equals to the multiplication of the domain size
	 * of each distinct variable appearing in this atom. That is,
	 * we assume cross product is used.
	 */
	public long groundSize(){
		long size = 1;
		
		// ASSUMPTION OF DATA STRUCTURE: $\forall$ i<j, args.get(i) > args.get(j) if
		// args.get(i), args.get(j) < 0. (i.e., naming new variables
		// sequentially from left to right.
		int lastVar = 0;
		for(int i=0; i<pred.arity(); i++){
			// if this is a new variable not seen before
			if(args.get(i) < lastVar){
				Type t = pred.getTypeAt(i);
				size *= t.size();
				lastVar = args.get(i);
			}
		}
		return size;
	}
	
	/**
	 * Create an evidence atom.
	 * @param p the predicate
	 * @param as the arguments
	 * @param t the truth value
	 */
	public Atom(Predicate p, ArrayList<Integer> as, boolean t){
		pred = p;
		args = new Tuple(as);
		
		truth = t;
		type = AtomType.EVIDENCE;
	}

	public Atom(ArrayList<String> as, double prior){
		pred = null;
		sargs = as;
		truth = null;
		this.prior = prior;
		type = AtomType.NONE;
	}

	public Atom(ArrayList<String> as, boolean t){
		pred = null;
		sargs = as;
		truth = t;
		type = AtomType.EVIDENCE;
	}
	
	
	
	/**
	 * Create an atom of type NONE.
	 * The default truth value of unknown is null.
	 * 
	 * @param p the predicate
	 * @param at the arguments in the form of a tuple
	 * 
	 * @see tuffy.ground.KBMC#run()
	 */
	public Atom(Predicate p, Tuple at){
		assert(at.list.length == p.arity());
		pred = p;
		args = at;
		truth = null;
		type = AtomType.NONE;
	}
	
	/**
	 * Returns this atom's human-friendly string representation.
	 */
	public String toString() {
		ArrayList<String> as = new ArrayList<String>();
		for(int v : args.list) {
			if(v >= 0){
				as.add("C" + v);
			}else{
				as.add("v" + (-v));
			}
		}
		return pred.getName() + StringMan.commaListParen(as);
	}

}
