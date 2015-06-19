package tuffy.infer.ds;

import java.util.ArrayList;

import tuffy.util.Config;

/**
 * A ground atom.
 */
public class GAtom {
	
	public boolean isquery_evid = false;
	
	/**
	 * ID of this GAtom.
	 */
	public int id = 0;
	
	/**
	 * ID of the partition containing this atom
	 */
	public int pid = 0;
	
	/**
	 * Whether this atom is referred to by multiple partitions
	 */
	public boolean cut = false;
	
	/**
	 * The truth value of this GAtom.
	 */
	public boolean truth = false;
	
	/** 
	 * top_freq_cache and top_truth_cache contains the top-k configuation of this gatom.
	 */
	public ArrayList<Integer> top_freq_cache = new ArrayList<Integer>();
	public ArrayList<Boolean> top_truth_cache = new ArrayList<Boolean>();
	
	public boolean isquery = false;
	
	/**
	 * The ideal truth value of this GAtom.
	 * 1 = false; 2 = true; 3 = both.
	 */
	public int wannabe = 0;
	
	/**
	 * Whether wannabe == 3. (both)
	 * 
	 */
	public boolean critical(){
		return wannabe == 3;
	}
	
	/**
	 * Whether wannabe == 1; (false)
	 * 
	 */
	public boolean wannaBeFalse(){
		return (wannabe & 1) > 0;
	}
	
	/**
	 * Whether wannabe == 2; (true)
	 * 
	 */
	public boolean wannaBeTrue(){
		return (wannabe & 2) > 0;
	}
	
	/**
	 * Set wannabe = 3. (both)
	 */
	public void markCritical(){
		wannabe = 3;
	}
	
	/**
	 * Set wannabe = 1. (false)
	 */
	public void markWannaBeFalse(){
		wannabe |= 1;
	}
	
	/**
	 * Set wannabe = 2. (true)
	 */
	public void markWannaBeTrue(){
		wannabe |= 2;
	}
	
	/**
	 * Recorded truth value for low-cost.
	 */
	public boolean lowTruth = false;
	
	/**
	 * 
	 */
	public boolean lowlowTruth = false;
	
	/**
	 * Whether the truth value of this GAtom is 
	 * fixed.
	 */
	public boolean fixed = false;
	
	public int nSamples = 0;
	
	public double tallyFreq = 0;
	public double tallyTrueFreq = 0;
	public double tallyLogWeight = Double.NEGATIVE_INFINITY;
	public double tallyTrueLogWeight = Double.NEGATIVE_INFINITY;
	
	public void clear(){
		tallyFreq = 0;
		tallyTrueFreq = 0;
		tallyLogWeight = Double.NEGATIVE_INFINITY;
		tallyTrueLogWeight = Double.NEGATIVE_INFINITY;
	}
	
	public synchronized void update(boolean isTrue, double freq, double weight){
		this.tallyFreq += freq;
		this.tallyLogWeight = 
				Config.logAdd(this.tallyLogWeight, 
						weight);
		
		if(isTrue){
			this.tallyTrueFreq += freq;
			this.tallyTrueLogWeight = 
					Config.logAdd(this.tallyTrueLogWeight, 
							weight);
		}
	}
	
	
	/**
	 * Tallies used in MCSAT.
	 */
	public int tallyTrue = 0;
	public float prob = 0;
	
	/**
	 * Two cost of changing the truth value of this GAtom. Violate
	 * is the cost that will increases after flipping. Rescue is the
	 * cost that will decreases after flipping.
	 */
	private double violate = 0, rescue = 0;
	
	/**
	 * String representation of this GAtom.
	 */
	public String rep = null;
	
	
	/**
	 * Flip the truth value of this GAtom.
	 */
	public void flip(){
		if(!fixed) 
			truth = !truth;
	}
	
	
	public void forceFlip(){
		truth = !truth;
	}

