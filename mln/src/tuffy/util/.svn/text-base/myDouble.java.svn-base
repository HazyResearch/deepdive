package tuffy.util;

public class myDouble{
	
	public double value = Double.NEGATIVE_INFINITY;
	
	public myDouble(){
	}
	
	public myDouble(double _initValue){
		value = _initValue;
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
	
	public void tallylog(double plus){
		value = logAdd(value, plus);
	}
	
	public synchronized void tallyDouble(double plus){
		value = value + plus;
	}
	
	public String toString(){
		return "" + value;
	}
	
}
