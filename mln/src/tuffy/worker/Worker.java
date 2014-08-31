package tuffy.worker;

import tuffy.infer.MRF;

public class Worker extends Thread{

	/**
	 * The MRF object that this Worker works on.
	 */
	protected MRF mrf;
	
	/**
	 * Constructor.
	 * @param _mrf
	 */
	public Worker(MRF _mrf){
		mrf = _mrf;
	}
	
}
