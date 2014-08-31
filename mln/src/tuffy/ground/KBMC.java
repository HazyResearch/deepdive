package tuffy.ground;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Iterator;


import tuffy.mln.Atom;
import tuffy.mln.Clause;
import tuffy.mln.Literal;
import tuffy.mln.MarkovLogicNetwork;
import tuffy.mln.Predicate;
import tuffy.mln.Term;
import tuffy.mln.Tuple;
import tuffy.util.UIMan;

/**
 * "Syntactic" Knowledge Base Model Constructor. 
 * Analyze the query and the MLN rules to identify
 * relevant atoms that need to be grounded to answer the query. 
 * Here KBMC only
 * materialize relevant portion of predicate tables, while further refinement
 * of active set of predicates and grounding
 * of clauses are left to class {@link Grounding}.
 */
public class KBMC {
	/**
	 * MLN used in this KBMC.
	 */
	private MarkovLogicNetwork mln;
	
	/**
	 * Constructor of KBMC.
	 * @param mln MLN used in this KBMC.
	 */
	public KBMC(MarkovLogicNetwork mln){
		this.mln = mln;
	}
	
	public HashSet<Clause> allowedClauses = null;
	
	/**
	 * Run KBMC to identify and materialize relevant groundings of predicates.
	 * 
	 * Here the relevant atoms needed to be grounded is determined as
	 * follows. One atom may be potentially relevant if it is in a clause $c$
	 * that contains a literal $a$ that has the same predicate as another
	 * potentially relevant tuple $b$. If $a$ and this predicate $b$
	 * has MGU $m$, substituting this atom with $m$ will obtain tuple that
	 * is potentially relevant. Recursively do this until no potentially
	 * relevant tuples are generated according current set of potentially
	 * relevant tuples. Here $m$'s role is providing restrictions on possible
	 * groundings.
	 * 
	 * Only ground a set of tuples, s.t., 1) $\forall$ tuple $a$, $b$ in this
	 * set, $a$ does not subsume $b$; and 2) $\forall$ tuple $c$ that is
	 * potentially relevant, there exists tuple $a$ in this set, that $a$
	 * subsumes $c$.
	 * 
	 * Note that, grounding both $a$ and $b$ when $a$ subsumes $b$ is not
	 * necessary, because the grounding results of $a$ will include $b$.
	 * 
	 */
	public void run() {
		// atoms to be explored
		Hashtable<Predicate, AtomCutSet> toExp = new 
			Hashtable<Predicate, AtomCutSet>();
		// atoms deemed relevant
		Hashtable<Predicate, AtomCutSet> relAtoms = new 
			Hashtable<Predicate, AtomCutSet>();
		UIMan.verbose(1, ">>> KBMC: Identifying relevant atoms/clauses...");
		// begin with queries
		for(Predicate qp : mln.getAllPred()) {
			AtomCutSet q = new AtomCutSet(qp);
			if(qp.hasQuery()) {
				for(Atom a : qp.getQueryAtoms()) {
					q.addTuple(a.args);
					UIMan.println(a.toString());
				}
			}
			toExp.put(qp, q);

			relAtoms.put(qp, new AtomCutSet(qp));
		}
		bigloop:
			while(true) {
				// pick next seed
				Predicate pred = null;
				Tuple seed = null;
				for(AtomCutSet aset : toExp.values()) {
					seed = aset.top();
					if(seed != null) {
						pred = aset.pred;
						break;
					}
				}
				// check if seeds have been exhausted
				if(seed == null) break;
				//System.out.println("SEED - " + pred.getName() + seed);

				// search
				for(Clause c : pred.getRelatedClauses()) {
					
					if(allowedClauses != null && !allowedClauses.contains(c)){
						continue;
					}
					
					//System.out.println("CLAUSE - " + c);
					mln.setClauseAsRelevant(c);
					for(Literal lit : c.getLiteralsOfPredicate(pred)) {
						if(lit.isBuiltIn()) continue;
						// calc mgu
						HashMap<String, Term> vmap = lit.mostGeneralUnification(seed);
						if(vmap == null) continue;
						// apply to other literals
						for(Literal ol : c.getRegLiterals()) {
							if(ol.isBuiltIn()) continue;
							if(ol.equals(lit)) continue;
							Literal sol = ol.substitute(vmap);
							Tuple newa = sol.toTuple();
							Predicate newp = sol.getPred();
							if(newp.noNeedToGround()) continue;
							if(toExp.get(newp).subsumes(newa) ||
									relAtoms.get(newp).subsumes(newa)) {
								continue;
							}
							//System.out.println("GOT " + newp.name + newa);
							toExp.get(newp).addTuple(newa);
							// short cut
							if(newp.equals(pred) && !toExp.get(pred).contains(seed)) {
								continue bigloop;
							}
						}

					}
				}
				toExp.get(pred).removeTuple(seed);
				relAtoms.get(pred).addTuple(seed);
			}
		// fill up tables
		UIMan.verbose(1, ">>> KBMC: Materializing predicates...");
		for(AtomCutSet as : relAtoms.values()) {
			Predicate p = as.pred;
			
			if(mln.isScoped(p) || p.noNeedToGround()) {
				UIMan.verbose(2, "    Skipped " + p.getName());
				continue;
			}
			boolean suc = as.collectAll();
			if(suc) {
				for(Tuple t : as.heap) {
					Atom a = new Atom(p, t);
					if(a.pred.isClosedWorld()){
						a.truth = false;
						a.type = Atom.AtomType.EVIDENCE;
					}
					UIMan.verbose(1, "    " + a.toString() + 
							" - " + UIMan.comma(a.groundSize()) + " tuples");
					if (a.groundSize() > 10000000) {
						UIMan.verbose(1, "    " + 
							"You may want to consider using scoping rules!");
					}
					p.groundAndStoreAtom(a);
				}
			}
		}
	}
	
