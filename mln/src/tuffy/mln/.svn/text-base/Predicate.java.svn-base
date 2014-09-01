package tuffy.mln;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.postgresql.PGConnection;

import tuffy.db.RDB;
import tuffy.db.SQLMan;
import tuffy.util.Config;
import tuffy.util.ExceptionMan;
import tuffy.util.FileMan;
import tuffy.util.StringMan;
import tuffy.util.UIMan;

/**
 * Predicate in First Order Logic.
 */
public class Predicate {

	private boolean isCompletelySpecified = false;
	
	public boolean isCurrentlyView = false;

	public boolean avoidGrounding = false;
	
	/**
	 * e.g., %model(feature, rel)
	 */
	public boolean isFuncPredicate = false;
	
	/**
	 * e.g., ~el(mention. entity)
	 */
	public boolean isParitialPredicate = false;
	
	
	public void setCompeletelySpecified(boolean t) {
		isCompletelySpecified = t;
	}

	public boolean isCompletelySepcified() {
		return isCompletelySpecified;
	}

	private ArrayList<Integer> dependentAttributes = new ArrayList<Integer>();

	/**
	 * Set the attribute at position i to be dependent. Non-dependent attributes
	 * form a possible-world key.
	 * 
	 * @param i
	 */
	public void addDependentAttrPosition(int i) {
		if (!dependentAttributes.contains(i)) {
			dependentAttributes.add(i);
		}
	}

	public ArrayList<Integer> getDependentAttrPositions() {
		return dependentAttributes;
	}

	public ArrayList<Integer> getKeyAttrPositions() {
		ArrayList<Integer> kpos = new ArrayList<Integer>();
		for (int i = 0; i < args.size(); i++) {
			if (!dependentAttributes.contains(i)) {
				kpos.add(i);
			}
		}
		return kpos;
	}
	
	public ArrayList<Integer> getLabelAttrPositions() {
		ArrayList<Integer> kpos = new ArrayList<Integer>();
		for (int i = 0; i < args.size(); i++) {
			if (dependentAttributes.contains(i)) {
				kpos.add(i);
			}
		}
		return kpos;
	}

	public boolean hasDependentAttributes() {
		return !dependentAttributes.isEmpty();
	}

	/**
	 * Get attributes whose value depend on other attributes in any possible
	 * world.
	 * 
	 * @see #getKeyAttrs
	 */
	public ArrayList<String> getDependentAttrs() {
		ArrayList<String> dargs = new ArrayList<String>();
		for (int i : dependentAttributes) {
			dargs.add(args.get(i));
		}
		return dargs;
	}

	/**
	 * Get attributes that form a possible world key.
	 * 
	 * @see #getDependentAttrs
	 */
	public ArrayList<String> getKeyAttrs() {
		ArrayList<String> dargs = new ArrayList<String>();
		for (int i = 0; i < args.size(); i++) {
			if (!dependentAttributes.contains(i)) {
				dargs.add(args.get(i));
			}
		}
		return dargs;
	}

	/**
	 * Map from name to built-in predicates, e.g., same.
	 */
	private static HashMap<String, Predicate> builtInMap = new HashMap<String, Predicate>();

	/**
	 * Return true if the argument is the name of a built-in predicate.
	 * 
	 * @param s
	 *            name of queried predicate
	 * @return true if s is a built-in predicate.
	 */
	public static boolean isBuiltInPredName(String s) {
		return builtInMap.containsKey(s.toLowerCase());
	}

	/**
	 * Return the predicate object with the name as the argument string.
	 * 
	 * @param s
	 *            name of queried predicate
	 * @return the predicate object with name s.
	 */
	public static Predicate getBuiltInPredByName(String s) {
		return builtInMap.get(s.toLowerCase());
	}

	// logic related fields
	/**
	 * Name of this predicate.
	 */
	private String name;

