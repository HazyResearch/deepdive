package tuffy.learn;


import java.util.HashMap;

import tuffy.infer.MRF;
import tuffy.infer.MRF.INIT_STRATEGY;
import tuffy.util.UIMan;

/**
 * DO NOT USE THIS CLASS! USE {@link DNLearner}.
 * 
 * Learner instance using diagonal Newton with stable
 * step size = 0.01.
 * 
 * Note, this is a very simple diagonal Newton
 * learner. When use it for real circumstance,
 * please check it carefully...
 * @author Ce Zhang
 *
 */
@Deprecated
public class NaiveDNLearner extends Learner{
	
	public HashMap<String, Double> oldG = null;
	public HashMap<String, Double> oldD = null;
	public HashMap<String, Double> oldDW = null;
	public double oldDHD = 0.0;
	public double oldDG = 0.0;
	public double lambda = 100.0;
	public double alpha = 0.01;
	public int nCall = 0;
	
	/**
	 * Loading training data's truth value into MRF.
	 */
	public void loadingTrainingData(MRF _mcsat){
		// CURRENT ATOM'S TRUTH VALUE IS IN THE MRF. JUST CALC.
		// COST AND BUILD INDEX.
		_mcsat.setInitStrategy(INIT_STRATEGY.NO_CHANGE);
		_mcsat.initMRF();
		_mcsat.setInitStrategy(INIT_STRATEGY.COIN_FLIP);
			
	}
	
	/**
	 * Updating {@link Learner#currentWeight} using Diagonal
	 * Newton method.
	 */
	public boolean updateWeight(MRF mcsat){
		double delta = 0;
		double delta2 = 0;
		int n = 0;
		
		nCall ++;
		
		alpha = 0.01;
		for(String k : mcsat.expectationOfViolation.keySet()){
			n++;
			Double ev = mcsat.expectationOfViolation.get(k);
			Double cw = currentWeight.get(k);
			assert(k!=null);
			Long trainv = this.trainingViolation.get(k);
			Double ev2 = mcsat.expectationOfSquareViolation.get(k);
			Double e2v = ev*ev;
			
			if(trainv == null)
				trainv = 0l;
			
			
			if(Math.abs(ev2 - e2v)<1){
				if(ev2 > e2v)
					ev2 = e2v + 1;
				else
					ev2 = e2v - 1;
			}
			
			if(Learner.isHardMappings.get(k) != null)
				continue;
			
			if(Math.abs(ev-trainv)/(trainv + 1) > delta2)
				delta2 = Math.abs(ev-trainv)/(trainv + 1);
			
			//Add Prior
			if(cw >= 0)
				ev -= (cw - 0) / (2*2);
			else
				ev += (cw - 0) / (2*2);
			
			if(Math.abs(ev - trainv)<0.00001)
				ev= Double.valueOf(trainv);
			
			if(cw>0){
				double newCW = cw + Math.abs(alpha) * (ev - trainv)/(Math.abs(e2v - ev2));
				//double newCW = cw + Math.abs(alpha) * newD.get(k);
				//oldDW.put(k, newCW);
				delta += (newCW - cw) * (ev - trainv);
				//delta2 += (ev - trainv) * (ev - trainv);
				Learner.currentWeight.put(k, newCW);
			}
			if(cw<0){
				double newCW = cw - Math.abs(alpha) * (ev - trainv)/(Math.abs(ev2 - e2v));
				//double newCW = cw + Math.abs(alpha) * newD.get(k);
				//oldDW.put(k, newCW);
				delta += (newCW - cw) * (-(ev - trainv));
				
				//delta2 += (ev - trainv) * (ev - trainv);
				Learner.currentWeight.put(k, newCW);
			}
			if(delta < 0)
				UIMan.println();
		}
		UIMan.println("DELTA = " + delta);
		UIMan.println("DELTA2 = " + delta2);
		UIMan.println("ALPHA = " + alpha);
		
		if(Math.abs(delta) < 0.1)
			return true;
		
		return false;
	}
}