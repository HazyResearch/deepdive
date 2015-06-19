package tuffy.mln;

import java.util.ArrayList;

/**
 * A tuple of constants/variables, represented as
 * a transparent list of integers.
 *
 */
public class Tuple {
	
	/**
	 * positive element = constant; 
	 * negative element = variable.
	 * variables are encoded as -1, -2, ...
	 */
	public int[] list = null;
	
	// ASSUMPTION OF DATA STRUCTURE: the naming of variables
	// are assigned sequentially with increment 1 for each new variable
	// not seen before.
	
	/**
	 * The degree of freedom, i.e. the number of distinct variables.
	 * Actually, it corresponds to the smallest integer name of variables.
	 */
	public int dimension;
	
	/**
	 * Constructor of Tuple.
	 * Assuming args is already canonicalized
	 * 
	 * @param args
	 */
	public Tuple(ArrayList<Integer> args) {
		list = new int[args.size()];
		dimension = 0;
		for(int i=0; i<args.size(); i++) {
			list[i] = args.get(i);
			if(list[i] < dimension) {
				dimension = list[i];
			}
		}
		dimension = -dimension;
	}
	
	
	
	/**
	 * Return the i-th element. This value is the variable/constant
	 * name of the i-th element.
	 */
	public int get(int i) {
		return list[i];
	}
	
	///**
	// * Test if this tuple subsumes the argument.
	// * 
	// * @return 1 if subsumes, 0 if equiv, -1 if neither
	// */
	
	/**
	 * Test if the tuple subsumes the argument tuple.
	 * Tuple $a$ subsumes tuple $b$, if there exists a mapping $\pi$ 
	 * from variable to variable/constant, s.t., $\forall i$,
	 * $\pi$(a.variable[i]) = b.variable[i].
	 * 
	 * @return true if subsumes, false otherwise;
	 */
	public boolean subsumes(Tuple other) {
		int[] l2 = other.list;
		assert(list.length == l2.length);
		int[] sub = new int[dimension+1];
		for(int i=0; i<list.length; i++) {
			if(list[i] > 0) { // a constant
				if(l2[i] != list[i]) return false;
			}else { // a variable
				int target = sub[-list[i]];
				if(target == 0) {
					sub[-list[i]] = l2[i];
				}else if(target != l2[i]) {
					return false;
				}
			}
		}
		return true;
	}
}
