package tuffy.ground;


import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;

import tuffy.db.RDB;
import tuffy.db.SQLMan;
import tuffy.infer.ds.GClause;
import tuffy.mln.Clause;
import tuffy.mln.Literal;
import tuffy.mln.MarkovLogicNetwork;
import tuffy.mln.Predicate;
import tuffy.mln.Term;
import tuffy.util.Config;
import tuffy.util.ExceptionMan;
import tuffy.util.StringMan;
import tuffy.util.Timer;
import tuffy.util.UIMan;

/**
 * This class handles the grounding process of MLN inference/learning
 * with SQL queries. See our technical report at
 * http://wwww.cs.wisc.edu/hazy/tuffy/tuffy-tech-report.pdf
 * 
 * as well as prior works:
 * http://alchemy.cs.washington.edu/papers/singla06a/singla06a.pdf
 * http://alchemy.cs.washington.edu/papers/pdfs/shavlik-natarajan09.pdf
 * 
 * Alchemy implements "lazy inference" with a one-step 
 * look-ahead strategy for initial groundings;
 * we generalize it into a closure algorithm that avoid incremental "activation"
 * altogether.
 */

public class Grounding {
	/**
	 * Relational database used for grounding.
	 */
	private RDB db;

	/**
	 * MLN to be grounded.
	 */
	private MarkovLogicNetwork mln;

	/**
	 * Number of active atoms.
	 */
	private int numAtoms;

	/**
	 * Number of active clauses.
	 */
	private int numClauses;

	/**
	 * Get the MLN object used for grounding.
	 */
	public MarkovLogicNetwork getMLN(){
		return mln;
	}

	/**
	 * Create a grounding worker for an MLN.
	 */
	public Grounding(MarkovLogicNetwork mln){
		bindDB(mln.getRDB());
		this.mln = mln;
	}

	/**
	 * Return the number of active atoms in the grounding result.
	 */
	public int getNumAtoms(){
		return numAtoms;
	}

	/**
	 * Return the number of active clauses in the grounding result.
	 */
	public int getNumClauses(){
		return numClauses;
	}

	private void createAtomTable(String rel){
		db.dropTable(rel);
		
		String seq = "seq_atom_ids";
		if (!db.tableExists(seq)) {
			String sql = "create sequence " + seq;
			db.update(sql);
		}
		
		// create atoms table
		if(Config.using_greenplum){
			String sql = "CREATE TABLE " + rel +
			"(" +
			"tupleID bigint DEFAULT 0, " +
			"atomID INT DEFAULT nextval('seq_atom_ids'), " +
			"predID INT DEFAULT 0, " +
			//"compID INT DEFAULT 0, " +
			//"partID INT DEFAULT 0, " +
			"blockID INT DEFAULT NULL, " +
			"prob FLOAT DEFAULT NULL, " +
			"truth BOOL DEFAULT FALSE, " +
			"isquery BOOL DEFAULT FALSE, " +
			"isqueryevid BOOL DEFAULT FALSE, " + 
			"useful BOOL DEFAULT FALSE " +
			") DISTRIBUTED BY (tupleID)";
			db.update(sql);
		}else{
			String sql = "CREATE TABLE " + rel +
			"(" +
			"tupleID bigint DEFAULT 0, " +
			"atomID INT DEFAULT nextval('seq_atom_ids'), " +
			"predID INT DEFAULT 0, " +
			//"compID INT DEFAULT 0, " +
			//"partID INT DEFAULT 0, " +
			"blockID INT DEFAULT NULL, " +
			"prob FLOAT DEFAULT NULL, " +
			"truth BOOL DEFAULT FALSE, " +
			"isquery BOOL DEFAULT FALSE, " +
			"isqueryevid BOOL DEFAULT FALSE, " + 
			"useful BOOL DEFAULT FALSE " +
			")";
			db.update(sql);
		}
	}

	private void createClauseTable(String rel){
		db.dropTable(rel);
		ArrayList<String> fields = new ArrayList<String>();
		fields.add("cid SERIAL PRIMARY KEY");
		fields.add("lits INT[]");
		fields.add("weight FLOAT8");
		fields.add("fcid INT[]");
		fields.add("ffcid text[]");
		String sql = "CREATE TABLE " + rel +
		StringMan.commaListParen(fields);
		db.update(sql);
	}


	private void createActTables(){
		for(Predicate p : mln.getAllPred()){
			db.dropTable(p.getRelAct());
			String sql = "CREATE TABLE " + p.getRelAct() +
			"(id bigint)";
			db.update(sql);
		}
	}

	private void destroyActTables(){
		for(Predicate p : mln.getAllPred()){
			db.dropTable(p.getRelAct());
		}
	}



	/**
	 * Activate "soft evidence" atoms.
	 */
	private void activateSoftEvidence(){
		int cnt = 0;
		UIMan.verbose(2, ">>> Activating soft evidence atoms...");
		for(Predicate p : mln.getAllPred()) {
			if(!p.hasSoftEvidence()){
				continue;
			}

			UIMan.verbose(2, ">>> Activating soft evidence from " + p.getName());
			String iql = "INSERT INTO " + p.getRelAct() +
			" SELECT id FROM " + p.getRelName() + 
			" WHERE prior >= " + 
			Config.soft_evidence_activation_threshold +
			" AND id NOT IN (SELECT id FROM " + p.getRelAct() + ")";
			db.update(iql);
			UIMan.verbose(2, "### activated atoms = " + 
					UIMan.comma(db.getLastUpdateRowCount()));
			cnt += db.getLastUpdateRowCount();
		}
		if(cnt > 0){
			UIMan.verbose(2, "### active soft evidence = " + 
					UIMan.comma(cnt));
		}
	}


	/**
	 * Activate all the query atoms that are true in the training data.
	 * Used by learning.
	 */
	private void activateQueryAtoms(){
		int cnt = 0;
		UIMan.verbose(2, ">>> Activating query atoms...");
		for(Predicate p : mln.getAllPred()) {
			String iql = "INSERT INTO " + p.getRelAct() +
			" SELECT id FROM " + p.getRelName() + 
			" WHERE (club = 3) OR club = 1 " +
			" EXCEPT SELECT id FROM " + p.getRelAct() + "";
			db.update(iql);
			cnt += db.getLastUpdateRowCount();
		}
		UIMan.verbose(2, "### active query atoms = " + UIMan.comma(cnt));
	}



