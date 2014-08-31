package tuffy.test;

import static org.junit.Assert.*;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;


import org.junit.BeforeClass;
import org.junit.Test;


import tuffy.db.RDB;
import tuffy.mln.Atom;
import tuffy.mln.Clause;
import tuffy.mln.Literal;
import tuffy.mln.MarkovLogicNetwork;
import tuffy.mln.Predicate;
import tuffy.mln.Term;
import tuffy.mln.Type;
import tuffy.ra.ConjunctiveQuery;
import tuffy.util.Config;
import tuffy.util.FileMan;

/**
 * Testing class for parsing and loading.
 *
 */
public class ParsingLoadingTest {

	private static Predicate p1, p2;
	private static Type type;
	
	@BeforeClass
	public static final void setUp(){
		p1 = new Predicate(null, "pred1", true);
		p2 = new Predicate(null, "pred2", true);
		type = new Type("imatype");
		type.addConstant(1);
		type.addConstant(2);
		type.addConstant(3);
		p1.appendArgument(type);
		p2.appendArgument(type);
		p2.appendArgument(type);
	}
	
	/*
	@Test
	public final void testRegisterNormalizeClauses() {
		Clause c1 = new Clause();
		Clause c2 = new Clause();
		Literal lit1 = new Literal(p1, true);
		lit1.appendTerm(new Term(2));
		c1.addLiteral(lit1);
		c2.addLiteral(lit1);

		Literal lit2 = new Literal(p2, false);
		lit2.appendTerm(new Term("x"));
		lit2.appendTerm(new Term(3));
		c1.addLiteral(lit2);

		Literal lit3 = new Literal(p2, false);
		lit3.appendTerm(new Term("g"));
		lit3.appendTerm(new Term(1));
		c2.addLiteral(lit3);
		
		c1.setWeight(3);
		c2.setHardWeight();
		
		MarkovLogicNetwork mln = new MarkovLogicNetwork();
		assertTrue(mln.getAllUnnormalizedClauses().isEmpty());
		mln.registerPred(p1);
		mln.registerPred(p2);
		mln.registerClause(c1);
		assertTrue(mln.getAllUnnormalizedClauses().size() == 1);
		
		mln.registerClause(c2);
		assertTrue(mln.getAllUnnormalizedClauses().size() == 2);

		assertTrue(mln.getAllNormalizedClauses().isEmpty());
		mln.normalizeClauses();
		assertTrue(mln.getAllNormalizedClauses().size() == 1);
		
		
	}
	 */

	/*
	@Test
	public final void testPred() {
		MarkovLogicNetwork mln = new MarkovLogicNetwork();
		assertNull(mln.getPredByName("pred1"));
		assertEquals(0, mln.getNumPredicates());
		mln.registerPred(p1);
		assertEquals(p1, mln.getPredByName("pred1"));
		assertEquals(1, mln.getNumPredicates());
		mln.registerPred(p2);
		assertEquals(2, mln.getNumPredicates());
		assertEquals(null, mln.getPredicateByAtomID(9));
		assertEquals(p1, mln.getPredicateByAtomID(-44));
		assertEquals(p2, mln.getPredicateByAtomID(-7));
	}*/

	/*
	@Test
	public final void testConstantType() {
		MarkovLogicNetwork mln = new MarkovLogicNetwork();
		Type type = mln.getOrCreateTypeByName("newtype");
		assertEquals(1, mln.getSymbolID("BOOK", type));
		assertEquals(2, mln.getSymbolID("MOVIE", type));
		assertEquals(1, mln.getSymbolID("BOOK", type));
		assertEquals(3, mln.getSymbolID("RIVER", type));
	}*/

	/**
	 * Test the parsing function of program and query.
	 */
	@Test
	public void testProgramQueryParser() {
		System.out.println("testing program parser");
		MarkovLogicNetwork mln = new MarkovLogicNetwork();
		String prog = "// predicates\r\n" + 
				"*wrote(person,paper)\r\n" + 
				"*refers(paper,paper)\r\n" + 
				"category(paper,cat)\r\n" + 
				"*sameCat(cat,cat)\r\n" + 
				"\r\n" + 
				"1  !wrote(a1,a3) v !wrote(a1,a2) v category(a3,a4) v !category(a2,a4)\r\n" + 
				"2  !refers(a1,a2) v category(a2,a3) v !category(a1,a3)\r\n" + 
				"2  !refers(a1,a2) v category(a1,a3) v !category(a2,a3)\r\n" + 
				"10  sameCat(a2,a3) v !category(a1,a3) v !category(a1,a2)\r\n" + 
				"-3  category(a,Networking)\r\n" + 
				"0.14  category(a1,Programming)\r\n" + 
				"0.09  category(a1,Operating_Systems)\r\n" + 
				"0.04  category(a1,Hardware_and_Architecture)\r\n" + 
				"0.11  category(a1,Data_Structures__Algorithms_and_Theory)\r\n" + 
				"0.04  category(a1,Encryption_and_Compression)\r\n" + 
				"0.02  category(a1,Information_Retrieval)\r\n" + 
				"0.05  category(a1,Databases)\r\n" + 
				"0.39  category(a1,Artificial_Intelligence)\r\n" + 
				"0.06  category(a1,Human_Computer_Interaction)\r\n" + 
				"0.06  category(a,Networking)\r\n" + 
				"";
		FileMan.ensureExistence(Config.dir_tests);
		String fprog = Config.dir_tests + "/prog.mln";
		FileMan.writeToFile(fprog, prog);
		
		
		/*
		mln.loadPrograms(new String[]{fprog});
		assertEquals(4, mln.getAllPred().size());
		assertNotNull(mln.getPredByName("wrote"));
		assertNotNull(mln.getPredByName("refers"));
		assertNotNull(mln.getPredByName("category"));
		assertNotNull(mln.getPredByName("sameCat"));
		assertTrue(mln.getPredByName("wrote").isClosedWorld());
		assertTrue(mln.getPredByName("refers").isClosedWorld());
		assertTrue(mln.getPredByName("sameCat").isClosedWorld());
		assertFalse(mln.getPredByName("category").isClosedWorld());
		assertEquals(15, mln.getAllUnnormalizedClauses().size());
		assertEquals(6, mln.getPredByName("category").getRelatedClauses().size());
		FileMan.removeFile(fprog);

		System.out.println("testing query parser");
		String query = "category(x,y)";
		String fquery = Config.dir_tests + "/query.db";
		FileMan.writeToFile(fquery, query);
		mln.loadQueries(new String[]{fquery});
		ArrayList<Atom> qs = mln.getPredByName("category").getQueryAtoms();
		assertEquals(1, qs.size());
		assertEquals(2, qs.get(0).args.dimension);
		FileMan.removeFile(fquery);
		*/
	}

