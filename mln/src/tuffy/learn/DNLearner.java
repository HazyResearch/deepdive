package tuffy.learn;


import java.util.HashMap;

import tuffy.infer.MRF;
import tuffy.infer.MRF.INIT_STRATEGY;
import tuffy.util.UIMan;

/**
 * Learner instance using diagonal Newton with
 * dynamic step size.
 * 
 * Method used here follows:
 * Lowd, Daniel and Domingos, Pedro (2007). Efficient Weight Learning for Markov Logic Networks. 
 * 
 * @author Ce Zhang
 *
 */
public class DNLearner extends Learner {
	
	/**
	 * Map from clause ID to gradient value in last iteration.
	 */
	public HashMap<String, Double> oldG = null;

	/**
	 * Map from clause ID to H^(-1)g value in last iteration.
	 */
	public HashMap<String, Double> oldD = null;
	
	/**
	 * Map from clause ID to weight in last iteration.
	 * It is used for backtracking.
	 */
	public HashMap<String, Double> oldWeight = null;
	
	/**
	 * Map from clause ID to current gradient value. This will
	 * be filled after the invocation of {@link DNLearner#getGradientAndD(MCSAT)}.
	 */
	public HashMap<String, Double> currentGradient = null;
	
	/**
	 * Map from clause ID to current H^(-1)g value. This will
	 * be filled after the invocation of {@link DNLearner#getGradientAndD(MCSAT)}.
	 */	
	public HashMap<String, Double> currentD = null;
	
	/**
	 * D'HD value of last iteration. Here D = H^(-1)g, H is Hessian.
	 */
	public double oldDHD = 0.0;
	
	/**
	 * D'g value of last iteration. Here D = H^(-1)g.
	 */
	public double oldDG = 0.0;
	
	/**
	 * Lambda used to control the step size.
	 */
	public double lambda = 100.0;
	
	/**
	 * Step size.
	 */
	public double alpha = 1;
	
	/**
	 * Number of invocations of {@link DNLearner#updateWeight(MCSAT)}.
	 */
	public int nCall = 0;
	//public int nBoot = 0;
	
	/**
	 * Load training data's truth value into MRF.
	 */
	public void loadingTrainingData(MRF _mcsat){

		// CURRENT ATOM'S TRUTH VALUE IS IN THE MRF. JUST CALC.
		// COST AND BUILD INDEX.
		_mcsat.setInitStrategy(INIT_STRATEGY.NO_CHANGE);
		_mcsat.initMRF();
		_mcsat.setInitStrategy(INIT_STRATEGY.COIN_FLIP);
	}
	
	/**
	 * Calculate the gradient and H^(-1)g by filling in 
	 * {@link DNLearner#currentD} and {@link DNLearner#currentGradient}.
	 * @param mcsat MCSAT instance used to estimate the expectation of violations.
	 */
	public void getGradientAndD(MRF mcsat){
		this.currentGradient = new HashMap<String, Double>();
		this.currentD = new HashMap<String, Double>();
		//Fill in gradient
		for(String k : mcsat.expectationOfViolation.keySet()){
			
			if(k.contains("fixed")){
				continue;
			}
			
			Double ev = mcsat.expectationOfViolation.get(k);
			Double cw = currentWeight.get(k);
			assert(k!=null);
			Long trainv = this.trainingViolation.get(k);
			Double ev2 = mcsat.expectationOfSquareViolation.get(k);
			Double e2v = ev*ev;
			
			if(trainv == null)
				trainv = 0l;
			
			if(Learner.isHardMappings.get(k) != null)
				continue;
			
			//Add PriorInteger
			if(cw >= 0)
				ev -= (cw - 0) / (2*2);
			else
				ev += (cw - 0) / (2*2);
			
			
			if(Math.abs(ev - trainv)<0.00001)
				ev= Double.valueOf(trainv);
			
			if(cw>=0){
				this.currentGradient.put(k, -(ev - trainv));
				this.currentD.put(k, (ev - trainv)/(ev2 - e2v + 0.25));
			}else if(cw<0){
				this.currentGradient.put(k, (ev - trainv));
				this.currentD.put(k, -(ev - trainv)/(ev2 - e2v + 0.25));
			}//else{
			//	this.currentGradient.put(k, 0.0);
			//	this.currentD.put(k, 0.0);				
			//}
		}
	}
	
