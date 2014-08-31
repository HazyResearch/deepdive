package tuffy.mln;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.zip.GZIPInputStream;

import tuffy.db.RDB;
import tuffy.parse.InputParser;
import tuffy.ra.ConjunctiveQuery;
import tuffy.ra.Function;
import tuffy.util.Config;
import tuffy.util.DebugMan;
import tuffy.util.ExceptionMan;
import tuffy.util.FileMan;
import tuffy.util.Timer;
import tuffy.util.UIMan;

/**
 * An MLN. Holds the symbol table.
 */
public class MarkovLogicNetwork implements Cloneable{
	private static int idGen = 0;
	private int id = 0;
	
	/**
	 * The db connection associated with this MLN.
	 */
	private RDB db = null;

	/**
	 * Database tables storing intermediate data.
	 */
	public String relClauses = "clauses";
	public String relAtoms = "atoms";
	public String relTrueAtoms = "true_atoms";
	public String relClausePart = "clause_part";
	public String relAtomPart = "atom_part";
	public String relComponents = "query_components";

	/**
	 * Parser of input.
	 */
	protected InputParser parser;

	public RDB getDB(){
		return db;
	}

	/**
	 * List of all predicates appearing in this MLN.
	 */
	private ArrayList<Predicate> listPred = new ArrayList<Predicate>();

	/**
	 * Map from string name to Predicate object.
	 */
	private Hashtable<String, Predicate> nameMapPred = 
		new Hashtable<String, Predicate>();

	private Hashtable<String, Function> nameMapFunc = 
		new Hashtable<String, Function>();

	/**
	 * Map from string name to Type object.
	 */
	private Hashtable<String, Type> nameMapType = 
		new Hashtable<String, Type>();

	public Type getTypeByName(String tname) {
		return nameMapType.get(tname);
	}
	
	/**
	 * List of clauses marked as relevant.
	 */
	private HashSet<Clause> relevantClauses = new HashSet<Clause>();
	
	private HashMap<Clause, Clause> unnormal2normal = new HashMap<Clause, Clause>();

	/**
	 * List of normalized clauses.
	 */
	public ArrayList<Clause> listClauses = new ArrayList<Clause>();

	/**
	 * List of unnormalized clauses.
	 */
	public ArrayList<Clause> unnormalizedClauses = new ArrayList<Clause>();

	/**
	 * Map from signature of clauses to Clause object.
	 * For the definition of ``signature'', see {@link Clause#normalize()}.
	 */
	private Hashtable<String, Clause> sigMap = new Hashtable<String, Clause>();

	/**
	 * Map from string name to integer constant ID.
	 */
	private HashMap<String, Integer> mapConstantID = new HashMap<String, Integer>();

	/**
	 * 
	 */
	private HashMap<Predicate, ArrayList<ConjunctiveQuery>> scopes =
		new HashMap<Predicate, ArrayList<ConjunctiveQuery>>();

	private ArrayList<ConjunctiveQuery> scopingRules = new ArrayList<ConjunctiveQuery>();
	
	private ArrayList<Predicate> clusteringPredicates = new ArrayList<Predicate>();
	
	public HashSet<ConjunctiveQuery> dedupalogRules = new HashSet<ConjunctiveQuery>();
	
	
	@SuppressWarnings("unchecked")
	public Object clone() throws CloneNotSupportedException {

	    MarkovLogicNetwork clone=(MarkovLogicNetwork) super.clone();

		clone.db = db;

		clone.parser = parser;

		clone.listPred = (ArrayList<Predicate>) listPred.clone();

		clone.nameMapPred = (Hashtable<String, Predicate>) nameMapPred.clone();

		clone.nameMapFunc = (Hashtable<String, Function>) nameMapFunc.clone();

		clone.nameMapType = (Hashtable<String, Type>) nameMapType.clone();

		clone.relevantClauses = (HashSet<Clause>) relevantClauses.clone();

		clone.listClauses = (ArrayList<Clause>) listClauses.clone();

		clone.unnormalizedClauses = (ArrayList<Clause>) unnormalizedClauses.clone();

		clone.sigMap = (Hashtable<String, Clause>) sigMap.clone();

//		clone.mapConstantID = (HashMap<String, Integer>) mapConstantID.clone();

		clone.scopes = (HashMap<Predicate, ArrayList<ConjunctiveQuery>>) scopes.clone();

		clone.scopingRules = (ArrayList<ConjunctiveQuery>) scopingRules.clone();
		
		clone.clusteringPredicates = (ArrayList<Predicate>) clusteringPredicates.clone();

	    return clone;

	  }
	
	
	public ArrayList<ConjunctiveQuery> getAllDatalogRules(){
		return datalogRules;
	}
		
