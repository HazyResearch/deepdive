package tuffy.test;

import static org.junit.Assert.*;

import java.util.HashMap;


import org.junit.BeforeClass;
import org.junit.Test;


import tuffy.db.RDB;
import tuffy.mln.Clause;
import tuffy.mln.Literal;
import tuffy.mln.MarkovLogicNetwork;
import tuffy.mln.Predicate;
import tuffy.mln.Term;
import tuffy.mln.Type;
import tuffy.ra.ConjunctiveQuery;
import tuffy.util.Config;

/**
 * Testing Class for {@link Clause} object.
 *
 */
public class ClauseTest {

	private static Predicate p1, p2;
	private static Type type;
	
	@BeforeClass
	public static final void setUp(){
		p1 = new Predicate(null, "pred1", true);
		p2 = new Predicate(null, "pred2", true);
		p2.setClosedWorld(false);
		type = new Type("imatype");
		type.addConstant(1);
		type.addConstant(2);
		type.addConstant(3);
		p1.appendArgument(type);
		p2.appendArgument(type);
		p2.appendArgument(type);
	}
	

	/**
	 * Test the DB-related functions of clauses, e.g.,
	 * clause instance table for each Clause.
	 */
	@Test
	public final void testClauseDB() {
		
		ConjunctiveQuery.clearIndexHistory();

		Clause.mappingFromID2Const = new HashMap<Integer, String>();
		Clause.mappingFromID2Desc = new HashMap<String, String>();
		
		Clause c1 = new Clause();
		Literal lit1 = new Literal(p1, true);
		lit1.appendTerm(new Term(2));
		c1.addLiteral(lit1);

		Literal lit2 = new Literal(p2, false);
		lit2.appendTerm(new Term("x"));
		lit2.appendTerm(new Term(2));
		c1.addLiteral(lit2);
		
		/*
		Literal lit3 = new Literal(Predicate.Same, false);
		lit3.appendTerm(new Term("x"));
		lit3.appendTerm(new Term("x"));
		c1.addLiteral(lit3);
		*/

		c1.setWeight(3);
		c1 = c1.normalize();
		c1.setName("dummyclause");
		RDB db = RDB.getRDBbyConfig();
		System.out.println(Config.db_schema);
		db.resetSchema(Config.db_schema);
		c1.prepareForDB(db);
		String table = c1.getName() + "_instances";
		long n = db.countTuples(table);
		assertEquals(1, n);
		db.dropSchema(Config.db_schema);
	}
	
	/**
	 * Test the normalize function of Clause.
	 */
	@Test
	public final void testNormalize() {
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
		assertEquals(c1.normalize().getSignature(), c2.normalize().getSignature());
	}

	/**
	 * Test the absorb function of clauses.
	 */
	@Test
	public final void testAbsorb() {
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
		c1 = c1.normalize();
		c2 = c2.normalize();
		c1.absorb(c2);
		assertTrue(c1.getWeightExp() != Double.toString(3));
		
		Config.reorder_literals = true;
		
		c1 = c1.normalize();
		String tmp = c1.toString();
		tmp = tmp.replaceAll("\n", "");
		tmp = tmp.replaceAll("\r", "");
		assertTrue(tmp.matches("^.*pred1.*pred2.*$"));
		
		c1.getRegLiterals().get(1).getPred().setClosedWorld(true);
		c1 = c1.normalize();
		tmp = c1.toString();
		tmp = tmp.replaceAll("\n", "");
		tmp = tmp.replaceAll("\r", "");
		assertTrue(tmp.matches("^.*!pred2.*pred1.*$"));
		
		c1.getRegLiterals().get(0).getPred().setClosedWorld(false);
		c1.getRegLiterals().get(0).setSense(true);
		c1 = c1.normalize();
		tmp = c1.toString();
		tmp = tmp.replaceAll("\n", "");
		tmp = tmp.replaceAll("\r", "");
		assertTrue(tmp.matches("^.*pred1.*[^!]pred2.*$"));
		
		Config.reorder_literals = false;
	}

	/**
	 * Test other functions related to clause.
	 */
	@Test
	public final void testMisc() {
		
		Clause c1 = new Clause();
		Literal lit1 = new Literal(p1, true);
		lit1.appendTerm(new Term(2));
		c1.addLiteral(lit1);

		Literal lit2 = new Literal(p2, false);
		lit2.appendTerm(new Term("x"));
		lit2.appendTerm(new Term(2));
		c1.addLiteral(lit2);

		c1.setWeight(3);
		c1.generateSQL();
		assertEquals("CAST(3.0 AS FLOAT8)", c1.getWeightExp());
		assertEquals(true, Math.abs(c1.getWeight()-3.0)<0.01);
		
		c1.setHardWeight();
		c1.generateSQL();
		assertEquals("CAST(" + Double.toString(Config.hard_weight) + " AS FLOAT8)", 
				c1.getWeightExp());
		assertTrue(c1.isPositiveClause());
		
		assertEquals(2, c1.getRegLiterals().size());
		assertEquals(2, c1.getReferencedPredicates().size());
		assertEquals(1, c1.getLiteralsOfPredicate(p1).size());
		
		assertFalse(c1.hasExistentialQuantifiers());
		c1.addExistentialVariable("x");
		assertTrue(c1.hasExistentialQuantifiers());
		
		////////////////////////////////////////////////////////////////////
		c1 = c1.normalize();
		assertEquals(true, c1.isTemplate());

		////////////////////////////////////////////////////////////////////
		
		MarkovLogicNetwork mln = new MarkovLogicNetwork();
		mln.registerPred(p1);
		mln.registerPred(p2);
		mln.registerClause(c1);
		mln.normalizeClauses();
		Clause tmpc = mln.getClauseById(1);
		assertEquals(0, tmpc.getId()-1-c1.getId());
		
		tmpc = mln.getClauseById(-1);
		assertEquals(0, tmpc.getId()-1-c1.getId());
		
		tmpc = mln.getClauseById(0);
		assertEquals(null, tmpc);
		
	}

}
