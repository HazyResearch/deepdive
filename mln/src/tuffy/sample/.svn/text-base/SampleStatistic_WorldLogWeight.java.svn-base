package tuffy.sample;

import java.util.BitSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import tuffy.util.myDouble;

public class SampleStatistic_WorldLogWeight extends MRFSampleStatistic{

	ConcurrentHashMap<BitSet, myDouble> worldLogWeights = 
			new ConcurrentHashMap<BitSet, myDouble>();
	
	public SampleStatistic_WorldLogWeight(){
		this.type = StatisticType.WorldLogWeight;
	}
			
	@Override
	public void process(MRFSampleResult sampleWorld) {
		
		this.worldLogWeights.putIfAbsent((BitSet) sampleWorld.world.clone(), new myDouble());
		this.worldLogWeights.get(sampleWorld.world).tallylog(-sampleWorld.getCost());
		
		this.nProcessedSample ++;
		
	}

	@Override
	public Set getStatisticDomain() {
		return worldLogWeights.keySet();
	}

	@Override
	public Double lookupStatistic(Object stat) {
		myDouble rs = this.worldLogWeights.get((BitSet) stat);
		if(rs == null){
			return null;
		}else{
			return rs.value;
		}
	}

	@Override
	public void merge(Set<MRFSampleStatistic> results) {
		for(MRFSampleStatistic sampler_g : results){
			SampleStatistic_WorldLogWeight sampler = (SampleStatistic_WorldLogWeight) sampler_g;
			for(Object world_g : sampler.getStatisticDomain()){
				BitSet world = (BitSet) world_g;
				
				worldLogWeights.putIfAbsent(world, new myDouble());
				
				worldLogWeights.get(world).tallylog(sampler.lookupStatistic(world));
				
			}
			
		}		
	}

}






