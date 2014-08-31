package tuffy.test;

import static org.junit.Assert.*;

import org.junit.Test;

import tuffy.infer.ds.GAtom;
import tuffy.infer.ds.GClause;

/**
 * Testing class for {@link GAtom} object.
 *
 */
public class GAtomTest {
	
	GAtom atom = new GAtom(100);

	/**
	 * Test the function of flipping a GAtom.
	 */
	@Test
	public final void test_GAtom_flip() {
		//flip
		atom.fixed = false;
		atom.truth = true;
		atom.flip();
		assertEquals(false, atom.truth);
		atom.flip();
		assertEquals(true, atom.truth);
		//flip (fixed)
		atom.fixed = true;
		atom.flip();
		assertEquals(true, atom.truth);
		atom.flip();
		assertEquals(true, atom.truth);
	}
	
	/**
	 * Test the function of invoke and revoke a GAtom, and
	 * its influence on calculating the cost of flipping
	 * a GAtom.
	 */
	@Test
	public final void test_GAtom_invoke_revoke() {
		//revoke/assign GClause.
		GClause f = new GClause();
		f.weight = 100;
		atom.assignSatPotential(f);
		assertTrue(Math.abs(atom.delta()-(-100))<0.1);
		atom.revokeSatPotential(f);
		assertTrue(Math.abs(atom.delta()-(0))<0.1);
		atom.assignUnsatPotential(f);
		assertTrue(Math.abs(atom.delta()-(100))<0.1);
		atom.revokeUnsatPotential(f);
		assertTrue(Math.abs(atom.delta()-(0))<0.1);
		f.weight = -100;
		atom.assignSatPotential(f);
		assertTrue(Math.abs(atom.delta()-(100))<0.1);
		atom.revokeSatPotential(f);
		assertTrue(Math.abs(atom.delta()-(0))<0.1);
		atom.assignUnsatPotential(f);
		assertTrue(Math.abs(atom.delta()-(-100))<0.1);
		atom.revokeUnsatPotential(f);
		assertTrue(Math.abs(atom.delta()-(0))<0.1);
	}
	
	/**
	 * Test the function of returning the ideal value of 
	 * a GAtom.
	 */
	@Test
	public final void test_GAtom_wannabe() {
		//wannabe
		atom.wannabe = 3;
		assertTrue(atom.critical());
		atom.wannabe = 1;
		assertTrue(atom.wannaBeFalse());
		atom.wannabe = 2;
		assertTrue(atom.wannaBeTrue());
		atom.markCritical();
		assertTrue(atom.critical());
		atom.markWannaBeFalse();
		assertTrue(atom.wannaBeFalse());
		atom.markWannaBeTrue();
		assertTrue(atom.wannaBeTrue());
	}


	
}