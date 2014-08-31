package tuffy.infer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import tuffy.db.RDB;
import tuffy.db.SQLMan;
import tuffy.ground.Grounding;
import tuffy.mln.MarkovLogicNetwork;
import tuffy.util.Config;
import tuffy.util.StringMan;
import tuffy.util.Timer;
import tuffy.util.UIMan;
/**
 * RDBM-based WalkSAT using PostgreSQL's stored procedures.
 * WARNING: IT IS SLOW!
 * @deprecated
 */
public class PGInfer {
	RDB db;
	MarkovLogicNetwork mln;
	Grounding grounding;
	
	String procWalk = "procWalk";
	String procSweep = "procSweep";
	String relLog = "log_pginfer";

	int numVars, numClauses;
	public double lowCost = Double.POSITIVE_INFINITY;
	double totalCost = 0;
	
	public PGInfer(Grounding g){
		grounding = g;
		mln = g.getMLN();
		db = mln.getRDB();
		numVars = g.getNumAtoms();
		numClauses = g.getNumClauses();
	}
	
	private void regUtils(){
		String sql = "CREATE OR REPLACE FUNCTION " + "count_nsat" +
		"(lits INT[], truth BOOL[]) RETURNS SMALLINT AS $$\n" +
		"DECLARE\n" +
		"vnsat SMALLINT := 0; \n" +
		"var INT; \n" +
		"BEGIN\n" +
		"FOR i IN 1 .. array_upper(lits,1) LOOP\n" +
		"var := lits[i];\n" +
		"IF (var>0 AND truth[var]) OR (var<0 AND NOT truth[-var]) THEN\n" +
		"vnsat := vnsat + 1;\n" +
		"END IF;\n" +
		"END LOOP;\n" +
		"RETURN vnsat;\n" +
		"END;\n" + SQLMan.procTail() + " IMMUTABLE";
		db.update(sql);

		sql = "CREATE OR REPLACE FUNCTION " + "count_negative" +
		"(list FLOAT8[]) RETURNS INT AS $$\n" +
		"DECLARE\n" +
		"cnt INT := 0; \n" +
		"BEGIN\n" +
		"FOR i IN 1 .. array_upper(list,1) LOOP\n" +
		"IF list[i]<-0.0001 THEN cnt:=cnt+1; END IF;\n" +
		"END LOOP;\n" +
		"RETURN cnt;\n" +
		"END;\n" + SQLMan.procTail() + " IMMUTABLE";
		db.update(sql);
		
		sql = "CREATE OR REPLACE FUNCTION randomIndex(int)\r\n" + 
				"RETURNS int AS\r\n" + 
				"$$\r\n" + 
				"   SELECT (0.5 + $1 * random())::int;\r\n" + 
				"$$ LANGUAGE 'sql' VOLATILE;";
		db.update(sql);
		
		db.dropTable(relLog);
		sql = "CREATE TEMPORARY TABLE " + relLog +
		"(xtime FLOAT, xcost FLOAT)";
		db.update(sql);
	}
	
