package tuffy.ground.partition;


import java.util.ArrayList;
import java.util.HashMap;

import tuffy.infer.MRF;
import tuffy.util.StringMan;
/**
 * A partitioning scheme on an MRF. Such a scheme consists of one or more components, with
 * each component consisting of one or more partitions. Components are disjoint from each other,
 * whereas partitions within the same component may share hyper-edges (i.e., clauses).
 * In the current policy, each hyper-edge being shared across partitioned is randomly
 * assigned to only one of the adjacent partitions. 
 *
 */
public class PartitionScheme {
	/**
	 * Components and partitions.
	 * Each component or partition has a unique ID.
	 */
	public ArrayList<Component> components = new ArrayList<Component>();
	private HashMap<Integer, Component> compMap = new HashMap<Integer, Component>();
	private HashMap<Integer, Partition> partMap = new HashMap<Integer, Partition>();
	
	/**
	 * Stats.
	 */
	private int ncomp = 0;
	private int npart = 0;
	public double totalSize = 0, maxCompSize = 0,  maxPartSize = 0, 
	maxNumAtomsInComp = 0, maxNumAtomsInPart = 0;
	private long numAtoms = 0, numClauses = 0, numCutClauses = 0;
	private int numSplitComps = 0, maxSplitFactor = 1;
	
	
	/**
	 * Show stats about this partitioning scheme.
	 */
	public String getStats(){
		ArrayList<String> lines = new ArrayList<String>();
		lines.add("------BEGIN: PARTITION STATS------");
		lines.add("#atoms = " + numAtoms);
		lines.add("#clauses = " + numClauses);
		lines.add("#components = " + ncomp);
		lines.add("#partitions = " + npart);
		
		lines.add("#max_comp_size = " + maxCompSize);
		lines.add("#max_part_size = " + maxPartSize);
		lines.add("#split_component = " + numSplitComps);
		lines.add("#max_num_atoms_in_comp = " + maxNumAtomsInComp);
		lines.add("#max_num_atoms_in_part = " + maxNumAtomsInPart);
		lines.add("#max_partitions_in_one_comp = " + maxSplitFactor);
		lines.add("#cut_clauses = " + numCutClauses);
		
		lines.add("------ END: PARTITION STATS-------");
		return StringMan.join("\n", lines);
	}
	
	public long getNumAtoms(){
		return numAtoms;
	}
	
	public Component getCompByID(int id){
		return compMap.get(id);
	}
	
	public Component getCompByPartID(int pid){
		return partMap.get(pid).parentComponent;
	}
	
	public Partition getPartitionByID(int pid){
		return partMap.get(pid);
	}
	
	public MRF getMRFByPartID(int pid){
		return partMap.get(pid).mrf;
	}
	
	public PartitionScheme(ArrayList<Component> comps){
		components = comps;
		ncomp = comps.size();
		npart = 0;
		for(Component c : comps){
			npart += c.numParts();
			totalSize += c.size();
			numAtoms += c.numAtoms;
			numClauses += c.numClauses;
			numCutClauses += c.numCutClauses;
			if(c.size() > maxCompSize){
				maxCompSize = c.size();
			}
			if(c.numAtoms > maxNumAtomsInComp){
				maxNumAtomsInComp = c.numAtoms;
			}
			if(c.parts.size() > 1){
				numSplitComps++;
				if(c.parts.size() > maxSplitFactor){
					maxSplitFactor = c.parts.size();
				}
			}
			compMap.put(c.id, c);
			for(Partition p : c.parts){
				partMap.put(p.id, p);
				if(p.numAtoms > maxNumAtomsInPart){
					maxNumAtomsInPart = p.numAtoms;
				}
				if(p.size() > maxPartSize){
					maxPartSize = p.size();
				}
			}
		}
	}
	
	/**
	 * Estimated RAM size required to hold everything.
	 */
	public double size(){
		return totalSize;
	}
	
	public int numComponents(){
		return ncomp;
	}
	
	public int numParts(){
		return npart;
	}
	
	
}
