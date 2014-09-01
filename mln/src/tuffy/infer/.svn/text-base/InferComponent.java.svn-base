package tuffy.infer;


import java.util.ArrayList;
import java.util.Collections;
import java.util.Random;

import tuffy.ground.partition.Component;
import tuffy.ground.partition.Partition;
import tuffy.infer.MRF.INIT_STRATEGY;
import tuffy.infer.ds.GAtom;
import tuffy.util.Config;
import tuffy.util.MathMan;
/**
 * Performing inference on one MRF component.
 */
public class InferComponent {
	private Component comp;
	//private MarkovLogicNetwork mln;

	private double lowCost = Double.MAX_VALUE;
	
	public double getCost(){
		return lowCost;
	}
	
	public Component getComponent(){
		return comp;
	}
	
	public InferComponent(Component comp){
		//this.mln = mln;
		this.comp = comp;
	}

	

	/**
	 * Run partition-aware MAP inference with the Gauss-Seidel scheme.
	 * 
	 */
	public void inferMAP(int totalTries, int totalFlipsPerTry){
		int nflips, rounds;
		if(comp.numParts() == 1){
			rounds = 1;
			nflips = totalFlipsPerTry; 
		}else{
			rounds = Config.gauss_seidel_infer_rounds;
			nflips = totalFlipsPerTry/Config.gauss_seidel_infer_rounds;
		}
		inferGaussSeidelMap(rounds, totalTries, nflips);
	}

	/**
	 * Run partition-aware marginal inference with the Gauss-Seidel scheme.
	 * 
	 */
	public double inferMarginal(int totalSamples, int totalFlipsPerSample){
		int nflips, rounds;
		if(comp.numParts() == 1){
			rounds = 1;
			nflips = totalFlipsPerSample; 
		}else{
			rounds = Config.gauss_seidel_infer_rounds;
			nflips = totalFlipsPerSample/Config.gauss_seidel_infer_rounds;
		}
		
		return inferGaussSeidelMarginal(rounds, totalSamples, nflips);
	}
	
	public double inferMLE(int totalSamples, int totalFlipsPerSample){
		int nflips, rounds;
		if(comp.numParts() == 1){
			rounds = 1;
			nflips = totalFlipsPerSample; 
		}else{
			rounds = Config.gauss_seidel_infer_rounds;
			nflips = totalFlipsPerSample/Config.gauss_seidel_infer_rounds;
		}
		
		return inferGaussSeidelMLE(rounds, totalSamples, nflips);
	}

	
	
	private void initTruthRandom(){
		Random rand = new Random();
		for(GAtom a : comp.atoms.values()){
			a.lowTruth = a.truth = rand.nextBoolean();
		}
	}
	
	private void setMrfInitStrategy(INIT_STRATEGY strategy){
		for(Partition p : comp.parts){
			if(p.mrf == null) continue;
			p.mrf.setInitStrategy(strategy);
		}
	}
	
	/**
	 * Gauss-Seidel MAP inference scheme. Calls WalkSAT on each
	 * partition in a round-robin manner.
	 * 
	 * @param rounds
	 * @param ntries
	 * @param nflips total number of flips per try in one round
	 */
	private double inferGaussSeidelMap(int rounds, int ntries, int nflips){
		initTruthRandom();
		saveLowLowTruth();
		//setMrfInitStrategy(INIT_STRATEGY.COIN_FLIP);
		setMrfInitStrategy(INIT_STRATEGY.COPY_LOW);
		ArrayList<Partition> iparts = null;
		iparts = new ArrayList<Partition>(comp.parts);
		Collections.shuffle(iparts);
		for(int r=1; r<=rounds; r++){
			for(Partition p : iparts){
				if(p.mrf == null) continue;
				p.mrf.invalidateLowCost();
				p.mrf.inferWalkSAT(ntries, MathMan.prorate(nflips, 
					((double)p.numAtoms)/comp.numAtoms));
				p.mrf.restoreLowTruth();
			}
			saveLowLowTruth();
			setMrfInitStrategy(INIT_STRATEGY.COPY_LOW);
			Collections.shuffle(iparts);
		}
		setMrfInitStrategy(INIT_STRATEGY.COIN_FLIP);
		restoreLowLowTruth();
		return lowCost;
	}
	
