package tuffy.worker;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import tuffy.infer.MRF;
import tuffy.infer.ds.GAtom;
import tuffy.infer.ds.GClause;
import tuffy.sample.MRFSampler;
import tuffy.sample.SampleAlgorithm_NaiveSampling;
import tuffy.sample.SampleStatistic_ClauseFreqViolation;
import tuffy.sample.SampleStatistic_ClauseLogWeightedViolation;
import tuffy.sample.SampleStatistic_WorldFrequency;
import tuffy.sample.SampleStatistic_WorldLogWeight;
import tuffy.sample.MRFSampleStatistic.StatisticType;
import tuffy.sample.SampleStatistic_WorldSumLogWeight;
import tuffy.util.Config;
import tuffy.worker.ds.MLEWorld;

public class MLEWorker_sgdWorker implements Runnable{

	boolean isReporter = false;
	
	int nSamples = 0;
	MRF mrf;
	
	double logZ;
	double logQueryZ;
	
	double logZ2;
	double logQueryZ2;
	
	HashMap<String, myDouble> logVioTally;
	HashMap<String, myDouble> logVioQueryTally;
	HashSet<String> changedWeights;
	
	HashSet<String> fixedWeights;
	
	ConcurrentHashMap<String, Double> weights = new ConcurrentHashMap<String, Double>();
	
	double alpha = 0;
	double mu = 0;
	
	public static double gradientNorm = 0;
	
	public static ConcurrentHashMap<String, myDouble> gradientCache = 
			new ConcurrentHashMap<String, myDouble>();
	
	public MLEWorker_sgdWorker(MRF _mrf, int _nSamples, double _alpha, 
			double _mu, ConcurrentHashMap<String, Double> _weights,
			HashSet<String> _fixedWeights, boolean _isReporter) {
		mrf = _mrf;
		nSamples = _nSamples;
		alpha = _alpha;
		mu = _mu;
		weights = _weights;
		fixedWeights = _fixedWeights;
		isReporter = _isReporter;
	}
	
	public double logAdd(double logX, double logY) {

		if (logY > logX) {
			double temp = logX;
			logX = logY;
			logY = temp;
		}

		if (logX == Double.NEGATIVE_INFINITY) {
			return logX;
		}

		double negDiff = logY - logX;
		if (negDiff < -200) {
			return logX;
		}

		return logX + java.lang.Math.log(1.0 + java.lang.Math.exp(negDiff)); 
	}
	
	HashSet<Integer> getNeighborExcept(int center, HashSet<Integer> except){
		HashSet<Integer> rs = new HashSet<Integer>();
		
		if(this.mrf.adj.containsKey(center)){
			for(GClause gc : this.mrf.adj.get(center)){
				for(Integer lit : gc.lits){
					int atom = Math.abs(lit);
					if(except.contains(atom)){
						continue;
					}
					rs.add(atom);
				}
			}
		}
		if(this.mrf.adj.containsKey(-center)){
			for(GClause gc : this.mrf.adj.get(-center)){
				for(Integer lit : gc.lits){
					int atom = Math.abs(lit);
					if(except.contains(atom)){
						continue;
					}
					rs.add(atom);
				}
			}
		}
		return rs;
	}
	