	private void activateActiveAtoms(){
		int cnt = 0;
		UIMan.verbose(2, ">>> Reactivating already active atoms...");
		for(Predicate p : mln.getAllPred()) {
			String iql = "INSERT INTO " + p.getRelAct() +
			" SELECT id FROM " + p.getRelName() + 
			" WHERE atomid is not null " +
			"AND id NOT IN (SELECT id FROM " + p.getRelAct() + ")";
			db.update(iql);
			cnt += db.getLastUpdateRowCount();
		}
		UIMan.verbose(2, "### active unknown atoms = " + UIMan.comma(cnt));
	}

	private void activateUnknownAtoms(){
		int cnt = 0;
		UIMan.verbose(2, ">>> Activating all unknown atoms...");
		for(Predicate p : mln.getAllPred()) {
			String iql = "INSERT INTO " + p.getRelAct() +
			" SELECT id FROM " + p.getRelName() + 
			" WHERE club < 2 " +
			"AND id NOT IN (SELECT id FROM " + p.getRelAct() + ")";
			db.update(iql);
			cnt += db.getLastUpdateRowCount();
		}
		UIMan.verbose(2, "### active unknown atoms = " + UIMan.comma(cnt));
	}

	/**
	 * Bind to a database connection, and initialize global database
	 * objects.
	 */
	private void bindDB(RDB adb) {
		db = adb;

		String sql;
		sql = "CREATE OR REPLACE FUNCTION " + "unitNegativeClause" +
		"(lits int[]) RETURNS INT AS $$\n" +
		"BEGIN\n" +
		"IF array_upper(lits, 1) > 1 THEN RETURN 0; END IF;\n" +
		"RETURN lits[1];\n" +
		"END;\n" + SQLMan.funcTail() + " IMMUTABLE";
		db.update(sql);

		/*
		seqActiveName = SQLMan.seqName("active_atoms");
		db.dropSequence(seqActiveName);
		db.commit();
		String sql = "CREATE SEQUENCE " + seqActiveName + 
		" START WITH 1";
		db.update(sql);

		sql = "CREATE OR REPLACE FUNCTION " + "convert_id" +
		"(list INT[], oid INT, nid INT) RETURNS INT[] AS $$\n" +
		"DECLARE\n" +
		"nlist INT[]; \n" +
		"BEGIN\n" +
		"nlist := list;" +
		"FOR i IN 1 .. array_upper(list,1) LOOP\n" +
		"IF list[i]=oid THEN nlist[i]:=nid;" +
		"ELSIF list[i]=-oid THEN nlist[i]:=-nid;" +
		"END IF;" +
		"END LOOP;\n" +
		"RETURN UNIQ(SORT(nlist));\n" +
		"END;\n" + SQLMan.funcTail() + " IMMUTABLE";
		db.update(sql);
		 */
	}


	/**
	 * Construct the MRF. First compute the closure of active atoms,
	 * then active clauses.
	 */
	public void constructMRF(){
		UIMan.println(">>> Grounding...");
		
		UIMan.verbose(1, ">>> Computing closure of active atoms...");
		String sql;
		createActTables();
		if(Config.activate_soft_evid) activateSoftEvidence();
		activateActiveAtoms();
		if(Config.learning_mode) 
			activateQueryAtoms();
		if(Config.mark_all_atoms_active){
			activateUnknownAtoms();
		}else{
			computeActiveAtoms();
		}
		
        UIMan.verbose(2, ">>> Gathering active atoms...");
		createAtomTable(mln.relAtoms);
		numAtoms = populateAtomTable(mln.relAtoms);
		UIMan.verbose(2, "### active atoms = " + numAtoms);

		UIMan.verbose(1, ">>> Computing active clauses...");
		String cbuffer = "mln" + mln.getID() + "_cbuffer";
		sql = "CREATE TABLE " + cbuffer + "(list INT[], weight FLOAT8, " +
			"fcid INT, ffcid text)";
		
		db.dropTable(cbuffer);
		db.update(sql);
		this.computeActiveClauses(cbuffer);
		this.addSoftEvidClauses(mln.relAtoms, cbuffer);
		
		//TODO!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
		this.addKeyConstraintClauses(mln.relAtoms, cbuffer);
		
		createClauseTable(mln.relClauses);
		numClauses = this.consolidateClauses(cbuffer, mln.relClauses);

		if(!Config.learning_mode){
			db.dropTable(cbuffer);
		}
		destroyActTables();
		
		UIMan.println("### atoms = " + UIMan.comma(numAtoms) + "; clauses = " + UIMan.comma(numClauses));
	}

