package tuffy.learn;

import tuffy.infer.MRF;
import tuffy.infer.MRF.INIT_STRATEGY;
import tuffy.infer.ds.GAtom;
import tuffy.util.UIMan;

/**
 * DO NOT USE THIS CLASS! USE {@link DNLearner}.
 * 
 * Learner instance using gradient descent.
 * 
 * Note, this class is for debugging and JUNIT test only. For
 * real circumstance usage, please turn to {@link NaiveDNLearner}.
 * If you are really a big fan of gradient descent, please
 * rewrite {@link NaiveGDLearner#loadingTrainingData(MCSAT)} according
 * to that in {@link NaiveDNLearner#loadingTrainingData(MCSAT)}.
 * @author Ce Zhang
 *
 */
public class NaiveGDLearner extends Learner {
	
	/**
	 * NEVER USE THIS IN REAL CIRCUMSTANCE... THIS IS FOR
	 * JUNIT TEST ONLY.
	 */
	public void loadingTrainingData(MRF _mcsat){

		while(true){
			// GENERATE SIMULATED TRAINING DATA
			_mcsat.setInitStrategy(INIT_STRATEGY.COIN_FLIP);
			_mcsat.initMRF();
			_mcsat.setInitStrategy(INIT_STRATEGY.COIN_FLIP);
			
			this.odds = 0;
			
			for(GAtom a : _mcsat.atoms.values()){
				if(a.truth == true)
					this.odds ++;
			}
			
			if(this.odds != 0 && this.odds != 8)
				break;
		}
		this.odds = this.odds / (8-this.odds);
		this.odds = Math.log(this.odds);
	}
	
	/**
	 * NEVER USE THIS IN REAL CIRCUMSTANCE... THIS IS FOR
	 * JUNIT TEST ONLY.
	 * Updating {@link Learner#currentWeight} using Gradient
	 * Descent method.
	 */
	public boolean updateWeight(MRF mcsat){
		double delta = 0;
		int n = 0;
		for(String k : mcsat.expectationOfViolation.keySet()){
			n++;
			Double ev = mcsat.expectationOfViolation.get(k);
			Double cw = currentWeight.get(k);
			assert(k!=null);
			Long trainv = this.trainingViolation.get(k);
			if(trainv == null)
				trainv = 0l;

			//if(cw>0 && cw*(cw + (0.01/(1)) * (ev - trainv))>0){
			if(cw>0){
				double newCW = cw + (0.01) * (ev - trainv);
			//	delta += (newCW - cw) * (ev - trainv);
				delta += (ev - trainv) * (ev - trainv);
				Learner.currentWeight.put(k, newCW);
			}
			//if(cw<0 && cw*(cw - (0.01/(1)) * (ev - trainv))>0){
			if(cw<0){
				double newCW = cw - (0.01) * (ev - trainv);
				//delta += (newCW - cw) * (-(ev - trainv));
				delta += (ev - trainv) * (ev - trainv);
				Learner.currentWeight.put(k, newCW);
			}
			//this.currentWeight.put(k, Math.random());
		}
		UIMan.println("AVG. DELTA = " + delta/n);

		// Terminating Standard
		//if(Math.abs(delta) < 0.1)
		if(delta == 0)
			return true;
		
		
		return false;
	}
}