	public void detectStar(){
		
		this.mrf.buildIndices();
		
		HashMap<Integer, myDouble> hub = new HashMap<Integer, myDouble>();
		for(GClause gc : this.mrf.clauses){
			if(gc.lits.length == 1){
				continue;
			}
			for(Integer lit : gc.lits){
				int iatom = Math.abs(lit);
				if(!hub.containsKey(iatom)){
					hub.put(iatom, new myDouble(0));
				}
				hub.get(iatom).tallyDouble(1);
			}
		}
		
		double max = -1;
		for(myDouble degree : hub.values()){
			if(max < degree.value){
				max = degree.value;
			}
		}
		
		HashSet<Integer> center = new HashSet<Integer>();
		for(Integer iatom : hub.keySet()){
			if(hub.get(iatom).value == max){
				center.add(iatom);
			}
		}
		
		HashMap<Integer, Integer> partition = new HashMap<Integer, Integer>();
		int cid = 0;
		HashSet<Integer> remains = new HashSet<Integer>();
		remains.addAll(this.mrf.atoms.keySet());
		remains.removeAll(center);
		while(!remains.isEmpty()){
			
			int workon = remains.iterator().next();
			cid = cid + 1;
			partition.put(workon, cid);
			remains.remove(workon);
			
			HashSet<Integer> toadd = this.getNeighborExcept(workon, center);
			toadd.retainAll(remains);
			toadd.removeAll(partition.keySet());
			while(!toadd.isEmpty()){
				
				int neighbor = toadd.iterator().next();
				partition.put(neighbor, cid);
				remains.remove(neighbor);
				
				toadd.addAll(this.getNeighborExcept(neighbor, center));
				toadd.remove(neighbor);
				
				toadd.retainAll(remains);
				toadd.removeAll(partition.keySet());
				
			}
		}
		
		//System.out.println(center + "\t" + partition);
		HashMap<Integer, HashSet<Integer>> clusters =
				new HashMap<Integer, HashSet<Integer>>();
		
		HashMap<Integer, myDouble> size = new HashMap<Integer, myDouble>();
		for(Integer atom : partition.keySet()){
			int cluster = partition.get(atom);
			if(!size.containsKey(cluster)){
				size.put(cluster, new myDouble(0));
			}
			size.get(cluster).tallyDouble(1);
			
			if(!clusters.containsKey(cluster)){
				clusters.put(cluster, new HashSet<Integer>());
			}
			
			clusters.get(cluster).add(atom);
			
		}
		
		double maxSize = -1;
		double second = -1;
		for(Integer cluster : size.keySet()){
			if(size.get(cluster).value > maxSize){
				second = maxSize;
				maxSize = size.get(cluster).value;
			}
		}
		
		System.out.println(center.size() + "\t" + maxSize + "\t" + second + "\t" + this.mrf.atoms.size());

		
	}
	
	
	
	
	@Override
	public void run() {
		
		this.mrf.cweights = weights;
		
		this.mrf.updateWeight(this.mrf.cweights);
		
		int lengthOfBitMap = this.mrf.atoms.size();
		
		ArrayList<Integer> candiateQueryForLearning = new ArrayList<Integer>();
		candiateQueryForLearning.addAll(this.mrf.isQueryForLearning);
		
		ArrayList<Integer> candidatePos = new ArrayList<Integer>();
		for(int j=1;j<lengthOfBitMap+1;j++){
			candidatePos.add(j);
		}
		
		logZ = Double.NEGATIVE_INFINITY;
		logQueryZ = Double.NEGATIVE_INFINITY;
		
		logZ2 = Double.NEGATIVE_INFINITY;
		logQueryZ2 = Double.NEGATIVE_INFINITY;
		
		logVioTally = new HashMap<String, myDouble>();
		logVioQueryTally = new HashMap<String, myDouble>();
		
		changedWeights = new HashSet<String>();
		
		//this.detectStar();
		
		//System.out.println("Sample for Z:");
		MRFSampler samplerZ = new MRFSampler(mrf, SampleAlgorithm_NaiveSampling.class, 
				null, nSamples, null);
		samplerZ.addOrReplaceSamplerStatistic(new SampleStatistic_WorldFrequency());
		samplerZ.addOrReplaceSamplerStatistic(new SampleStatistic_WorldLogWeight());
		samplerZ.addOrReplaceSamplerStatistic(new SampleStatistic_ClauseLogWeightedViolation());
		samplerZ.addOrReplaceSamplerStatistic(new SampleStatistic_ClauseFreqViolation());
		samplerZ.addOrReplaceSamplerStatistic(new SampleStatistic_WorldSumLogWeight());
		samplerZ.sample();

		
		//System.out.println("Sample for Query:");
		HashMap<String, Object> samplerProperty = new HashMap<String, Object>();
		samplerProperty.put("maintain_fixed_query_in_mrf", true);
		MRFSampler sampler = new MRFSampler(mrf, SampleAlgorithm_NaiveSampling.class, 
				samplerProperty, nSamples, null);
		sampler.addOrReplaceSamplerStatistic(new SampleStatistic_WorldFrequency());
		sampler.addOrReplaceSamplerStatistic(new SampleStatistic_WorldLogWeight());
		sampler.addOrReplaceSamplerStatistic(new SampleStatistic_ClauseLogWeightedViolation());
		sampler.addOrReplaceSamplerStatistic(new SampleStatistic_ClauseFreqViolation());
		sampler.addOrReplaceSamplerStatistic(new SampleStatistic_WorldSumLogWeight());
		sampler.sample();
		
		
		SampleStatistic_ClauseLogWeightedViolation stat_clause_log = 
				 (SampleStatistic_ClauseLogWeightedViolation) sampler.getSamplerStatistic(StatisticType.ClauseLogWeightedViolation);
		SampleStatistic_ClauseLogWeightedViolation stat_clause_logZ = 
				 (SampleStatistic_ClauseLogWeightedViolation) samplerZ.getSamplerStatistic(StatisticType.ClauseLogWeightedViolation);
		
		SampleStatistic_WorldSumLogWeight stat_logPartfunc = 
				 (SampleStatistic_WorldSumLogWeight) sampler.getSamplerStatistic(StatisticType.WorldSumLogWeight);
		SampleStatistic_WorldSumLogWeight stat_logPartfuncZ = 
				 (SampleStatistic_WorldSumLogWeight) samplerZ.getSamplerStatistic(StatisticType.WorldSumLogWeight);
		
		logZ = stat_logPartfuncZ.lookupStatistic(null);
		logQueryZ = stat_logPartfunc.lookupStatistic(null);
		
		for(String ffcid : (Set<String>) stat_clause_log.getStatisticDomain()){
			logVioQueryTally.put(ffcid, new myDouble(Math.exp(stat_clause_log.lookupStatistic(ffcid) - logQueryZ)));
			changedWeights.add(ffcid);
		}
		
		for(String ffcid : (Set<String>) stat_clause_logZ.getStatisticDomain()){
			logVioTally.put(ffcid, new myDouble(Math.exp(stat_clause_logZ.lookupStatistic(ffcid) - logZ)));
			changedWeights.add(ffcid);
		}
		
		
			
		update();
	}

