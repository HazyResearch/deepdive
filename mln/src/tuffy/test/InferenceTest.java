package tuffy.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.FileReader;


import org.junit.Test;

import tuffy.infer.DataMover;
import tuffy.infer.MRF;
import tuffy.infer.ds.GClause;
import tuffy.main.Infer;
import tuffy.main.NonPartInfer;
import tuffy.parse.CommandOptions;
import tuffy.util.Config;
import tuffy.util.DebugMan;
import tuffy.util.Timer;
import tuffy.util.UIMan;

/**
 * Testing class for Inference.
 *
 */
public class InferenceTest extends Infer {
	
	/**
	 * Test marginal inference on a small RC set. This test is
	 * automatically passed, and will be MANUALLY compared with
	 * Alchemy's output. If there are no significant differences,
	 * it is regarded as real pass.
	 */
	@Test
	public final void test_MCSAT_inference() throws Exception {
		// THE RESULT OF THIS FUNCTION IS CHECKED MANUALLY
		Config.verbose_level = 2;
		//Config.init_strategy = INIT_STRATEGY.ALL_FALSE;
		String[] args = {"-e", "test/rc1000_evidence.db", "-i", "test/rc1000_prog2.mln",
				"-o", "test/testOutput.txt", "-queryFile", "test/rc1000_query.db"};
		CommandOptions options = UIMan.parseCommand(args);
		setUp(options);
		ground();
		Timer.runStat.markGroundingDone();
		if(options.maxFlips == 0){
			options.maxFlips = 100 * grounding.getNumAtoms();
		}
		if(options.maxTries == 0){
			options.maxTries = 1;
		}
		
		//Config.init_strategy = Config.INIT_STRATEGY.COIN_FLIP;

		MRF mcsat = new MRF(mln);
		dmover.loadMrfFromDb(mcsat, mln.relAtoms, mln.relClauses);
		
		options.mcsatSamples = 11;
		
		mcsat.mcsat(options.mcsatSamples, options.maxFlips);
		Timer.runStat.markInferDone();

		//mcsat.dumpAtomProb(options.mcsatSamples, options.fout);
		dmover.dumpProbsToFile(mln.relAtoms, options.fout);
		
		cleanUp();
	}
	
	/**
	 * Test MAP inference on a small RC set. This test is
	 * automatically passed, and will be MANUALLY compared with
	 * Alchemy's output. If there are no significant differences,
	 * it is regarded as real pass.
	 */
	@Test
	public final void test_WalkSAT_inference() throws Exception {
		// THE RESULT OF THIS FUNCTION IS CHECKED MANUALLY
		Config.verbose_level = 2;
		//Config.init_strategy = INIT_STRATEGY.ALL_FALSE;
		String[] args = {"-e", "test/rc1000_evidence.db", "-i", "test/rc1000_prog2.mln",
				"-o", "test/testOutput.txt", "-queryFile", "test/rc1000_query.db"};
		CommandOptions options = UIMan.parseCommand(args);
		setUp(options);
		ground();
		
		if(options.maxFlips == 0){
			options.maxFlips = 100 * grounding.getNumAtoms();
		}
		if(options.maxTries == 0){
			options.maxTries = 1;
		}
		

		DataMover mover = new DataMover(mln);
		MRF mrf = mover.loadMrfFromDb(mln.relAtoms, mln.relClauses);
		DebugMan.checkPeakMem();
		mrf.inferWalkSAT(options.maxTries, options.maxFlips); //hTuffy
		//mrf.flushLowTruth(db);
		dmover.flushAtomStates(mrf.atoms.values(), mln.relAtoms);

		//mln.dumpMapAnswer(options.fout);
		dmover.dumpTruthToFile(mln.relAtoms, options.fout);
		cleanUp();
	}
	