	/**
	 * Returns the RDB used by this MLN.
	 */
	public RDB getRDB() {
		return db;
	}
	
	
	public int getID(){
		return id;
	}
	
	/**
	 * Constructor of MLN. {@link MarkovLogicNetwork#parser} will be 
	 * constructed here.
	 * 
	 */
	public MarkovLogicNetwork(){
		parser = new InputParser(this);
		id = (idGen++);
		String relp = "mln" + id + "_";
		relAtoms = relp + "atoms";
		relClauses = relp + "clauses";
		relClausePart = relp + "clause_part";
		relAtomPart = relp + "atom_part";
		relComponents = relp + "query_components";
	}

	/**
	 * Marks a clause as relevant. Called by KBMC.
	 * 
	 * @see tuffy.ground.KBMC#run()
	 */
	public void setClauseAsRelevant(Clause c){
		relevantClauses.add(c);
	}

	public void setAllClausesAsRelevant() {
		relevantClauses.addAll(listClauses);
	}
	
	/**
	 * Returns the set of relevant clauses.
	 */
	public HashSet<Clause> getRelevantClauses(){
		return relevantClauses;
	}

	/**
	 * Registers a new, unnormalized clause.
	 * 
	 * @param c the clause to be registered
	 */
	public void registerClause(Clause c){
		if(c == null) return;
		unnormalizedClauses.add(c);
	}

	/**
	 * Add a scoping rule
	 * @param cq
	 */
	public void registerScopingRule(ConjunctiveQuery cq){
		Predicate p = cq.head.getPred();
		ArrayList<ConjunctiveQuery> qs = scopes.get(p);
		if(qs == null){
			qs = new ArrayList<ConjunctiveQuery>();
			scopes.put(p, qs);
		}
		qs.add(cq);
		cq.setScopingRule(true);
		scopingRules.add(cq);
	}

	ArrayList<ConjunctiveQuery> datalogRules = new ArrayList<ConjunctiveQuery>();

	ArrayList<ConjunctiveQuery> intermediateRules = new ArrayList<ConjunctiveQuery>();

	ArrayList<ConjunctiveQuery> postprocRules = new ArrayList<ConjunctiveQuery>();

	/**
	 * Add a datalog rule
	 * @param cq
	 */
	public void registerDatalogRule(ConjunctiveQuery cq){
		datalogRules.add(cq);
	}

	public void registerPostprocRule(ConjunctiveQuery cq){
		postprocRules.add(cq);
	}

	public void registerIntermediateRule(ConjunctiveQuery cq){
		intermediateRules.add(cq);
	}


	/**
	 * Test whether a predicate is scoped
	 */
	public boolean isScoped(Predicate p){
		return scopes.containsKey(p);
	}

	/**
	 * Execute all Datalog rules
	 */
	public void executeAllDatalogRules(){
		if(datalogRules.isEmpty()) return;

		long total = 0;
		UIMan.println(">>> Executing Datalog rules...");
		for(ConjunctiveQuery cq : datalogRules){
			Predicate p = cq.head.getPred();
			UIMan.verbose(1, cq.toString());
			Timer.start("datalogq");
			cq.buildIndexes(db, null, null, null, false, new ArrayList<String>());
			cq.materialize(db, true, new ArrayList<String>());
			int ni = db.getLastUpdateRowCount();
			total += ni;
			UIMan.verbose(1, "### inserted " + UIMan.comma(ni) + 
					(ni!=1 ? " new tuples" : " new tuple"));
			UIMan.verbose(1, "### current cardinality of '" + 
					p.getName() + "' = " + 
					UIMan.comma(db.countTuples(p.getRelName())));
			String tm = Timer.elapsed("datalogq");
			UIMan.verbose(1, "### took time " + tm +
					"\n");
			db.analyze(p.getRelName());
		}
	}


	/**
	 * Execute all Postprocessing rules
	 */
	public void executeAllPostprocRules(){
		UIMan.println(">>> Executing Postprocessing rules...");
		for(ConjunctiveQuery cq : postprocRules){
			UIMan.verbose(1, cq.toString());
			String sql = "DELETE FROM " + cq.head.getPred().getRelName();
			db.update(sql);
			db.vacuum(cq.head.getPred().getRelName());
			cq.materialize(db, true, new ArrayList<String>());
			int ni = db.getLastUpdateRowCount();
			UIMan.verbose(1, "### inserted " + ni + (ni!=1 ? " new tuples" : " new tuple") + "\n");
		}
	}

