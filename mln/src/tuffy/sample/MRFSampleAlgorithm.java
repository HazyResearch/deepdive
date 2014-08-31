package tuffy.sample;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;

import tuffy.infer.MRF;

public abstract class MRFSampleAlgorithm {

	MRF mrf;
	
	boolean capable_for_small_components_optimization = false;
	
	boolean maintain_fixed_query_in_mrf = false;
	
	public abstract void init(MRF mrf);
	
	public ArrayList<Integer> sampleDomain;
	
	public boolean hasStopped = false;
	
	public HashMap<String, Object> property = null;
	
	public MRFSampleAlgorithm(HashMap<String, Object> property, ArrayList<Integer> sampleDomain) {
			this.sampleDomain = sampleDomain;
			
			this.property = property;
			
			if(property != null){
				this.maintain_fixed_query_in_mrf = (Boolean) 
					property.get("maintain_fixed_query_in_mrf");
			}
	}
	
	public abstract MRFSampleResult getNextSample();
	
	
	
}



