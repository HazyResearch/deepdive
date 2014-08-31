package tuffy.util;

import org.junit.internal.TextListener;
import org.junit.runner.JUnitCore;

import tuffy.test.*;

public class runAllTestCases {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		JUnitCore junit = new JUnitCore();
	    junit.addListener(new TextListener(System.out));
	    junit.run(
	    	LearnerTest.class,
			AtomTest.class,
			ClauseTest.class,
			ConfigTest.class,
			GAtomTest.class,
			GClauseTest.class,
			GroundingTest.class,
			InferenceTest.class,
			LiteralTest.class,
			ParsingLoadingTest.class,
			PredicateTest.class,
			TermTest.class,
			TupleTest.class,
			TypeTest.class
		);

	}

}