	private int populateAtomTable(String relAtoms){
		String sql;
		for(Predicate p : mln.getAllPred()) {
			if(p.isImmutable() && !p.hasQuery() && p.isClosedWorld()){
				UIMan.verbose(2, "--- Not using atoms from " + p.getName());
				continue;
			} else {
				UIMan.verbose(2, "+++ Using atoms from " + p.getName());
			}
			
			
			if(Config.using_greenplum){
				sql = "update " + p.getRelName() + " set atomid=null where atomid=-1";
				db.update(sql);
			}
			
			if(Config.learning_mode){
				sql = "INSERT INTO " + relAtoms + "(atomid,tupleID,predID,truth,prob,isquery,isqueryevid) " +
						"SELECT (case when atomid is null then nextval('seq_atom_ids') else atomid end)," +
						"p.id," + p.getID() + 
						",truth,prior,(club=1),(club=3) FROM " + p.getRelName() + 
						" p LEFT OUTER JOIN " + p.getRelAct() + " ta " +
						" ON p.id=ta.id WHERE p.atomid>0 or ta.id IS NOT NULL";
						UIMan.verbose(2, sql);
						db.update(sql);
			}else{
				sql = "INSERT INTO " + relAtoms + "(atomid,tupleID,predID,truth,prob,isquery,isqueryevid) " +
				"SELECT (case when atomid is null then nextval('seq_atom_ids') else atomid end)," +
				"p.id," + p.getID() + 
				",truth,prior,(club=1),(club=3) FROM " + p.getRelName() + 
				" p LEFT OUTER JOIN " + p.getRelAct() + " ta " +
				" ON p.id=ta.id WHERE p.atomid>0 or ta.id IS NOT NULL";
				UIMan.verbose(2, sql);
				db.update(sql);
			}
			
			//sql = "UPDATE " + p.getRelName() + " pt SET atomID=NULL";
			//db.update(sql);
			
			sql = "UPDATE " + p.getRelName() + " pt SET atomID=ra.atomID " +
			" FROM " + relAtoms + " ra " +
			" WHERE ra.predID=" + p.getID() + " AND ra.tupleID=pt.id";
			
			//UIMan.verbose(3, "----- popularting " + p + "\t" + db.explain(sql));
			UIMan.verbose(2, sql);
			db.update(sql);
			UIMan.verbose(2, "analyzing table");
			db.vacuum(p.getRelName());
			db.analyze(p.getRelName());
			
		}
		db.vacuum(relAtoms);
		db.analyze(relAtoms);
		int numVars = (int)db.countTuples(relAtoms);
		return numVars;
	}

