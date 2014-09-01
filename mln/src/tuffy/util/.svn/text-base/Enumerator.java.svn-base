package tuffy.util;

/**
 * A Enumerator is used to enumerate exponentially many configurations.
 * @author czhang
 *
 */
public class Enumerator {

	int[] upperBounds;
	int[] currentState;
	
	/**
	 * Enumerate 2**n worlds.
	 * @param n
	 */
	public Enumerator(int n){
		upperBounds = new int[n];
		currentState = new int[n];
		for(int i=0;i<n;i++){
			currentState[i] = 0;
			upperBounds[i] = 2;
		}
		currentState[0] = -1;
	}
	
	
	public void clear(){
		for(int i=0;i<currentState.length;i++){
			currentState[i] = 0;
		}
		currentState[0] = -1;
	}
	
	public int[] next(){
		
		currentState[0] ++;
		for(int i=0;i<currentState.length;i++){
			
			if(currentState[i] == upperBounds[i]){
				currentState[i] = 0;
				if(i+1 == currentState.length){
					return null;
				}else{
					currentState[i+1] ++;
				}
			}else{
				return currentState;
			}
			
		}
		return null;
		
	}
	
	
	
}
