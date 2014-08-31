package tuffy.test;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

import tuffy.mln.Term;
import tuffy.mln.Tuple;

/**
 * Testing class for {@link Term} class.
 *
 */
public class TermTest {
	
	/**
	 * Test functions of transforming Term object to String
	 * representation.
	 */
	@Test
	public final void test_toString() {
		Term t1 = new Term("testT");
		assertEquals(true, t1.isVariable());
		
		Term t2 = new Term(1000);
		assertEquals(true, t2.isConstant());
		
		assertEquals("testT", t1.toString());
		assertEquals("1000", t2.toString());
		
	}
	


}
