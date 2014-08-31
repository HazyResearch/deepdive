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
import tuffy.infer.ds.GAtom;
import tuffy.util.Config;
import tuffy.util.Timer;
import tuffy.util.UIMan;

public class SampleAlgorithm_SerialMixture extends MRFSampleAlgorithm{

	int nFlips = 10000;
	
	int lengthOfBitMap = -1;
	Random random = new Random();
		
	int nRuns = 0;
	
	HashSet<Integer> cannotBeTrue = new HashSet<Integer>();
	
	SampleAlgorithm_NaiveSampling sampler_naive = null;
	SampleAlgorithm_MCSAT sampler_mcsat = null;
	
	public SampleAlgorithm_SerialMixture(HashMap<String, Object> property, ArrayList<Integer> sampleDomain) {
		super(property, sampleDomain);
		this.capable_for_small_components_optimization = true;
	}

	@Override
	public void init(MRF _mrf) {
		
		this.mrf = _mrf;
		
		lengthOfBitMap = mrf.atoms.size();
		random.setSeed(System.currentTimeMillis());
		
		sampler_naive = new SampleAlgorithm_NaiveSampling(this.property, this.sampleDomain);
		sampler_mcsat = new SampleAlgorithm_MCSAT(this.property, this.sampleDomain);
		
		sampler_naive.init(_mrf);
		sampler_mcsat.init(_mrf);
		
	}
	
	@Override
	public MRFSampleResult getNextSample() {
		
		nRuns ++;
		if(nRuns < Config.mle_serialmix_constant){
			return this.sampler_mcsat.getNextSample();
		}else{
			return this.sampler_naive.getNextSample();
		}
		
	}

	
}















