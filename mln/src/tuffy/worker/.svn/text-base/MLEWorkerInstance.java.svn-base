package tuffy.worker;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import tuffy.infer.MRF;
import tuffy.infer.ds.GAtom;
import tuffy.sample.DS_JunctionTree;
import tuffy.sample.MRFSampleAlgorithm;
import tuffy.sample.MRFSampler;
import tuffy.sample.SampleAlgorithm_NaiveSampling;
import tuffy.sample.SampleStatistic_WorldFrequency;
import tuffy.sample.SampleStatistic_WorldLogWeight;
import tuffy.sample.MRFSampleStatistic.StatisticType;
import tuffy.util.Config;
import tuffy.util.ExceptionMan;
import tuffy.util.Timer;
import tuffy.util.UIMan;
import tuffy.worker.ds.MLEWorld;

public class MLEWorkerInstance extends MLEWorker{

	Class<? extends MRFSampleAlgorithm> sampleAlgo = null;
	
	HashMap<String, Object> prop = null;
	
	int nSamples = 0;
	HashMap<BitSet, MLEWorld> worlds = new HashMap<BitSet, MLEWorld>();
	
	public MLEWorkerInstance(MRF _mrf, int _nSamples, Class<? extends MRFSampleAlgorithm> _sampleAlgo, HashMap<String, Object> _prop) {
		super(_mrf);
		nSamples = _nSamples;
		sampleAlgo = _sampleAlgo;
		
		//if(_sampleAlgo == SampleAlgorithm_NaiveSampling.class){
		//	nSamples = (int) Math.min(Math.pow(2, this.mrf.atoms.size()+1), nSamples);
		//}
		
		prop = _prop;
	}
	
	@Override
	public ArrayList<MLEWorld> getTopK(int k) {
		
		ArrayList<MLEWorld> rs = new ArrayList<MLEWorld>();
		rs.addAll(worlds.values());
		
		if(k == -1){
			return rs;
		}
		
		Collections.sort(rs, new Comparator<MLEWorld>(){
			@Override
			public int compare(MLEWorld o1, MLEWorld o2) {
				if(o1.logCost > o2.logCost){
					return -1;
				}else if (o1.logCost == o2.logCost){
					return 0;
				}else{
					return 1;
				}
			}
			
		});
		
		ArrayList<MLEWorld> nrs = new ArrayList<MLEWorld>();
		for(int i=0;i<k;i++){
			nrs.add(rs.get(i));
		}
		
		return nrs;
	}
	
	public HashSet<Integer> toEnumerate = new HashSet<Integer>();
	
