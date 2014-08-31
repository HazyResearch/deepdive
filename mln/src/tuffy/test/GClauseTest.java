package tuffy.test;

import static org.junit.Assert.*;


import java.util.HashMap;

import org.junit.Test;

import tuffy.infer.ds.GAtom;
import tuffy.infer.ds.GClause;

/**
 * Testing class for {@link GClause} object.
 *
 */
public class GClauseTest {

	static GClause gc = new GClause();
	
	/**
	 * Test weight-related function of GClause, i.e.,
	 * the relationships between 1) weight; 2) cost and
	 * 3) # satisfied GAtom in GClause.
	 */
	@Test
	public final void test_GClause_weight(){
		gc.weight = 100;
		assertFalse(gc.isHardClause());
		gc.weight = 999999999;
		assertTrue(gc.isHardClause());
		
		assertTrue(Math.abs(gc.cost()-gc.weight)<0.1 );
		gc.weight = -1;
		assertTrue(Math.abs(gc.cost()-0)<0.1 );
		gc.nsat = 1;
		assertTrue(Math.abs(gc.cost()+gc.weight)<0.1 );
	}
	
	/**
	 * Test the relationship between GClause and GAtom. E.g.,
	 * 1) determining whether a GClause object containing a
	 * GAtom object; 2) replacing atoms in a GClause, etc.
	 */
	@Test
	public final void test_GClause_contain(){
		gc.lits = new int[2];
		gc.lits[0] = -1;
		gc.lits[1] = 0;
		assertTrue(gc.linkType(1)==-1);
		assertTrue(gc.linkType(2)==0);
		gc.lits[1] = 2;
		assertTrue(gc.linkType(2)==1);
		assertTrue(gc.linkType(3)==0);
		
		gc.lits[0] = 1;
		gc.lits[1] = -2;
		assertEquals(1, gc.replaceAtomID(1, 3));
		assertEquals(-1, gc.replaceAtomID(2, 4));
		assertEquals(0, gc.replaceAtomID(100, 101));
		assertTrue(gc.linkType(1)==0);
		assertTrue(gc.linkType(2)==0);
		assertTrue(gc.linkType(3)==1);
		assertTrue(gc.linkType(4)==-1);
	}
	
	/**
	 * Test function transforming GClause object to String
	 * representation.
	 */
	@Test
	public final void test_GClause_toString() {
		
		assertEquals("{3,-4} | -1.0", gc.toPGString());
		HashMap<Integer, GAtom> map = new HashMap<Integer, GAtom>();
		GAtom atom3 = new GAtom(3);
		atom3.truth = true;
		atom3.rep = "atom3";
		GAtom atom4 = new GAtom(4);
		atom4.truth = true;
		atom4.rep = "atom4";
		map.put(3, atom3);
		map.put(4, atom4);

		assertEquals(true, gc.toLongString(map).contains("ViolatedGroundClause0"));
		assertEquals(true, gc.toLongString(map).contains("weight=-1.0"));
		assertEquals(true, gc.toLongString(map).contains("satisfied=true"));
		assertEquals(true, gc.toLongString(map).contains("atom3"));
		assertEquals(false, gc.toLongString(map).contains("atom4"));
		
		assertEquals("-1.0: [3,-4,]", gc.toString());
		
	}

	
}