	/**
	 * Test the parsing function of evidence.
	 */
	@Test
	public final void testLoadEvidences()  throws Exception{
		
		ConjunctiveQuery.clearIndexHistory();

		Clause.mappingFromID2Const = new HashMap<Integer, String>();
		Clause.mappingFromID2Desc = new HashMap<String, String>();
		
		MarkovLogicNetwork mln = new MarkovLogicNetwork();
		String prog = "// predicates\r\n" + 
				"*wrote(person,paper)\r\n" + 
				"*refers(paper,paper)\r\n" + 
				"category(paper,cat)\r\n" + 
				"*sameCat(cat,cat)\r\n" + 
				"\r\n" + 
				"1  !wrote(a1,a3) v !wrote(a1,a2) v category(a3,a4) v !category(a2,a4)\r\n" + 
				"2  !refers(a1,a2) v category(a2,a3) v !category(a1,a3)\r\n" + 
				"2  !refers(a1,a2) v category(a1,a3) v !category(a2,a3)\r\n" + 
				"10  sameCat(a2,a3) v !category(a1,a3) v !category(a1,a2)\r\n" + 
				"-3  category(a,Networking)\r\n" + 
				"0.14  category(a1,Programming)\r\n" + 
				"0.09  category(a1,Operating_Systems)\r\n" + 
				"0.04  category(a1,Hardware_and_Architecture)\r\n" + 
				"0.11  category(a1,Data_Structures__Algorithms_and_Theory)\r\n" + 
				"0.04  category(a1,Encryption_and_Compression)\r\n" + 
				"0.02  category(a1,Information_Retrieval)\r\n" + 
				"0.05  category(a1,Databases)\r\n" + 
				"0.39  category(a1,Artificial_Intelligence)\r\n" + 
				"0.06  category(a1,Human_Computer_Interaction)\r\n" + 
				"0.06  category(a,Networking)\r\n" + 
				"";
		FileMan.ensureExistence(Config.dir_tests);
		String fprog = Config.dir_tests + "/prog.mln";
		FileMan.writeToFile(fprog, prog);
		
		/*
		mln.loadPrograms(new String[]{fprog});
		FileMan.removeFile(fprog);

		System.out.println("testing loading evidence");
		String evid = "wrote(D_B_Unger,Paper218961)\r\n" + 
		"wrote(D_A_Kozek,Paper119952)\r\n" + 
		"wrote(D_J_Leslie,Paper1123754)\r\n" + 
		"wrote(D_E_Schuster,Paper1123754)\r\n" + 
		"wrote(D_A_Kozek,Paper1123754)\r\n" + 
		"refers(Paper33991,Paper6840)\r\n" + 
		"refers(Paper33991,Paper4531)\r\n" + 
		"refers(Paper128796,Paper1248)\r\n" + 
		"refers(Paper138991,Paper128796)";
		String fevid = Config.dir_tests + "/evidence.db";
		FileMan.writeToFile(fevid, evid);
		
		RDB db = RDB.getRDBbyConfig();
		db.resetSchema(Config.db_schema);
		mln.prepareDB(db);
		mln.loadEvidences(new String[]{fevid});
		mln.materializeTables();
		String sql = "SELECT COUNT(*) FROM pred_wrote";
		ResultSet rs = db.query(sql);
		assertTrue(rs.next());
		int count = rs.getInt(1);
		rs.close();
		assertEquals(5, count);
		sql = "SELECT COUNT(*) FROM pred_refers";
		rs= db.query(sql);
		assertTrue(rs.next());
		count = rs.getInt(1);
		rs.close();
		assertEquals(4, count);
		mln.closeFiles();
		FileMan.removeFile(fevid);
		assertTrue(mln.cleanUp());
		*/
	}

}
