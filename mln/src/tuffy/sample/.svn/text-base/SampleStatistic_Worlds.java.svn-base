package tuffy.sample;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import tuffy.util.myDouble;

public class SampleStatistic_Worlds extends MRFSampleStatistic{

	ArrayList<BitSet> worlds = 
			new ArrayList<BitSet>();
	
	public SampleStatistic_Worlds(){
		this.type = StatisticType.WorldFrequency;
	}
			
	@Override
	public void process(MRFSampleResult sampleWorld) {
		
		worlds.add((BitSet) sampleWorld.world.clone());
		
		this.nProcessedSample ++;
		
		
	}

	@Override
	public Collection getStatisticDomain() {
		return worlds;
	}

	@Override
	public Double lookupStatistic(Object stat) {
		return null;
	}

	@Override
	public void merge(Set<MRFSampleStatistic> results) {
		for(MRFSampleStatistic sampler_g : results){
			SampleStatistic_Worlds sampler = (SampleStatistic_Worlds) sampler_g;
			this.worlds.addAll(sampler_g.getStatisticDomain());
		}		
	}
	
}





