	/**
	 * Whether this predicate obeys closed-world assumption.
	 */
	private boolean closedWorld = false;

	private boolean hasSoftEvidence = false;


	public boolean isImmutable() {
		return closedWorld && !hasSoftEvidence;
	}


	/**
	 * List of argument types of this predicate.
	 */
	private ArrayList<Type> types = new ArrayList<Type>();

	/**
	 * TODO: if unsat then {if scope then do scope, else do cross product}
	 */
	private boolean safeRefOnly = true;

	/**
	 * Whether this predicate is a built-in predicate.
	 */
	private boolean isBuiltIn = false;

	// DB related fields
	/**
	 * DB object associated with this predicate.
	 */
	private RDB db = null;

	/**
	 * Name of the table of this predicate in DB.
	 */
	private String relName = null;

	/**
	 * The list of arguments of this predicate. The K-th argument is named
	 * "TypeK" by default, where "Type" if the type name of this argument,
	 * unless explicitly named.
	 */
	private ArrayList<String> args = new ArrayList<String>();

	/**
	 * The assigned ID for this predicate in its parent MLN
	 * {@link Predicate#mln}.
	 */
	private int id = -1;

	/**
	 * The file object used to load evidence to DB
	 */
	protected File loadingFile = null;

	/**
	 * The buffer writer object used to flush evidence to file
	 */
	protected BufferedWriter loadingFileWriter = null;

	// MLN related fields
	/**
	 * The parent MLN containing this predicate.
	 */
	private MarkovLogicNetwork mln;

	/**
	 * Set of clauses referencing this predicate.
	 */
	private HashSet<Clause> iclauses = new HashSet<Clause>();

	/**
	 * Set of queries referencing this predicate.
	 */
	private ArrayList<Atom> queries = new ArrayList<Atom>();

	/**
	 * Whether all unknown atoms of this predicate are queries.
	 */
	private boolean isAllQuery = false;

	/**
	 * Return the name of relational table containing the ID of active atoms
	 * associated with this predicate.
	 */
	public String getRelAct() {
		return "act_" + relName;
	}

	/**
	 * Specify that all atoms of this predicate are queries.
	 */
	public void setAllQuery() {
		if (isAllQuery)
			return;
		isAllQuery = true;
		queries.clear();
		ArrayList<Integer> list = new ArrayList<Integer>();
		for (int i = 1; i <= arity(); i++) {
			list.add(-i);
		}
		Tuple t = new Tuple(list);
		Atom a = new Atom(this, t);
		a.type = Atom.AtomType.QUERY;
		queries.add(a);
	}

	/**
	 * Specify whether this predicate obeys the closed world assumption.
	 */
	public void setClosedWorld(boolean t) {
		closedWorld = t;
	}

	/**
	 * Return the assigned ID of this predicate in its parent MLN.
	 */
	public int getID() {
		return id;
	}

	/**
	 * Return argument names of this predicate. The K-th argument is named
	 * "TypeK", where "Type" if the type name of this argument.
	 */
	public ArrayList<String> getArgs() {
		return args;
	}

	/**
	 * Check if we need to ground this predicate on top of its evidence. A
	 * predicate needs not to ground if 1) it only appears in negative literal
	 * and 2) it follows closed world assumption.
	 * 
	 * @return true if the closed world assumption is made on this predicate,
	 *         and all literals of this predicate are negative
	 */
	public boolean noNeedToGround() {
		return (safeRefOnly && closedWorld) || isCompletelySpecified;
	}

	/**
	 * Assign an ID for this predicate. This predicate ID is used to encode
	 * tuple IDs of this predicate.
	 */
	public boolean setID(int aid) {
		if (id == -1) {
			id = aid;
			return true;
		}
		return false;
	}

	/**
	 * Return query atoms of this predicate. Used by KBMC.
	 */
	public ArrayList<Atom> getQueryAtoms() {
		return queries;
	}

