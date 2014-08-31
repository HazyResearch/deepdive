package tuffy.test;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import tuffy.learn.DNLearner;
import tuffy.learn.Learner;
import tuffy.learn.MultiCoreSGDLearner;
import tuffy.learn.NaiveGDLearner;
import tuffy.parse.CommandOptions;
import tuffy.util.Config;
import tuffy.util.UIMan;

/**
 * Testing class for {@link Learner}.
 *
 */
public class LearnerTest {
		
	/**
	 * Test on Univ data set. This data set is as the same as
	 * Alchemy's learning example. This test is passed by comparing
	 * the learned result with Alchemy.
	 */
	@Test
	public final void test_Learning_On_univ() throws Exception {
		//Config.fastSample = true;
		Config.verbose_level = 2;
		//Config.init_strategy = INIT_STRATEGY.COIN_FLIP;
		String[] args = {"-e", "test/univ_evidence.db", "-i", "test/univ_prog.mln",
				"-o", "testOutput.txt", "-queryFile", "test/univ_query.db"};
		CommandOptions options = UIMan.parseCommand(args);
		
		//NaiveDNLearner ngdl = new NaiveDNLearner();
		DNLearner ngdl = new DNLearner();
		
		//options.mcsatSamples = 311;
		options.mcsatSamples = 11;
		
		ngdl.run(options);
		
		assertTrue( Learner.currentWeight.get("1.0") > 0);
		assertTrue( Learner.currentWeight.get("2.0") > 0);
		assertTrue( Learner.currentWeight.get("3.0") > 0);
		assertTrue( Learner.currentWeight.get("4.0") > 0);
		assertTrue( Learner.currentWeight.get("5.1") > 0);
		assertTrue( Learner.currentWeight.get("5.2") > 0);
		assertTrue( Learner.currentWeight.get("6.1") > 0);
		assertTrue( Learner.currentWeight.get("6.2") > 0);
		assertTrue( Learner.currentWeight.get("7.0") > 0);
		assertTrue( Learner.currentWeight.get("8.4") > 0);
		assertTrue( Learner.currentWeight.get("8.1") < 1);
		assertTrue( Learner.currentWeight.get("8.2") < 1);
		assertTrue( Learner.currentWeight.get("8.3") < 1);
		assertTrue( Learner.currentWeight.get("12.0") < 1);
		assertTrue( Learner.currentWeight.get("13.0") < 1);
		assertTrue( Learner.currentWeight.get("14.0") < 1);
	}
	
	/**
	 * Test on manually generated world. If there is only one
	 * clause, the learned weight should equal to the log odd
	 * of #True world and #False world. This test is passed if
	 * the learning results on all 10 randomly generated
	 * training sets obey this assumption. This test can also
	 * be used as a justification of current MCSAT implementation.
	 */
	@Test
	public final void test_NaiveGDLearner_Negative() throws Exception {
		Config.stop_samplesat_upon_sat = false;
		Config.verbose_level = 2;
		//Config.init_strategy = INIT_STRATEGY.COIN_FLIP;
		String[] args = {"-e", "test/nlearningEvidence.db", "-i", "test/nlearningProg.mln",
				"-o", "test/testOutput.txt", "-queryFile", "test/nlearningQuery.db"};
		CommandOptions options = UIMan.parseCommand(args);
		
		for(int i=0;i<10;i++){
			NaiveGDLearner ngdl = new NaiveGDLearner();
			
			options.mcsatSamples = 311;
			ngdl.run(options);
			
			//ngdl.odds = - ngdl.odds;
			
			System.out.println( ngdl.odds + "\t" + Learner.currentWeight.get("1.0"));
			
			assertTrue( Math.abs(ngdl.odds - Learner.currentWeight.get("1.0"))/
				(Math.abs(ngdl.odds)>1?Math.abs(ngdl.odds):1) < 0.4);
				
		}
		
	}
	
	/**
	 * Test on manually generated world. If there is only one
	 * clause, the learned weight should equal to the log odd
	 * of #True world and #False world. This test is passed if
	 * the learning results on all 10 randomly generated
	 * training sets obey this assumption. This test can also
	 * be used as a justification of current MCSAT implementation.
	 */
	@Test
	public final void test_NaiveGDLearner_Positive() throws Exception {
		Config.stop_samplesat_upon_sat = false;
		Config.verbose_level = 2;
		//Config.init_strategy = INIT_STRATEGY.COIN_FLIP;
		String[] args = {"-e", "test/plearningevidence.db", "-i", "test/plearningprog.mln",
				"-o", "test/testOutput.txt", "-queryFile", "test/plearningquery.db"};
		CommandOptions options = UIMan.parseCommand(args);
		
		for(int i=0;i<10;i++){
			NaiveGDLearner ngdl = new NaiveGDLearner();
			
			options.mcsatSamples = 11;
			ngdl.run(options);
			
			System.out.println( ngdl.odds + "\t" + Learner.currentWeight.get("1.0"));
			
			assertTrue( Math.abs(ngdl.odds - Learner.currentWeight.get("1.0"))/
					(Math.abs(ngdl.odds)>1?Math.abs(ngdl.odds):1) < 0.4);
			
		}
	}
	
	/**
	 * Test on RC set. This is a real-world data set.
	 * This test is passed by comparing result with Alchemy.
	 */
	@Test
	public final void test_Learning_On_1000rc() throws Exception {
		Config.stop_samplesat_upon_sat = true;
		Config.verbose_level = 2;
		//Config.init_strategy = INIT_STRATEGY.COIN_FLIP;
		String[] args = {"-e", "test/rc1000_evidence.db", "-i", "test/rc1000_prog.mln",
				"-o", "testOutput.txt", "-queryFile", "test/rc1000_query.db", "-db", 
				"ss", "-keepData", "-sgdMetaSample", "1000"};
		CommandOptions options = UIMan.parseCommand(args);
		
		/*
		//NaiveDNLearner ngdl = new NaiveDNLearner();
		DNLearner ngdl = new DNLearner();
		
		//options.mcsatSamples = 311;
		//options.mcsatSamples = 51;
		options.mcsatSamples = 111;
		
		ngdl.run(options);*/
		
		options.verboseLevel = 3;
		Config.verbose_level = 3;
		
		MultiCoreSGDLearner l = new MultiCoreSGDLearner();
		l.run(options);
		
		assertTrue( Learner.currentWeight.get("1.0") > 0);
		assertTrue( Learner.currentWeight.get("2.0") > 0);
		assertTrue( Learner.currentWeight.get("3.0") > 0);
		assertTrue( Learner.currentWeight.get("4.0") > 0);
		assertTrue( Learner.currentWeight.get("5.1") > 0);
		assertTrue( Learner.currentWeight.get("6.1") > 0);
		assertTrue( Learner.currentWeight.get("6.10") > 0);
		assertTrue( Learner.currentWeight.get("6.2") > 0);
		assertTrue( Learner.currentWeight.get("6.3") < 0);
		assertTrue( Learner.currentWeight.get("6.4") > 0);
		assertTrue( Learner.currentWeight.get("6.5") < 1);
		assertTrue( Learner.currentWeight.get("6.6") < 0);
		assertTrue( Learner.currentWeight.get("6.7") < 1.3);
		assertTrue( Learner.currentWeight.get("6.8") > 0);
		assertTrue( Learner.currentWeight.get("6.9") < 1);
	}
		
}





