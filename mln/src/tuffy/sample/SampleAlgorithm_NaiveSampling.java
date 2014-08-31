package tuffy.sample;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Random;

import tuffy.infer.MRF;
import tuffy.infer.MRF.INIT_STRATEGY;
import tuffy.util.Config;

public class SampleAlgorithm_NaiveSampling extends MRFSampleAlgorithm{

	int lengthOfBitMap = -1;
	Random random = new Random();
		
	int nRuns = 0;
	
	HashSet<Integer> cannotBeTrue = new HashSet<Integer>();
		
	public SampleAlgorithm_NaiveSampling(HashMap<String, Object> property, ArrayList<Integer> sampleDomain) {
		super(property, sampleDomain);
		this.capable_for_small_components_optimization = true;
		
	}

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
		
		
	}
	
	@Override
	public MRFSampleResult getNextSample() {
			
		//if(this.mrf.atoms.size() > 10000){
		//	return null;
		//}

		BitSet bitmap = new BitSet();
		
		nRuns ++;

		if(nRuns < -1){
					// heuristic: always use the walksat world as the first sample.
			
			Config.avoid_breaking_hard_clauses = true;
			
			HashSet<Integer> notChange = new HashSet<Integer>();
			
			if(maintain_fixed_query_in_mrf){
				for(int i : sampleDomain){
					if(this.mrf.isQueryForLearning.contains(i)){
						if(Math.random() > 0.5){
							this.mrf.globalAtom.get(i).truth = true;
						}else{
							this.mrf.globalAtom.get(i).truth = false;
						}
						continue;
					}
					if(this.mrf.isFiexedForLearning.contains(i)){
						this.mrf.globalAtom.get(i).truth = true;
					}else{
						this.mrf.globalAtom.get(i).truth = false;
					}
					notChange.add(this.mrf.globalAtom.get(i).id);
					//notChange.add(i);
				}
			}else{
				for(int i : sampleDomain){
					double random = Math.random();
					if(random > 0.5){
						this.mrf.globalAtom.get(i).truth = true;
					}else{
						this.mrf.globalAtom.get(i).truth = false;
					}
					if(this.mrf.isQueryForLearning.contains(i)){
						// random fix.
						if(Math.random() > 0.9){
							notChange.add(this.mrf.globalAtom.get(i).id);
						}
					}else{
						notChange.add(this.mrf.globalAtom.get(i).id);
						//notChange.add(i);
					}
				}
			}
			
			//System.out.println("*");
			this.mrf.inferWalkSAT(1, 10000, notChange);
			//System.out.println(".");
			
			for(int j=1;j<lengthOfBitMap+1;j++){	
				if(this.mrf.globalAtom.get(j).truth){
					bitmap.set(j);
				}else{
					bitmap.set(j, false);
				}
			}

		}else{
				
			if(!Config.mle_use_key_constraint){
				
				for(int j : sampleDomain){
					
					if(this.maintain_fixed_query_in_mrf == true &&
							this.mrf.isFiexedForLearning.contains(j)){
						bitmap.set(j);
					}else{
						
						if(Math.random() > 0.5){
						
							if(this.maintain_fixed_query_in_mrf == true 
									&& this.mrf.isQueryForLearning.contains(j)){
								bitmap.set(j);
							}else if(this.maintain_fixed_query_in_mrf == false){
							
								bitmap.set(j);
							}
						}
					}
				}
				
			}else{
				
				cannotBeTrue.clear();
				Collections.shuffle(sampleDomain);
				
				if(this.maintain_fixed_query_in_mrf == true){
					for(int j : sampleDomain){
						if(this.mrf.isFiexedForLearning.contains(j)){
							bitmap.set(j);
							if(this.mrf.localAtomsToKey.containsKey(j)){
								for(Integer key : this.mrf.localAtomsToKey.get(j)){
									cannotBeTrue.addAll(this.mrf.keyToLocalAtoms.get(key));
								}
							}
						}
					}
				}
				
				for(Integer j: sampleDomain){
					
					if(this.maintain_fixed_query_in_mrf == true &&
							!this.mrf.isQueryForLearning.contains(j)){
						continue;
					}
					
					if(Math.random() > 0.5 && !cannotBeTrue.contains(j)){
						bitmap.set(j);
						if(this.mrf.localAtomsToKey.containsKey(j)){
							for(Integer key : this.mrf.localAtomsToKey.get(j)){
								cannotBeTrue.addAll(this.mrf.keyToLocalAtoms.get(key));
							}
						}
					}
				}	
			}
		
		}
		
		return new MRFSampleResult(mrf, bitmap);
		
	}

	
}