	/**
	 * Return clauses referencing this predicate.
	 */
	public HashSet<Clause> getRelatedClauses() {
		return iclauses;
	}

	/**
	 * Register a query atom.
	 * 
	 * @param q
	 *            the query atom; could contain variables
	 * @see Predicate#storeQueries()
	 */
	public void addQuery(Atom q) {
		if (isAllQuery)
			return;
		queries.add(q);
	}

	/**
	 * Ground query atoms and store the result in the database.
	 */
	public void storeQueries() {
		for (Atom a : queries) {
			UIMan.verbose(2, ">>> Storing query " + a);
			groundAndStoreAtom(a);
		}
	}
	
	public boolean hasEvid = false;

	public boolean scoped = false;
	
	/**
	 * Ground an atom and store the result in the database. Repetitive
	 * invocations of this method could be expensive, since it involves both
	 * updates and inserts to the predicate table.
	 * 
	 * First, for the grounded tuples satisfying this atom $a$ and already
	 * existing in database, it only update its club values. If this $a$ is
	 * query, then add query to club (0->1, 2->3). If this $a$ is evidence, then
	 * add evidence to club (0->2, 1->3).
	 * 
	 * Then, for the grounded tuples satisfying this atom $a$ and not existing
	 * in database, it 1) select them with a $arity-way join in corresponding
	 * type instance table; 2) left join them with current version database; 3)
	 * select those not matching with any existing tuples; and 4) insert into
	 * the database.
	 * 
	 */
	public void groundAndStoreAtom(Atom a) {
		/*
		if (mln.getPredByName("mentionPhonemap") != null &&
				a.pred.getName().equals("pcluster") &&
				Config.product_name.contains("Felix") &&
				Config.warmTuffy){
			UIMan.println("####### Not grounding " + name);
			return;
		}
		*/
		if (avoidGrounding) {
			UIMan.verbose(2, "    Not grounding " + name);
			return;
		}
		// input must be query or KBMC result
		assert (a.club() >= 0 && a.club() <= 2);
		// update. a is query/evidence
		if (a.club() == 1 || a.club() == 2) {
			String sql = "";
			ArrayList<String> conds = new ArrayList<String>();
			if (a.club() == 1) { // query
				sql = "UPDATE " + getRelName() + " SET "
				+ " club = " + // not change the truthvalue
				"club + 1 " + " WHERE ";
				conds.add("(club = 0 OR club = 2)"); // turn none->query,
				// evidence->query evidence
			} else if (a.club() == 2) { // evidence
				sql = "UPDATE " + getRelName() + " SET " + "truth = '"
				+ (a.truth ? 1 : 0) + "', club = " + // override the
				// truth values.
				"club + 2 " + " WHERE ";
				conds.add("(club = 0 OR club = 1)"); // turn none->evidence,
				// query->query evidence
			}
			int[] firstOccur = new int[a.args.dimension + 1];
			for (int i = 0; i < args.size(); i++) {
				int v = a.args.get(i); // name of i-th element
				if (v > 0) { // constant
					conds.add(args.get(i) + "=" + v); // <i-th name> = v
				} else { // variable
					if (firstOccur[-v] == 0) {
						firstOccur[-v] = i + 1;
					} else {
						int f = firstOccur[-v] - 1;
						conds.add(args.get(i) + "=" + args.get(f)); // <i-th
						// variable>
						// = <f-th
						// variable>
					}
				}
			}
			sql += SQLMan.andSelCond(conds);
			// System.out.println(sql);
			db.update(sql);
			UIMan.verbose(2, "    updated " + db.getLastUpdateRowCount() + " rows");
			// System.out.println(" updated " + DB.lastUpdateRowCount);
		}

		// insert here, queries are not grounded because it has been grounded as NONE/EVIDENCE.
		if (   ((mln != null && !mln.isScoped(this)) || (mln == null && !scoped) ) && !noNeedToGround() && a.club() != 1) {
			String sql = "INSERT INTO " + getRelName()
			+ "(truth,prior,club," + StringMan.commaList(args)
			+ ")\n";
			ArrayList<String> selInsert = new ArrayList<String>();
			ArrayList<String> selNT = new ArrayList<String>();
			ArrayList<String> condMatch = new ArrayList<String>();
			ArrayList<String> condNull = new ArrayList<String>();
			ArrayList<String> baseTables = new ArrayList<String>();

			// //////////////////////////////////
			//selInsert.add(nextTupleID());
			selInsert.add(a.truth == null ? "NULL" : "'" + (a.truth ? 1 : 0)
					+ "'");
			selInsert.add(a.prior != null ? Double.toString(a.prior) : "NULL");
			selInsert.add(Integer.toString(a.club()));

			int[] firstOccur = new int[a.args.dimension + 1];
			for (int i = 0; i < types.size(); i++) {
				Type t = types.get(i);
				int v = a.args.get(i);
				String sNT;
				if (v > 0) { // constant
					sNT = "" + v;
				} else { // variable
					if (firstOccur[-v] == 0) {
						firstOccur[-v] = i + 1;
						sNT = "t" + i + ".constantID";
						baseTables.add(t.getRelName() + " t" + i);
					} else {
						int f = firstOccur[-v] - 1;
						sNT = "t" + f + ".constantID";
					}
				}
				sNT += " AS " + args.get(i);
				selNT.add(sNT);
				selInsert.add("nt." + args.get(i));
				condMatch.add("nt." + args.get(i) + "=ot." + args.get(i));
				condNull.add("ot." + args.get(i) + " IS NULL");
			}

			sql += "SELECT " + StringMan.commaList(selInsert) + " FROM \n"
			+ "(SELECT " + StringMan.commaList(selNT)
			+ (baseTables.isEmpty() ? "" : " FROM ")
			+ StringMan.join(" CROSS JOIN ", baseTables) + ") nt \n"
			+ "LEFT JOIN\n" + getRelName() + " ot ON (\n"
			+ SQLMan.andSelCond(condMatch) + ") " + "WHERE "
			+ SQLMan.andSelCond(condNull);

			// System.out.println(sql);
			db.update(sql);
			// System.out.println(db.getLastUpdateRowCount());
		}
		// System.out.println("tttt " + name + " " + db.countTuples(relName));
	}