	private void regWalk(){
		if(Config.timeout > 75000) Config.timeout = 75000;
		String relClauses = mln.relClauses;
		regUtils();
		String sql;
		String vecZero = "'{" + StringMan.repeat("0,", numVars-1) + "0}'";

		ArrayList<String> lines = new ArrayList<String>();
		sql = "CREATE OR REPLACE FUNCTION " + procWalk +
		"(nTries INT, nSteps INT) RETURNS FLOAT8 AS $$\n";
		sql += "DECLARE\n" +
		"timeBegin REAL := " + Timer.elapsedSeconds() + ";\n" +
		"timeNow REAL := 0; \n" +
		"cost FLOAT8 := 0; \n" +
		"lowCost FLOAT8 := 1E+100; \n" +
		
		"truth BOOL[] := " + vecZero + ";\n" +
		"lowTruth BOOL[];\n" +
		"delta FLOAT8[] := " + vecZero + ";\n" +
		"violate FLOAT8[] := " + vecZero + ";\n" +

		"repickClause BOOL := TRUE; \n" +
		"pickedCID int := 0;\n" +
		"pickedClause RECORD;\n" +
		"rec RECORD; \n" +
		"nsat SMALLINT := 0; \n" +
		"weight FLOAT8 := 0; \n" +
		"tallyViolation INT := 0; \n" +
		"inferOps INT := 0; \n" +
		"pickedAtom INT := 0; \n" +
		"lowDelta FLOAT8 := 0; \n" +
		"rivals INT := 0; \n" +
		"contact INT := 0; \n" +

		"cur INT := 0; \n" +
		"curDelta FLOAT8 := 0; \n" +
		"vlits INT[];\n" +
		"var INT;\n" +

		"BEGIN\n";
		lines.add(sql);
		
		// initMRF
		String initMRF = 
			// random truth assignment, reset aux data
			"FOR i IN 1.." + numVars + " LOOP\n" +
			"truth[i] := (CASE WHEN random() < 0.5 THEN '1' ELSE '0' END);\n" +
			"END LOOP;\n" +
			"cost := 0;\n" +
			"delta := " + vecZero + ";\n" +
			"violate := " + vecZero + ";\n" +
			// recalc cost and aux data
			"FOR rec IN SELECT * FROM " + relClauses + " LOOP\n" +
			"nsat := count_nsat(rec.lits, truth);\n" +
			"weight := rec.weight;\n" +
			"IF (sign(weight)=1 AND nsat=0) OR (sign(weight)=-1 AND nsat>0) THEN\n" +
			"cost := cost + abs(weight);\n" +
			"END IF;\n" +
			"IF nsat = 0 THEN\n" + // unsat clause
			"FOR i IN 1 .. array_upper(rec.lits,1) LOOP\n" +
			"var := abs(rec.lits[i]);\n" +
			"delta[var] := delta[var] - weight;\n" +
			"IF weight < 0 THEN\n" +
			"violate[var] := violate[var] - weight;\n" +
			"END IF;\n" +
			"END LOOP;\n" +
			"ELSIF nsat = 1 THEN\n" + // 'singly' sat clause
			"FOR i IN 1 .. array_upper(rec.lits,1) LOOP\n" +
			"var := rec.lits[i];\n" +
			"IF (var>0 AND truth[var]) OR (var<0 AND NOT truth[-var]) THEN\n" +
			"var = abs(var);\n" +
			"delta[var] := delta[var] + rec.weight;\n" +
			"IF weight > 0 THEN\n" +
			"violate[var] := violate[var] + weight;\n" +
			"END IF;\n" +
			"EXIT;\n" + // = break
			"END IF;\n" + // the only sat lit
			"END LOOP;\n" + // all lits
			"END IF;\n" + // branching on nsat
			"END LOOP;\n"; // records

		String clausePicking = 
			"tallyViolation := 0;\n" +
			"pickedCID := 0;\n" +
			"FOR rec IN SELECT * FROM " + relClauses + " LOOP\n" +
			"nsat := count_nsat(rec.lits, truth);\n" +
			"weight := rec.weight;\n" +
			"IF (sign(weight)=1 AND nsat=0) OR (sign(weight)=-1 AND nsat>0) THEN\n" +
			"tallyViolation := tallyViolation + 1;\n" +
			"IF random() < 1.0/tallyViolation THEN\n" +
			"pickedCID := rec.cid;\n" +
			"END IF;\n" +
			"END IF;\n" +
			"END LOOP;\n" +
			"repickClause := FALSE;";
		
		String selCandAtoms =
			"vlits := '{}'::int[];\n" +
			"FOR i IN 1 .. array_upper(pickedClause.lits,1) LOOP\n" +
			"var := pickedClause.lits[i];\n" +
			"IF pickedClause.weight > 0 OR (var>0 AND truth[var]) " +
			"OR (var<0 AND NOT truth[-var]) THEN\n" +
			"vlits := vlits + abs(var);\n" +
			"END IF;\n" +
			"END LOOP;\n";
		
		String pickRandom =  selCandAtoms +
			"pickedAtom := vlits[randomIndex(array_upper(vlits,1))];";
		
		String pickBest = selCandAtoms +
			"pickedAtom := vlits[1];\n" +
			"lowDelta := delta[pickedAtom]; rivals := 1;\n" +
			"FOR i IN 2 .. array_upper(vlits,1) LOOP\n" +
			"cur := vlits[i];\n" +
			"curDelta := delta[cur];\n" +
			"IF curDelta < lowDelta THEN " +
			"pickedAtom := cur; lowDelta = curDelta; rivals := 1;\n" +
			"ELSIF curDelta = lowDelta THEN " +
			"rivals := rivals + 1; " +
			"IF random() < 1.0/rivals THEN pickedAtom := cur; END IF; " +
			"END IF;" +
			"END LOOP;\n";
		
		sql = "FOR itry IN 1 .. nTries LOOP\n" + // main loop of tries
		initMRF +
		"IF cost < 0.0001 THEN " +
		"lowTruth := truth;" +
		"lowCost := cost; "+
		"timeNow := extract(EPOCH FROM " +
		"clock_timestamp()-transaction_timestamp());\n" +
		"insert into " + relLog + "(xtime, xcost) " +
		"values(999999, inferOps::float/timeNow);\n" +
		" RETURN cost; " +
		"END IF;\n" +
		"repickClause := TRUE; pickedCID := 0;\n" +
		"FOR istep IN 1 .. nSteps LOOP\n" + // main loop of steps
		"inferOps := inferOps + 1;\n" +
		
		// check timeout
		"timeNow := timeBegin + extract(EPOCH FROM " +
		"clock_timestamp()-transaction_timestamp());\n" +
		"IF timeNow > " + Config.timeout + " THEN \n" +
		"insert into " + relLog + "(xtime, xcost) " +
		"values(timeNow, lowCost::float);\n" +
		"timeNow := extract(EPOCH FROM " +
		"clock_timestamp()-transaction_timestamp());\n" +
		"insert into " + relLog + "(xtime, xcost) " +
		"values(999999, inferOps::float/timeNow);\n" +
		"RETURN lowCost; \n" +
		"END IF; \n" +
		
		
		"IF istep > 1 AND pickedCID = 0 THEN\n" + // check terminal condition
		"lowTruth := truth; " +
		"lowCost := cost; " +
		"timeNow := extract(EPOCH FROM " +
		"clock_timestamp()-transaction_timestamp());\n" +
		"insert into " + relLog + "(xtime, xcost) " +
		"values(999999, inferOps::float/timeNow);\n" +
		"RETURN cost; " +
		"END IF;\n" +
		"IF repickClause THEN \n" + clausePicking + " END IF;\n" + // repick clause
		
		"pickedAtom := 0;\n" +
		"SELECT * INTO pickedClause FROM " + relClauses + 
		" WHERE cid = pickedCID;\n" + 
		"IF random() < " + Config.walksat_random_step_probability + " THEN\n" +
		pickRandom +
		"ELSE\n" +
		pickBest +
		"END IF;\n" + // random v. greedy
		"IF violate[pickedAtom] > " + Config.hard_threshold + " THEN " + // hard constraint watch
		"repickClause := TRUE; CONTINUE; " +
		"END IF; " +
		
		"cost := cost + delta[pickedAtom]; " +
		"truth[pickedAtom] := NOT truth[pickedAtom]; " +
		"violate[pickedAtom] := violate[pickedAtom] - delta[pickedAtom]; " +
		"delta[pickedAtom] := - delta[pickedAtom]; " +
		
		// check quality progress
		"IF cost < lowCost THEN " +
		"lowTruth := truth; " +
		"lowCost := cost; " +
		"timeNow := timeBegin + extract(EPOCH FROM " +
		"clock_timestamp()-transaction_timestamp());" +
		"insert into " + relLog + "(xtime, xcost) " +
		"values(timeNow, lowCost::float);\n" +
		"END IF; " +
		
		// update aux data and sample for next
		"tallyViolation := 0;\n" +
		"pickedCID := 0;\n" +
		"FOR rec IN SELECT * FROM " + relClauses + " LOOP\n" +
		"nsat := count_nsat(rec.lits, truth);\n" +
		"weight := rec.weight;\n" +
		
		"IF (sign(weight)=1 AND nsat=0) OR (sign(weight)=-1 AND nsat>0) THEN\n" +
		"tallyViolation := tallyViolation + 1;\n" +
		"IF random() < 1.0/tallyViolation THEN\n" +
		"pickedCID := rec.cid;\n" +
		"END IF;\n" +
		"END IF;\n" +
		
		"contact := 0; " +
		"IF rec.lits @> array[pickedAtom] THEN contact := 1; " +
		"ELSIF rec.lits @> array[-pickedAtom] THEN contact := -1; " +
		"END IF;" +
		"IF NOT truth[pickedAtom] THEN contact := -contact; END IF; " +
		"IF contact > 0 THEN " + // lit 0->1
		"IF nsat = 1 THEN " +
		"FOR i IN 1 .. array_upper(rec.lits,1) LOOP\n" +
		"var := abs(rec.lits[i]);\n" +
		"IF var = pickedAtom THEN CONTINUE; END IF; " +
		"delta[var] := delta[var] + weight;\n" +
		"IF weight < 0 THEN\n" +
		"violate[var] := violate[var] + weight;\n" +
		"END IF;\n" +
		"END LOOP;\n" +
		
		"ELSIF nsat = 2 THEN " +
		"FOR i IN 1 .. array_upper(rec.lits,1) LOOP\n" +
		"var := rec.lits[i];\n" +
		"IF (var>0 AND truth[var]) OR (var<0 AND NOT truth[-var]) THEN\n" +
		"var = abs(var);\n" +
		"IF var = pickedAtom THEN CONTINUE; END IF; " +
		"delta[var] := delta[var] - rec.weight;\n" +
		"IF weight > 0 THEN\n" +
		"violate[var] := violate[var] - weight;\n" +
		"END IF;\n" +
		"EXIT;\n" + // = break
		"END IF;\n" + // the only sat lit
		"END LOOP;\n" + // all lits
		
		"END IF; " + // branch nsat 1 or 2
		
		"ELSIF contact < 0 THEN " + // lit 1->0
		"IF nsat = 0 THEN " +
		"FOR i IN 1 .. array_upper(rec.lits,1) LOOP\n" +
		"var := abs(rec.lits[i]);\n" +
		"IF var = pickedAtom THEN CONTINUE; END IF; " +
		"delta[var] := delta[var] - weight;\n" +
		"IF weight < 0 THEN\n" +
		"violate[var] := violate[var] - weight;\n" +
		"END IF;\n" +
		"END LOOP;\n" +
		
		"ELSIF nsat = 1 THEN " +
		"FOR i IN 1 .. array_upper(rec.lits,1) LOOP\n" +
		"var := rec.lits[i];\n" +
		"IF (var>0 AND truth[var]) OR (var<0 AND NOT truth[-var]) THEN\n" +
		"var = abs(var);\n" +
		"IF var = pickedAtom THEN CONTINUE; END IF; " +
		"delta[var] := delta[var] + rec.weight;\n" +
		"IF weight > 0 THEN\n" +
		"violate[var] := violate[var] + weight;\n" +
		"END IF;\n" +
		"EXIT;\n" + // = break
		"END IF;\n" + // the only sat lit
		"END LOOP;\n" + // all lits
		
		"END IF; " + // branch nsat 0 or 1
		"END IF;" + // contact type branch

		"END LOOP;\n" + // loop over clause table
		"END LOOP;\n" + // loop of steps
		"END LOOP;\n"; // loop of tries
		lines.add(sql);
		sql = "timeNow := extract(EPOCH FROM " +
		"clock_timestamp()-transaction_timestamp());\n" +
		"insert into " + relLog + "(xtime, xcost) " +
		"values(999999, inferOps::float/timeNow);\n" +
		"RETURN lowCost;";
		lines.add(sql);
		lines.add("END;\n" + SQLMan.procTail());
		db.update(StringMan.join("\n", lines));
	}
	