	/**
	 * Test inference on whether it can deal with hard clause constrains.
	 * This test is passed if the output of both marginal and MAP inference
	 * obey all the hard clauses.
	 */
	@Test
	public final void test_hardClause() throws Exception {
		Config.verbose_level = 2;
		//Config.init_strategy = INIT_STRATEGY.ALL_FALSE;
		String[] args = {"-e", "test/ie_evidence.db", "-i", "test/ie_prog.mln",
				"-o", "test/testOutput.txt", "-queryFile", "test/ie_query.db", "-maxFlips", "1000000"};
		CommandOptions options = UIMan.parseCommand(args);
		setUp(options);
		ground();
		Timer.runStat.markGroundingDone();
		if(options.maxFlips == 0){
			options.maxFlips = 100 * grounding.getNumAtoms();
		}
		if(options.maxTries == 0){
			options.maxTries = 1;
		}
		
		//Config.init_strategy = Config.INIT_STRATEGY.COIN_FLIP;

		MRF mcsat = new MRF(mln);
		dmover.loadMrfFromDb(mcsat, mln.relAtoms, mln.relClauses);
		
		mcsat.mcsat(options.mcsatSamples, options.maxFlips);
		Timer.runStat.markInferDone();
		
		
		for(GClause c : mcsat.clauses){
			if(c.isHardClause()){
				if(c.weight > 0)
					assertTrue(c.nsat > 0);
			}
		}

		//mcsat.dumpAtomProb(options.mcsatSamples, options.fout);
		dmover.dumpProbsToFile(mln.relAtoms, options.fout);
		
		// WALKSAT

		DataMover mover = new DataMover(mln);
		MRF mrf = mover.loadMrfFromDb(mln.relAtoms, mln.relClauses);
		DebugMan.checkPeakMem();
		mrf.inferWalkSAT(options.maxTries, options.maxFlips); //hTuffy
		//mrf.flushLowTruth(db);
		dmover.flushAtomStates(mrf.atoms.values(), mln.relAtoms);
		
		int violate = 0;
		int all = 0;
		for(GClause c : mrf.clauses){
			if(c.isHardClause()){
				if(c.weight > 0 && c.nsat == 0)
					violate ++;
				all ++;
			}
		}
		assertTrue(violate < all/20);
		
		//UIMan.display(">>> Writing answer to file: " + options.fout);

		//if(Config.report_runtime_stat) Timer.runStat.report();
		
		cleanUp();
	}
	
	
	@Test
	public final void test_initStatic() throws Exception{
/*		Type t = new Type("testType1");
		
		Config.test.flushTestConfiguration();
		RDB db = RDB.getRDBbyConfig();
		MarkovLogicNetwork mln = new MarkovLogicNetwork();
		
		MarkovLogicNetwork.initStatic();
			 * @throws Exception
		//assertEquals(true,Predicate.builtInMap.containsKey("same"));
		//assertEquals(true,Predicate.BuiltInSAME.arity() == 2);
		
		db.close();*/
		
	}
	
	private final String getSimpleInferenceResult() throws Exception{

		BufferedReader br = new BufferedReader(new FileReader("test/testOutput.txt"));
		
		String rs = "";
		String tmp;
		
		while((tmp = br.readLine())!=null){
			rs += tmp;
		}
		
		br.close();
		
		return rs;
	}
	
