package tuffy.sample;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Properties;

import tuffy.infer.MRF;
import tuffy.infer.ds.GClause;
import tuffy.util.Enumerator;

public class SampleAlgorithm_JunctionTree extends MRFSampleAlgorithm{

	int lengthOfBitMap = -1;
	Enumerator enumerator = null;
	
	public SampleAlgorithm_JunctionTree(HashMap<String, Object> property, ArrayList<Integer> sampleDomain) {
		super(property, sampleDomain);
		capable_for_small_components_optimization = false;
	}

	GClause gc = new GClause();
	
	
	
	
	
	
	@Override
	public void init(MRF _mrf) {
		mrf = _mrf;
		lengthOfBitMap = mrf.atoms.size();
		
		if(this.sampleDomain == null){
			this.sampleDomain = new ArrayList<Integer>();
			for(int j=1;j<lengthOfBitMap+1;j++){
				this.sampleDomain.add(j);
			}
		}
		hasStopped = false;
	

	
	}
		
	@Override
	public MRFSampleResult getNextSample() {
			
		int[] states = enumerator.next();
		if(states == null){
			hasStopped = true;
			return null;
		}
		
		BitSet bitmap = new BitSet();

		for(int i=0;i<states.length;i++){
			int j = sampleDomain.get(i);

			if(this.maintain_fixed_query_in_mrf == true &&
					this.mrf.isFiexedForLearning.contains(j) &&
					states[i] == 0){
				return null;
			}
			
			if(this.maintain_fixed_query_in_mrf == true &&
					!this.mrf.isFiexedForLearning.contains(j) &&
					!this.mrf.isQueryForLearning.contains(j) &&
					states[i] == 1
					){
				return null;
			}
			
			if(this.maintain_fixed_query_in_mrf == true &&
					this.mrf.isQueryForLearning.contains(j) &&
					states[i] == 1){
				bitmap.set(j);
				continue;
			}
			
			if(this.maintain_fixed_query_in_mrf == true &&
					this.mrf.isFiexedForLearning.contains(j)){
				bitmap.set(j);
				continue;
			}
			if(this.maintain_fixed_query_in_mrf == false &&
					states[i] == 1){
				bitmap.set(sampleDomain.get(i));
			}
		}
				
		return new MRFSampleResult(mrf, bitmap);
		
	}

	
}