	public void executeAllIntermediateRules(){
		UIMan.println(">>> Executing intermediate rules...");
		for(ConjunctiveQuery cq : intermediateRules){
			UIMan.verbose(1, cq.toString());
			String sql = "DELETE FROM " + cq.head.getPred().getRelName();
			db.update(sql);
			db.vacuum(cq.head.getPred().getRelName());
			cq.materialize(db, true, new ArrayList<String>());
			int ni = db.getLastUpdateRowCount();
			UIMan.verbose(1, "### inserted " + ni + (ni!=1 ? " new tuples" : " new tuple") + "\n");
			Predicate p = cq.head.getPred();
			db.analyze(p.getRelName());
		}
	}


	/**
	 * Execute the scoping rules for a predicate
	 * @param p the target predicate
	 */
	private void applyScopeForPred(Predicate p){
		for(ConjunctiveQuery cq : scopes.get(p)){
			UIMan.verbose(1, cq.toString());
			cq.materialize(db, p.isClosedWorld() ? false : null, new ArrayList<String>());
			int ni = db.getLastUpdateRowCount();
			UIMan.verbose(1, "### Inserted " + UIMan.comma(ni) + (ni!=1 ? " new tuples" : " new tuple") + "\n");
			db.analyze(p.getRelName());
		}
	}
	

	/**
	 * Execute all scoping rules
	 * 
	 * @return true iff there is at least one scoping rule
	 */
	public boolean applyAllScopes(){
		if(scopes.isEmpty()) return false;
		UIMan.println(">>> Applying scoping rules...");
		ArrayList<Predicate> up = new ArrayList<Predicate>();
		for(Predicate p : scopes.keySet()){
			if(p.isClosedWorld() && !p.isCompletelySepcified()){
				applyScopeForPred(p);
			}else{
				up.add(p);
			}
		}
		for(Predicate p : up){
			if(!p.isCompletelySepcified()){
				applyScopeForPred(p);
			}
		}
		return true;
	}

	/**
	 * Get the clause object by integer ID. Accepts negative id, and will
	 * translate it into positive. Does not accept zero id or id larger
	 * than the number of clauses, and will return null.
	 * @param id ID of wanted clause.
	 */
	public Clause getClauseById(int id){
		if(id < 0) id = -id;
		if(id < 1 || id > listClauses.size()) return null;
		return listClauses.get(id-1);
	}

	/**
	 * Normalize all clauses. If the signature of this clause is
	 * as the same as some some existing clauses in {@link MarkovLogicNetwork#listClauses},
	 * then {@link Clause#absorb(Clause)} this new clause. If not
	 * absorbed, this new clause is set an ID sequentially and a name
	 * Clause$id. Predicates in this clause is registered 
	 * by {@link Predicate#addRelatedClause(Clause)}.
	 * 
	 * @see Clause#normalize()
	 * @see Clause#absorb(Clause)
	 */
	public void normalizeClauses(){
		listClauses.clear();
		for(Clause c : unnormalizedClauses){
			
			// applyScopes(c);
			if(c.hasEmbeddedWeight()){
				listClauses.add(c);
				int id = listClauses.size();
				c.setId(id);
				c.setName("Clause" + id);
				for(Predicate p : c.getReferencedPredicates()){
					p.addRelatedClause(c);
				}
				this.unnormal2normal.put(c, c);
				continue;
			}
			Clause tmpc = c;
			c = c.normalize();
			
			this.unnormal2normal.put(tmpc, c);
			
			if(c == null) continue;
			c.checkVariableSafety();
			Clause ec = sigMap.get(c.getSignature());
			if(ec == null){
				listClauses.add(c);
				sigMap.put(c.getSignature(), c);
				int id = listClauses.size();
				c.setId(id);
				c.setName("Clause" + id);
				for(Predicate p : c.getReferencedPredicates()){
					p.addRelatedClause(c);
				}
			}else{
				ec.absorb(c);
			}
		}
		for(Clause c : listClauses){
			UIMan.verbose(2, "\n" + c.toString());
		}
	}

	/**
	 * Finalize the definitions of all clauses, i.e., prepare
	 * the database table used by each clause, including
	 * 1) instance table for each clause; 2) SQL needed to
	 * ground this clause.
	 * Call this when all clauses have been parsed.
	 */
	public void finalizeClauseDefinitions(RDB adb){
		for(Clause c : listClauses){
			c.prepareForDB(adb);
		}
	}

	/**
	 * Return the type of a given name; create if this type does not exist.
	 */
	public Type getOrCreateTypeByName(String name){
		Type t= nameMapType.get(name);
		if(t == null){
			t = new Type(name);
			nameMapType.put(name, t);
		}
		return t;
	}


