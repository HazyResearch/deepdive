package tuffy.sample;

import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;

import tuffy.infer.MRF;
import tuffy.util.myDouble;

public class MRFSampleResult {

	MRF mrf;
	BitSet world;
	
	public MRFSampleResult(MRF _mrf, BitSet _world){
		mrf = _mrf;
		world = _world;
	}
	
	public double getCost(){
		return this.mrf.getCost(world);
	}
	
	public ConcurrentHashMap<String, myDouble> getClauseViolations(){
		
		ConcurrentHashMap<String, myDouble> rs = new 
				ConcurrentHashMap<String, myDouble>();
		
		Integer[] tallies = this.mrf.getClauseTallies(world);
		for(int i=0;i<tallies.length;i++){
			for(String ffcid : (String[]) this.mrf.clauseToFFCID[i]){
				rs.putIfAbsent(ffcid, new myDouble(0));
				rs.get(ffcid).tallyDouble(tallies[i]);
			}
		}
		
		return rs;
		
	}
	
	
	
	
}
