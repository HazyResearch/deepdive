package tuffy.infer;



import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import tuffy.db.RDB;
import tuffy.ground.Grounding;
import tuffy.ground.partition.Bucket;
import tuffy.ground.partition.Component;
import tuffy.ground.partition.Partition;
import tuffy.ground.partition.PartitionScheme;
import tuffy.ground.partition.Partitioning;
import tuffy.infer.ds.GAtom;
import tuffy.mln.MarkovLogicNetwork;
import tuffy.util.Config;
import tuffy.util.Settings;
import tuffy.util.Timer;
import tuffy.util.UIMan;

/**
 * Scheduler of partition-aware inference.
 *
 */
public class InferPartitioned {
	MarkovLogicNetwork mln;
	DataMover dmover;
	RDB db;
	Grounding grounding;
	Partitioning parting;
	PartitionScheme pmap;
	public ArrayList<Bucket> wholeBuckets = new ArrayList<Bucket>();
	HashMap<Component, ArrayList<Bucket>> partBuckets = 
		new HashMap<Component, ArrayList<Bucket>>();
	
	public PartitionScheme getPartitionScheme(){
		return pmap;
	}
	
	
	public InferPartitioned(Grounding g, DataMover dmover){
		grounding = g;
		mln = g.getMLN();
		db = mln.getRDB();
		this.dmover = dmover;
		partition();
	}
	
	/**
	 * Partition the MRF produced by the grounding process.
	 */
	private void partition(){
		parting = new Partitioning(grounding);
		UIMan.println(">>> Partitioning MRF...");
		pmap = parting.partitionMRF(Config.partition_size_bound);
		UIMan.verbose(2, pmap.getStats());
		groupPartitionsIntoBuckets();
		int ncomp = pmap.numComponents();
		int npart = pmap.numParts();
		int nbuck = getNumBuckets();
		String sp = "### " + ncomp + " components; " + npart + " partitions; " + nbuck + " buckets"; 
		
		UIMan.println(sp);
		
		UIMan.verbose(1, sp);
	}

	public int getNumBuckets(){
		int nb = wholeBuckets.size();
		for(Component c : partBuckets.keySet()){
			nb += partBuckets.get(c).size();
		}
		return nb;
	}
	