	int totalSamples = 0;
	
	public int getTotalSamples(){
		return totalSamples;
	}

	/**
	 * Gauss-Seidel MAP inference scheme. Calls WalkSAT on each
	 * partition in a round-robin manner.
	 * 
	 * @param rounds
	 * @param ntries
	 * @param nflips total number of flips per try in one round
	 */
	private double inferGaussSeidelMarginal(int rounds, int nsamples, int nflips){
		if(!Config.snapshoting_so_do_not_do_init_flip){
			initTruthRandom();
		}
		saveLowLowTruth();
		ArrayList<Partition> iparts = null;
		iparts = new ArrayList<Partition>(comp.parts);
		Collections.shuffle(iparts);
		int rnsamples = Math.max(3, nsamples/rounds);
		
		double sumCost = 0;
		
		for(int r=1; r<=rounds; r++){
			for(Partition p : iparts){
				if(p.mrf == null) continue;
				p.mrf.invalidateLowCost();
				
				int flips = MathMan.prorate(nflips, 
						((double)p.numAtoms)/comp.numAtoms);
				
				rnsamples = nsamples;
				double cc = p.mrf.mcsat(rnsamples, flips);//TODO
				sumCost += cc;
				//p.mrf.updateAtomMarginalProbs(r*rnsamples);
			}
			Collections.shuffle(iparts);
		}
		
		totalSamples = rnsamples * rounds;
		return sumCost/totalSamples;
	}
	
	private double inferGaussSeidelMLE(int rounds, int nsamples, int nflips){
		if(!Config.snapshoting_so_do_not_do_init_flip){
			initTruthRandom();
		}
		saveLowLowTruth();
		ArrayList<Partition> iparts = null;
		iparts = new ArrayList<Partition>(comp.parts);
		Collections.shuffle(iparts);
		int rnsamples = Math.max(3, nsamples/rounds);
		
		double sumCost = 1;
		
		for(int r=1; r<=rounds; r++){
			for(Partition p : iparts){
				if(p.mrf == null) continue;
				p.mrf.invalidateLowCost();
				
				int flips = MathMan.prorate(nflips, 
						((double)p.numAtoms)/comp.numAtoms);
				
				double cc;
				//if(Config.mleTopK == -1){
				//	cc = p.mrf.mle_naiveMCMC(rnsamples, flips, null);//TODO
				//}else{
				//	cc = p.mrf.mle_naiveMCMC(rnsamples, flips, p.mle_freq_cache);//TODO
				//}
				
				///////TODO
				rnsamples = nsamples;
				cc = p.mrf.MLE_naiveSampler(rnsamples);
				//cc = p.mrf.MLE_populateTaskToDBFast(p.id, rnsamples, Config.innerPara);
				
				
				sumCost *= cc;
				//p.mrf.updateAtomMarginalProbs(r*rnsamples);
			}
			Collections.shuffle(iparts);
		}
		
		totalSamples = rnsamples * rounds;
		System.out.print(".");
		return sumCost;
	}
	
	private void saveLowLowTruth(){
		double cost = recalcCost();
		if(cost >= lowCost) return;
		lowCost = cost;
		for(GAtom a : comp.atoms.values()){
			a.lowlowTruth = a.lowTruth;
		}
	}
	

	private void restoreLowLowTruth(){
		for(GAtom a : comp.atoms.values()){
			a.truth = a.lowlowTruth;
		}
	}
	
	/**
	 * Recalculate the cost on this component, which is the sum
	 * of the cost on the MRF of each partition.
	 * 
	 * @see MRF#recalcCost()
	 */
	private double recalcCost(){
		double cost = 0;
		for(Partition p : comp.parts){
			if(p.mrf == null) continue;
			cost += p.mrf.recalcCost();
		}
		return cost;
	}



}
