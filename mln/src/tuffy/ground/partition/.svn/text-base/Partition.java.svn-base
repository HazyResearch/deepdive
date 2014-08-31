package tuffy.ground.partition;

import java.util.ArrayList;

import tuffy.infer.MRF;
import tuffy.util.BitSetIntPair;
import tuffy.util.UIMan;

/**
 * A partition is a subgraph of an MRF component.
 */
public class Partition implements Comparable<Partition>{
	public int id; // ID of this partition
	public int numAtoms = 0;
	public int numIncidentClauses = 0;
	public double ramSize = 0;

	public Component parentComponent = null;

	public MRF mrf = null;
	
	public ArrayList<BitSetIntPair> mle_freq_cache = new ArrayList<BitSetIntPair>();

	
	public int compareTo(Partition c){
		double d = c.size() - size();
		return (int)(Math.signum(d));
	}

	/**
	 * Get the estimated RAM size of this partition.
	 */
	public double size(){
		return ramSize;
	}

	/**
	 * Discard all data structures to facilitate GC. (Does it really work?)
	 */
	public void discard() {
		if(mrf != null) mrf.discard();
		mrf = null;
	}
	

	/**
	 * Show basic stats about this partition.
	 */
	public void showStats(){
		String s = "[Partition #" + id + "]" +
		"\tRAM = " + ramSize +
		"\t#atoms = " + numAtoms +
		"\t#incident_clauses = " + numIncidentClauses;
		if(mrf != null){
			s += "\t#core_atoms = " + mrf.getCoreAtoms().size();
			s += "\t#core_clauses = " + mrf.clauses.size();
		}
		UIMan.println(s);
	}

}