	/**
	 * Store an evidence in the "buffer". There is a buffer (in the form of a
	 * CSV file) for each predicate that holds the DB tuple formats of its
	 * evidence; this buffer will be flushed into the database once all evidence
	 * has been read.
	 * 
	 * @param a
	 *            the evidence; following Alchemy, it must be a ground atom
	 * @see Predicate#flushEvidence()
	 */
	public void addEvidence(Atom a) {
		
		hasEvid = true;
		
		if (a.isSoftEvidence())
			setHasSoftEvidence(true);
		addEvidenceTuple(a);
	}

	/**
	 * Determine whether this predicate can ground more atoms. This predicate
	 * does not have more to ground if the number of unknown grounds (none and
	 * query) in this predicate is smaller than the number of active atoms of
	 * this predicate. I.e., all the unknown atoms of this predicate is
	 * activated.
	 * 
	 * @return whether there are more groundings can be generated
	 */
	public boolean hasMoreToGround() {
		try {
			String sql = "SELECT COUNT(*) from " + relName + " where club < 2";
			ResultSet rs = db.query(sql);
			rs.next();
			int unknown = rs.getInt(1);
			sql = "SELECT COUNT(*) FROM " + getRelAct();
			rs = db.query(sql);
			rs.next();
			int active = rs.getInt(1);
			if (unknown <= active)
				return false;
		} catch (Exception e) {
			ExceptionMan.handle(e);
		}
		return true;
	}
	
