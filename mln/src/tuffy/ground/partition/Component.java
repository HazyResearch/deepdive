package tuffy.ground.partition;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import tuffy.infer.ds.GAtom;
import tuffy.util.UIMan;
/**
 * A component in the MRF.
 */

public class Component implements Comparable<Component>{
	public int id = 0;
	public int rep = 0; // representative atom id
	public int numAtoms = 0;
	public int numClauses = 0;
	public int numCutClauses = 0;
	public int numPins = 0;
	public double totalWeight = 0;
	public double totalCutWeight = 0;
	public double ramSize = 0;
	public boolean hasQueryAtom = false;

	// partitions in this component; maybe only one
	public ArrayList<Partition> parts = new ArrayList<Partition>();
	// atoms that are at the boundary
	public HashSet<Integer> cutset = new HashSet<Integer>();

	// aid --> atom
	public HashMap<Integer, GAtom> atoms;


	/**
	 * Add a new atom into this component.
	 * @param a the atom
	 */
	public void addAtom(GAtom a){
		atoms.put(a.id, a);
	}
	
	/**
	 * Discard all data structures to reclaim the RAM.
	 */
	public void discard(){
		cutset.clear();
		if(atoms != null){
			atoms.clear();
		}
		atoms = null;
		for(Partition p : parts){
			p.discard();
		}
	}
	
	/**
	 * Show basic stats of this component.
	 */
	public void showStats(){
		String s = "[Component #" + id + "]" +
		"\n\tRAM = " + ramSize + " bytes" +
		"\n\t#parts = " + parts.size() + 
		"\n\t#atoms = " + numAtoms +
		"\n\t#clauses = " + numClauses +
		"\n\t#cut_atoms = " + cutset.size();
		UIMan.println(s);
	}
	
	public int compareTo(Component c){
		double d = c.size() - size();
		return (int)(Math.signum(d));
	}
	
	/**
	 * The size of this component estimated in
	 * the number fo bytes consumed to store this component in RAM.
	 */
	public double size(){
		return ramSize;
	}
	
	/**
	 * Get the number of partitions in this component.
	 */
	public int numParts(){
		return parts.size();
	}
}
