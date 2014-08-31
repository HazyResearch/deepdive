package tuffy.test;

import static org.junit.Assert.*;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;


import org.junit.Test;

import tuffy.db.RDB;
import tuffy.mln.Atom;
import tuffy.mln.Clause;
import tuffy.mln.MarkovLogicNetwork;
import tuffy.mln.Predicate;
import tuffy.mln.Type;
import tuffy.util.Config;


/**
 * Testing class for {@link Predicate} object.
 * 
 */
public class PredicateTest {

	/**
	 * Test the function setAllQuery().
	 */
	@Test
	public final void testSetAllQuery() {
		Predicate p = new Predicate(null, "dummy", true);
		assertTrue(p.getQueryAtoms().isEmpty());
		p.appendArgument(new Type("type1"));
		p.appendArgument(new Type("type2"));
		p.setAllQuery();
		assertFalse(p.getQueryAtoms().isEmpty());
	}

	/**
	 * Test closed-world-related functions.
	 */
	@Test
	public final void testSetClosedWorld() {
		Predicate p = new Predicate(null, "dummy", false);
		assertFalse(p.isClosedWorld());
		p.setClosedWorld(true);
		assertTrue(p.isClosedWorld());
		p.setClosedWorld(false);
		assertFalse(p.isClosedWorld());
	}

	/**
	 * Test argument-related functions.
	 */
	@Test
	public final void testGetArgs() {
		Predicate p = new Predicate(null, "dummy", false);
		assertTrue(p.getArgs().isEmpty());
		Type type = new Type("xx");
		p.appendArgument(type);
		assertEquals(1, p.getArgs().size());
		assertEquals(type, p.getTypeAt(0));
	}

	/**
	 * Test ID-related functions.
	 */
	@Test
	public final void testSetID() {
		Predicate p = new Predicate(null, "dummy", true);
		assertFalse(774 == p.getID());
		p.setID(774);
		assertTrue(774 == p.getID());
		assertTrue(p.noNeedToGround());
	}

	/**
	 * Test functions of grounding a predicate and store
	 * the resulting atoms to database table.
	 */
	@Test
	public final void testGroundAndStoreAtom() throws Exception {
		MarkovLogicNetwork mln = new MarkovLogicNetwork();
		Predicate p = new Predicate(mln, "dummy", false);
		mln.registerPred(p);
		
		//Config.test.flushTestConfiguration();
		RDB db = RDB.getRDBbyConfig();
		db.resetSchema(Config.db_schema);
		Type type = new Type("xx");
		type.addConstant(1);
		type.addConstant(2);
		type.addConstant(3);
		type.storeConstantList(db);
		p.appendArgument(type);
		p.appendArgument(type);
		p.prepareDB(db);
		assertEquals(0, db.countTuples(p.getRelName()));
		ArrayList<Integer> list = new ArrayList<Integer>();
		list.add(2);
		list.add(3);
		Atom atom = new Atom(p, list, true);
		p.groundAndStoreAtom(atom);
		assertEquals(1, db.countTuples(p.getRelName()));
		list = new ArrayList<Integer>();
		list.add(3);
		list.add(-1);
		atom = new Atom(p, list, true);
		p.groundAndStoreAtom(atom);
		assertEquals(4, db.countTuples(p.getRelName()));
		p.closeFiles();
		
		list = new ArrayList<Integer>();
		list.add(-1);
		list.add(-1);
		atom = new Atom(p, list, true);
		p.groundAndStoreAtom(atom);
		assertEquals(6, db.countTuples(p.getRelName()));
		p.closeFiles();
		
		
		list = new ArrayList<Integer>();
		list.add(-1);
		list.add(-1);
		atom = new Atom(p, list, true);
		
		atom.type = Atom.AtomType.QUERY;
		p.groundAndStoreAtom(atom);
		assertEquals(6, db.countTuples(p.getRelName()));
		
		PreparedStatement ps = db.getPrepareStatement("SELECT COUNT(*) AS CT FROM " +
				p.getRelName() + " WHERE club = 1 OR club = 3");
		ps.execute();
		ResultSet rss = ps.getResultSet();
		rss.next();
		int rs = rss.getInt("CT");
		assertEquals(3, rs);
		
		//TODO: CHANGE FOR ``SAME''
		//test getPredicateByName
		// assertTrue(mln.getPredByName("same")!=null);
		
	}

	/**
	 * Test functions of marking some atoms as evidence.
	 */
	@Test
	public final void testEvidence() {
		/*
		MarkovLogicNetwork mln = new MarkovLogicNetwork();
		Predicate p = new Predicate(mln, "dummy", false);
		mln.registerPred(p);
		RDB db = RDB.getRDBbyConfig();
		db.resetSchema("tuffy_test");
		Type type = new Type("xx");
		type.addConstant(1);
		type.addConstant(2);
		type.addConstant(3);
		type.storeConstantList(db);
		p.appendArgument(type);
		p.appendArgument(type);
		p.prepareDB(db);
		assertEquals(0, db.countTuples(p.getRelName()));
		ArrayList<Integer> list = new ArrayList<Integer>();
		list.add(1);
		list.add(3);
		Atom atom = new Atom(p, list, false);
		p.addEvidence(atom);
		p.flushEvidence();
		assertEquals(1, db.countTuples(p.getRelName()));
		p.closeFiles();
		*/
	}
	
	/**
	 * Test functions of marking some atoms as query.
	 */
	@Test
	public final void testStoreQueries() {
		MarkovLogicNetwork mln = new MarkovLogicNetwork();
		Predicate p = new Predicate(mln, "dummy", false);
		mln.registerPred(p);
		RDB db = RDB.getRDBbyConfig();
		db.resetSchema("tuffy_test");
		Type type = new Type("xx");
		type.addConstant(1);
		type.addConstant(2);
		type.addConstant(3);
		type.storeConstantList(db);
		p.appendArgument(type);
		p.appendArgument(type);
		p.prepareDB(db);
		assertEquals(0, db.countTuples(p.getRelName()));
		ArrayList<Integer> list = new ArrayList<Integer>();
		list.add(-1);
		list.add(3);
		Atom atom = new Atom(p, list, false);
		p.addQuery(atom);
		p.storeQueries();
		assertEquals(3, db.countTuples(p.getRelName()));
		p.closeFiles();
	}
	
	/**
	 * Test functions of determining whether there are query atoms
	 * belonging to this predication.
	 */
	@Test
	public final void testHasQuery() {
		Predicate p = new Predicate(null, "dummy", true);
		assertFalse(p.hasQuery());
		p.setAllQuery();
		assertTrue(p.hasQuery());
	}

	/**
	 * Test arity-related functions of Predicate.
	 */
	@Test
	public final void testArity() {
		Predicate p = new Predicate(null, "dummy", false);
		assertEquals(0, p.arity());
		p.appendArgument(new Type("imatype"));
		assertEquals(1, p.arity());
	}

	/**
	 * Test functions of building clause-predicate relationships.
	 */
	@Test
	public final void testAddRelatedClause() {
		Predicate p = new Predicate(null, "dummy", false);
		assertEquals(0, p.getRelatedClauses().size());
		p.addRelatedClause(new Clause());
		assertEquals(1, p.getRelatedClauses().size());
	}

}