	private void regSweep(){
		if(Config.timeout > 75000) Config.timeout = 75000;
		String relClauses = mln.relClauses;
		regUtils();
		String sql;
		String vecZero = "'{" + StringMan.repeat("0,", numVars-1) + "0}'";

		ArrayList<String> lines = new ArrayList<String>();
		sql = "CREATE OR REPLACE FUNCTION " + procSweep +
		"(nTries INT, nSteps INT) RETURNS FLOAT8 AS $$\n";
		sql += "DECLARE\n" +
		"timeBegin REAL := " + Timer.elapsedSeconds() + ";\n" +
		"timeNow REAL := 0; \n" +
		"cost FLOAT8 := 0; \n" +
		"lowCost FLOAT8 := 1E+100; \n" +
		
		"truth BOOL[] := " + vecZero + ";\n" +
		"lowTruth BOOL[];\n" +
		"delta FLOAT8[] := " + vecZero + ";\n" +
		"violate FLOAT8[] := " + vecZero + ";\n" +

		"rec RECORD; \n" +
		"nsat SMALLINT := 0; \n" +
		"weight FLOAT8 := 0; \n" +
		"inferOps INT := 0; \n" +

		"cur INT := 0; \n" +
		"vlits INT[];\n" +
		"var INT;\n" +
		"nflips INT;\n" +
		"numGood INT;\n" +

		"BEGIN\n";
		lines.add(sql);
		
		String assignRandom = 
			// random truth assignment, reset aux data
			"FOR i IN 1.." + numVars + " LOOP\n" +
			"truth[i] := (CASE WHEN random() < 0.5 THEN '1' ELSE '0' END);\n" +
			"END LOOP;\n";
		
		String recalcCost =
			// recalc cost and aux data
			"cost := 0;\n" +
			"delta := " + vecZero + ";\n" +
			"violate := " + vecZero + ";\n" +
			"FOR rec IN SELECT * FROM " + relClauses + " LOOP\n" +
			"nsat := count_nsat(rec.lits, truth);\n" +
			"weight := rec.weight;\n" +
			"IF (sign(weight)=1 AND nsat=0) OR (sign(weight)=-1 AND nsat>0) THEN\n" +
			"cost := cost + abs(weight);\n" +
			"END IF;\n" +
			"IF nsat = 0 THEN\n" + // unsat clause
			"FOR i IN 1 .. array_upper(rec.lits,1) LOOP\n" +
			"var := abs(rec.lits[i]);\n" +
			"delta[var] := delta[var] - weight;\n" +
			"IF weight < 0 THEN\n" +
			"violate[var] := violate[var] - weight;\n" +
			"END IF;\n" +
			"END LOOP;\n" +
			"ELSIF nsat = 1 THEN\n" + // 'singly' sat clause
			"FOR i IN 1 .. array_upper(rec.lits,1) LOOP\n" +
			"var := rec.lits[i];\n" +
			"IF (var>0 AND truth[var]) OR (var<0 AND NOT truth[-var]) THEN\n" +
			"var = abs(var);\n" +
			"delta[var] := delta[var] + rec.weight;\n" +
			"IF weight > 0 THEN\n" +
			"violate[var] := violate[var] + weight;\n" +
			"END IF;\n" +
			"EXIT;\n" + // = break
			"END IF;\n" + // the only sat lit
			"END LOOP;\n" + // all lits
			"END IF;\n" + // branching on nsat
			"END LOOP;\n"; // records

		// initMRF
		String initMRF = assignRandom + recalcCost;
		
		sql = "FOR itry IN 1 .. nTries LOOP\n" + // main loop of tries
		initMRF +
		// test terminal condition
		"IF cost < 0.0001 THEN " +
		"lowTruth := truth;" +
		"lowCost := cost; "+
		"timeNow := extract(EPOCH FROM " +
		"clock_timestamp()-transaction_timestamp());\n" +
		"insert into " + relLog + "(xtime, xcost) " +
		"values(999999, inferOps::float/timeNow);\n" +
		" RETURN cost; " +
		"END IF;\n" +
		
		"nflips := 0; \n" +
		"WHILE nflips < nSteps LOOP\n" + // main loop of steps
		// check timeout
		"timeNow := timeBegin + extract(EPOCH FROM " +
		"clock_timestamp()-transaction_timestamp());\n" +
		"IF timeNow > " + Config.timeout + " THEN \n" +
		"insert into " + relLog + "(xtime, xcost) " +
		"values(timeNow, lowCost::float);\n" +
		"timeNow := extract(EPOCH FROM " +
		"clock_timestamp()-transaction_timestamp());\n" +
		"insert into " + relLog + "(xtime, xcost) " +
		"values(999999, inferOps::float/timeNow);\n" +
		"RETURN lowCost; \n" +
		"END IF; \n" +
		
		"numGood := count_negative(delta); " + 
		"IF numGood > 0 THEN \n" + // some are good
		"FOR i IN 1 .. " + numVars + " LOOP " +
		"IF delta[i] < 0 AND random() < " + Config.sweepsat_greedy_probability + " THEN " +
		"truth[i] := NOT truth[i];" +
		"inferOps := inferOps + 1;" +
		"nflips := nflips + 1;" +
		"END IF;" +
		"END LOOP; " +
		"ELSE " + // no atom is good
		"FOR i IN 1 .. " + numVars + " LOOP " +
		"IF violate[i] < " + Config.hard_threshold + " AND random() < 0.5 THEN " +
		"truth[i] := NOT truth[i];" +
		"inferOps := inferOps + 1;" +
		"nflips := nflips + 1;" +
		"END IF;" +
		"END LOOP; " +
		"END IF;\n" + // numGood branches
		
		recalcCost +
		
		"IF cost < lowCost THEN\n" + // check progress
		"lowTruth := truth; " +
		"lowCost := cost; " +
		"timeNow := timeBegin + extract(EPOCH FROM " +
		"clock_timestamp()-transaction_timestamp());" +
		"insert into " + relLog + "(xtime, xcost) " +
		"values(timeNow, lowCost::float);\n" +
		"END IF;\n" +
		
		"END LOOP;\n" + // loop of steps
		"END LOOP;\n"; // loop of tries
		lines.add(sql);
		sql = "timeNow := timeBegin + extract(EPOCH FROM " +
		"clock_timestamp()-transaction_timestamp());\n" +
		"insert into " + relLog + "(xtime, xcost) " +
		"values(timeNow, lowCost::float);\n" +
		"timeNow := extract(EPOCH FROM " +
		"clock_timestamp()-transaction_timestamp());\n" +
		"insert into " + relLog + "(xtime, xcost) " +
		"values(999999, inferOps::float/timeNow);\n" +
		"RETURN lowCost; \n";
		lines.add(sql);
		lines.add("END;\n" + SQLMan.procTail());
		db.update(StringMan.join("\n", lines));
	}
	
