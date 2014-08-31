package tuffy.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;

import org.junit.Test;

import tuffy.db.RDB;
import tuffy.ground.Grounding;
import tuffy.ground.KBMC;
import tuffy.main.Infer;
import tuffy.mln.Clause;
import tuffy.mln.MarkovLogicNetwork;
import tuffy.ra.ConjunctiveQuery;
import tuffy.util.Config;
import tuffy.util.FileMan;

/**
 * Testing class for grounding.
 *
 */
public class GroundingTest extends Infer{
	
	/**
	 * Test the function of grounding by counting the number
	 * of resulting atoms and clauses.
	 */
	@Test
	public void testGrounding() throws Exception{
		
		ConjunctiveQuery.clearIndexHistory();

		Clause.mappingFromID2Const = new HashMap<Integer, String>();
		Clause.mappingFromID2Desc = new HashMap<String, String>();

		
		System.out.println("testing grounding");
		MarkovLogicNetwork mln = new MarkovLogicNetwork();
		String prog = "*Friends(person, person)\r\n" + 
				"Smokes(person)\r\n" + 
				"Cancer(person)\r\n" + 
				"0.5  !Smokes(a1) v Cancer(a1)\r\n" + 
				"0.4  !Friends(a1,a2) v !Smokes(a1) v Smokes(a2)\r\n" + 
				"0.4  !Friends(a1,a2) v !Smokes(a2) v Smokes(a1)\r\n" + 
				"";
		FileMan.ensureExistence(Config.dir_tests);
		String fprog = Config.dir_tests + "/prog.mln";
		FileMan.writeToFile(fprog, prog);
		mln.loadPrograms(new String[]{fprog});
		FileMan.removeFile(fprog);

		String query = "Cancer(x)";
		mln.parseQueryCommaList(query);
		
		String evid = "Friends(Anna, Bob)\r\n" + 
				"Friends(Anna, Edward)\r\n" + 
				"Friends(Anna, Frank)\r\n" + 
				"Friends(Edward, Frank)\r\n" + 
				"Friends(Gary, Helen)\r\n" + 
				"!Friends(Gary, Frank)\r\n" + 
				"Smokes(Anna)\r\n" + 
				"Smokes(Edward)\r\n" + 
				"";
		String fevid = Config.dir_tests + "/evidence.db";
		FileMan.writeToFile(fevid, evid);
		RDB db = RDB.getRDBbyConfig();
		db.resetSchema(Config.db_schema);
		mln.prepareDB(db);
		mln.loadEvidences(new String[]{fevid});
		FileMan.removeFile(fevid);
		
		mln.materializeTables();

		KBMC kbmc = new KBMC(mln);
		kbmc.run();
		Grounding grounding = new Grounding(mln);
		grounding.constructMRF();
		
		assertEquals(6, db.countTuples(mln.relClauses));
		assertEquals(6, grounding.getNumAtoms());
		assertEquals(6, grounding.getNumClauses());
		
		assertTrue(mln.cleanUp());
		FileMan.removeDirectory(new File(Config.dir_tests));
	}
	
	public final String getSimpleInferenceResult() throws Exception{

		BufferedReader br = new BufferedReader(new FileReader("testOutput.txt"));
		
		String rs = "";
		String tmp;
		
		while((tmp = br.readLine())!=null){
			rs += tmp;
		}
		
		br.close();
		
		return rs;
	}

}