	/**
	 * Compute the closure of active atoms.
	 * 
	 * For a positive clause, active atoms are those with positive sense,
	 * plus those with negative sense but truth value may not be the
	 * default FALSE.
	 * Those with negative sense and default truth value will be true if 
	 * we set default value of atoms as false, and therefore do not generate
	 * violated grounded clauses.
	 * 
	 * For a negative clause, active atoms are those with negative sense.
	 * 
	 * There are multiple rounds in this function. 
	 * The goal of multiple rounds is compute a closure for all possible
	 * active atoms. For example, say we assume the default truth for 
	 * atom is FALSE. Although at first round, we do not need 
	 * to compute the groundings for negative literature $!p$ in a positive clause
	 * except those positive evidences (because they do not introduce any
	 * violations), other clauses may introduce active atoms with the same 
	 * predicate $p$ in the active set.
	 * In this case, these introduced atoms can be flipped, so their truth
	 * values are not necessarily FALSE. Under this circumstance, the negative
	 * literals may also be FALSE.
	 * So there may be more groundings for the first clause, and therefore,
	 * more than one rounds is necessary to adjust that.
	 * 
	 * If in the first round, no more atoms of predicate $p$ is introduced
	 * in active table, then according to the previous analysis, re-grounding
	 * of this clause is not necessary.
	 * 
	 * @return number of active atoms
	 */
	private void computeActiveAtoms() {
		boolean converged = false;
		int cnt = 1;
		int frontier = -2;
		String relTemp = "temp_clauses";
		HashSet<Predicate> changedLastTime = new HashSet<Predicate>();
		HashSet<Predicate> changedThisTime = new HashSet<Predicate>();

		for(Predicate p : mln.getAllPred()){
			changedLastTime.add(p);
			db.analyze(p.getRelName());
		}

		while(!converged) {
			
			if(Config.using_greenplum){
				for(Predicate p : mln.getAllPred()) {
					if(p.isImmutable()) continue;
			
					db.execute("ALTER TABLE " + p.getRelAct() + " SET DISTRIBUTED BY (id)");
					db.execute("ALTER TABLE " + p.getRelName() + " SET DISTRIBUTED BY (id)");
					
					String sql2 = "UPDATE " + p.getRelName() + " pt SET atomID=NULL";
					db.update(sql2);
				
					sql2 = "UPDATE " + p.getRelName() + " pt SET atomID=-1 " +
							" WHERE id IN (SELECT id FROM " + p.getRelAct() + ")";
					UIMan.verbose(3, sql2);
					db.execute(sql2);
					
				}
			}
			
			
			
			
			UIMan.verboseInline(1, ">>> Round #" + (cnt++) + ":");
			UIMan.verbose(2, "");
			converged = true;
			for(Clause c : mln.getRelevantClauses()) {
				
				HashSet<Boolean> possibleClausePos = new HashSet<Boolean>();
				if(c.hasEmbeddedWeight()){
					possibleClausePos.add(true);
					possibleClausePos.add(false);
				}else{
					possibleClausePos.add(c.isPositiveClause());
				}
				
				for(boolean posClause : possibleClausePos){
				
					// optimization: check necessity
					boolean worth = false, fresh = false;
					for(Literal lit : c.getRegLiterals()){
						if((lit.getSense()==posClause) && !lit.getPred().isImmutable()){
							worth = true;
						}
						if(changedLastTime.contains(lit.getPred())) {
							fresh = true;
						}
					}
					if(!worth || !fresh) continue;
					// ground could-be-violated clauses
					ArrayList<String> clubs = new ArrayList<String>();
					ArrayList<String> ids = new ArrayList<String>();
					
					ArrayList<String> actFrom = new ArrayList<String>();
					
					for(Literal l : c.getRegLiterals()){
						
						if(Config.learning_mode
								&& c.isFixedWeight == false
								&& c.hasEmbeddedWeight()){
							
							boolean skip = false;
							for(int k=0;k<l.getTerms().size();k++){
								if(l.getTerms().get(k).var().equals(c.getVarWeight())){
									skip = true;
								}
							}
							
							if(skip == true){
								continue;
							}
						}
						
						actFrom.add(l.getPred().getRelAct() + " at" + l.getIdx()
								+ " ON (at" + l.getIdx() + ".id = t" + l.getIdx() + ".id)");
						
						if(l.getPred().isImmutable()) continue;
						if(l.getSense() != posClause) continue;
						ids.add("t" + l.getIdx() + ".id as id" + l.getIdx());
						clubs.add("t" + l.getIdx() + ".club as club" + l.getIdx());
						
					}
					
					String sql; 
					
					if(Config.using_greenplum == true){
						//TODO: SLOW
						sql = "SELECT DISTINCT " + StringMan.commaList(clubs)+ ", " + 
								StringMan.commaList(ids)  + " FROM " + 
								(Config.learning_mode && c.isFixedWeight == false && c.hasEmbeddedWeight() ? c.sqlFromList_noModel : c.sqlFromList)
								+ " WHERE ";
						
					}else{
						sql = "SELECT DISTINCT " + StringMan.commaList(clubs)+ ", " + 
								StringMan.commaList(ids)  + " FROM " + 
								(Config.learning_mode && c.isFixedWeight == false && c.hasEmbeddedWeight() ? c.sqlFromList_noModel : c.sqlFromList)
								 + " WHERE ";
								;
								//+ 	
								//" LEFT OUTER JOIN " + StringMan.join(" LEFT OUTER JOIN ", actFrom) + " WHERE ";
						
					}
					
					ArrayList<String> conds = new ArrayList<String>();
					// used to exclude all-evidence clauses
					ArrayList<String> negActConds = new ArrayList<String>();
					
					for(Literal lit : c.getRegLiterals()) {
						
						if(Config.learning_mode
								&& c.isFixedWeight == false
								&& c.hasEmbeddedWeight()){
							
							boolean skip = false;
							for(int k=0;k<lit.getTerms().size();k++){
								if(lit.getTerms().get(k).var().equals(c.getVarWeight())){
									skip = true;
								}
							}
							
							if(skip == true){
								continue;
							}
							
							
						}
						
						
						Predicate p = lit.getPred();
						String rp = "t" + lit.getIdx();
						String sra = "(SELECT * FROM " + p.getRelAct() + ")";
						
						ArrayList<String> iconds = new ArrayList<String>();
						String fc = (lit.getSense()?"'0'" : "'1'");
						iconds.add(rp + ".truth=" + fc); // explicit evidence
						if(lit.getSense()){
							//TODO: double check
							if(!lit.getPred().isCompletelySepcified()){
								iconds.add(rp + ".id IS NULL"); // implicit false evidence
							}
						}
						
						if(Config.using_greenplum == false){
							iconds.add(rp + ".id IN " + sra); // current active atoms
						}else{
						//iconds.add(rp + ".id = at" + lit.getIdx() + ".id");
						//	iconds.add("at" + lit.getIdx() + ".id <> NULL");
							iconds.add(rp + ".atomID = -1");
						}
						
						if(lit.getPred().isClosedWorld()){
								
						}else{
							
							if(lit.getSense() || !posClause) {
								iconds.add("(" + rp + ".club < 2)"); // unknown truth
							}
		
							if(!posClause && !lit.getSense()) { // negative clause, negative literal
								negActConds.add("(" + rp + ".club < 2)");
							}
							
						}
	
						conds.add(SQLMan.orSelCond(iconds));
					}
					if(!posClause) {
						if(negActConds.isEmpty()) continue;
						conds.add(SQLMan.orSelCond(negActConds));
					}
	
					/*
					// discard same-variable-opposite-sense truism
					for(Predicate p : c.getReferencedPredicates()) {
						ArrayList<Literal> pos = new ArrayList<Literal>();
						ArrayList<Literal> neg = new ArrayList<Literal>();
						for(Literal lit : c.getLiteralsOfPredicate(p)) {
							if(lit.getSense()) pos.add(lit);
							else neg.add(lit);
						}
						if(pos.isEmpty() || neg.isEmpty()) continue;
						for(Literal plit : pos) {
							for(Literal nlit : neg) {
								String pid = "t" + plit.getIdx() + ".id";
								String nid = "t" + nlit.getIdx() + ".id";
								ArrayList<String> oconds = new ArrayList<String>();
								oconds.add(pid + " IS NULL");
								oconds.add(nid + " IS NULL");
								oconds.add(pid + "<>" + nid);
								conds.add(SQLMan.orSelCond(oconds));
							}
						}
					}
					 */
	
					sql += SQLMan.andSelCond(conds);
					if(!c.sqlWhereBindings.isEmpty()){
						sql += " AND " + c.sqlWhereBindings;
					}
					
					if(c.hasEmbeddedWeight() && Config.ground_atoms_ignore_neg_embedded_wgts){
						
						if(Config.learning_mode && c.isFixedWeight == false){
							
						}else{
							String embedWeightTable = "";
							for(Literal l : c.getRegLiterals()){
								for(int k=0;k<l.getTerms().size();k++){
									if(l.getTerms().get(k).var().equals(c.getVarWeight())){
										embedWeightTable = "t" + l.getIdx() + "." + l.getPred().getArgs().get(k);
									}
								}
							}
							
							if(posClause == true){	
								sql = sql + " AND " + embedWeightTable + " > 0 ";
							}else{
								sql = sql + " AND " + embedWeightTable + " < 0 ";
							}
						}
					}
	
					
					if(Config.verbose_level == 1) UIMan.print(".");
					UIMan.verbose(2, ">>> Grounding " + c.toString());
					UIMan.verbose(3, sql);
					//UIMan.verbose(3, db.explain(sql));
	
					db.dropTable(relTemp);
					sql = "CREATE TABLE " + relTemp +
					" AS " + sql;
				
					
					if(Config.using_greenplum == true){
						for(Predicate toa : c.getReferencedPredicates()){
							
							//db.analyze(toa.getRelName());
							//db.commit();
							
							if(toa.isClosedWorld()){
								continue;
							}
							
							UIMan.verbose(3, ">>> Analyze " + toa.getName());
							
							db.execute("DROP TABLE IF EXISTS _tmp_copy_" + toa.getName() +" CASCADE ");
							db.execute("CREATE TABLE _tmp_copy_" + toa.getName() + " AS SELECT * FROM " + toa.getRelName());
							db.execute("DROP TABLE " + toa.getRelName() + " CASCADE ");
							db.execute("CREATE TABLE " + toa.getRelName() + " AS SELECT * FROM _tmp_copy_" + toa.getName());
							db.commit();
							
						}
					}
					
					
					db.update(sql);
	
					long ngc = db.countTuples(relTemp);
					UIMan.verbose(2, "    Created " + UIMan.comma(ngc) + " groundings");
					UIMan.verbose(3, ">>> Expanding active atoms...");
					// activate more atoms
					boolean found = false;
					for(Literal lit : c.getRegLiterals()) {
						if(lit.getPred().isImmutable()) continue;
						if(lit.getSense() != posClause) continue;
						String iql = "INSERT INTO " + lit.getPred().getRelAct() +
						" SELECT DISTINCT t.id" + lit.getIdx() + " FROM " + relTemp +
						" t WHERE t.club" + lit.getIdx() + " <2" + 
						" EXCEPT " +
						" SELECT * FROM " + lit.getPred().getRelAct();
						if(Config.verbose_level == 1) UIMan.print(".");
						db.update(iql);
						if(db.getLastUpdateRowCount() > 0) {
							found = true;
							UIMan.verbose(2, "    Found " + 
									UIMan.comma(db.getLastUpdateRowCount()) +
									" new active atoms for predicate [" + 
									lit.getPred().getName() + "]" );
							changedThisTime.add(lit.getPred());
							converged = false;
							db.analyze(lit.getPred().getRelAct());
						}
					}
					if(!found){
						UIMan.verbose(2, "    Found no new atoms.");
					}
					UIMan.verbose(2, "");
				}
			}
			
			
			int nmore = 0;
			for(Predicate p : mln.getAllPred()){
				if(p.isImmutable()) continue;
				if(p.hasMoreToGround()){
					nmore++;
					break;
				}
			}
			UIMan.verbose(1, "");
			if(nmore == 0) break;
			changedLastTime = changedThisTime;
			changedThisTime = new HashSet<Predicate>();
			-- frontier;
		}
		db.dropTable(relTemp);
	}


