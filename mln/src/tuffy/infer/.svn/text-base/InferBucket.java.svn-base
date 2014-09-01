package tuffy.infer;


import java.util.ArrayList;
import java.util.HashMap;

import tuffy.ground.partition.Bucket;
import tuffy.ground.partition.Component;
import tuffy.ground.partition.Partition;
import tuffy.infer.MRF.INIT_STRATEGY;
import tuffy.infer.ds.GAtom;
import tuffy.util.Config;
import tuffy.util.ExceptionMan;
import tuffy.util.Settings;
import tuffy.util.UIMan;
/**
 * 
 * A bucket of inference tasks that can run in prallel. Currently, each task
 * correspond to an MRF component so that the components can be processed in parallel.
 */
public class InferBucket{
	
	public Bucket bucket;
	private int numThreads = 2;
	private double cost;
	
	private int totalSamples = 0;

	
	Config.TUFFY_INFERENCE_TASK task = null;
	Settings settings = null;
	
	public void infer(Settings s){
		settings = s;
		task = Config.TUFFY_INFERENCE_TASK.valueOf(s.getString("task"));
		this.runInferParallel();
	}
	
	
	public InferBucket(Bucket bucket){
		this.bucket = bucket;
		numThreads = Config.getNumThreads();
	}

	public InferBucket(Bucket bucket, int nThreads){
		this.bucket = bucket;
		numThreads = nThreads;
	}
	
	public void flushAtomStates(DataMover dmover, String relAtoms, boolean isMAP){
		ArrayList<GAtom> gatoms = new ArrayList<GAtom>();
		for(Component c: bucket.getComponents()){
			if(c.atoms == null) continue;
			gatoms.addAll(c.atoms.values());
		}
		dmover.flushAtomStates(gatoms, relAtoms, isMAP);
		
		if(Config.mleTopK != -1){
			dmover.flushTopKAtomStates(gatoms, bucket.getComponents(), relAtoms, isMAP);
		}
	}
	
	
	public void setMrfInitStrategy(INIT_STRATEGY strategy){
		for(Partition p : bucket.getPartitions()){
			if(p.mrf == null) continue;
			p.mrf.setInitStrategy(strategy);
		}
	}
	
	/**
	 * Get the cost after inference.
	 */
	public double getCost(){
		return cost;
	}
	
	public double getSamples(){
		return this.totalSamples;
	}

	private Object sentinel = new Object();
	
	/**
	 * Add up the cost.
	 * 
	 * @see CompWorker#run()
	 * @param c
	 */
	public void addCost(double c){
		synchronized(sentinel){
			cost += c;
		}
	}
	
	/**
	 * Add up the cost.
	 * 
	 * @see CompWorker#run()
	 * @param c
	 */
	public void addSamples(int c){
		synchronized(sentinel){
			this.totalSamples += c;
		}
	}
	
	/**
	 * A worker thread that runs inference on one component at a time.
	 * Used for parallelization in a producer-consumer model.
	 */
	public static class CompWorker extends Thread{
		InferBucket ibucket;
		Config.TUFFY_INFERENCE_TASK task;
		Settings settings;
		
		public CompWorker(InferBucket bucket){
			this.ibucket = bucket;
			settings = bucket.settings;
			task = bucket.task;
		}
		
		public void run(){
			while(true){
				Component comp = ibucket.getTask();
				if(comp == null) return;
				//UIMan.println(this + " is processing " + comp + " with " + comp.numAtoms + " atoms and " + comp.numClauses);
				InferComponent ic = new InferComponent(comp);
				switch(task){
				case MAP:
					int ntries = (Integer)(settings.get("ntries"));
					double flipsPerAtom = (Double)(settings.get("flipsPerAtom"));
					int nflips = (int)(flipsPerAtom * comp.numAtoms);
					//UIMan.println("flips = " + nflips + "; tries = " + ntries);
					ic.inferMAP(ntries, nflips);
					ibucket.addCost(ic.getCost());
					break;
				case MARGINAL:
					int nsamples = (Integer)(settings.get("nsamples"));
					flipsPerAtom = (Double)(settings.get("flipsPerAtom"));
					nflips = (int)(flipsPerAtom * comp.numAtoms);
					ibucket.addCost(ic.inferMarginal(nsamples, nflips));
					break;
				case MLE:
					nsamples = (Integer)(settings.get("nsamples"));
					flipsPerAtom = (Double)(settings.get("flipsPerAtom"));
					nflips = (int)(flipsPerAtom * comp.numAtoms);
					
					ibucket.addCost(Math.log(ic.inferMLE(nsamples, nflips)));
										
					break;
				}
			}
		}
	}
	
	/**
	 * The queue of components to be processed
	 */
	private ArrayList<Component> q = null;
	
	/**
	 * Get the next unprocessed component in the queue
	 * 
	 * @see CompWorker#run()
	 */
	public Component getTask(){
		synchronized(sentinel){
			if(q.isEmpty()) return null;
			Component c = q.remove(q.size()-1);
			//UIMan.println("    got " + c + " from the queue");
			return c;
		}
	}

	/**
	 * Solve the components in parallel.
	 * @param ntries
	 * @param nflips
	 */
	private void runInferParallel(){
		cost = 0;
		q = new ArrayList<Component>();
		int skipped = 0;
		for (Component comp : bucket.getComponents()) {
			if (comp.hasQueryAtom || Config.no_pushdown) {
				q.add(comp);
			} else {
				skipped++;
			}
		}
		if (skipped > 0) {
			UIMan.verbose(1, "    Skipped " + skipped + " components that do not have queries.");
		}
		//q.addAll(bucket.getComponents());
		ArrayList<CompWorker> workers = new ArrayList<CompWorker>();
		
		
		boolean initSilent = UIMan.isSilent();
		if(numThreads > 1 || Config.silent_on_single_thread){
			UIMan.setSilent(true);
		}
		for(int i=0; i<numThreads; i++){
			CompWorker t = new CompWorker(this);
			workers.add(t);
			t.start();
		}
		for(CompWorker t : workers){
			try {
				t.join();
			} catch (InterruptedException e) {
				ExceptionMan.handle(e);
			}
		}
		UIMan.setSilent(initSilent);
	}


	public void setNumThreads(int numThreads) {
		this.numThreads = numThreads;
	}


	public int getNumThreads() {
		return numThreads;
	}
	

	
}