	/**
	 * Test inference on simple manually generated MLN world. The MAP
	 * truth assignment of this world is manually calculated. This
	 * test is passed if the Tuffy output is consistent with manual
	 * calculation.
	 */
	@Test
	public final void test_simpleInference() throws Exception {
		//Config.track_clause_source = true;
		Config.verbose_level = 2;
		//Config.init_strategy = INIT_STRATEGY.ALL_FALSE;
		String[] args = {"-e", "test/toyevidence.db", "-i", "test/toyprog.mln",
				"-o", "test/testOutput.txt", "-queryFile", "test/toyquery.db"};
		CommandOptions options = UIMan.parseCommand(args);
		new NonPartInfer().run(options);
		
		
		String rs = getSimpleInferenceResult();
		//rs = rs.replaceAll("Friends\\(Bob,.Edward\\)Friends\\(Edward,.Bob\\)", "");
		//rs = rs.replaceAll("Friends\\(Edward,.Bob\\)Friends\\(Bob,.Edward\\)", "");
		assertEquals(true, rs.length()>0);
		rs = rs.replaceAll("Friends\\(\"Bob\",.\"Edward\"\\)", "");
		assertEquals(true, rs.length()==0);
		
		Config.evidence_file_chunk_size = 30;
		
		String[] args2 = {"-e", "test/toyevidence.db.gz", "-i", "test/toyprog.mln",
				"-o", "test/testOutput.txt", "-queryFile", "test/toyquery.db"};
		options = UIMan.parseCommand(args2);
		new NonPartInfer().run(options);		
		
		rs = getSimpleInferenceResult();
		//rs = rs.replaceAll("Friends\\(Bob,.Edward\\)Friends\\(Edward,.Bob\\)", "");
		//rs = rs.replaceAll("Friends\\(Edward,.Bob\\)Friends\\(Bob,.Edward\\)", "");
		assertEquals(true, rs.length()>0);
		rs = rs.replaceAll("Friends\\(\"Bob\",.\"Edward\"\\)", "");
		assertEquals(true, rs.length()==0);
		
		String[] args3 = {"-e", "test/toyevidence.db.gz", "-i", "test/toyprog.mln",
				"-o", "test/testOutput.txt", "-queryFile", "test/toyquery.db"};
		options = UIMan.parseCommand(args3);
		new NonPartInfer().run(options);
		
		rs = getSimpleInferenceResult();
		//rs = rs.replaceAll("Friends\\(Bob,.Edward\\)Friends\\(Edward,.Bob\\)", "");
		//rs = rs.replaceAll("Friends\\(Edward,.Bob\\)Friends\\(Bob,.Edward\\)", "");
		assertEquals(true, rs.length()>0);
		rs = rs.replaceAll("Friends\\(\"Bob\",.\"Edward\"\\)", "");
		assertEquals(true, rs.length()==0);
		
		
		String[] args4 = {"-e", "test/toyevidence.db", "-i", "test/toyprog.mln",
				"-o", "test/testOutput.txt", "-queryFile", "test/toyquery2.db"};
		options = UIMan.parseCommand(args4);
		new NonPartInfer().run(options);		
		
		rs = getSimpleInferenceResult();
		assertEquals(true, rs.length()>0);
		rs = rs.replaceAll("Friends\\(\"Anna\", \"Bob\"\\)Friends\\(\"Anna\", \"Edward\"\\)", "");
		rs = rs.replaceAll("Friends\\(\"Bob\",.\"Edward\"\\)Friends\\(\"Edward\",.\"Bob\"\\)", "");
		rs = rs.replaceAll("Friends\\(\"Edward\",.\"Bob\"\\)Friends\\(\"Bob\",.\"Edward\"\\)", "");
		assertEquals(true, rs.length()==0);
		
		
		String[] args5 = {"-e", "test/toyevidence.db", "-i", "test/toyprog.mln",
				"-o", "test/testOutput.txt", "-q", "Friends(x, y)"};
		options = UIMan.parseCommand(args5);
		new NonPartInfer().run(options);		
		
		rs = getSimpleInferenceResult();
		assertEquals(true, rs.length()>0);
		rs = rs.replaceAll("Friends\\(\"Anna\", \"Bob\"\\)Friends\\(\"Anna\", \"Edward\"\\)", "");
		rs = rs.replaceAll("Friends\\(\"Bob\",.\"Edward\"\\)Friends\\(\"Edward\",.\"Bob\"\\)", "");
		rs = rs.replaceAll("Friends\\(\"Edward\",.\"Bob\"\\)Friends\\(\"Bob\",.\"Edward\"\\)", "");
		assertEquals(true, rs.length()==0);
		
		// need multiple times to decrease the probability of incidence
		for(int i=0;i<3;i++){
			String[] args6 = {"-e", "test/toyevidence.db", "-i", "test/toyprog_no_close_world.mln",
					"-o", "test/testOutput.txt", "-q", "Friends(x, y)", "-cw", "mySame"};
			options = UIMan.parseCommand(args6);
			new NonPartInfer().run(options);		
			
			rs = getSimpleInferenceResult();
			assertEquals(true, rs.length()>0);
			rs = rs.replaceAll("Friends\\(\"Anna\", \"Bob\"\\)Friends\\(\"Anna\", \"Edward\"\\)", "");
			rs = rs.replaceAll("Friends\\(\"Bob\",.\"Edward\"\\)Friends\\(\"Edward\",.\"Bob\"\\)", "");
			rs = rs.replaceAll("Friends\\(\"Edward\",.\"Bob\"\\)Friends\\(\"Bob\",.\"Edward\"\\)", "");
			assertEquals(true, rs.length()==0);
		}
		
	}
	

	
}
