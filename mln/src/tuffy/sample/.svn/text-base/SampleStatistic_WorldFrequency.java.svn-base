package tuffy.sample;

import java.util.BitSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import tuffy.util.myDouble;

public class SampleStatistic_WorldFrequency extends MRFSampleStatistic{

	ConcurrentHashMap<BitSet, myDouble> worldFreqs = 
			new ConcurrentHashMap<BitSet, myDouble>();
	
	public SampleStatistic_WorldFrequency(){
		this.type = StatisticType.WorldFrequency;
	}
			
	@Override
	public void process(MRFSampleResult sampleWorld) {
		
		this.worldFreqs.putIfAbsent((BitSet) sampleWorld.world.clone(), new myDouble(0));
		this.worldFreqs.get(sampleWorld.world).tallyDouble(1);
		
		this.nProcessedSample ++;
		
	}

	@Override
	public Set getStatisticDomain() {
		return worldFreqs.keySet();
	}

	@Override
	public Double lookupStatistic(Object stat) {
		myDouble rs = this.worldFreqs.get((BitSet) stat);
		if(rs == null){
			return null;
		}else{
			return rs.value;
		}
	}

	@Override
	public void merge(Set<MRFSampleStatistic> results) {
		for(MRFSampleStatistic sampler_g : results){
			SampleStatistic_WorldFrequency sampler = (SampleStatistic_WorldFrequency) sampler_g;
			for(Object world_g : sampler.getStatisticDomain()){
				BitSet world = (BitSet) world_g;
				
				worldFreqs.putIfAbsent(world, new myDouble(0));
				
				worldFreqs.get(world).tallyDouble(sampler.lookupStatistic(world));
				
			}
		}		
	}
	
}





















