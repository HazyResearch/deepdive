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

public class SampleAlgorithm_MCSAT extends MRFSampleAlgorithm{

	int nFlips = 10000;
	
	int lengthOfBitMap = -1;
	Random random = new Random();
		
	int nRuns = 0;
	
	HashSet<Integer> cannotBeTrue = new HashSet<Integer>();
	
	public SampleAlgorithm_MCSAT(HashMap<String, Object> property, ArrayList<Integer> sampleDomain) {
		super(property, sampleDomain);
		this.capable_for_small_components_optimization = true;
		
	}

	@Override
	public void init(MRF _mrf) {
		mrf = _mrf;
		lengthOfBitMap = mrf.atoms.size();
		random.setSeed(System.currentTimeMillis());
		
		if(this.sampleDomain == null){
			this.sampleDomain = new ArrayList<Integer>();
			for(int j=1;j<lengthOfBitMap+1;j++){
				this.sampleDomain.add(j);
			}
		}
		
		this.mrf.initStrategy = INIT_STRATEGY.COIN_FLIP;
		
		this.mrf.sampleSatMode = false;

		boolean isFirstTime = true;

		if(isFirstTime){
			UIMan.verbose(1, ">>> MC-SAT INIT: running WalkSAT on hard clauses...");
			int x = this.mrf.retainOnlyHardClauses();
			UIMan.verbose(1, "### hard clauses = " + x);
			this.mrf.sampleSAT(nFlips);
		}

		isFirstTime = false;
		this.mrf.enableAllClauses();
		this.mrf.restoreLowTruth();

		this.mrf.sampleSatMode = true;
		
	}
	
	@Override
	public MRFSampleResult getNextSample() {
		
		//TODO: sampleDomain does not work for now.
		
		BitSet bitmap = new BitSet();
		
		this.mrf.performMCSatStep(nFlips);

		for(int j=1;j<lengthOfBitMap+1;j++){	
			if(this.mrf.globalAtom.get(j).truth){
				bitmap.set(j);
			}else{
				bitmap.set(j, false);
			}
		}
		
		
		
		return new MRFSampleResult(mrf, bitmap);
	}

	
}