	/**
	 * Call materialize() for all types. This will put
	 * the domain members of each type into corresponding
	 * database tables.
	 * 
	 * @see Type#storeConstantList(RDB)
	 */
	private void materializeAllTypes(RDB adb){
		for(Type t : nameMapType.values()){
			t.storeConstantList(adb);
		}
	}



	/**
	 * Return the set of all predicates.
	 */
	public HashSet<Predicate> getAllPred() {
		return new HashSet<Predicate>(listPred);
	}

	public ArrayList<Predicate> getAllPredOrderByName(){
		ArrayList<String> pnames = new ArrayList<String>();
		for(Predicate p : listPred){
			pnames.add(p.getName());
		}
		Collections.sort(pnames);
		ArrayList<Predicate> ps = new ArrayList<Predicate>();
		for(String pn : pnames){
			ps.add(this.getPredByName(pn));
		}
		return ps;
	}



	/**
	 * Register a new predicate. Here by ``register''
	 * it means 1) set ID for this predicate sequentially; 2) push
	 * it into {@link MarkovLogicNetwork#listPred}; 3)
	 * building the map from predicate name to this predicate.
	 */
	public void registerPred(Predicate p){
		if(nameMapPred.containsKey(p.getName())){
			ExceptionMan.die("Duplicate predicate definitions - " + p.getName());
		}
		if(Predicate.isBuiltInPredName(p.getName())){
			System.err.println("WARNING: user-defined predicate '" +
					p.getName() + "' will be overridden by the built-in one!");
			return;
		}
		p.setMLN(this);
		p.setID(listPred.size());
		listPred.add(p);
		nameMapPred.put(p.getName(), p);
	}

	/**
	 * Return the predicate of the given name; null if such predicate does not exist.
	 */
	public Predicate getPredByName(String name) {
		Predicate bip = Predicate.getBuiltInPredByName(name);
		if(bip != null){
			return bip;
		}
		return nameMapPred.get(name);
	}


	/**
	 * Get a function by its name; can be built-in.
	 * @param name
	 * 
	 */
	public Function getFunctionByName(String name) {
		Function f = Function.getBuiltInFunctionByName(name);
		if(f != null){
			return f;
		}
		return nameMapFunc.get(name);
	}


	/**
	 * Return all unnormalized clauses as read from the input file.
	 */
	public ArrayList<Clause> getAllUnnormalizedClauses(){
		return unnormalizedClauses;
	}


	/**
	 * Return all normalized clauses.
	 */
	public ArrayList<Clause> getAllNormalizedClauses(){
		return listClauses;
	}

	public HashSet<String> additionalHardClauseInstances = new HashSet<String>();
	
	/**
	 * Return assigned ID of a constant symbol.
	 * If this symbol is a new one, a new ID will be assigned to it,
	 * and the symbol table will be updated.
	 */
	public Integer getSymbolID(String symbol, Type type) {
		Integer id = mapConstantID.get(symbol);
		if(id == null) {
			id = mapConstantID.size() + 1;
			mapConstantID.put(symbol, id);
			if(Config.learning_mode){
				Clause.mappingFromID2Const.put(id, symbol);
			}
		}
		if(type != null && !type.isNonSymbolicType()) {
			type.addConstant(id);
		}
		return id;
	}

	/**
	 * Ground and store all query atoms.
	 * 
	 * @see Predicate#addQuery(Atom)
	 * @see Predicate#groundAndStoreAtom(Atom)
	 */
	public void storeAllQueries(){
		for(Predicate p : getAllPred()){
			if(p.isClosedWorld()){
				continue;
			}
			p.storeQueries();
		}
	}

	/**
	 * Store all evidences into the database by flushing the "buffers".
	 * These tuples are pushed into the relational table {@link Predicate#getRelName()}
	 * in the database.
	 */
	public void storeAllEvidence(){
		for(Predicate p : getAllPred()){
			p.flushEvidence();
		}
	}

	/**
	 * Close all file handles used by each predicate in {@link MarkovLogicNetwork#listPred}.
	 */
	public void closeFiles(){
		for(Predicate p : listPred){
			p.closeFiles();
		}
	}

	/**
	 * Parse multiple MLN program files.
	 * 
	 * @param progFiles list of MLN program files (in Alchemy format)
	 */
	public void loadPrograms(String[] progFiles){
		for(String f : progFiles){
			String g = FileMan.getGZIPVariant(f);
			if(g == null){
				ExceptionMan.die("non-existent file: " + f);
			}else{
				f = g;
			}
			UIMan.println(">>> Parsing program file: " + f);
			parser.parseProgramFile(f);
		}
		normalizeClauses();
	}
	
