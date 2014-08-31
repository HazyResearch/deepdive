package tuffy.test;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;


import org.junit.Test;

import tuffy.mln.Literal;
import tuffy.mln.Predicate;
import tuffy.mln.Term;
import tuffy.mln.Tuple;
import tuffy.mln.Type;
import tuffy.util.Config;

/**
 * Testing class for {@link Literal} class.
 *
 */
public class LiteralTest {
	
	@Test
	public final void test_scopeLit() {
/*
		Type t = new Type("testType1");
		t.addConstant(1);
		t.addConstant(2);
		
		Predicate p = new Predicate(null, "pred", false);
		p.appendArgument(t);
		Literal l = new Literal(p, false);*/
		
	}
	
	
	/**
	 * Test functions of transforming Literal object to String
	 * representation. Also test the relationship between
	 * Literal and Term.
	 */
	@Test
	public final void test_toString_and_getVars() {

		Type t = new Type("testType1");
		t.addConstant(1);
		t.addConstant(2);
		
		Predicate p = new Predicate(null, "pred", false);
		p.appendArgument(t);
		p.appendArgument(t);
		
		Literal l = new Literal(p, false);
		l.appendTerm(new Term(111));
		l.appendTerm(new Term("variable"));
		
		assertEquals("!pred(111, variable)",l.toString());
		
		assertEquals(true, l.getVars().contains("variable"));
		
	}
	
	/**
	 * Test isSameAs function of Literal.
	 */
	@Test
	public final void test_isSameAs() {
		Type t1 = new Type("testType1");
		t1.addConstant(1);
		t1.addConstant(2);
		
		Predicate p1 = new Predicate(null, "pred1", false);
		p1.appendArgument(t1);
		p1.appendArgument(t1);
		
		Type t2 = new Type("testType2");
		t2.addConstant(1);
		t2.addConstant(2);
		
		Predicate p2 = new Predicate(null, "pred2", false);
		p2.appendArgument(t2);
		p2.appendArgument(t2);
		
		Literal l1 = new Literal(p1, false);
		l1.appendTerm(new Term(1));
		l1.appendTerm(new Term("variable"));
		
		Literal l2 = new Literal(p2, false);
		l1.appendTerm(new Term(1));
		l1.appendTerm(new Term("variable"));

		assertEquals(false, l2.isSameAs(l1));
		
		Literal l3 = new Literal(p1, false);
		l3.appendTerm(new Term(2));
		l3.appendTerm(new Term("variable"));
		
		assertEquals(false, l3.isSameAs(l1));
		
		Literal l4 = new Literal(p1, false);
		l4.appendTerm(new Term("w"));
		l4.appendTerm(new Term("variable"));
		
		assertEquals(false, l4.isSameAs(l1));
		
		assertEquals(true, l1.isSameAs(l1));
		
	}
	
	/**
	 * Test function of getting the MGU (most general unification) 
	 * of two literals. Also test the function of substitution.
	 */
	@Test
	public final void test_mostGeneralUnification_and_substitute_and_toTuple(){
		Type t = new Type("testType1");
		t.addConstant(1);
		t.addConstant(2);
		
		Predicate p = new Predicate(null, "pred", false);
		p.appendArgument(t);
		p.appendArgument(t);
		
		Literal l = new Literal(p, false);
		l.appendTerm(new Term(1));
		l.appendTerm(new Term("variable"));
		l.appendTerm(new Term(3));
		
		ArrayList<Integer> list = new ArrayList<Integer>();
		list.add(1);
		list.add(2);
		list.add(3);
		Tuple tu = new Tuple(list);
		HashMap<String, Term> mgu = l.mostGeneralUnification(tu);
		assertEquals(true, mgu.containsKey("variable"));
		assertFalse(Config.constants_as_raw_string);
		assertEquals(2, mgu.get("variable").constant());
		assertEquals(2, l.substitute(mgu).getTerms().get(1).constant());
		assertEquals(-1, l.toTuple().get(1));
		
		list = new ArrayList<Integer>();
		list.add(2);
		list.add(2);
		list.add(3);
		tu = new Tuple(list);
		mgu = l.mostGeneralUnification(tu);
		assertEquals(null, mgu);
		
		list = new ArrayList<Integer>();
		list.add(-1);
		list.add(2);
		list.add(-1);
		tu = new Tuple(list);
		mgu = l.mostGeneralUnification(tu);
		assertEquals(null, mgu);
		
		list = new ArrayList<Integer>();
		list.add(-1);
		list.add(1);
		list.add(3);
		tu = new Tuple(list);
		mgu = l.mostGeneralUnification(tu);
		assertEquals(true, mgu.containsKey("variable"));
		assertEquals(1, mgu.get("variable").constant());
		
		l = new Literal(p, false);
		l.appendTerm(new Term(1));
		l.appendTerm(new Term("variable"));
		l.appendTerm(new Term("variable"));
		
		list = new ArrayList<Integer>();
		list.add(1);
		list.add(2);
		list.add(3);
		tu = new Tuple(list);
		mgu = l.mostGeneralUnification(tu);
		assertEquals(null, mgu);
		
		l = new Literal(p, false);
		l.appendTerm(new Term("variable"));
		l.appendTerm(new Term(5));
		l.appendTerm(new Term("variable"));
		
		list = new ArrayList<Integer>();
		list.add(1);
		list.add(-1);
		list.add(-1);
		tu = new Tuple(list);
		mgu = l.mostGeneralUnification(tu);
		assertEquals(null, mgu);
		
		
	}

}