	void update() {
		
		if(this.isReporter == false){
			// is worker
		
			double ngradient = 0.0;
			
			//safe gradient
			for(String ffcid : changedWeights){
				
				if(this.fixedWeights.contains(ffcid)){
					continue;
				}
				
				double vio_training = 0;
				double vio_random = 0;
				
				myDouble logtally_training = logVioQueryTally.get(ffcid);
				myDouble logtally_random = logVioTally.get(ffcid);
				
				if(logtally_training != null){
					vio_training  = logtally_training.value;
				}
				
				if(logtally_random != null){
					vio_random = logtally_random.value;
				}
				
				
				double oriWeight = this.weights.get(ffcid);
				
				ngradient += (vio_random - vio_training - this.mu * oriWeight)*
						(vio_random - vio_training - this.mu * oriWeight);
			}	
			
			if(ngradient == Double.POSITIVE_INFINITY){
				System.out.println("Throw One Sample because the gradient norm is " + ngradient);
				return;
			}
			
			for(String ffcid : changedWeights){
				
				if(this.fixedWeights.contains(ffcid)){
					continue;
				}
				
				double vio_training = 0;
				double vio_random = 0;
				
				myDouble logtally_training = logVioQueryTally.get(ffcid);
				myDouble logtally_random = logVioTally.get(ffcid);
				
				//System.out.println(ffcid + "\t" + this.weights.get(ffcid) + "\tTrain:" 
				//			+ logtally_training + "\tTest" + logtally_random);
				
				if(logtally_training != null){
					vio_training  = logtally_training.value;
				}
				
				if(logtally_random != null){
					vio_random = logtally_random.value;
				}
				
				double oriWeight = this.weights.get(ffcid);
				
				double gradient = this.alpha * (vio_random - vio_training - this.mu * oriWeight);
				
				//System.out.println("~~  " + gradient);
				
				//if(Config.debug_mode){
				//	synchronized(this){
				//		UIMan.println("---");
				//		UIMan.println("$ Gradient update for " + ffcid);
				//		UIMan.println("  vio_training: " + vio_training);
				//		UIMan.println("  vio_random: " + vio_random);
				//		UIMan.println("  oriWeight: " + oriWeight);
				//		UIMan.println("  gradient: " + gradient);
				//		
				//		UIMan.println("---");				
				//	}
				//}
				
				if(ngradient > 100){
					gradient /= Math.sqrt(ngradient);
				}
	
				//TODO: Check!!!!!!!!!!!!!!!!!!!
				if(oriWeight > 0){
					this.weights.put(ffcid, oriWeight + gradient);
				}else{
					this.weights.put(ffcid, oriWeight - gradient);
				}
				
			}
			
			for(int ct=0;ct<this.mrf.bitmaps_weight.length;ct++){
				
				GClause gc = this.mrf.clauses.get(ct);
				
				this.mrf.bitmaps_weight[ct] = 0;
				
				for(String ffcid : gc.ffcid){
					
					if(ffcid.startsWith("-")){
						this.mrf.bitmaps_weight[ct] -= weights.get(ffcid.substring(1));
					}else{
						this.mrf.bitmaps_weight[ct] += weights.get(ffcid);
					}
				
				}
				
				gc.weight = this.mrf.bitmaps_weight[ct];
			
			}
			
			gradientNorm += ngradient;
			
			//UIMan.verbose(4, "" + this.weights);
			
		}else{
			// is reporter
			double ngradient = 0.0;
			
			//safe gradient
			for(String ffcid : changedWeights){
				
				if(this.fixedWeights.contains(ffcid)){
					continue;
				}
				
				double vio_training = 0;
				double vio_random = 0;
				
				myDouble logtally_training = logVioQueryTally.get(ffcid);
				myDouble logtally_random = logVioTally.get(ffcid);
				
				if(logtally_training != null){
					vio_training  = logtally_training.value;
				}
				
				if(logtally_random != null){
					vio_random = logtally_random.value;
				}
				
				double oriWeight = this.weights.get(ffcid);
				
				ngradient += (vio_random - vio_training - this.mu * oriWeight) *
						(vio_random - vio_training - this.mu * oriWeight);
				
				//this.tallyGrad(ffcid, vio_random - vio_training - this.mu * oriWeight);
								
			}
		
			
			if(ngradient == Double.POSITIVE_INFINITY){
				System.out.println("Throw One Sample because the gradient norm is " + ngradient);
				return;
			}
			
			for(String ffcid : changedWeights){
				
				if(this.fixedWeights.contains(ffcid)){
					continue;
				}
				
				double vio_training = 0;
				double vio_random = 0;
				
				myDouble logtally_training = logVioQueryTally.get(ffcid);
				myDouble logtally_random = logVioTally.get(ffcid);
				
				if(logtally_training != null){
					vio_training  = logtally_training.value;
				}
				
				if(logtally_random != null){
					vio_random = logtally_random.value;
				}
				
				double oriWeight = this.weights.get(ffcid);
				
				ngradient += (vio_random - vio_training - this.mu * oriWeight) *
						(vio_random - vio_training - this.mu * oriWeight);
				
				this.tallyGrad(ffcid, vio_random - vio_training - this.mu * oriWeight);
								
			}
			
			
		}
		
		//Runtime.getRuntime().gc();
		
	}
	
