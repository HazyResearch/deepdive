package tuffy.sample;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import tuffy.sample.MRFSampleStatistic.StatisticType;
import tuffy.util.myDouble;

public class SampleStatistic_ClauseFreqViolation extends MRFSampleStatistic{

	ConcurrentHashMap<String, myDouble> clauseLogWeightedViolations = 
			new ConcurrentHashMap<String, myDouble>();
	
	public SampleStatistic_ClauseFreqViolation(){
		this.type = StatisticType.ClauseFreqViolation;
	}
			
	@Override
	public void process(MRFSampleResult sampleWorld) {
		
		double logWeight = -sampleWorld.getCost();
		Integer[] clauseVio = sampleWorld.mrf.getClauseTallies(sampleWorld.world);
		
		for(int i=0;i<clauseVio.length;i++){
			for(String ffcid : (String[]) sampleWorld.mrf.clauseToFFCID[i]){
				if(ffcid.startsWith("-")){
					ffcid = ffcid.substring(1);
				}
				this.clauseLogWeightedViolations.putIfAbsent(ffcid, new myDouble(0));
				if(clauseVio[i] != 0){
					this.clauseLogWeightedViolations.get(ffcid).tallyDouble(clauseVio[i]);
				}
			}
			
		}
		
		this.nProcessedSample ++;
	}

	@Override
	public Set getStatisticDomain() {
		return clauseLogWeightedViolations.keySet();
	}

	@Override
	public Double lookupStatistic(Object stat) {
		myDouble rs = this.clauseLogWeightedViolations.get((String) stat);
		if(rs == null){
			return null;
		}else{
			return rs.value;
		}
	}

	@Override
	public void merge(Set<MRFSampleStatistic> results) {
		for(MRFSampleStatistic sampler_g : results){
			SampleStatistic_ClauseFreqViolation sampler = (SampleStatistic_ClauseFreqViolation) sampler_g;
			for(Object ffcid_g : sampler.getStatisticDomain()){
				String ffcid = (String) ffcid_g;
				
				clauseLogWeightedViolations.putIfAbsent(ffcid, new myDouble());
				clauseLogWeightedViolations.get(ffcid).tallylog(sampler.lookupStatistic(ffcid));
				
			}
			
		}		
	}

}






