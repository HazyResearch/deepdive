package tuffy.sample;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;

import tuffy.infer.MRF;
import tuffy.infer.ds.GAtom;
import tuffy.infer.ds.GClause;
import tuffy.util.Config;
import tuffy.util.ExceptionMan;
import tuffy.util.Timer;
import tuffy.worker.Worker;
import tuffy.sample.MRFSampleStatistic.StatisticType;

public class MRFSampler extends Worker{

	public Class<? extends MRFSampleAlgorithm> sampleAlgo = null; 
	
	public ArrayList<Integer> sampleDomain = null;
	
	public int nSample = 0;
	public HashMap<String, Object> prop = null;
	
	public MRFSampler(MRF _mrf, Class<? extends MRFSampleAlgorithm> _sampleAlgo, 
			HashMap<String, Object> _prop, int _nSample){
		
		super(_mrf);
		sampleAlgo = _sampleAlgo;
		prop = _prop;
		nSample = _nSample;
		
	}
	
	public MRFSampler(MRF _mrf, Class<? extends MRFSampleAlgorithm> _sampleAlgo, 
			HashMap<String, Object> _prop, int _nSample, ArrayList<Integer> _sampleDomain){
		
		super(_mrf);
		sampleAlgo = _sampleAlgo;
		prop = _prop;
		nSample = _nSample;
		sampleDomain = _sampleDomain;
		
	}

	public HashMap<StatisticType, MRFSampleStatistic> statMaps = new 
			HashMap<StatisticType, MRFSampleStatistic>();
	
	public void addOrReplaceSamplerStatistic(MRFSampleStatistic samplerStatistic){
		this.statMaps.put(samplerStatistic.type, samplerStatistic);
	}
	
	public MRFSampleStatistic getSamplerStatistic(StatisticType type){
		return this.statMaps.get(type);
	}
	
	private MRFSampleAlgorithm getSampleAlgorithmInstance(Class<? extends MRFSampleAlgorithm> _sampleAlgo) {
			
		try{
			Class[] types = new Class[] { HashMap.class, ArrayList.class };
			Constructor cons = _sampleAlgo.getConstructor(types);
			Object[] args = new Object[] { prop, sampleDomain};
			MRFSampleAlgorithm sampleAlgoInstance = (MRFSampleAlgorithm) cons.newInstance(args);
			return sampleAlgoInstance;
		}catch(Exception e){
			ExceptionMan.handle(e);
		}
		return null;
		
	}
	
	public boolean isPowerOfTwo(int n){
		return ((n!=0) && (n&(n-1))==0);
	}
	
	public double sample(){
		
		double logWeight = Double.NEGATIVE_INFINITY;
		
		MRFSampleAlgorithm sampleAlgoInstance = this.getSampleAlgorithmInstance(this.sampleAlgo);
		
		if(Config.mle_optimize_small_components && 
				sampleAlgoInstance.capable_for_small_components_optimization == true &&
				(Math.pow(2, this.mrf.atoms.size()) <= nSample
				||
				this.mrf.atoms.size()<7)){
			//System.out.print("~");
			sampleAlgoInstance = this.getSampleAlgorithmInstance(SampleAlgorithm_BruteForceEnumerate.class);
		}
		
		sampleAlgoInstance.init(this.mrf);
		
		double knob = Math.pow(2, this.mrf.atoms.size()+1);
		int size = this.mrf.atoms.size();
		
		HashSet<Integer> history_po2 = new HashSet<Integer>();
		
		double time = 0;
		
		
		double maxWeight = Double.NEGATIVE_INFINITY;
		double treeWidth = 0;
		double avgDegree = 0;
		if(Config.sampleLog != null){
			for(GClause gc : this.mrf.clauses){
				if(maxWeight <= gc.weight){
					maxWeight = gc.weight;
				}
			}
			this.mrf.buildIndices();
			int nn = 0;
			int nneigh = 0;
			for(Integer atom : this.mrf.adj.keySet()){
				nn += 1;
				nneigh +=this.mrf.adj.get(atom).size();
			}
			avgDegree = 1.0*nneigh/nn;
			DS_JunctionTree jt = new DS_JunctionTree(this.mrf);
			treeWidth = jt.getTreeWidth();
		}

		for(int i=1;i<this.nSample+1;i++){
			

			if(Config.sampleLog != null){
				Timer.start(this + "");
			}
			
			MRFSampleResult rs = sampleAlgoInstance.getNextSample();
			
			if(Config.sampleLog != null){
				time += Timer.elapsedMilliSeconds(this + "");
			}
			
			if(rs == null && sampleAlgoInstance.hasStopped == true){
				break;
			}
			
			if(rs == null){	// if MRFSampleResult == null, it means the sampler
							// does not want to continue.
				continue;
			}
			
			for(StatisticType statType : statMaps.keySet()){
				MRFSampleStatistic stat = statMaps.get(statType);
				stat.process(rs);
			}
			
			double cost = -this.mrf.getCost(rs.world);
			logWeight = Config.logAdd(logWeight, cost);
			
			
			if(Config.sampleLog != null){
				
				for(int iatom : this.mrf.globalAtom.keySet()){
					GAtom atom = this.mrf.globalAtom.get(iatom);
					atom.update(rs.world.get(iatom), 1, cost);
				}
				
				if( Math.log10(i) == (int)Math.log10(i)
						|| (i % (nSample/100) == 0) 
						|| isPowerOfTwo(nSample/i)){
					
					String sig = "";
					if(isPowerOfTwo(nSample/i)){
						sig += "exp" + (nSample/i) + "|";
					}
					
					if(Math.log10(i) == (int)Math.log10(i)){
						sig += "log10|";
					}
					
					if((i%100 == 0 && (i/100) % size == 0)){
						sig += "linear|";
					}
					
					if(history_po2.contains(nSample/i) && sig.equals("exp" + (nSample/i) + "|")){
						continue;
					}
					history_po2.add(nSample/i);
					
					for(int iatom : this.mrf.globalAtom.keySet()){
						GAtom atom = this.mrf.globalAtom.get(iatom);
						//atom.update(rs.world.get(iatom), 1, cost);
				
						double prob = Math.exp(atom.tallyTrueLogWeight - atom.tallyLogWeight);
						if(prob < 0.00000000001){
							prob = 0.00000000001;
						}
						
						Config.sampleLog.println(
						//System.out.println(
									i + "\t" + 
									sig + "\t" + 
									this.sampleAlgo.getName() + "\t" + 
									this.mrf.atoms.size() + "\t" +
									time + "\t" +
									atom.id + "\t" +
									atom.pid + "\t" + 
									atom.tallyTrueLogWeight + "\t" +
									atom.tallyLogWeight + "\t" + 
									atom.tallyTrueFreq + "\t" +
									atom.tallyFreq + "\t" +
									prob + "\t" + 
									atom.tallyTrueFreq/atom.tallyFreq + "\t" + 
									maxWeight + "\t" + 
									treeWidth + "\t" + 
									avgDegree
						);
					}
				}
				
				
			}
		
			
		}
				
		return logWeight;
	
	}
	
	
}