	/**
	 * Create the atom-clause incidence relation.
	 */
	@SuppressWarnings("unused")
	private void computeIncidenceTable(String relClauses, String relIncidence){
		UIMan.println(">>> Computing incidence table...");
		db.dropTable(relIncidence);
		String sql = "CREATE TABLE " + relIncidence +
		"(cid INT, aid INT)";
		db.update(sql);
		sql = "INSERT INTO " + relIncidence + "(cid, aid) " +
		"SELECT cid, ABS(UNNEST(lits)) FROM " + relClauses;
		db.update(sql);
		UIMan.println("### pins = " + db.getLastUpdateRowCount());
	}

	/**
	 * Computes ground clauses activated by the current set 
	 * of active atoms.
	 * 
	 * Grounding the clause using set of active atoms. Then
	 * merge the weight of clauses with the same atom set. 
	 * Another optimization is merging clauses with only one
	 * active atom whose sense is opposite. This is by
	 * reverse the sense of negative clause, together with the sense
	 * of the only literal.
	 * 
	 * The grounding result is saved in table {@value Config#relClauses}.
	 * With the schema like
	 * <br/>
	 * +------+--------+---------+---------+------+-------+<br/>
	 * | lit  | weight | (posWt) | (negWt) | fcid | ffcid |<br/>
	 * +------+--------+---------+---------+------+-------+<br/>
	 * <br/>
	 * where posWt and negWt depend on {@link Config#calcCostOffset},
	 * and fcid {@link Config#track_clause_provenance}.
	 * 
	 * @param retainInactiveAtoms set this to true when
	 * the original lazy inference is in use. Otherwise, i.e. the closure
	 * algorithm is in use, set it to false.
	 * 
	 */
	private void computeActiveClauses(String cbuffer) {
		Timer.start("totalgrounding");
		UIMan.verboseInline(1, ">>> Grounding clauses...");
		UIMan.verbose(2, "");
		double longestSec = 0;
		Clause longestClause = null;
		
		String sql;
		int totalclauses = 0;
		ArrayList<Clause> relevantClauses = new ArrayList<Clause>(mln.getRelevantClauses());

		int clsidx = 1;
		int clstotal = relevantClauses.size();
		for(Clause c : relevantClauses) {
			
			HashSet<Boolean> possibleClausePos = new HashSet<Boolean>();
			if(c.hasEmbeddedWeight()){
				if(Config.learning_mode && c.isFixedWeight == false){
					possibleClausePos.add(true);
				}else{
					possibleClausePos.add(true);
					possibleClausePos.add(false);
				}
			}else{
				possibleClausePos.add(c.isPositiveClause());
			}
			
			for(boolean posClause : possibleClausePos){
			
				ArrayList<String> ids = new ArrayList<String>();
				ArrayList<String> conds = new ArrayList<String>();
				ArrayList<String> negActConds = new ArrayList<String>();
				
				// discard irrelevant variables
				for(Literal lit : c.getRegLiterals()) {
					
					
					if(Config.learning_mode
							&& c.isFixedWeight == false
							&& c.hasEmbeddedWeight()){
						
						boolean skip = false;
						for(int k=0;k<lit.getTerms().size();k++){
							if(lit.getTerms().get(k).var().equals(c.getVarWeight())){
								skip = true;
							}
						}
						
						if(skip == true){
							continue;
						}
					}
					
					
					Predicate p = lit.getPred();
					String r = "t" + lit.getIdx();
					if(!p.isImmutable()) {
						ids.add((lit.getSense()?"" : "-") + "(CASE WHEN " + 
								r + ".id IS NULL THEN 0 WHEN " +
								r + ".atomID IS NULL THEN 0 ELSE " +
								r + ".atomID END)");
					}
					String fc = (lit.getSense()?"FALSE" : "TRUE");
	
					ArrayList<String> iconds = new ArrayList<String>();
					String rp = r;
					
					//if(Config.learning_mode && lit.getPred().hasQuery()){
					//	iconds.add(rp + ".truth=" + rp + ".truth");
					//}else{
						iconds.add(rp + ".truth=" + fc); // explicit evidence
					//}
					
					if(lit.getSense()){
						//TODO: double check
						if(!lit.getPred().isCompletelySepcified()){
							iconds.add(rp + ".id IS NULL"); // implicit false evidence
						}
					}
	
					if(lit.getSense() || !posClause) {
						
						if(lit.getPred().isClosedWorld()){
							//TODO: double check!!!!!!!!!!!!!!
							if(lit.getPred().hasSoftEvidence()){
								iconds.add(r + ".atomID IS NOT NULL");
							}
							
						}else{
							if(Config.learning_mode){
								iconds.add(rp + ".club < 2 OR " + rp + ".club = 3");
							}else{
								iconds.add(rp + ".club < 2 OR " + rp + ".prior IS NOT NULL"); // unknown truth
							}
						}
						if(!posClause) {
							negActConds.add(r + ".atomID IS NOT NULL"); // active atom
						}
					}else {
						iconds.add(r + ".atomID IS NOT NULL"); // active atoms
					}
	
					if(!posClause && !lit.getSense()) { // negative clause, negative literal
						if(lit.getPred().isClosedWorld()){
							
						}else{
							if(Config.learning_mode){
								negActConds.add("(" + rp + ".club < 2 OR " + rp + ".club = 3" + ")");
							}else{
								negActConds.add("(" + rp + ".club < 2" + ")");
							}
						}
						
					}
	
					conds.add(SQLMan.orSelCond(iconds));
	
				}
				if(ids.isEmpty()) continue;
				if(!posClause) {
					if(negActConds.isEmpty()) continue;
					conds.add(SQLMan.orSelCond(negActConds));
				}
				/*
				// discard same-variable-opposite-sense truism
				for(Predicate p : c.getReferencedPredicates()) {
					ArrayList<Literal> pos = new ArrayList<Literal>();
					ArrayList<Literal> neg = new ArrayList<Literal>();
					for(Literal lit : c.getLiteralsOfPredicate(p)) {
						if(lit.getSense()) pos.add(lit);
						else neg.add(lit);
					}
					if(pos.isEmpty() || neg.isEmpty()) continue;
					for(Literal plit : pos) {
						for(Literal nlit : neg) {
							String pid = "t" + plit.getIdx() + ".id";
							String nid = "t" + nlit.getIdx() + ".id";
							ArrayList<String> oconds = new ArrayList<String>();
							oconds.add(pid + " IS NULL");
							oconds.add(nid + " IS NULL");
							oconds.add(pid + "<>" + nid);
							conds.add(SQLMan.orSelCond(oconds));
						}
					}
				}
				*/
				
				String ffid = "0";
				String signature = "";
				
				if(c.hasEmbeddedWeight()){
					
					
					for(Literal lit : c.getRegLiterals()) {
						boolean found = false;
						signature = "";
						
						for(int k=0;k<lit.getTerms().size();k++){
							
							if(lit.getTerms().get(k).var().equals(c.getVarWeight())){
								found = true;
							}
						}
						
						if(found == false){
							continue;
						}
						
						for(int k=0;k<lit.getTerms().size();k++){
						
							if(lit.getTerms().get(k).var().equals(c.getVarWeight())){
								found = true;
								signature += "'%f,' || ";
							}else{
								
								String fields = "";
								for(Literal l : c.getRegLiterals()) {
									for(int w=0;w<l.getTerms().size();w++){
										
										if(l != lit && 
												l.getTerms().get(w).var().equals(lit.getTerms().get(k).var())){
											fields = "t" + l.getIdx() + "." + 
												l.getPred().getArgs().get(w);
											
										}
									}
								}
								
								if(fields.equals("")){
									ExceptionMan.die("No safe: " + c);
								}
							
								signature += fields + " || ',' || ";
							}
														
						}
						
						signature += " ''";
						
						if(found == true){
							break;
						}
					}
					
					
					if(c.isFixedWeight && c.isHardClause()){
						signature += "|| 'hardfixed'";
					}else if(c.isFixedWeight){
						signature += "|| 'fixed'";
					}
				}else if(c.getWeightExp().contains("metaTable")){
					signature = "metaTable.myid::TEXT";
					
					//TODO: bug -- consider clause instances
					//if(c.isFixedWeight){
					signature += "|| metaTable.myisfixed";
					//}
				}else{
					signature = "CAST('0' as TEXT)";
					if(c.isFixedWeight && c.isHardClause()){
						signature += "|| 'hardfixed'";
					}else if(c.isFixedWeight){
						signature += "|| 'fixed'";
					}
				}
					
				
				
				
				if(!c.hasExistentialQuantifiers()) {
					sql = "SELECT " +
					"UNIQ(SORT(ARRAY[" + StringMan.commaList(ids) + "]-0)) as list2, " + 

(Config.learning_mode && c.isFixedWeight == false && c.hasEmbeddedWeight() ? "0.01 as weight2" : c.getWeightExp() + " as weight2 ") + 

					
					// ffid
					//(!c.getWeightExp().contains("metaTable")?
					//		(", CAST(0 as Integer) as ffid"):
					//			(", metaTable.myid as ffid "))+
					"," + signature + " as ffid " + 
					" FROM " +
					
(Config.learning_mode && c.isFixedWeight == false && c.hasEmbeddedWeight() ? c.sqlFromList_noModel : c.sqlFromList)

					
					+ " WHERE " + c.sqlWhereBindings;
					if(!conds.isEmpty()) {
						sql += " AND " + SQLMan.andSelCond(conds);
					}
					
					if(Config.learning_mode && c.hasEmbeddedWeight() && c.isFixedWeight==false){
						
					}else{
						if(posClause == true){
							sql = sql + " AND " + c.getWeightExp() + " > 0 ";
						}else{
							sql = sql + " AND " + c.getWeightExp() + " < 0 ";
						}
					}
					
				}else {
					ArrayList<String> aggs = new ArrayList<String>();
					for(String ide : ids) {
						aggs.add("array_agg(" + ide + ")");
					}
					sql = "SELECT " + c.sqlPivotAttrsList +
					(c.sqlPivotAttrsList.length() > 0 ? "," : "") +
					" UNIQ(SORT("+ StringMan.join("+", aggs) +"-0)) as list2, " +

(Config.learning_mode && c.isFixedWeight == false && c.hasEmbeddedWeight() ? "0.01 as weight2" : c.getWeightExp() + " as weight2 ") + 

					
					// ffid
					//(c.getWeightExp().contains("FLOAT8")?
					//		(", CAST(0 as Integer) as ffid"): 
					//		(", metaTable.myid as ffid ")) +
					"," + signature + " as ffid " + 
					" FROM " +
					
(Config.learning_mode && c.isFixedWeight == false && c.hasEmbeddedWeight() ? c.sqlFromList_noModel : c.sqlFromList)

					
					+ " WHERE " + c.sqlWhereBindings;
					if(posClause){
						sql += " AND " + c.getWeightExp() + " > 0 ";
					}else{
						sql += " AND " + c.getWeightExp() + " < 0 ";
					}
					if(!conds.isEmpty()) {
						sql += " AND " + SQLMan.andSelCond(conds);
					}
					if(c.sqlPivotAttrsList.length() > 0){
						sql += " GROUP BY " + c.sqlPivotAttrsList + " , ffid";
					}
					sql = "SELECT list2, weight2, ffid FROM " +
					"(" + sql + ") tpivoted";
				}

				boolean unifySoftUnitClauses = true;
				if(Config.learning_mode){
					unifySoftUnitClauses = false;
				}
				if(unifySoftUnitClauses) {
					sql = "SELECT (CASE WHEN unitNegativeClause(list2)>=0 THEN " +
					"list2 ELSE array[-list2[1]] END) AS list, " +
					"(CASE WHEN unitNegativeClause(list2)>=0 THEN weight2 " +
					"ELSE -weight2 END) AS weight, " + 
					"(CASE WHEN unitNegativeClause(list2)>=0 THEN " + c.getId() + " " +
					"ELSE -" + c.getId() + " END) AS fcid " +
					// ffcid, for learning
					", (CASE WHEN unitNegativeClause(list2)>=0 THEN ('" + 
					c.getId() + ".' || ffid) " + 
					"ELSE ('-" + c.getId() + ".' || ffid) END) AS ffcid " + 
					//
					"FROM (" + sql + ") as " + c.getName() +
					" WHERE array_upper(list2,1)>=1";
				}else {
					sql = "SELECT list2 AS list, " +
					"weight2 AS weight, " + c.getId() + " AS fcid, '" +
					c.getId() + ".' || ffid as ffcid " + 
					"FROM (" + sql + ") as " + c.getName() +
					" WHERE array_upper(list2,1)>=1";
				}
				if(Config.verbose_level == 1) UIMan.print(".");
				UIMan.verbose(2, ">>> Grounding clause " + 
						(clsidx++) + " / " + clstotal + "\n" +
						c.toString());
				UIMan.verbose(3, sql);
				sql = "INSERT INTO " + cbuffer + "\n" + sql;
				Timer.start("gnd");
	
				
				if(Config.using_greenplum == true){
					for(Predicate toa : c.getReferencedPredicates()){
						
						//db.analyze(toa.getRelName());
						//db.commit();
						
						if(toa.isClosedWorld()){
							continue;
						}
						
						UIMan.verbose(3, ">>> Analyze " + toa.getName());
						
						db.execute("DROP TABLE IF EXISTS _tmp_copy_" + toa.getName() + " CASCADE ");
						db.execute("CREATE TABLE _tmp_copy_" + toa.getName() + " AS SELECT * FROM " + toa.getRelName());
						db.execute("DROP TABLE " + toa.getRelName() + " CASCADE ");
						db.execute("CREATE TABLE " + toa.getRelName() + " AS SELECT * FROM _tmp_copy_" + toa.getName());
						
						
					}
				}
				
				
				db.update(sql);
	
				// report stats
				totalclauses += db.getLastUpdateRowCount();
				if(Timer.elapsedSeconds("gnd") > longestSec){
					longestClause = c;
					longestSec = Timer.elapsedSeconds("gnd");
				}
				UIMan.verbose(2, "### took " + Timer.elapsed("gnd"));
				UIMan.verbose(2, "### new clauses = " + 
						UIMan.comma(db.getLastUpdateRowCount()) +
						"; total = " + UIMan.comma(totalclauses) + "\n");
			}
		}
		if(longestClause != null){
			UIMan.verbose(3, "### Longest per-clause grounding time = " + longestSec + " sec, by");
			UIMan.verbose(3, longestClause.toString());
		}
		if(Config.verbose_level == 1) UIMan.println(".");
		UIMan.verbose(1, "### total grounding = " + Timer.elapsed("totalgrounding"));
	}

	
	private int consolidateClauses(String cbuffer, String relClauses){
				
		UIMan.verbose(1, ">>> Consolidating ground clauses...");
		String sql;
		// combine equivalent classes
		ArrayList<String> args = new ArrayList<String>();
		ArrayList<String> sels = new ArrayList<String>();
		args.add("lits");
		args.add("weight");
		sels.add("list");
		sels.add("sum(weight)");
		if(Config.track_clause_provenance){
			args.add("fcid");
			args.add("ffcid");
			sels.add("UNIQ(SORT(array_agg(fcid)))");
			sels.add("array_agg(ffcid)");
		}
		sql = "INSERT INTO " + relClauses +
		StringMan.commaListParen(args) + 
		" SELECT " +
		StringMan.commaList(sels) +
		"FROM " + cbuffer +
		" GROUP BY list";
		Timer.start("gnd");
		db.update(sql);
		UIMan.verbose(1, "### took " + Timer.elapsed("gnd"));
		int numClauses = db.getLastUpdateRowCount();
		return numClauses;
	}
	
