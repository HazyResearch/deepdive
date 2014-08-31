package tuffy.sample;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import tuffy.sample.MRFSampleStatistic.StatisticType;
import tuffy.util.myDouble;

public class SampleStatistic_ClauseLogWeightedViolation extends MRFSampleStatistic{

	ConcurrentHashMap<String, myDouble> clauseLogWeightedViolations = 
			new ConcurrentHashMap<String, myDouble>();
	
	public SampleStatistic_ClauseLogWeightedViolation(){
		this.type = StatisticType.ClauseLogWeightedViolation;
	}
			
	@Override
	public void process(MRFSampleResult sampleWorld) {
		
		double logWeight = -sampleWorld.getCost();
		Integer[] clauseVio = sampleWorld.mrf.getClauseTallies(sampleWorld.world);
		Integer[] clauseSat = sampleWorld.mrf.getClauseSat(sampleWorld.world);
		
		for(int i=0;i<clauseVio.length;i++){
			for(String ffcid : (String[]) sampleWorld.mrf.clauseToFFCID[i]){
			
				/*
				if(ffcid.equals("2.40117") || ffcid.equals("-2.40117")){
					System.out.println("");
				}*/
				
				if(ffcid.startsWith("-")){
					ffcid = ffcid.substring(1);
				}
				if(sampleWorld.mrf.cweights.get(ffcid) * sampleWorld.mrf.bitmaps_weight[i] < 0){
					this.clauseLogWeightedViolations.putIfAbsent(ffcid, new myDouble());
					if(clauseSat[i] != 0){
						this.clauseLogWeightedViolations.get(ffcid).
								tallylog(logWeight + Math.log(clauseSat[i]));		
					}
				}else{
					this.clauseLogWeightedViolations.putIfAbsent(ffcid, new myDouble());
					if(clauseVio[i] != 0){
						this.clauseLogWeightedViolations.get(ffcid).
								tallylog(logWeight + Math.log(clauseVio[i]));		
					}
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
			SampleStatistic_ClauseLogWeightedViolation sampler = (SampleStatistic_ClauseLogWeightedViolation) sampler_g;
			for(Object ffcid_g : sampler.getStatisticDomain()){
				String ffcid = (String) ffcid_g;
				
				clauseLogWeightedViolations.putIfAbsent(ffcid, new myDouble());
				clauseLogWeightedViolations.get(ffcid).tallylog(sampler.lookupStatistic(ffcid));
				
			}
			
		}		
	}

}






