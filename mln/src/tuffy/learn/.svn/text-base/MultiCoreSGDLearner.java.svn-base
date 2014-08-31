package tuffy.learn;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import tuffy.ground.partition.Bucket;
import tuffy.ground.partition.Component;
import tuffy.ground.partition.Partition;
import tuffy.ground.partition.PartitionScheme;
import tuffy.infer.InferBucket;
import tuffy.infer.InferPartitioned;
import tuffy.infer.MRF;
import tuffy.main.Infer;
import tuffy.mln.Clause;
import tuffy.mln.Clause.ClauseInstance;
import tuffy.mln.Predicate;
import tuffy.parse.CommandOptions;
import tuffy.util.BitSetIntPair;
import tuffy.util.Config;
import tuffy.util.ExceptionMan;
import tuffy.util.FileMan;
import tuffy.util.Settings;
import tuffy.util.StringMan;
import tuffy.util.Timer;
import tuffy.util.UIMan;
import tuffy.worker.MLEWorker_sgdWorker;

public class MultiCoreSGDLearner extends Infer{
	
	public ConcurrentHashMap<String, Double> weights = null;
	
	public HashSet<String> fixedWeights = null;
	
	public InferPartitioned ip = null;
	
	public ArrayList<Partition> parts = null;
	
	ExecutorService threadExecutor = null;
	
	CommandOptions options = null;
	
	public void initThreadsPool(int nThreads){
		
		this.threadExecutor = Executors.newFixedThreadPool( nThreads );		
		
	}
	