	private void addSoftEvidClauses(String relAtoms, String cbuffer){
		UIMan.verbose(1, ">>> Adding unit clauses for soft evidence...");
		int cnt = 0;
		String iql = "INSERT INTO " + cbuffer + "(list, weight, fcid, ffcid) " +
		" SELECT array[atomID], " +
		" (CASE WHEN prob>=1 THEN " + Config.hard_weight +
		" WHEN prob<=0 THEN -" + Config.hard_weight +
		" ELSE ln(prob / (1-prob)) END), " +
		" 0, '0' " +
		" FROM " + relAtoms + 
		" WHERE prob IS NOT NULL";
		db.update(iql);
		cnt += db.getLastUpdateRowCount();
		UIMan.verbose(1, "### soft-evidence clauses = " + UIMan.comma(cnt));
	}

	private void addKeyConstraintClauses(String relAtoms, String cbuffer){

		UIMan.verbose(1, ">>> Adding key constraint clauses...");
		
		int fcid = mln.getAllNormalizedClauses().size();
		int ffcid_tail = 1;
		
		for(Predicate p : mln.getAllPred()){
			
			if(!p.hasDependentAttributes()){
				continue;
			}
			
			fcid ++;
			String ffcid = fcid + "." + ffcid_tail;
			
			
			this.mln.additionalHardClauseInstances.add(ffcid);
			
			ArrayList<String> whereList_key = new ArrayList<String>();
			ArrayList<String> whereList_label = new ArrayList<String>();
			
			// goal:
			//select array[-t0.atomid, -t1.atomid], Integer.Max, 1, 1.1 
			//from pred_dwinner t0, pred_dwinner t1 
			//where (t0.key1 = t1.key1 AND ...) AND (t0.label2 <> t1.label2 OR ... )
			//AND t0.atomid in (select atomid from mln0_atoms)
			
			
			/**
			 * Mutex
			 */
			for(String keyAttr : p.getKeyAttrs()){
				whereList_key.add("(t0." + keyAttr + "=" + "t1." + keyAttr + ")");
			}
			
			for(String labelAttr : p.getDependentAttrs()){
				whereList_label.add("(t0." + labelAttr + "<>" + "t1." + labelAttr + ")");
			}
			
			String sql = "INSERT INTO " + cbuffer + 
						 " SELECT array[-t0.atomid, -t1.atomid], " + 
					Config.hard_weight + ", " + fcid + ", '" + ffcid +"'" +
						 " FROM " + p.getRelName() + " t0, " + p.getRelName() + " t1 " +
						 " WHERE (" + StringMan.join(" AND ", whereList_key) + ") AND " +
						        "(" + StringMan.join(" OR ", whereList_label) + ") AND " +
						        " t0.atomid IN (SELECT atomid FROM " + relAtoms +") AND " +
						        " t1.atomid IN (SELECT atomid FROM " + relAtoms +")";

			UIMan.verbose(3, sql);
			db.execute(sql);
			UIMan.verbose(2, "    added " + db.getLastUpdateRowCount() + 
					" clauses for " + p.getName());
			
			/**
			 * Existence
			 */
			if (!Config.key_constraint_allows_null_label) {
				sql = "INSERT INTO " + cbuffer + 
						" SELECT lits, " + Config.hard_weight + ", " + fcid + ", '" + ffcid +"'" +
						 " FROM (SELECT array_agg(atomid) as lits, " + StringMan.join(",", p.getKeyAttrs()) +
							 " FROM " + p.getRelName() + 
							 " WHERE atomid IN (SELECT atomid FROM " + relAtoms +") " +
							 " GROUP BY " + StringMan.join(",", p.getKeyAttrs()) +") nt";
				UIMan.verbose(3, sql);
				db.execute(sql);
			}
		}
		
	}
	
