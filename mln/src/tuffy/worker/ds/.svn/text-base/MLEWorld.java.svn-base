package tuffy.worker.ds;

import java.util.BitSet;



public class MLEWorld{
	
	public BitSet bitmap;
	public double freq = 0;
	public double weight = 0;
	public double logCost = Double.NEGATIVE_INFINITY;
	
	public MLEWorld(BitSet _world){
		bitmap = _world;
	}
	
	public void tallyFreq(){
		freq ++;
	}
	
	public void tallyFreq(double _freq){
		freq += _freq;
	}
	
	public void tallyWeight(double _weight){
		weight += _weight;
	}
	
	public void tallyLogCost(double _logY){
		logCost = logAdd(logCost, _logY);
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
	
}