	public synchronized void tallyGrad(String ffcid, double delta){

		MLEWorker_sgdWorker.gradientCache.putIfAbsent(ffcid, new myDouble(0));
		
		myDouble toTally = MLEWorker_sgdWorker.gradientCache.get(ffcid);
		
		if(toTally == null){
			System.out.println("~~~~~~~~~~~~~~~~~~~~~~");
		}else{
			toTally.tallyDouble(delta);
		}
		
	}
	
	

	public class myDouble{
		
		public double value = Double.NEGATIVE_INFINITY;
		
		public myDouble(){
		}
		
		public myDouble(double _initValue){
			value = _initValue;
		}
		
		public double logAdd(double logX, double logY) {

		       if (logY > logX) {
		           double temp = logX;
		           logX = logY;
		           logY = temp;
		       }

		       if (logX == Double.NEGATIVE_INFINITY) {
		           return logX;
		       }
		       
		       double negDiff = logY - logX;
		       if (negDiff < -200) {
		           return logX;
		       }
		       
		       return logX + java.lang.Math.log(1.0 + java.lang.Math.exp(negDiff)); 
		 }
		
		public void tallylog(double plus){
			value = logAdd(value, plus);
		}
		
		public synchronized void tallyDouble(double plus){
			value = value + plus;
		}
		
		public String toString(){
			return "" + value;
		}
		
	}

}










