package tuffy.main;

import tuffy.parse.CommandOptions;
import tuffy.util.UIMan;
/**
 * Non-parition-aware inference.
 */
public class NonPartInfer extends Infer{
	public void run(CommandOptions opt){
		UIMan.println(">>> Running non-partition inference.");
		setUp(opt);
	}

}