	/**
	 * AtomCutSet is set of atoms with the same predicate.
	 * These atoms are organized in strata, with each stratum
	 * responds to a different degree-of-freedom. Atoms in a AtomCutSet
	 * satisfy that $\forall$ atom $a$ in this set, there does not exist atom $b$
	 * in this set that $a$ subsumes $b$. For the definition of ``subsume'', see 
	 * {@link Tuple#subsumes(Tuple)}.
	 *
	 */
	private static class AtomCutSet {
		/**
		 * Predicate of this AtomCutSet. 
		 */
		Predicate pred;
		
		/**
		 * Arity of {@link AtomCutSet#pred}. 
		 */
		int arity;
		
		/**
		 * Whether this AtomCutSet is complete. Here by ``complete''
		 * it means arbitrary tuple is subsumed by some tuples in this
		 * AtomCutSet. For the definition of ``subsume'', see 
		 * {@link Tuple#subsumes(Tuple)}.
		 */
		boolean complete = false;
		
		/**
		 * List of stratum in this AtomCutSet.
		 */
		ArrayList<Stratum> strata = new ArrayList<Stratum>();
		
		/**
		 * Set of all tuples in each stratum.
		 */
		HashSet<Tuple> heap = new HashSet<Tuple>();
		
		/**
		 * Copy all tuples in each stratum to {@link AtomCutSet#heap}. 
		 * @return true if some of this stratum is not empty.
		 */
		public boolean collectAll() {
			for(Stratum s : strata) {
				heap.addAll(s.tuples);
			}
			return (!heap.isEmpty());
		}
		
		/**
		 * Constructor of AtomCutSet. Given a predicate, 
		 * initialize a stratum for each number between
		 * 0 and this predicate's arity. Each stratum
		 * for number $n$ means tuples in this stratum 
		 * have degree-of-freedom $n$. 
		 * @param p
		 */
		public AtomCutSet(Predicate p) {
			pred = p;
			arity = p.arity();
			for(int i=0; i<=p.arity(); i++) {
				strata.add(new Stratum());
			}
		}

