package tuffy.test;

import static org.junit.Assert.*;

import java.util.ArrayList;


import org.junit.Test;

import tuffy.mln.Atom;
import tuffy.mln.Predicate;
import tuffy.mln.Tuple;
import tuffy.mln.Type;

/**
 * Testing class for {@link Atom} object.
 * 
 */
public class AtomTest {
	
	/**
	 * Test function grounding atoms according to Predicate
	 * and Constant Table.
	 */
	@Test
	public final void testGroundSize() {
		Predicate p = new Predicate(null, "dummyp", true);
		Type t = new Type("imatype");
		t.addConstant(1);
		t.addConstant(2);
		t.addConstant(3);
		p.appendArgument(t);
		p.appendArgument(t);
		

		ArrayList<Integer> list = new ArrayList<Integer>();
		list.add(2);
		list.add(3);
		Atom atom = new Atom(p, list, true);

		assertEquals(1, atom.groundSize());
		
		list = new ArrayList<Integer>();
		list.add(-1);
		list.add(3);
		Tuple tmpTuple = new Tuple(list);
		atom = new Atom(p, tmpTuple);
		assertEquals(p.arity(), tmpTuple.list.length);
		assertEquals(3, atom.groundSize());

		list = new ArrayList<Integer>();
		list.add(-1);
		list.add(-1);
		atom = new Atom(p, list, true);
		assertEquals(3, atom.groundSize());

		list = new ArrayList<Integer>();
		list.add(-1);
		list.add(-2);
		atom = new Atom(p, list, true);
		assertEquals(9, atom.groundSize());
		
		atom.type = Atom.AtomType.QUERY;
		assertEquals(1, atom.club());
		
		atom.type = Atom.AtomType.QUEVID;
		assertEquals(3, atom.club());
		
	}

	/**
	 * Test function transforming Atom object to String
	 * representation.
	 */
	@Test
	public final void testToString() {
		Predicate p = new Predicate(null, "pred", true);
		Type t = new Type("imatype");
		t.addConstant(1);
		t.addConstant(2);
		t.addConstant(3);
		p.appendArgument(t);
		p.appendArgument(t);

		ArrayList<Integer> list = new ArrayList<Integer>();
		list.add(2);
		list.add(3);
		Atom atom = new Atom(p, list, true);
		assertEquals("pred(C2, C3)", atom.toString());
		
		list = new ArrayList<Integer>();
		list.add(-1);
		list.add(3);
		atom = new Atom(p, list, true);
		assertEquals("pred(v1, C3)", atom.toString());

	}

}
