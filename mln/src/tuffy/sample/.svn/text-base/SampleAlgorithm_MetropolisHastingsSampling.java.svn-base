package tuffy.sample;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import tuffy.infer.MRF;
import tuffy.util.Config;
import tuffy.util.ExceptionMan;

public class SampleAlgorithm_MetropolisHastingsSampling extends MRFSampleAlgorithm{

	int lengthOfBitMap = -1;
	BitSet bitmap;
	Random random = new Random();
	
	int nRuns = 0;
	
	public SampleAlgorithm_MetropolisHastingsSampling(HashMap<String, Object> property, ArrayList<Integer> sampleDomain) {
		super(property, sampleDomain);
		this.capable_for_small_components_optimization = false;
	}

	double currentWorldLogWeight = -1;
	
	
	@Override
	public void init(MRF _mrf) {
			
		mrf = _mrf;
		lengthOfBitMap = mrf.atoms.size();
		random.setSeed(System.currentTimeMillis());
				
		if(this.sampleDomain == null){
			this.sampleDomain = new ArrayList<Integer>();
			for(int j=1;j<lengthOfBitMap+1;j++){
				this.sampleDomain.add(j);
			}
		}
		
		// initial world
		bitmap = new BitSet();
		for(int j : this.sampleDomain){	
			if(Math.random() > 0.5){
				bitmap.set(j);
			}	
		}
		currentWorldLogWeight = - this.mrf.getCost(bitmap);
		
		/*
		for(int j=1;j<lengthOfBitMap+1;j++){
			this.calcDelta(j);
		}*/
		
	}
	
	ConcurrentHashMap<Integer, Double> flipDelta = 
			new ConcurrentHashMap<Integer, Double>();
	
	public void calcDelta(int atomID){
		flipDelta.put(atomID, this.mrf.getFlipDelta(bitmap, atomID));
	}
	
	public void UpdateDelta(int changed){
		
		this.currentWorldLogWeight += this.flipDelta.get(changed);
		
		HashSet<Integer> atoms = new HashSet<Integer>();
		
		for(int clause : this.mrf.localAtom2Clause.get(changed)){
			for(int atom : this.mrf.localClause2Atom.get(clause)){
				atoms.add(atom);
			}
		}
		
		for(int atom : atoms){
			calcDelta(atom);
		}
	}
	
	public void adjustHardConstraint(int flipped){
		if(Config.mle_use_key_constraint && bitmap.get(flipped)){
			if(this.mrf.localAtomsToKey.containsKey(flipped)){
				for(Integer key : this.mrf.localAtomsToKey.get(flipped)){
					for(Integer toBeFalse : this.mrf.keyToLocalAtoms.get(key)){
						this.bitmap.set(toBeFalse, false);
					}
				}
			}
		}
		
	}
	
	public int proposeRandomWorld(){

		BitSet oriBitSet = (BitSet) bitmap.clone();
		
		// get proposal distribution
		double logSum = Double.NEGATIVE_INFINITY;
		double[] logWeightCache = new double[lengthOfBitMap + 1];
		for(int i : this.sampleDomain){
			//double logCost = this.currentWorldLogWeight + this.flipDelta.get(i);
			
				//Ce: not too slow actually...
			bitmap.flip(i);this.adjustHardConstraint(i);
			double logCost = -this.mrf.getCost(bitmap);
			//bitmap.flip(i);
			bitmap = (BitSet) oriBitSet.clone();
			
			logWeightCache[i] = logCost;
			logSum = Config.logAdd(logSum, logCost);
		}
		
		// sample from proposal distribution
		double rand = Math.random();
		double agg = 0;
		int toFlip = -1;
		for(int i : this.sampleDomain){
			agg += Math.exp(logWeightCache[i] - logSum);
			if(rand <= agg){
				toFlip = i;
				break;
			}
		}
		
		if(toFlip == -1){
			ExceptionMan.die("MH does not proposal anything!");
		}
		
		// reject or accept the proposal
		double oriWorldCost = this.mrf.getCost(bitmap);
		bitmap.flip(toFlip);this.adjustHardConstraint(toFlip);
		BitSet oriBitSetFlipped = (BitSet) bitmap.clone();
		
		double proposedWorldCost = this.mrf.getCost(bitmap);
		double newLogSum = Double.NEGATIVE_INFINITY;
		double[] newLogWeightCache = new double[lengthOfBitMap + 1];
		for(int i : this.sampleDomain){
			
			bitmap.flip(i);this.adjustHardConstraint(i);
			double logCost = -this.mrf.getCost(bitmap);
			//bitmap.flip(i);
			bitmap = (BitSet) oriBitSetFlipped.clone();
			
			newLogWeightCache[i] = logCost;
			newLogSum = Config.logAdd(newLogSum, logCost);
		}
		//bitmap.flip(toFlip);
		bitmap = (BitSet) oriBitSet.clone();
		
		//double alpha = proWeight*qflip_back/oriWeight/qflip;
		double alpha = Math.exp(
					    (-proposedWorldCost)
					  + (newLogWeightCache[toFlip] - newLogSum)
					  - (-oriWorldCost)
					  - (logWeightCache[toFlip] - logSum)
				);
				
		if(Math.random() < alpha){
			return toFlip;
		}else{
			return -1;
		}
	}
	
	@Override
	public MRFSampleResult getNextSample() {
		
		nRuns ++;
		
		if(nRuns == 1){
					// heuristic: always use the walksat world as the first sample.
			Config.avoid_breaking_hard_clauses = true;
			
			this.mrf.inferWalkSAT(1, 100000);
			for(int j=1;j<lengthOfBitMap+1;j++){	
				if(this.mrf.globalAtom.get(j).truth){
					bitmap.set(j);
				}else{
					bitmap.set(j, false);
				}
			}
			//bitmap = new BitSet();
			//for(int j : this.sampleDomain){	
			//	if(Math.random() > 0.5){
			//		bitmap.set(j);
			//	}	
			//}
		}else{
		
			for(int i=0;i<Config.mle_gibbs_mcmc_steps;i++){
				int toFlip = this.proposeRandomWorld();
				if(toFlip == -1){
					continue;
				}
				this.bitmap.flip(toFlip);this.adjustHardConstraint(toFlip);
				//this.UpdateDelta(toFlip);
			}
		}
		
		return new MRFSampleResult(mrf, bitmap);
		
	}

	
}


