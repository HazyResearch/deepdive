package tuffy.ground.partition;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

//import javaewah.EWAHCompressedBitmap;

import tuffy.db.RDB;
import tuffy.infer.MRF;
import tuffy.infer.ds.GAtom;
import tuffy.infer.ds.GClause;
import tuffy.mln.MarkovLogicNetwork;
import tuffy.util.ExceptionMan;
import tuffy.util.UIMan;
/**
 * A partition bucket is either
 * 1) one or multiple components in whole;
 * or 
 * 2) one or multiple partitions of one component.
 * Partitions are grouped into buckets and solved bucket by bucket.
 */
public class Bucket {
	
//	EWAHCompressedBitmap partFilter = new EWAHCompressedBitmap();
	
	private static int guid = 0;

	public HashMap<Integer, Component> atom2comp = new HashMap<Integer, Component>();
	
	public boolean loaded = false;
	
	public void updateAtomBiases(HashMap<Integer, Double> deltas, boolean inv) {
		if (!loaded) {
			System.err.println("!!!!!!! BUCKET NOT LOADED YET!");
			System.exit(2);
			return;
		}
		System.out.println("###### updating atom biases for " + 
		parts.size() + " partitions");
		for (Partition p : parts) {
			if (p.mrf != null) {
				p.mrf.updateSingletonClauses(deltas, inv);
			} else {
				System.err.println("!!!!!!! Partition NOT LOADED YET!");
				System.exit(2);
			}
		}
	}
	
	private RDB db;
	private PartitionScheme pmap;
	private int id;
	
	private HashSet<Component> comps = new HashSet<Component>();
	private HashSet<Partition> parts = new HashSet<Partition>();
	
	public Set<Component> getComponents(){
		return (comps);
	}

	public Set<Partition> getPartitions(){
		return (parts);
	}
	
	/**
	 * Estimated size in bytes.
	 */
	private long size = 0;
	
	/**
	 * Number of components in the current bucket.
	 */
	public long nComp = 0;
	
	/**
	 * Discard all data structues to facilitate GC.
	 */
	public void discard(){
		for(Component c : comps){
			c.discard();
		}
		loaded = false;
	}
	
	public String toString(){
		return "Bucket #" + id + " (" + UIMan.comma(comps.size()) + (comps.size()>1? " components)":" component)");
	}
	
	public long size(){
		return size;
	}
	
	/**
	 * Construct an initially empty memory zone.
	 * 
	 * 
	 * @see Bucket#addComponent(Component)
	 * @see Bucket#addPart(Partition)
	 */
	public Bucket(RDB db, PartitionScheme pmap){
		synchronized(Bucket.class){
			id = (++guid);
		}
		this.db = db;
		this.pmap = pmap;
	}
	
	/**
	 * Add a component to this bucket.
	 * 
	 */
	public void addComponent(Component c){
		comps.add(c);
		parts.addAll(c.parts);
		size += c.size();
		nComp ++;
	}

	/**
	 * Add a partition to this bucket.
	 * 
	 */
	public void addPart(Partition p){
		comps.add(p.parentComponent);
		parts.add(p);
		size += p.size();
	}
	
	
	/**
	 * Load the set of partitions from DB to RAM.
	 */
	public void load(MarkovLogicNetwork mln){
		
		if (loaded) return;
		
		loaded = true;
		
		ArrayList<Integer> clausePids = new ArrayList<Integer>();
		ArrayList<Integer> atomPids = new ArrayList<Integer>();	
		
		int max = 0;
		for(Partition p : parts){
			if(p.id > max){
				max = p.id;
			}
		}
		boolean[] filter = new boolean[max+1];
		for(int i=0;i<filter.length;i++){
			filter[i] = false;
		}
		for(Partition p : parts){
			filter[p.id] = true;
		}
		
		for(Component com : comps){
			com.atoms = new HashMap<Integer, GAtom>();
			for(Partition p : com.parts){
				//if(parts.contains(p)){
				if(filter[p.id]){
					p.mrf = new MRF(mln, p.id, com.atoms);
					clausePids.add(p.id);
				}
				atomPids.add(p.id);
			}
		}
		
		max = 0;
		for(Integer p : clausePids){
			if(p > max){
				max = p;
			}
		}
		boolean[] clausePidsFilter = new boolean[max+1];
		for(int i=0;i<clausePidsFilter.length;i++){
			clausePidsFilter[i] = false;
		}
		for(Integer p : clausePids){
			clausePidsFilter[p] = true;
		}
		
		
		String sql;
		try {
			// store pids to be loaded
			String relClausePIDs = "mzone_clausepids";
			db.createTempTableIntList(relClausePIDs, clausePids);

			String relAtomPIDs = "mzone_atompids";
			db.createTempTableIntList(relAtomPIDs, atomPids);
			
			// load atoms
			sql = "SELECT ra.atomid, rap.partid, ra.truth, ra.isquery, ra.isqueryevid FROM " + 
			mln.relAtoms + " ra, " + mln.relAtomPart + " rap " + 
			" WHERE ra.atomID = rap.atomID AND rap.partID IN (SELECT id FROM " + relAtomPIDs + ")";
			ResultSet rs = db.query(sql);
			
			while(rs.next()){
				int pid = rs.getInt("partid");
				int aid = rs.getInt("atomid");
				boolean truth = rs.getBoolean("truth");
				boolean isquery = rs.getBoolean("isquery");
				boolean isqueryevid = rs.getBoolean("isqueryevid");
				if(clausePidsFilter[pid]){
					pmap.getMRFByPartID(pid).addAtom(aid);
				}
				GAtom a = new GAtom(aid);
				a.truth = a.lowTruth = a.lowlowTruth = truth;
				a.pid = pid;
				a.isquery = isquery;
				a.isquery_evid = isqueryevid;
				
				Component com = pmap.getCompByPartID(pid);
				com.addAtom(a);
			}
			rs.close();
			
			// load clauses
			db.disableAutoCommitForNow();
			sql = "SELECT rc.cid, rc.lits, rc.weight, rc.fcid, rc.ffcid, cp.partID FROM " + 
			mln.relClauses + " rc, " +
			mln.relClausePart + " cp " + " WHERE cp.cid=rc.cid AND " +
			" partID IN (SELECT id FROM " + relClausePIDs + ")";
			rs = db.query(sql);
			while(rs.next()){
				int owner = rs.getInt("partID");
				Partition ppart = pmap.getPartitionByID(owner);
				GClause c = new GClause();
				c.parse(rs);
				ppart.mrf.clauses.add(c);
			}
			rs.close();
			db.restoreAutoCommitState();
			
			db.dropTable(relClausePIDs);
			db.dropTable(relAtomPIDs);
			
			for(Component com : comps){
				for(Partition p : com.parts){
					MRF mrf = p.mrf;
					if(mrf == null) continue;
					if(mrf.getCoreAtoms().size() == mrf.atoms.size()){
						mrf.ownsAllAtoms = true;
					}
				}
			}
			
		} catch (SQLException e) {
			ExceptionMan.handle(e);
		}
	}
	
	
	
}
