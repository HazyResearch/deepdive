package tuffy.test;

import static org.junit.Assert.*;

import java.util.ArrayList;

import org.junit.Test;

import tuffy.mln.Tuple;

/**
 * Testing class for {@link Tuple} object. 
 *
 */
public class TupleTest {
	
	/**
	 * Test relationships between tuple and term ID.
	 */
	@Test
	public final void test_get() {

		ArrayList<Integer> al = new ArrayList<Integer>();
		al.add(1);
		al.add(-1);
		al.add(1212);
		
		Tuple t = new Tuple(al);
		
		assertEquals(1, t.get(0));
		assertEquals(-1, t.get(1));
		assertEquals(1212, t.get(2));
	}
	
	/**
	 * Test subsume functions of two tuple.
	 */
	@Test
	public final void test_subsume() {
		ArrayList<Integer> al1 = new ArrayList<Integer>();
		al1.add(-1);
		al1.add(-2);
		al1.add(-1);
		al1.add(1212);
		Tuple t1 = new Tuple(al1);
		
		ArrayList<Integer> al2 = new ArrayList<Integer>();
		al2.add(12);
		al2.add(-1000);
		al2.add(12);
		al2.add(1212);
		Tuple t2 = new Tuple(al2);
		
		ArrayList<Integer> al3 = new ArrayList<Integer>();
		al3.add(11);
		al3.add(-1);
		al3.add(13);
		al3.add(1212);
		Tuple t3 = new Tuple(al3);
		
		ArrayList<Integer> al4 = new ArrayList<Integer>();
		al4.add(5);
		al4.add(-1);
		al4.add(-2);
		al4.add(-1);
		al4.add(1212);
		Tuple t4 = new Tuple(al4);
		
		ArrayList<Integer> al5 = new ArrayList<Integer>();
		al5.add(-1);
		al5.add(-2);
		al5.add(-1);
		al5.add(1);
		Tuple t5 = new Tuple(al5);
		
		assertEquals(true, t1.subsumes(t2));
		assertEquals(false, t1.subsumes(t3));
		assertEquals(false, t1.subsumes(t4));
		assertEquals(false, t1.subsumes(t5));
				
	}


}