	/**
	 * Group components/partitions to enable efficient batch loading and parallel inference.
	 */
	private void groupPartitionsIntoBuckets(){
		
		UIMan.println(">>> Grouping Components into Buckets...");
		for(Component c : pmap.components){
			if (Config.skipUselessComponents && !c.hasQueryAtom && !Config.no_pushdown) continue;
			if(c.size() <= Config.ram_size){
				boolean taken = false;
				for(Bucket z : wholeBuckets){
					if(z.size() + c.size() <= Config.ram_size && z.nComp < Config.max_number_components_per_bucket){
						taken = true;
						z.addComponent(c);
					}
				}
				if(!taken){
					Bucket z = new Bucket(db, pmap);
					z.addComponent(c);
					wholeBuckets.add(z);
				}
			}else{
				ArrayList<Bucket> zones = new ArrayList<Bucket>();
				Bucket z = new Bucket(db, pmap);
				zones.add(z);
				for(Partition p : c.parts){
					if(z.size() + p.size() <= Config.ram_size){
						z.addPart(p);
					}else{
						z = new Bucket(db, pmap);
						z.addPart(p);
						zones.add(z);
					}
				}
				partBuckets.put(c, zones);
			}
		}
	}

	
	public void setAtomBiases(HashMap<Integer, Double> deltas, boolean inv) {
		for(Bucket z : wholeBuckets){
			z.updateAtomBiases(deltas, inv);
		}
	}
	
	
	/**
	 * Run partition-aware MAP inference.
	 */
	public double infer(Settings s){
		
		double cost = 0;
		int numberOfSnap = 0;
		
		if(s.getString("task").equals("MAP")){
						
				cost = 0;
				numberOfSnap = 1;
		
				for(Bucket z : wholeBuckets){
					UIMan.println(">>> Processing " + z);
					UIMan.println("    Loading data...");
					z.load(mln);
					InferBucket ib = new InferBucket(z);
					UIMan.println("    Running inference with " + ib.getNumThreads() + " thread(s)...");
					ib.infer(s);
					UIMan.verbose(1, "    Flushing states...");
					ib.flushAtomStates(dmover, mln.relAtoms, true);
					cost += ib.getCost();
					if (!Config.warmTuffy) z.discard();
				}
				
				// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
				// TODO: bucket-wise merge of MLE result
				// !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
				
				
				// large components requires some swapping
				for(Component c : partBuckets.keySet()){
					double licost = Double.MAX_VALUE;
					ArrayList<Bucket> zones = partBuckets.get(c);
					for(int t=1; t<=Config.gauss_seidel_infer_rounds; t++){
						double icost = 0;
						for(Bucket z : zones){
							UIMan.println(">>> Processing " + z);
							UIMan.println("    Loading data...");
							z.load(mln);
							InferBucket ib = new InferBucket(z);
							if(t==1){
								ib.setMrfInitStrategy(tuffy.infer.MRF.INIT_STRATEGY.COIN_FLIP);
							}else{
								ib.setMrfInitStrategy(tuffy.infer.MRF.INIT_STRATEGY.COPY_LOW);
							}
							UIMan.println("    Running inference with " + ib.getNumThreads() + " thread(s)...");
							ib.infer(s);
							UIMan.verbose(1, "    Flushing states...");
							ib.flushAtomStates(dmover, mln.relAtoms, true);
							icost += ib.getCost();
							if (!Config.warmTuffy) z.discard();
						}
						if(icost < licost){
							licost = icost;
						}
					}
					cost += licost;
				}
				
			
		}else if(s.getString("task").equals("MLE")){

			cost = 0;
			
			int beginTime = (int) Timer.elapsedSeconds();
			
			// small components that fit into memory can be sovled in one shot

			int nsamples = s.getInt("nsamples");
			
			//if(Config.snapshot_mode){
			//	s.put("nsamples", 100);
			//}
			
			for(int i=0; i< nsamples; i+= s.getInt("nsamples")){
				
				numberOfSnap ++;
				
				int curTime = (int) Timer.elapsedSeconds();
							
				Config.currentSampledNumber += s.getInt("nsamples");
				
				if(i != 0){
					Config.snapshoting_so_do_not_do_init_flip = true;
				}
				
				UIMan.println(">>> MCSAT FOR SAMPLES " + i + " ~ " + (i+s.getInt("nsamples")));
				
				for(Bucket z : wholeBuckets){
					UIMan.println(">>> Processing " + z);
					UIMan.println("    Loading data...");
					if(!Config.snapshoting_so_do_not_do_init_flip){
						z.load(mln);
					}
					InferBucket ib = new InferBucket(z, Config.getNumThreads()/Config.innerPara + 1);
					UIMan.println("    Running inference with " + ib.getNumThreads() + " thread(s)...");
					ib.infer(s);
					UIMan.verbose(1, "    Flushing states...");
					ib.flushAtomStates(dmover, mln.relAtoms, false);
					cost += ib.getCost();
					//z.discard();
				}
				
				// large components requires some swapping
				for(Component c : partBuckets.keySet()){
					double licost = Double.MAX_VALUE;
					ArrayList<Bucket> zones = partBuckets.get(c);
					for(int t=1; t<=Config.gauss_seidel_infer_rounds; t++){
						double icost = 0;
						for(Bucket z : zones){
							UIMan.println(">>> Processing " + z);
							UIMan.println("    Loading data...");
							if(Config.snapshoting_so_do_not_do_init_flip){
								z.load(mln);
							}
							InferBucket ib = new InferBucket(z, Config.getNumThreads()/Config.innerPara + 1);
							if(t==1){
								ib.setMrfInitStrategy(tuffy.infer.MRF.INIT_STRATEGY.COIN_FLIP);
							}else{
								ib.setMrfInitStrategy(tuffy.infer.MRF.INIT_STRATEGY.COPY_LOW);
							}
							UIMan.println("    Running inference with " + ib.getNumThreads() + " thread(s)...");
							ib.infer(s);
							UIMan.verbose(1, "    Flushing states...");
							ib.flushAtomStates(dmover, mln.relAtoms, false);
							icost += ib.getCost();
							//z.discard();
						}
						licost += icost;
					}
					cost += licost/Config.gauss_seidel_infer_rounds;
				}
				
				int endTime = (int) Timer.elapsedSeconds();
				beginTime += endTime - curTime;
				
				if(beginTime > Config.timeout){
					UIMan.println("!!! TIME OUT AT " + (beginTime) + " sec.");
					break;
				}
			}
			
			return Math.exp(cost);
			
		}else if(s.getString("task").equals("MARGINAL")){
		
			cost = 0;
			
			int beginTime = (int) Timer.elapsedSeconds();
			
			// small components that fit into memory can be sovled in one shot

			int nsamples = s.getInt("nsamples");
			
			//if(Config.snapshot_mode){
			//	s.put("nsamples", 100);
			//}
			
			for(int i=0; i< nsamples; i+= s.getInt("nsamples")){
				
				numberOfSnap ++;
				
				int curTime = (int) Timer.elapsedSeconds();
							
				Config.currentSampledNumber += s.getInt("nsamples");
				
				if(i != 0){
					Config.snapshoting_so_do_not_do_init_flip = true;
				}
				
				UIMan.println(">>> MCSAT FOR SAMPLES " + i + " ~ " + (i+s.getInt("nsamples")));
				
				for(Bucket z : wholeBuckets){
					UIMan.println(">>> Processing " + z);
					UIMan.println("    Loading data...");
					if(!Config.snapshoting_so_do_not_do_init_flip){
						z.load(mln);
					}
					InferBucket ib = new InferBucket(z);
					UIMan.println("    Running inference with " + ib.getNumThreads() + " thread(s)...");
					ib.infer(s);
					UIMan.verbose(1, "    Flushing states...");
					ib.flushAtomStates(dmover, mln.relAtoms, false);
					cost += ib.getCost();
					//z.discard();
				}
				
				// large components requires some swapping
				for(Component c : partBuckets.keySet()){
					double licost = Double.MAX_VALUE;
					ArrayList<Bucket> zones = partBuckets.get(c);
					for(int t=1; t<=Config.gauss_seidel_infer_rounds; t++){
						double icost = 0;
						for(Bucket z : zones){
							UIMan.println(">>> Processing " + z);
							UIMan.println("    Loading data...");
							if(Config.snapshoting_so_do_not_do_init_flip){
								z.load(mln);
							}
							InferBucket ib = new InferBucket(z);
							if(t==1){
								ib.setMrfInitStrategy(tuffy.infer.MRF.INIT_STRATEGY.COIN_FLIP);
							}else{
								ib.setMrfInitStrategy(tuffy.infer.MRF.INIT_STRATEGY.COPY_LOW);
							}
							UIMan.println("    Running inference with " + ib.getNumThreads() + " thread(s)...");
							ib.infer(s);
							UIMan.verbose(1, "    Flushing states...");
							ib.flushAtomStates(dmover, mln.relAtoms, false);
							icost += ib.getCost();
							//z.discard();
						}
						licost += icost;
					}
					cost += licost/Config.gauss_seidel_infer_rounds;
				}
				
				int endTime = (int) Timer.elapsedSeconds();
				beginTime += endTime - curTime;
				
				if(beginTime > Config.timeout){
					UIMan.println("!!! TIME OUT AT " + (beginTime) + " sec.");
					break;
				}
			}
		
		}
		
		return cost/numberOfSnap;
	}
	
}