	public ArrayList<HashSet<Integer>> partitionMRFIfToLarge(){
		ArrayList<HashSet<Integer>> sampleDomains = new 
				ArrayList<HashSet<Integer>>();
				
		int nAtom = this.mrf.atoms.size();
		if(nAtom > 15){
			
			TreeSet<Integer> remain = new TreeSet<Integer>();
			remain.addAll(this.mrf.globalAtom.keySet());
			
			int minhub = Integer.MAX_VALUE;
			int start = -1;
			for(int atom : remain){
				
				int hub = this.mrf.localAtom2Clause.get(atom).size();
				if(hub < minhub){
					minhub = hub;
					start = atom;
				}
			}
			
			if(start == -1){
				ExceptionMan.die("What happens ?!");
			}
			
			boolean isFirst = true;
			
			
			
			HashSet<Integer> sampleDomain = new HashSet<Integer>();
			while(!remain.isEmpty()){
				
				if(sampleDomain.size() < 60){
					
					int toinsert = -1;
					for(int atom : sampleDomain){
						for(int clause : this.mrf.localAtom2Clause.get(atom)){
							for(int atom2 : this.mrf.localClause2Atom.get(clause)){
								if(remain.contains(atom2)){
									toinsert = atom2;
									break;
								}
							}
							if(toinsert != -1){
								break;
							}
						}
						if(toinsert != -1){
							break;
						}
					}
					
					if(toinsert == -1){			
						if(isFirst){
							toinsert = start;
							isFirst = false;
						}else{
							toinsert = remain.iterator().next();
						}
					}
					
					remain.remove(toinsert);
					sampleDomain.add(toinsert);
					
					if(this.mrf.localAtomsToKey.containsKey(toinsert)){
						for(int key : this.mrf.localAtomsToKey.get(toinsert)){
							sampleDomain.addAll(this.mrf.keyToLocalAtoms.get(key));
							remain.removeAll(this.mrf.keyToLocalAtoms.get(key));
						}
					}
					
				}else{
					sampleDomains.add(sampleDomain);
					sampleDomain = new HashSet<Integer>();
				}
			}
			
			sampleDomains.add(sampleDomain);
			sampleDomain = new HashSet<Integer>();
			
			
			toEnumerate.clear();
			HashSet<Integer> fixed = new HashSet<Integer>();
			for(HashSet<Integer> domain1 : sampleDomains){
				for(int atom1 : domain1){
				
					boolean shouldAdd = false;
					
					for(HashSet<Integer> domain2 : sampleDomains){
						if(domain1 == domain2){
							continue;
						}
					
						for(int atom2 : domain2){
							
							if(fixed.contains(atom2)){
								continue;
							}
							
							HashSet<Integer> tmp = (HashSet<Integer>) this.mrf.localAtom2Clause.get(atom1).clone();
							tmp.retainAll(this.mrf.localAtom2Clause.get(atom2));
							if(!tmp.isEmpty()){
								shouldAdd = true;
								break;
							}
						}
						if(shouldAdd){
							break;
						}
					}
					if(shouldAdd){
						toEnumerate.add(atom1);
						fixed.add(atom1);
					}
				}
			}
			
			return sampleDomains;
			
		}else{
			return null;
		}
	}
	
	
	@Override
	public void run() {
		
		if(Config.mle_use_junction_tree){
			
			DS_JunctionTree jt = new DS_JunctionTree(this.mrf);
			
			
			
			
			
			
			return;
		}
		
		
		
		
		
		ArrayList<ArrayList<Integer>> sampleDomains = new 
				ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> sampledomain = new ArrayList<Integer>();
		ArrayList<Integer> enumDomain = new ArrayList<Integer>();
		
		ArrayList<HashSet<Integer>> smallparts = null;
		
		if(!Config.mle_partition_components && this.mrf.atoms.size() > 1000){
			System.out.println("---Skip large component (size " + this.mrf.atoms.size() + ")");
			return;
		}
		
		if(Config.mle_partition_components){
			smallparts = this.partitionMRFIfToLarge();
			for(HashSet<Integer> part : smallparts){
				sampledomain = new ArrayList<Integer>();
				sampledomain.addAll(part);
				sampledomain.removeAll(this.toEnumerate);
				
				sampleDomains.add(sampledomain);
			}
		}
		
		if(smallparts == null){
			
			MRFSampler sampler = new MRFSampler(mrf, sampleAlgo, prop, nSamples, null);
			sampler.addOrReplaceSamplerStatistic(new SampleStatistic_WorldFrequency());
			sampler.addOrReplaceSamplerStatistic(new SampleStatistic_WorldLogWeight());
			
			sampler.sample();
			
			Set<BitSet> sampledWorlds = 
					(Set<BitSet>) sampler.getSamplerStatistic(StatisticType.WorldFrequency).getStatisticDomain();
			
			SampleStatistic_WorldFrequency stat_freq = 
					(SampleStatistic_WorldFrequency) sampler.getSamplerStatistic(StatisticType.WorldFrequency);
			SampleStatistic_WorldLogWeight stat_weight = 
					(SampleStatistic_WorldLogWeight) sampler.getSamplerStatistic(StatisticType.WorldLogWeight);
			
			//double knob = Math.pow(2, this.mrf.atoms.size());
			//
			//int nn = 0;
			
			for(int i : this.mrf.globalAtom.keySet()){
				
				this.mrf.globalAtom.get(i).clear();
				
			}
			
			for(BitSet bitmap : sampledWorlds){
				
				BitSet mleworld = this.getProjectedBitmap(bitmap);
				
				if(!worlds.containsKey(mleworld)){
					worlds.put(mleworld, new MLEWorld(mleworld));
				}
				
				worlds.get(mleworld).tallyFreq(stat_freq.lookupStatistic(bitmap));
				worlds.get(mleworld).tallyLogCost(stat_weight.lookupStatistic(bitmap));

				double weight = stat_weight.lookupStatistic(bitmap);
				double freq = stat_freq.lookupStatistic(bitmap);
				
				for(int i : this.mrf.globalAtom.keySet()){
					
					GAtom atom = this.mrf.globalAtom.get(i);
					atom.update(bitmap.get(i), freq, weight);
					
				}
				
			}

		}else{
			enumDomain.addAll(this.toEnumerate);
			
			ArrayList<MRFSampler> samplers = new ArrayList<MRFSampler>();
			int ct = 0;
			
			int ratio = (int) Math.sqrt(nSamples);
			
			for(ArrayList<Integer> sampleDomain : sampleDomains){
				ct ++;
				System.out.println("Domain " + ct + ": size = " + sampleDomain.size());
				MRFSampler sampler = new MRFSampler(mrf, sampleAlgo, prop, nSamples/ratio + 1, sampleDomain);
				sampler.addOrReplaceSamplerStatistic(new SampleStatistic_WorldFrequency());
				sampler.addOrReplaceSamplerStatistic(new SampleStatistic_WorldLogWeight());
				
				samplers.add(sampler);
				
			}
			
			for(int i=0;i< ratio + 1;i++){
				//System.out.println("*");
				HashSet<Integer> cannotBeTrue = new HashSet<Integer>();
				Collections.shuffle(enumDomain);
				
				BitSet world = new BitSet();
				
				for(Integer localAtom : enumDomain){
					if(Math.random() > 0.5 && !cannotBeTrue.contains(localAtom)){
						this.mrf.globalAtom.get(localAtom).truth = true;
						if(this.mrf.localAtomsToKey.containsKey(localAtom)){
							for(Integer key : this.mrf.localAtomsToKey.get(localAtom)){
								cannotBeTrue.addAll(this.mrf.keyToLocalAtoms.get(key));
							}
						}
						world.set(localAtom);
					}else{
						this.mrf.globalAtom.get(localAtom).truth = false;
					}
				}	
			
				double logWeight = 0;
				for(MRFSampler sampler : samplers){
					logWeight += sampler.sample();
				}
				
				BitSet mleworld = this.getProjectedBitmap(world);
				
				if(!worlds.containsKey(mleworld)){
					worlds.put(mleworld, new MLEWorld(mleworld));
				}
				
				for(int atom : enumDomain){

					//TODO: concurrent bug.
					this.mrf.globalAtom.get(atom).update(world.get(atom), 1, logWeight);
					
				}
				
			}
			
			for(int i : this.mrf.globalAtom.keySet()){
				if(!enumDomain.contains(i)){
					this.mrf.globalAtom.get(i).clear();
				}
			}
			
			ct = 0;
			for(MRFSampler sampler : samplers){
				Set<BitSet> sampledWorlds = 
						(Set<BitSet>) sampler.getSamplerStatistic(StatisticType.WorldFrequency).getStatisticDomain();
				
				SampleStatistic_WorldFrequency stat_freq = 
						(SampleStatistic_WorldFrequency) sampler.getSamplerStatistic(StatisticType.WorldFrequency);
				SampleStatistic_WorldLogWeight stat_weight = 
						(SampleStatistic_WorldLogWeight) sampler.getSamplerStatistic(StatisticType.WorldLogWeight);
				
				for(BitSet bitmap : sampledWorlds){
					
					BitSet mleworld = this.getProjectedBitmap(bitmap);
					
					if(!worlds.containsKey(mleworld)){
						worlds.put(mleworld, new MLEWorld(mleworld));
					}
					
					worlds.get(mleworld).tallyFreq(stat_freq.lookupStatistic(bitmap));
					worlds.get(mleworld).tallyLogCost(stat_weight.lookupStatistic(bitmap));

					double weight = stat_weight.lookupStatistic(bitmap);
					double freq = stat_freq.lookupStatistic(bitmap);
					
					for(int i : sampleDomains.get(ct)){

						this.mrf.globalAtom.get(i).update(bitmap.get(i), freq, weight);
						
					}
					
				}
				ct ++;
			
			}
		}
		
		
		
		

		
		
		
	}


	
	
}




