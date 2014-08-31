package tuffy.util;

@SuppressWarnings("serial")
public class TuffyThrownError extends Error{
	public TuffyThrownError(){
		super();
	}
	
	public TuffyThrownError(String msg){
		super(msg);
	}
}
