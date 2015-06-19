package tuffy.util;

public class MathMan {
	public static int prorate(int total, double ratio){
		return (int)(total * ratio);
	}
	
	public static double getLogOddsWeight(double prob){
		if(prob < 0 || prob > 1) return 0;
		if(prob == 0) return -Config.hard_weight;
		if(prob == 1) return Config.hard_weight;
		return Math.log(prob/(1-prob));
	}
	
}