		/**
		 * Returns the first tuple in the first stratum.
		 */
		public Tuple top() {
			for(int i=arity; i>=0; i--) {
				Stratum s = strata.get(i);
				if(!s.isEmpty()) {
					return s.tuples.iterator().next();
				}
			}
			return null;
		}

		/**
		 * Remove the input tuple from corresponding stratum. Here
		 * the corresponding stratum is found by the degree-of-freedom
		 * of the input tuple.
		 * @param t input tuple.
		 */
		public void removeTuple(Tuple t) {
			strata.get(t.dimension).tuples.remove(t);
		}
		
		/**
		 * Return true if the input tuple is in the corresponding stratum.
		 * Here the corresponding stratum is found by the degree-of-freedom
		 * of input tuple.
		 * @param t input tuple.
		 */
		public boolean contains(Tuple t) {
			return strata.get(t.dimension).tuples.contains(t);
		}
		
		
		/**
		 * Returns true if there exists a tuple in this AtomCutSet
		 * that subsumes the input tuple. For the definition of ``subsume'', see 
		 * {@link Tuple#subsumes(Tuple)}.
		 * @param t input tuple.
		 */
		public boolean subsumes(Tuple t) {
			int dim = t.dimension;
			if(complete) return true;
			if(dim == arity) {
				return false;
			}
			for(int i=arity; i>= dim; i--) {
				if(strata.get(i).subsumes(t)) return true;
			}
			return false;
		}
		
		/**
		 * Add a tuple to corresponding stratum. Remove all the
		 * existing tuples that can be subsumed by the new input
		 * tuple. The goal is $\forall$ tuples $a$ and $b$ in 
		 * a certain stratum, $a$ does not subsume $b$. This function
		 * pluses a condition in {@link KBMC#run()} ensure this.
		 * 
		 * @param t input tuple
		 */
		public void addTuple(Tuple t) {
			int dim = t.dimension;
			if(dim == arity) {
				for(Stratum s : strata) {
					s.clear();
				}
				strata.get(dim).add(t);
				complete = true;
				return ;
			}
			strata.get(dim).add(t);
			for(int i=dim-1; i>=0; i--) {
				strata.get(i).removeTuplesSubsumedBy(t);
			}
		}
		
		/**
		 * A stratum is a set of tuples with the same degree-of-freedom.
		 */
		class Stratum{
			/**
			 * Set of tuples in this stratum.
			 */
			HashSet<Tuple> tuples = new HashSet<Tuple>();

			/**
			 * Add a tuple to this stratum.
			 * @param t Added tuple.
			 */
			public void add(Tuple t) {
				tuples.add(t);
			}
			
			/**
			 * Test whether there is a tuple in this stratum
			 * subsumes the input tuple. For the definition of 
			 * ``subsume'', see {@link Tuple#subsumes(Tuple)}.
			 * @param t tuple to be tested.
			 * @return true if there is a tuple subsumes the input tuple.
			 */
			public boolean subsumes(Tuple t) {
				for(Tuple at : tuples) {
					if(at.subsumes(t)) return true;
				}
				return false;
			}
			
			/**
			 * Remove tuples in this stratum that subsumes the input tuple.
			 * 
			 * @param t input tuple.
			 * @see Stratum#subsumes(Tuple). 
			 */
			public void removeTuplesSubsumedBy(Tuple t) {
				for(Iterator<Tuple> it = tuples.iterator(); it.hasNext();) {
					if(t.subsumes(it.next())) {
						it.remove();
					}
				}
			}
			
			/**
			 * Return true if this stratum is empty.
			 */
			public boolean isEmpty() {
				return tuples.isEmpty();
			}
			
			/**
			 * Empty this stratum.
			 */
			public void clear() {
				tuples.clear();
			}
		}
	}

}
