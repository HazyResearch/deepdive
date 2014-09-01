package tuffy.util;

public class myInt{
	public int value = -1;
	
	public void addOne(){
		value ++;
	}
	
	public void subOne(){
		value --;
	}
	
	public myInt(int _value){
		value = _value;
	}
	
	public String toString(){
		return "" + value;
	}
	
}