package tuffy.sample;

import java.util.BitSet;
import java.util.Collection;
import java.util.Set;

public abstract class MRFSampleStatistic {

	public static enum StatisticType {
		WorldFrequency, 
		WorldLogWeight, 
		ClauseLogWeightedViolation,
		ClauseFreqViolation,
		WorldSumLogWeight,
		Worlds
	};
	
	int nProcessedSample = 0;
	
	StatisticType type = null;
	
	public abstract void process(MRFSampleResult sampleWorld);
	
	public abstract Collection getStatisticDomain();
	
	public abstract Double lookupStatistic(Object stat);
	
	public abstract void merge(Set<MRFSampleStatistic> results);
	
}