	public void mainMemorySampler(InferPartitioned ip){
		
		parts = new ArrayList<Partition>();
		
		boolean isOnlyOneBucket = false;
		//if(ip.wholeBuckets.size() == 1){
			isOnlyOneBucket = true;
		
			for(Bucket z : ip.wholeBuckets){
				UIMan.println(">>> Processing " + z);
				UIMan.println("    Loading data...");
				z.load(mln);
				InferBucket ib = new InferBucket(z);
				for (Component comp : ib.bucket.getComponents()) {
					if (comp.hasQueryAtom || Config.no_pushdown) {
						parts.addAll(comp.parts);
					}
				}
			}
		//}
				
		UIMan.println(">>> Running MLE Learning on " + parts.size() + " partitions with " 
				+ Config.getNumThreads() + " threads...");
				
		// init ori weights
		UIMan.println(">> Init Weights...");
		
		weights = new ConcurrentHashMap<String, Double>();
		fixedWeights = new HashSet<String>();
		
		String sql = "SELECT DISTINCT weight, ffcid FROM " + Config.db_schema + "." + "mln" + mln.getID() + "_cbuffer" + ";";
		ResultSet rs = db.query(sql);
		try {
			while(rs.next()){
				String ffcid = rs.getString("ffcid");
				Double wght = rs.getDouble("weight");
				String newCID = ffcid;
				if(newCID.charAt(0) == '-'){
					newCID = newCID.substring(1, newCID.length());
					wght = -wght;
				}
				
				
				if(newCID.endsWith("fixed")){
					fixedWeights.add(newCID);
				}

				String[] clauses = newCID.replace("check", "").split("\\.");
				int clauseID = Math.abs(Integer.parseInt(clauses[0]));
				
				Clause c = this.mln.getClauseById(clauseID);

				if(newCID.endsWith("check") || (c != null && c.isTemplate()) ){
					int insCount = 0;
	
					int instanceID = Integer.parseInt(clauses[1]);				
									
					//for(ClauseInstance ci : c.instances){
					ClauseInstance ci = c.instances.get(instanceID - 1);
						
					if(ci.isFixedWeight){
						fixedWeights.add(newCID);
					}	
				}
				
				weights.put(newCID, wght);
				
			}
			rs.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		
		/*
		for(Clause c : this.mln.listClauses){
			
			//TODO: change the non-fixed assumption
			
			if(c.isTemplate() == false){
				String ffcid = c.getId() + "." + "0";
				weights.put(ffcid, c.getWeight());
				
				//TODO
				//c.isFixedWeight = false;
				
				if(c.isFixedWeight){
					fixedWeights.add(ffcid);
				}
				
				continue;
			}
			int insCount = 0;
			for(ClauseInstance ci : c.instances){
				insCount ++;
				String ffcid = c.getId() + "." + insCount;
				weights.put(ffcid, ci.weight);
				
				//TODO
				//ci.isFixedWeight = false;
				
				if(ci.isFixedWeight){
					fixedWeights.add(ffcid);
				}
				
			}
		}*/
		
		for(String ffcid : this.mln.additionalHardClauseInstances){
			weights.put(ffcid, Config.hard_weight);
			fixedWeights.add(ffcid);
		}
		
		
		
		System.out.println(">>> I thought I need to deal with " + parts.size() + " partitions... :-(");
		// compile
		ArrayList<Partition> prunedPartitions = new ArrayList<Partition>();
		ArrayList<ArrayList<Partition>> chunks = new ArrayList<ArrayList<Partition>>();
		ArrayList<Partition> currentChunk = new ArrayList<Partition>();
		chunks.add(currentChunk);
		
		int size =0;
		if(isOnlyOneBucket){
			UIMan.println(">> Compiling Components...");
			for(Partition p : parts){
				p.mrf.compile();
				
				if(p.mrf.isQueryForLearning.size() == p.mrf.globalAtom.size()){
					continue;
				}else{
					//prunedPartitions.add(p);
					size ++;
					
					if(currentChunk.size() < 10000){
						currentChunk.add(p);
					}else{
						currentChunk = new ArrayList<Partition>();
						chunks.add(currentChunk);
						currentChunk.add(p);
					}
					
				}
				
				UIMan.print("");
			}
		}

		parts.clear();
		System.out.println(">>> Actually I only need to work on " + size + " partitions... :-)");
		System.out.println(">>> I will work on " + chunks.size() + " chunks...");
		
		// run
		int nEpoch = options.nDIteration;
		double alpha = this.options.sgd_stepSize;
		double mu = this.options.sgd_mu;
		int nSample = this.options.sgd_metaSample;
		double decay = this.options.sgd_decay;
		
		if(isOnlyOneBucket){
			for(int i=0; i< nEpoch; i++){
							
				//this.initThreadsPool(1);
				
				Timer.start("epoch");
				
				UIMan.println("|>>> Epoch = " + i);
				UIMan.println("|---------------------------------------");
				UIMan.println("|  |- Step size             = " + alpha);
				UIMan.println("|  |- MU                    = " + mu);
				UIMan.println("|  |- Sample for Each Comp. = " + nSample);
				UIMan.println("|  |- Decay Factor          = " + decay);
				UIMan.println("|---------------------------------------");
				
				
				MLEWorker_sgdWorker.gradientNorm = 0;
				//HashSet<MLEWorker_sgdWorker> workers = new HashSet<MLEWorker_sgdWorker>();
				int npart = 0;
				
				for(ArrayList<Partition> parts : chunks){
				
					npart ++;
					if(Config.debug_mode) System.out.println("       start shuffle " + npart + "...");
					this.initThreadsPool(Config.getNumThreads());
					Collections.shuffle(parts);
					if(Config.debug_mode) System.out.println("       end shuffle...");
					
						
					for(Partition p : parts){
						
						if(Config.debug_mode) System.out.print(".");
						
						if(p.mrf.clauses.size() == 0){
							continue;
						}
						
						if(p.mrf.isQueryForLearning.size() > 10000){
							continue;
						}
	
						if(Config.debug_mode) System.out.print("*");
						
						if(Math.random() > 1){	// TODO: stochastic on component side
							continue;
						}
						
						
								//MRF _mrf, int _nSamples, double _alpha, double _mu, HashMap<String, Double> _weights
						MLEWorker_sgdWorker worker = new MLEWorker_sgdWorker(p.mrf, nSample, alpha, mu, weights, fixedWeights, false);
							//workers.add(worker);
						
						if(Config.debug_mode) System.out.print("#");
						
						this.threadExecutor.execute(worker);
						
						if(Config.debug_mode) System.out.print("-");
						
					
					}
					threadExecutor.shutdown();
					while (!threadExecutor.isTerminated()) {
					}
					
					//System.out.println(">>> Run GC for " + npart + "...");
					//System.gc();
					//System.out.println(">>> Finish GC...");				
				}
				
				if(Config.addReporter){
					
					UIMan.println("|  Running Reporters...");
				
					MLEWorker_sgdWorker.gradientCache.clear();
					
					
					
					npart = 0;
					for(ArrayList<Partition> parts : chunks){
						
						npart ++;
						this.initThreadsPool(Config.getNumThreads());
						for(Partition p : parts){
							
							if(p.mrf.clauses.size() == 0){
								continue;
							}
							
							if(p.mrf.isQueryForLearning.size() > 10000){
								continue;
							}
								
							if(Math.random() > 1){	// TODO: stochastic on component side
								continue;
							}
							
								
									//MRF _mrf, int _nSamples, double _alpha, double _mu, HashMap<String, Double> _weights
							MLEWorker_sgdWorker worker = new MLEWorker_sgdWorker(p.mrf, nSample, alpha, mu, weights, fixedWeights, true);
								//workers.add(worker);
							this.threadExecutor.execute(worker);
							
						}
						threadExecutor.shutdown();
						while (!threadExecutor.isTerminated()) {
						}
					
						//System.out.println(">>> Run GC for " + npart + "...");
						//System.gc();
						//System.out.println(">>> Finish GC...");
					}
					
					double gradNorm = 0.0;
					for(String ffcid : MLEWorker_sgdWorker.gradientCache.keySet()){
						
						double value = MLEWorker_sgdWorker.gradientCache.get(ffcid).value;
						gradNorm += value*value;
						
					}
					gradNorm = Math.sqrt(gradNorm);
					
					UIMan.println("|  Gradient Norm = " + gradNorm);
					//System.out.println(weights);
					
					if(Config.snapshot_mode){
						Timer.runStat.markInferDone();
						UIMan.println("|   (Snapshot: Writing answer to file: " + options.fout + "_epoch_" + i + ".prog)");
						this.dumpAnswers(weights, options.fout + "_epoch_" + i + ".prog");
					}
					
					if(MLEWorker_sgdWorker.gradientNorm < 0.01){
						break;
					}

				}
				
				UIMan.println("|>>> Epoch " + i + " uses " + Timer.elapsed("epoch"));
				UIMan.println();
				
				UIMan.verbose(4, "" + weights);
				
				alpha = alpha * decay;
					
			}
		}else{
			
			ExceptionMan.die("Features for multiple buckets are coming soon...");
			
		}

//		System.out.println(weights);
		
		//double sumCost = mrf.MLE_naiveSampler(options.mcsatSamples);
		Object[] keySet1 = weights.keySet().toArray();
		java.util.Arrays.sort(keySet1);
				
		Timer.runStat.markInferDone();
		UIMan.println(">>> Writing answer to file: " + options.fout + ".prog");
		this.dumpAnswers(weights, options.fout + ".prog");
		
	}

	public void run(CommandOptions opt) throws SQLException{
		
		UIMan.println(">>> Running partition-aware MLE Learning.");
		
		this.options = opt;
		
		Config.track_clause_provenance = true;
		Config.learning_mode = true;
		
		setUp(opt);
		ground();
		
		ip = new InferPartitioned(grounding, dmover);
		
		if(options.maxFlips == 0){
			options.maxFlips = 100 * grounding.getNumAtoms();
		}
		if(options.maxTries == 0){
			options.maxTries = 3;
		}
		
		PartitionScheme pmap = ip.getPartitionScheme();
		int ncomp = pmap.numComponents();
		int nbuck = ip.getNumBuckets();
		String sdata = UIMan.comma(ncomp) + (ncomp > 1 ? " components" : " component");
		sdata += " (grouped into ";
		sdata += UIMan.comma(nbuck) + (nbuck > 1 ? " buckets" : " bucket)");
				
		UIMan.println(">>> Running MLE inference...");
		String mfout = options.fout;
		if(opt.dual) mfout += ".mle";
			
		mainMemorySampler(ip);
				
		//cleanUp();
		
	}
	
	public void run_noSetup(CommandOptions opt) throws SQLException{
		
		UIMan.println(">>> Running partition-aware MLE Learning.");
		
		this.options = opt;
		
		Config.track_clause_provenance = true;
		Config.learning_mode = true;
		
		setUp_noloading(opt);
		ground();
		
		ip = new InferPartitioned(grounding, dmover);
		
		if(options.maxFlips == 0){
			options.maxFlips = 100 * grounding.getNumAtoms();
		}
		if(options.maxTries == 0){
			options.maxTries = 3;
		}
		
		PartitionScheme pmap = ip.getPartitionScheme();
		int ncomp = pmap.numComponents();
		int nbuck = ip.getNumBuckets();
		String sdata = UIMan.comma(ncomp) + (ncomp > 1 ? " components" : " component");
		sdata += " (grouped into ";
		sdata += UIMan.comma(nbuck) + (nbuck > 1 ? " buckets" : " bucket)");
				
		UIMan.println(">>> Running MLE inference...");
		String mfout = options.fout;
		if(opt.dual) mfout += ".mle";
			
		mainMemorySampler(ip);
				
		//cleanUp();
		
	}
	
	public void dumpAnswers(Map<String, Double> currentWeight, String fout){
		ArrayList<String> lines = new ArrayList<String>();
		DecimalFormat twoDForm = new DecimalFormat("#.####");
		
		HashSet<Predicate> allp = mln.getAllPred();
		for(Predicate p : allp){
			String s = "";
			if(p.isClosedWorld()){
				//System.out.print("*");
				s += "*";
			}
			//System.out.print(p.getName() + "(");
			s += p.getName() + "(";
			for(int i=0;i<p.arity();i++){
				//System.out.print(p.getTypeAt(i).name());
				s += p.getTypeAt(i).name();
				if(i!=p.arity()-1){
					//System.out.print(",");
					s += ",";
				}
			}
			//System.out.println(")");
			s += ")";
			lines.add(s);
		}
		lines.add("\n");
		//System.out.println();
		
		Object[] keySet = currentWeight.keySet().toArray();
		java.util.Arrays.sort(keySet);

		lines.add("\n");
		
		lines.add("//////////////WEIGHT OF LAST ITERATION//////////////");
		keySet = currentWeight.keySet().toArray();
		java.util.Arrays.sort(keySet);
		for(Object ss : keySet){
			String s = (String) ss;
			String sid = s.replaceAll("fixed", "").replaceAll("check", "");
			//System.out.println(s + "\t" + Learner.currentWeight.get(s) + ":" + 
			//		this.trainingSatisification.get(s) + "/" + this.trainingViolation.get(s) + 
			//		"\t" + Clause.mappingFromID2Desc.get(s));
			
			String[] clauses = s.split("\\.");
			int clauseID = Math.abs(Integer.parseInt(clauses[0]));
			
			if(Clause.mappingFromID2Desc.containsKey(sid)){
				lines.add("" + twoDForm.format(currentWeight.get(s)) + " " + Clause.mappingFromID2Desc.get(sid) + " //" + s);
			}else{
				if(this.mln.getClauseById(clauseID) != null){
					lines.add(this.mln.getClauseById(clauseID).
							toStringForFunctionClause(s, currentWeight.get(s)));
				}
				//	lines.add("" + this.mln.getClauseById(id)
			//
				//		
				//		twoDForm.format(currentWeight.get(s)) + " " + Clause.mappingFromID2Desc.get(s) + " //" + s);
			}
		}
		
		System.out.println(">> Flushing Learnt Weight...");
		for(Bucket z : ip.wholeBuckets){
			for(Partition p : z.getPartitions()){
				p.mrf.updateWeight(currentWeight);
			}
		}
		
		FileMan.writeToFile(fout, StringMan.join("\n", lines));
	}
	
	public void inferencePhase() throws SQLException{
		
		this.options.isDLearningMode = false;
		Config.learning_mode = false;
		Config.track_clause_provenance = false;
		
		if(this.options.marginal == false){
		
			if(options.maxFlips == 0){
				options.maxFlips = 100 * grounding.getNumAtoms();
			}
			if(options.maxTries == 0){
				options.maxTries = 3;
			}
			
			PartitionScheme pmap = ip.getPartitionScheme();
			int ncomp = pmap.numComponents();
			int nbuck = ip.getNumBuckets();
			String sdata = UIMan.comma(ncomp) + (ncomp > 1 ? " components" : " component");
			sdata += " (grouped into ";
			sdata += UIMan.comma(nbuck) + (nbuck > 1 ? " buckets" : " bucket)");
			
			Settings settings = new Settings(); 
			Double fpa = ((double)options.maxFlips)/grounding.getNumAtoms();
			
			UIMan.println(">>> Running MLE inference...");
			String mfout = options.fout;
			
			settings.put("task", "MLE");
			settings.put("nsamples", new Integer(options.mcsatSamples));
			settings.put("flipsPerAtom", fpa);
			double lowCost = ip.infer(settings);
	
			UIMan.println("### Prob = " + lowCost);
			
			UIMan.println(">>> Writing answer to file: " + mfout);
			dmover.dumpTruthToFile(mln.relAtoms, mfout);
			
			//dmover.dumpMLETruthToFile(mfout);
			
			UIMan.println(">>> Writing answer to file: " + mfout + ".prob");
			dmover.dumpProbsToFile(mln.relAtoms, mfout + ".prob");
		}else{
			
			if(options.maxFlips == 0){
				options.maxFlips = 100 * grounding.getNumAtoms();
			}
			if(options.maxTries == 0){
				options.maxTries = 3;
			}
			
			PartitionScheme pmap = ip.getPartitionScheme();
			int ncomp = pmap.numComponents();
			int nbuck = ip.getNumBuckets();
			String sdata = UIMan.comma(ncomp) + (ncomp > 1 ? " components" : " component");
			sdata += " (grouped into ";
			sdata += UIMan.comma(nbuck) + (nbuck > 1 ? " buckets" : " bucket)");
			
			Settings settings = new Settings(); 
			Double fpa = ((double)options.maxFlips)/grounding.getNumAtoms();
			
			UIMan.println(">>> Running marginal inference on " + sdata);
			String mfout = options.fout;
			if(options.dual) mfout += ".marginal";
			
			settings.put("task", "MARGINAL");
			settings.put("nsamples", new Integer(options.mcsatSamples));
			settings.put("flipsPerAtom", fpa);
			double aveCost = ip.infer(settings);
			
			UIMan.println("### Average Cost " + UIMan.decimalRound(2,aveCost));
			UIMan.println(">>> Writing answer to file: " + mfout);
			dmover.dumpProbsToFile(mln.relAtoms, mfout);
			
		}
		
		
	}
	
}