	public ArrayList<GAtom> flip(KeyBlock keyBlock){
		
		if(!keyBlock.hasKey(this)){
			ArrayList<GAtom> influenced = new ArrayList<GAtom>();
			influenced.add(this);
			this.forceFlip();
			return influenced;
		}
		
		// flip from TRUE to FALSE will not influence others
		if(this.truth == true){
			
			ArrayList<GAtom> influenced = new ArrayList<GAtom>();
			influenced.add(this);
			this.forceFlip();
			return influenced;
		}
		
		ArrayList<GAtom> blockMates = keyBlock.getBlockMates(this);
		ArrayList<GAtom> influenced = new ArrayList<GAtom>();
		
		for(GAtom gatom : blockMates){
			
			if(gatom == this || gatom.truth == true){
				influenced.add(gatom);
				gatom.forceFlip();
			}	
		}
	
		return influenced;
		
	}
	
	/**
	 * Reset {@link GAtom#rescue} and {@link GAtom#violate} to zero.
	 */
	public void resetDelta(){
		violate = rescue = 0;
	}
	
	/**
	 * Exchange the value of {@link GAtom#rescue} and 
	 * {@link GAtom#violate}. When the truth value of this
	 * GAtom is flipped, this function is called.
	 */
	public void invertDelta(){
		double tv = violate;
		violate = rescue;
		rescue = tv;
	}
	
	/**
	 * 
	 * Whether changing the truth value of this GAtom will
	 * violate several hard clauses.
	 */
	public boolean criticalForHardClauses(){
		return violate > Config.hard_threshold;
	}
	
	/**
	 * @return Delta to the total cost if this node is flipped.
	 */
	public double delta(){
		return violate - rescue;
	}
	
	/**
	 * @return Delta to the total cost if this node is flipped.
	 */
	public double delta(KeyBlock keyBlock){
		
		if(!keyBlock.hasKey(this)){
			return violate - rescue;
		}
		
		double sum = 0;
			
		// flip from TRUE to FALSE will not influence others
		if(this.truth == true){
			return violate - rescue;
		}
		
		ArrayList<GAtom> blockMates = keyBlock.getBlockMates(this);
		
		for(GAtom gatom : blockMates){
			
			if(gatom == this || gatom.truth == true){
				sum += gatom.delta();
			}	
		}
			
		return sum;
	}
	
	/**
	 * Flipping this node will make f unsat->sat.
	 * @param f GClause that currently not registered.
	 */
	public void assignSatPotential(GClause f){
		if(f.weight > 0){
			rescue += f.weight;
		}else{
			violate -= f.weight;
		}
	}

	/**
	 * Flipping this node will make f sat->unsat.
	 * @param f GClause that currently not registered.
	 */
	public void assignUnsatPotential(GClause f){
		if(f.weight > 0){
			violate += f.weight;
		}else{
			rescue -= f.weight;
		}
	}

	/**
	 * Flipping this node no longer makes f unsat->sat.
	 * @param f GClause that currently registered as unsat->sat.
	 */
	public void revokeSatPotential(GClause f){
		if(f.weight > 0){
			rescue -= f.weight;
		}else{
			violate += f.weight;
		}
	}

	/**
	 * Flipping this node no longer makes f sat->unsat.
	 * @param f GClause that currently registered as sat->unsat.
	 */
	public void revokeUnsatPotential(GClause f){
		if(f.weight > 0){
			violate -= f.weight;
		}else{
			rescue += f.weight;
		}
	}

	/**
	 * Constructor for GAtom.
	 * @param nid ID of this GAtom.
	 */
	public GAtom(int nid) {
		id = nid;
		
		if(Config.mleTopK != -1){
			this.top_freq_cache = new ArrayList<Integer>();
			this.top_truth_cache = new ArrayList<Boolean>();
		}
		
	}
	
	public String toString(){
		return "GAtom ID = " + this.id;
	}
}