	public void appendToWriter(String str) {
		try {
			if (loadingFileWriter == null) {
				loadingFileWriter = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(loadingFile), "UTF8"));
			}
			this.loadingFileWriter.append(str);
		} catch (IOException e) {
			ExceptionMan.handle(e);
		}
	}

	/**
	 * Add evidence tuple related to this predicate. The output of this function
	 * is written to file in format like:
	 * 
	 * $tuple_id,$truth_value,$prior,$club_value,$variable_name,......
	 * 
	 * @param a
	 *            the atom as evidence. This atom need to be a grounded atom.
	 */
	protected void addEvidenceTuple(Atom a) {
		ArrayList<String> parts = new ArrayList<String>();
		if (a.truth == null) {
			parts.add("TRUE");
			parts.add(a.prior != null ? Double.toString(a.prior) : "\\N");
		} else {
			parts.add(a.truth ? "TRUE" : "FALSE");
			parts.add("");
		}
		parts.add(Integer.toString(a.club()));
		for (String s : a.sargs) {
			parts.add(SQLMan.quoteSqlString(s));
		}

		try {
			if (loadingFileWriter == null) {
				loadingFileWriter = new BufferedWriter(new OutputStreamWriter(
						new FileOutputStream(loadingFile), "UTF8"));
			}
			loadingFileWriter.append(StringMan.join(",", parts) + "\n");
		} catch (IOException e) {
			ExceptionMan.handle(e);
		}
	}

	/**
	 * Close all file handles.
	 */
	public void closeFiles() {
		try {
			if (loadingFileWriter != null) {
				loadingFileWriter.close();
				loadingFileWriter = null;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Flush the evidence buffer to the predicate table, using the COPY
	 * statement in PostgreSQL.
	 * 
	 * @see Predicate#addEvidence(Atom)
	 */
	public void flushEvidence(boolean... specialMode) {
		try {
			
			if(this.hasEvid == true && specialMode.length>0){
				String sql = "DELETE FROM " + getRelName();
				db.execute(sql);
			}
			
			// flush the file
			if (loadingFileWriter != null) {
				loadingFileWriter.close();
				loadingFileWriter = null;
			}
			if (!loadingFile.exists()) return;
			// copy into DB
			ArrayList<String> cols = new ArrayList<String>();
			cols.add("truth");
			cols.add("prior");
			cols.add("club");
			cols.addAll(args);
			FileInputStream in = new FileInputStream(loadingFile);
			PGConnection con = (PGConnection) db.getConnection();
			String sql = "COPY " + getRelName() + 
			StringMan.commaListParen(cols) + " FROM STDIN CSV";
			con.getCopyAPI().copyIn(sql, in);
			in.close();
			db.analyze(getRelName());
			FileMan.removeFile(loadingFile.getAbsolutePath());
		} catch (Exception e) {
			ExceptionMan.handle(e);
		}
	}

	/**
	 * Constructor of Predicate.
	 * 
	 * @param mln
	 *            the parent MLN that hosts this predicate
	 * @param aname
	 *            the name; must be unique
	 * @param aClosedWorld
	 *            indicates whether to make the closed-world asssumption
	 */
	public Predicate(MarkovLogicNetwork mln, String aname, boolean aClosedWorld) {
		this.mln = mln;
		name = aname;
		closedWorld = aClosedWorld;
		relName = "pred_" + name.toLowerCase();
	}

	/**
	 * Checks if there are any queries associated with this predicate.
	 */
	public boolean hasQuery() {
		return !queries.isEmpty();
	}

	public void setDB(RDB adb){
		db = adb;
	}
	
	
	public RDB getDB(){
		return db;
	}
	
	/**
	 * Initialize database objects for this predicate.
	 */
	public void prepareDB(RDB adb) {
		db = adb;
		loadingFile = new File(Config.getLoadingDir(), "loading_" + getName());

		if (!Config.reuseTables || !this.isClosedWorld() || 
				!db.tableExists(this.getRelName()) ) {
			UIMan.verbose(1, ">>> Creating predicate table " + this.getRelName());
			createTable();
		} else if (this.isClosedWorld()) {
			UIMan.verbose(1, ">>> Reusing predicate table " + this.getRelName());
		}
	}

	public void prepareDbForceNew(RDB adb) {
		db = adb;
		loadingFile = new File(Config.getLoadingDir(), "loading_" + getName());
		createTable();
	}
	
	/**
	 * Create table for storing groundings of this predicate. club: 1 = NONE; 2
	 * = EVIDENCE: atom in evidence; 3 = QUERY: atom in query; 4 = QUEVID: atom
	 * in query as evidence.
	 */
	public void createTable() {
		db.dropTable(relName);
		db.dropView(relName);
		db.dropSequence(relName + "_id_seq");
		String sql = "CREATE TABLE " + getRelName() + "(\n";
		sql += "id bigserial,\n";
		sql += "truth BOOL,\n"; // evidence truth
		sql += "prior FLOAT DEFAULT NULL,\n"; // evidence prior
		sql += "club INT DEFAULT 0,\n";
		sql += "atomID INT DEFAULT NULL,\n";
		sql += "itruth BOOL DEFAULT NULL,\n"; // inferred truth
		sql += "prob FLOAT DEFAULT NULL,\n"; // infererred probability
		sql += "useful BOOL DEFAULT FALSE,\n";
		ArrayList<String> argDefs = new ArrayList<String>();
		for (int i = 0; i < args.size(); i++) {
			String attr = args.get(i);
			Type t = types.get(i);
			String ts = " BIGINT";
			if (Config.constants_as_raw_string) {
				ts = " TEXT";
			}
			if (t.isNonSymbolicType()) {
				// TODO: fix expression variable binding
				Type tn = t.getNonSymbolicType();
				if (tn == Type.Float) {
					ts = " FLOAT8";
				} else if (tn == Type.Integer) {
					ts = " BIGINT";
				}
			}
			argDefs.add(attr + ts);
		}
		sql += StringMan.commaList(argDefs) + ")";
		
		if(Config.using_greenplum){
			sql = sql + " DISTRIBUTED BY (" + args.get(0) + ")";
		}
		
		db.update(sql);
		
		if (Config.build_predicate_table_indexes) {
			for (String a : args) {
				sql = "CREATE INDEX idx_" + id + "_" + a + " ON " + relName
				+ "(" + a + ")";
				db.update(sql);
			}
		}
	}

	/**
	 * Return the arity of this predicate.
	 */
	public int arity() {
		return args.size();
	}

	/**
	 * Register a clause referencing this predicate
	 * 
	 * @param c
	 *            a clause referencing this predicate
	 */
	public void addRelatedClause(Clause c) {
		iclauses.add(c);
	}

	/**
	 * Append a new argument without a user-provided name.
	 * 
	 * @param t
	 *            the type of the new argument
	 */
	public void appendArgument(Type t) {
		types.add(t);
		args.add(t.name() + types.size());
		argNameList.add(null);
	}

	private HashMap<String, Integer> argNameMap = new HashMap<String, Integer>();
	private ArrayList<String> argNameList = new ArrayList<String>();

	/**
	 * Append a new argument with a user provided name.
	 * 
	 * @param t
	 *            the type of the new argument
	 * @param name
	 *            user-provided name for this argument/attribute
	 */
	public void appendArgument(Type t, String name) {
		types.add(t);
		if (name == null) {
			args.add(t.name() + types.size());
		} else {
			args.add(name);
		}
		if (argNameMap.containsKey(name)) {
			ExceptionMan.die("duplicate argument name '" + name
					+ "' in predicate '" + this.name + "'");
		} else if (name != null) {
			argNameList.add(name);
			argNameMap.put(name, args.size() - 1);
		}
	}

	/**
	 * Return the position of the given argument name.
	 * 
	 * @param aname
	 *            argument name
	 */
	public int getArgPositionByName(String aname) {
		if (!argNameMap.containsKey(aname))
			return -1;
		return argNameMap.get(aname);
	}

	/**
	 * Mark the point when all arguments have been given. Go through the
	 * arguments again to try to give unnamed arguments names.
	 */
	public void sealDefinition() {
		HashSet<Type> tset = new HashSet<Type>();
		HashSet<Type> dset = new HashSet<Type>();
		for (Type t : types) {
			if (tset.contains(t))
				dset.add(t);
			tset.add(t);
		}
		tset.removeAll(dset);
		for (int i = 0; i < argNameList.size(); i++) {
			if (argNameList.get(i) == null) {
				Type t = types.get(i);
				String tn = t.name();
				if (tset.contains(t) && !argNameMap.containsKey(tn)) {
					argNameList.set(i, tn);
					argNameMap.put(tn, i);
				}
			}
		}
	}

	/**
	 * TODO: look into the implications of FDs
	 * 
	 */
	@SuppressWarnings("unused")
	private class FunctionalDependency {
		HashSet<Integer> determinant = null;
		public int dependent = -1;
	}

	/**
	 * Functional dependencies for this predicate.
	 */
	private ArrayList<FunctionalDependency> fds = new ArrayList<FunctionalDependency>();

	public void setMLN(MarkovLogicNetwork _mln){
		mln = _mln;
	}
	
	public MarkovLogicNetwork getMLN() {
		return mln;
	}
	
	/**
	 * Add a functional dependency for the attributes of this predicate
	 * 
	 * @param determinant
	 * @param dependent
	 */
	public void addFunctionalDependency(List<String> determinant,
			String dependent) {
		HashSet<Integer> det = new HashSet<Integer>();
		for (String s : determinant) {
			int idx = argNameMap.get(s);
			if (idx < 0) {
				ExceptionMan.die("unknown attribute name '" + s
						+ "' for predicate '" + name + "' when defining "
						+ "functional dependency.");
			} else {
				det.add(idx);
			}
		}
		int dep = argNameMap.get(dependent);
		if (dep < 0) {
			ExceptionMan.die("unknown attribute name '" + dependent
					+ "' for predicate '" + name + "' when defining "
					+ "functional dependency.");
		}
		FunctionalDependency fd = new FunctionalDependency();
		fd.dependent = dep;
		fd.determinant = det;
		fds.add(fd);
	}

	/**
	 * Return the type of the k-th argument.
	 */
	public Type getTypeAt(int k) {
		return types.get(k);
	}

	/**
	 * Check if this predicate makes the closed-world assumption.
	 */
	public boolean isClosedWorld() {
		return closedWorld;
	}

	/**
	 * Return the name of this predicate.
	 */
	public String getName() {
		return name;
	}

	/**
	 * Return the relational table name of this predicate..
	 */
	public String getRelName() {
		return relName;
	}

	/**
	 * Set whether all references to this predicate are safe; i.e., all
	 * variables in corresponding positive literals are bound to other literals
	 * in the same clause.
	 * 
	 * @param safeRefOnly
	 */
	public void setSafeRefOnly(boolean safeRefOnly) {
		this.safeRefOnly = safeRefOnly;
	}

	public boolean isSafeRefOnly() {
		return safeRefOnly;
	}

	public void setHasSoftEvidence(boolean hasSoftEvidence) {
		this.hasSoftEvidence = hasSoftEvidence;
	}

	public boolean hasSoftEvidence() {
		return hasSoftEvidence;
	}

	public boolean isBuiltIn() {
		return isBuiltIn;
	}
	
	
	public String toString(){
		String ret = "";
		
		ret = this.getName();
		ret += "(";
		ret += StringMan.commaList(this.getArgs());
		ret += ")";
		
		return ret;
	}

}