	private void displayTrace(){
		System.out.println("=====TRACE BEGIN=====");
		String sql = "SELECT * FROM " + relLog;
		ResultSet rs = db.query(sql);
		try {
			while(rs.next()){
				System.out.println(UIMan.decimalRound(2, rs.getFloat("xtime")) + 
						"\t" + rs.getFloat("xcost"));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		System.out.println("=====TRACE  END =====");
	}
	
	private boolean debug = false;
	
	/**
	 * Run WalkSAT inside the RDBMS.
	 * @param maxTries
	 * @param maxSteps
	 */
	public double walk(int maxTries, int maxSteps){
		regWalk();
		UIMan.println(">>> Running WalkSAT[PSQL] for " + maxTries +
				" tries, " + maxSteps + " flips/try");
		double c = db.callFunctionDouble(procWalk, maxTries + "," + maxSteps);
		System.out.println("### PG COST = " + c);
		lowCost = c;
		if(debug) displayTrace();
		return c;
	}

	/**
	 * Run SweepSAT in the RDBMS.
	 * @param maxTries
	 * @param maxSteps
	 */
	public double sweep(int maxTries, int maxSteps){
		regSweep();
		UIMan.println(">>> Running SweepSAT[PSQL] for " + maxTries +
				" tries, " + maxSteps + " flips/try");
		double c = db.callFunctionDouble(procSweep, maxTries + "," + maxSteps);
		System.out.println("### PG COST = " + c);
		lowCost = c;
		if(debug) displayTrace();
		return lowCost;
	}
}