	/**
	 * 
	 * An attempt of computing cost lower bounds with the probabilistic method.
	 */
	@SuppressWarnings("unused")
	private void reportCostStats(String relClauses){
		try {
			String sql = "SELECT SUM(ABS(weight)) FROM " + relClauses;
			ResultSet rs = db.query(sql);
			if(rs.next()){
				double tw = rs.getDouble(1);
				UIMan.println("total weight = " + tw);
			}
			rs.close();

			sql = "SELECT * FROM " + relClauses;
			rs = db.query(sql);
			double[] posApp = new double[numAtoms+1];
			double[] negApp = new double[numAtoms+1];
			double[] freq = new double[numAtoms+1];
			double[] probs = {0, 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1};
			double[] ecosts = new double[probs.length];
			double[] punsat = new double[probs.length];
			while(rs.next()){
				GClause c = new GClause();
				c.parse(rs);
				for(int i=0; i<probs.length; i++){
					punsat[i] = 1;
				}
				for(int lit : c.lits){
					if(lit > 0){
						if(c.weight>0) posApp[lit]+=c.weight/c.lits.length;
						else negApp[lit]-=c.weight*c.lits.length;
						for(int i=0; i<probs.length; i++){
							punsat[i] = punsat[i] * (1-probs[i]);
						}
					}
					if(lit < 0){
						if(c.weight>0) negApp[-lit]+=c.weight/c.lits.length;
						else posApp[-lit]-=c.weight*c.lits.length;
						for(int i=0; i<probs.length; i++){
							punsat[i] = punsat[i] * (probs[i]);
						}
					}
				}
				if(c.weight > 0){
					for(int i=0; i<probs.length; i++){
						ecosts[i] += punsat[i] * c.weight;
					}
				}else{
					for(int i=0; i<probs.length; i++){
						ecosts[i] -= (1-punsat[i]) * c.weight;
					}
				}
			}
			rs.close();

			for(int i=0; i<probs.length; i++){
				System.out.println("For all x P[x] = " + probs[i] + " --> E[cost] = " + ecosts[i]);
			}

			for(int i=1; i<=numAtoms; i++){
				if(posApp[i] + negApp[i] == 0) continue;
				freq[i] = posApp[i] / (double)(posApp[i] + negApp[i]);
			}

			rs = db.query(sql);
			double ocost = 0;
			while(rs.next()){
				GClause c = new GClause();
				c.parse(rs);
				double opunsat = 1;
				for(int lit : c.lits){
					if(lit > 0){
						opunsat *= 1 - freq[lit];
					}else{
						opunsat *= freq[-lit];
					}
				}
				if(c.weight > 0){
					ocost += opunsat * c.weight;
				}else{
					ocost -= (1-opunsat) * c.weight;
				}
			}
			rs.close();
			System.out.println("Heterogeneous probs --> E[cost] = " + ocost);

		} catch (SQLException e) {
			ExceptionMan.handle(e);
		}
	}


}