	public void loadProgramsButNotNormalizeClauses(String[] progFiles){
		for(String f : progFiles){
			String g = FileMan.getGZIPVariant(f);
			if(g == null){
				ExceptionMan.die("non-existent file: " + f);
			}else{
				f = g;
			}
			UIMan.println(">>> Parsing program file: " + f);
			parser.parseProgramFile(f);
		}
	}

	/**
	 * Parse multiple MLN evidence files. If file size is larger
	 * than 1MB, then uses a file stream
	 * incrementally parse this file. Can also accept .gz file (see {@link GZIPInputStream#GZIPInputStream(InputStream)}).
	 * 
	 * @param evidFiles list of MLN evidence files (in Alchemy format)
	 */
	public void loadEvidences(String[] evidFiles){
		int chunkSize = Config.evidence_file_chunk_size;
		for(String f : evidFiles){
			String g = FileMan.getGZIPVariant(f);
			if(g == null){
				ExceptionMan.die("File does not exist: " + f);
			}else{
				f = g;
			}
			UIMan.println(">>> Parsing evidence file: " + f);
			
			if(FileMan.getFileSize(f) <= chunkSize){
				parser.parseEvidenceFile(f);
			}else{
				try{
					long lineOffset = 0, lastChunkLines = 0;
					BufferedReader reader = FileMan.getBufferedReaderMaybeGZ(f);
					StringBuilder sb = new StringBuilder();
					String line = reader.readLine();
					while(line != null){
						sb.append(line);
						sb.append("\n");
						lastChunkLines ++;
						if(sb.length() >= chunkSize){
							parser.parseEvidenceString(sb.toString(), lineOffset);
							sb.delete(0, sb.length());
							sb = new StringBuilder();
							lineOffset += lastChunkLines;
							lastChunkLines = 0;
							UIMan.print(".");
						}
						line = reader.readLine();
					}
					reader.close();
					if(sb.length() > 0){
						parser.parseEvidenceString(sb.toString(), lineOffset);
					}
					UIMan.println();
				}catch(Exception e){
					ExceptionMan.handle(e);
				}
			}
			try {
				DebugMan.runGC();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * Parse multiple MLN query files.
	 * 
	 * @param queryFiles list of MLN query files (in Alchemy format)
	 */
	public void loadQueries(String[] queryFiles){
		for(String f : queryFiles){
			String g = FileMan.getGZIPVariant(f);
			if(g == null){
				ExceptionMan.die("non-existent file: " + f);
			}else{
				f = g;
			}
			UIMan.println(">>> Parsing query file: " + f);
			parser.parseQueryFile(f);
		}
	}


	/**
	 * Read in the query atoms provided by the command line.
	 */
	public void parseQueryCommaList(String queryAtoms){
		parser.parseQueryCommaList(queryAtoms);
	}

	/**
	 * Prepare the database for each predicate and clause.
	 * @see Predicate#prepareDB(RDB)
	 * @see MarkovLogicNetwork#finalizeClauseDefinitions(RDB)
	 */
	public void prepareDB(RDB adb){
		db = adb;
		for(Predicate p : listPred){
			p.prepareDB(adb);
		}
		finalizeClauseDefinitions(adb);
	}
	
	public void setDB(RDB adb){
		this.db = adb;
	}

	/**
	 * Clean up temporary data in DB and working dir, including
	 * 1) drop schema in PostgreSQL; 2) remove directory.
	 * 
	 * @return true on success
	 */
	public boolean cleanUp(){
		closeFiles();
		return db.dropSchema(Config.db_schema) &&
		FileMan.removeDirectory(new File(Config.getWorkingDir()));
	}

	/**
	 * Stores constants and evidence into database table.
	 * 
	 * @see MarkovLogicNetwork#materializeAllTypes(RDB)
	 * @see MarkovLogicNetwork#storeAllEvidence()
	 */
	public void materializeTables(){
		UIMan.verbose(1, ">>> Storing symbol tables...");
		UIMan.verbose(1, "    constants = " + mapConstantID.size());
		db.createConstantTable(mapConstantID, Config.relConstants);
		mapConstantID = null;
		try {
			DebugMan.runGC();
			DebugMan.runGC();
			DebugMan.runGC();
		} catch (Exception e) {
			e.printStackTrace();
		}
		materializeAllTypes(db);
		UIMan.println(">>> Storing evidence...");
		storeAllEvidence();
	}

}
