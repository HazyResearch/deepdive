package tuffy.worker;

//TODO, complete this guy!

import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import tuffy.infer.MRF;
import tuffy.util.Config;
import tuffy.util.Enumerator;
import tuffy.util.UIMan;
import tuffy.worker.ds.MLEWorld;

public class MLEWorker_gibbsSampler extends MLEWorker{

	int nSamples = 0;
	HashMap<BitSet, MLEWorld> worlds = new HashMap<BitSet, MLEWorld>();
	
	public MLEWorker_gibbsSampler(MRF _mrf, int _nSamples) {
		super(_mrf);
		nSamples = _nSamples;
	}
	
	@Override
	public ArrayList<MLEWorld> getTopK(int k) {
		
		ArrayList<MLEWorld> rs = new ArrayList<MLEWorld>();
		rs.addAll(worlds.values());
		
		if(k == -1){
			return rs;
		}
		
		Collections.sort(rs, new Comparator<MLEWorld>(){
			@Override
			public int compare(MLEWorld o1, MLEWorld o2) {
				if(o1.logCost > o2.logCost){
					return -1;
				}else if (o1.logCost == o2.logCost){
					return 0;
				}else{
					return 1;
				}
			}
		});
		
		ArrayList<MLEWorld> nrs = new ArrayList<MLEWorld>();
		for(int i=0;i<k;i++){
			nrs.add(rs.get(i));
		}
		
		return nrs;
	}

	
	HashMap<Integer, Double> flipDelta = 
			new HashMap<Integer, Double>();
	
	public void updateDelta(BitSet currentWorld, int flipped){
		
		HashSet<Integer> impactedAtoms = new HashSet<Integer>();
		for(Integer impactedClause : this.mrf.localAtom2Clause.get(flipped)){
			for(Integer impactedAtom : this.mrf.localClause2Atom.get(impactedClause)){
				impactedAtoms.add(impactedAtom);
			}
		}
		
		double cost_before_flip = this.mrf.getCost(currentWorld);
		
		for(Integer atom : impactedAtoms){
			
			//TODO, can be much faster for larger components.
			currentWorld.flip(atom);
			
			double cost_after_flip = this.mrf.getCost(currentWorld);
			this.flipDelta.put(atom, cost_after_flip - cost_before_flip);
			
			currentWorld.flip(atom);
		
		}
		
	}
	
	
	
	public int sample(BitSet currentWorld){
		
		double pi_w = Math.exp(-this.mrf.getCost(currentWorld));
		
		
		double logSum = Double.NEGATIVE_INFINITY;
		for(Integer atom : this.mrf.globalAtom.keySet()){
			
			currentWorld.flip(atom);
			logSum = Config.logAdd(logSum, -this.mrf.getCost(currentWorld));
			currentWorld.flip(atom);
		}
		
		double rand = Math.random();
		double agg = 0;
		
		Integer toFlip = -1;
		
		double pi_wp = -1;
		double q_wp_w = -1; 
		
		
		for(Integer atom : this.mrf.globalAtom.keySet()){
			
			currentWorld.flip(atom);
			double prob = Math.exp(-this.mrf.getCost(currentWorld) - logSum);
			currentWorld.flip(atom);
			
			agg += prob;
			if(rand <= agg){
				currentWorld.flip(atom);
				toFlip = atom;
				
				pi_wp = Math.exp(-this.mrf.getCost(currentWorld));
				q_wp_w = prob;
				
				break;
			}
		}
		
		if(toFlip == -1){
			return -1;
		}
		
		double backLogSum = Double.NEGATIVE_INFINITY;
		for(Integer atom : this.mrf.globalAtom.keySet()){
			
			currentWorld.flip(atom);
			backLogSum = Config.logAdd(backLogSum, -this.mrf.getCost(currentWorld));
			currentWorld.flip(atom);
			
		}
		
		currentWorld.flip(toFlip);
		double q_w_wp = Math.exp(-this.mrf.getCost() - backLogSum);
		
		double alpha = Math.min(1, pi_wp*q_w_wp/pi_w/q_wp_w);
		
		if(Math.random() < alpha){
			return toFlip;
		}
		
		return -1;
	}
	
	
	
	
	@Override
	public void run() {
		
		int lengthOfBitMap = this.mrf.atoms.size();
		
		// init world
		BitSet currentWorld = new BitSet();
		for(int i=1;i<lengthOfBitMap+1;i++){
			if(Math.random() > 0.5){
				currentWorld.set(i);
			}
		}
		
		//System.out.println(">> Start MLE sampler of with # samples = " + nSamples);
		
		for(int i=0;i<this.nSamples;i++){
			
			for(int j=0;j<Config.mle_gibbs_mcmc_steps;j++){
				int flipped = sample(currentWorld);
				if(flipped == -1){
					continue;
				}
				currentWorld.flip(flipped);
				
				//this.updateDelta(currentWorld, flipped);
				
			}
			
//			double logcost = -this.mrf.getCost(bitmap);
			
			BitSet mleworld = this.getProjectedBitmap(currentWorld);
//			
			if(!worlds.containsKey(mleworld)){
				worlds.put(mleworld, new MLEWorld(mleworld));
			}
//			
			worlds.get(mleworld).tallyFreq();
			worlds.get(mleworld).tallyLogCost(0);
			
		}
		
	}


	
	
}