	/**
	 * Update {@link Learner#currentWeight} using Diagonal
	 * Newton method.
	 * 
	 * @return true if it wants the learner to terminate iterations. This
	 * may happen when converge.
	 */
	@SuppressWarnings("unchecked")
	public boolean updateWeight(MRF mcsat){
		double delta = 0;
		double delta2 = 0;
		double delta3 = 0;
		int n = 0;
	    double actual = 1.0;
	    double pred = 1.0;
		
		nCall ++;
		
		this.getGradientAndD(mcsat);
		
		// if not the first run
		if (oldD != null && !backtracked){
			actual = 0.0;
			pred = alpha*oldDG + 0.5*oldDHD*alpha*alpha;
			
			//actual
			for(String k : oldD.keySet()){
				if(k.contains("fixed")){
					continue;
				}
				actual += alpha* oldD.get(k) * this.currentGradient.get(k); 
			}
		}
		
		if (oldD != null){
			
	        if (!backtracked && pred == 0)
	            lambda /= 4;
	        
	        if (!backtracked && pred != 0 && (actual/pred > 0.75) && lambda > 12)
	            lambda /= 2;
	        
	        if (actual/pred < 0.25)
	        {
	          if (lambda * 4 > 1000000)
	        	  lambda = 1000000;
	          else
	        	  lambda *= 4;
	        }
	        
	        if (actual/pred < 0.25 && backtrackCount_ < 1000){
			
				Learner.currentWeight = (HashMap<String, Double>) this.oldWeight.clone();
				
		        //  for (int i = 0; i < domainCnt_; i++)
		        //      inferences_[i]->restoreCnts();
				
		        backtracked = true;
		        backtrackCount_++;
			}else{
		        backtracked = false;
		        backtrackCount_ = 0;
		    }
		}
		
		if(!backtracked){
			alpha = 0.0;
			double normOfD = 0.0;
			for(String k : mcsat.expectationOfViolation.keySet()){
				if(k.contains("fixed")){
					continue;
				}
				alpha -= this.currentD.get(k) * this.currentGradient.get(k);
				normOfD += this.currentD.get(k) * this.currentD.get(k);
			}
			
			oldDG = -alpha;
			
			double denominator = 0.0;
	
			for(String k : mcsat.expectationOfNiNjViolation.keySet()){
				if(k.contains("fixed")){
					continue;
				}
				String[] tmps = k.split(",");
				if(Learner.currentWeight.get(tmps[0]) * Learner.currentWeight.get(tmps[1]) > 0){
					denominator += this.currentD.get(tmps[0]) * this.currentD.get(tmps[1]) 
						* (mcsat.expectationOfNiNjViolation.get(k) - 
								mcsat.expectationOfViolation.get(tmps[0])*
								mcsat.expectationOfViolation.get(tmps[1]));
				}else{
					denominator += this.currentD.get(tmps[0]) * this.currentD.get(tmps[1]) 
					* (-mcsat.expectationOfNiNjViolation.get(k) +
							mcsat.expectationOfViolation.get(tmps[0])*
							mcsat.expectationOfViolation.get(tmps[1]));
				}
			}
			
			
			oldDHD = denominator;
			
			denominator += lambda * normOfD;
			
			if(denominator == 0)
				denominator = 1.0;
			
			alpha /= denominator;
		}
		
		if (!backtracked && alpha <= 0.0){
			backtracked = true;
		}
		
		if (!backtracked){
			oldD = (HashMap<String, Double>) this.currentD.clone();
			oldG = (HashMap<String, Double>) this.currentGradient.clone();	
			oldWeight = (HashMap<String, Double>) Learner.currentWeight.clone();
			
			for(String k : mcsat.expectationOfViolation.keySet()){
				if(k.contains("fixed")){
					continue;
				}
				n++;
				Double ev = mcsat.expectationOfViolation.get(k);
				Double cw = currentWeight.get(k);
				assert(k!=null);
				Long trainv = this.trainingViolation.get(k);
				if(trainv == null)
					trainv = 0l;
				
				if(Learner.isHardMappings.get(k) != null)
					continue;
				
				//Add Prior
				
				if(cw >= 0)
					ev -= (cw - 0) / (2*2);
				else
					ev += (cw - 0) / (2*2);
				
				if(Math.abs(ev - trainv)<0.00001)
					ev= Double.valueOf(trainv);
				
				if(cw>=0){
					//double newCW = cw + Math.abs(alpha) * (ev - trainv)/Math.abs(e2v - ev2);
					double newCW = cw + Math.abs(alpha) * this.currentD.get(k);
					//oldDW.put(k, newCW);
					delta3 += Math.abs(ev - trainv)/(trainv+1);
					delta += (newCW - cw) * (ev - trainv);
					delta2 += (ev - trainv) * (ev - trainv);
					Learner.currentWeight.put(k, newCW);
				}
				if(cw<0){
					//double newCW = cw - Math.abs(alpha) * (ev - trainv)/Math.abs(e2v - ev2);
					double newCW = cw + Math.abs(alpha) * this.currentD.get(k);
					//oldDW.put(k, newCW);
					delta3 += Math.abs(ev - trainv)/(trainv+1);
					delta += (newCW - cw) * (-(ev - trainv));
					delta2 += (ev - trainv) * (ev - trainv);
					Learner.currentWeight.put(k, newCW);
				}
				if(delta < 0)
					UIMan.println();
			}
			UIMan.println("\nDELTA = " + delta);
			UIMan.println("DELTA3 = " + delta3);
			UIMan.println("ALPHA = " + alpha);
			UIMan.println("LAMBDA = " + lambda);
	
			// Terminating Standard
			if(Math.abs(delta) < 0.0001)
			//if(Math.abs(delta3) < 0.05*mcsat.expectationOfViolation.size())
				return true;
		}
		
		return false;
	}
}
