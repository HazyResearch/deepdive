package tuffy.worker;

import java.util.ArrayList;
import java.util.BitSet;

import tuffy.infer.MRF;
import tuffy.worker.ds.MLEWorld;

public abstract class MLEWorker extends Worker{

	public MLEWorker(MRF _mrf) {
		super(_mrf);
	}
	
	/**
	 * Get the top-k worlds of MLE inference. When k=-1, return all worlds. </br>
	 * !!!IMPORTANT!!!
	 * This function needs run() to be invoked ealier.
	 * 
	 * @param k
	 * @return
	 */
	public abstract ArrayList<MLEWorld> getTopK(int k);
	
	
	public BitSet getProjectedBitmap(BitSet bitmap){
		
		BitSet rs = new BitSet();
		
		for (int i = bitmap.nextSetBit(0); i >= 0; i = bitmap.nextSetBit(i+1)) {
			if(this.mrf.globalAtom.get(i).isquery){
				rs.set(i);
			}
		}
		
		return rs;
	}
	
}









