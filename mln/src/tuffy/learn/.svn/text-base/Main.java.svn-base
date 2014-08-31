package tuffy.learn;

import java.sql.SQLException;

import tuffy.parse.CommandOptions;
import tuffy.util.Config;
import tuffy.util.UIMan;

/**
 * A class for calling Learner.
 * @author Ce Zhang
 *
 */
public class Main {
	public static void main(String[] args) throws SQLException {
		UIMan.println("Welcome to " + Config.product_name + "!");
		CommandOptions options = UIMan.parseCommand(args);
		if(options == null){
			return;
		}
		
		options.mcsatSamples = 51;

		//Learner l = new NaiveDNLearner();
		Learner l = new DNLearner();
		l.run(options);
	}
}
