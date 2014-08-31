package tuffy.db;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.OutputStreamWriter;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.postgresql.PGConnection;

import tuffy.mln.Predicate;
import tuffy.util.Config;
import tuffy.util.ExceptionMan;
import tuffy.util.FileMan;
import tuffy.util.StringMan;
import tuffy.util.Timer;
import tuffy.util.UIMan;

/**
 * Interface with the RDBMS. Currently only supports PostgreSQL (8.4 or later).
 */
public class RDB {
	private int lastUpdateRowCount = -1;
	private boolean savedAutoCommit = false;

	public static final long constantIdBase = 1 << 29;
	
	static ArrayList<RDB> allRDBs = new ArrayList<RDB>();
	static int currentDBCounter = 0;

	public Connection con = null;

	private Statement currentlyRunningQuery = null;

	public String db;
	public String user;
	public String password;
	public String schema = null;

	public static HashSet<RDB> historyInstances = new HashSet<RDB>();

	/**
	 *  Disable auto-commit so that JDBC won't fetch all query results at once. 
	 *  Call this before retrieving data from a huge table.
	 *  After the big query is done, call {@link RDB#restoreAutoCommitState()} to
	 *  restore the initial auto-commit state.
	 *  
	 *  @see RDB#restoreAutoCommitState()
	 *  @see <a href='http://jdbc.postgresql.org/documentation/84/query.html#query-with-cursor'>
	 *  PostgreSQL's JDBC doc</a>
	 */
	public void disableAutoCommitForNow(){
		try {
			savedAutoCommit = con.getAutoCommit();
			con.setAutoCommit(false);
		} catch (SQLException e) {
			ExceptionMan.handle(e);
		}
	}

	/**
	 * Register a stored procedure to explain SQL queries.
	 * @param pname name of the stored procedure
	 */
	public void regExplainProc(String pname){
		String sql = "create or replace function " + pname +
		"(q text) returns setof text as $$\r\n" + 
		"declare r record;\r\n" + 
		"begin\r\n" + 
		"  for r in execute 'explain ' || q loop\r\n" + 
		"    return next r.\"QUERY PLAN\";\r\n" + 
		"  end loop;\r\n" + 
		"end$$ language plpgsql";

		update(sql);
	}


	public void estimateQuery(String sql, boolean analyze){
		RDB db = this;
		db.estimateCost(sql);
		UIMan.verbose(2, "ESTIMATED cost = " + db.estimatedCost + " ; rows = " + db.estimatedRows);
		if(analyze){
			Timer.start("cqmat");
			db.update(sql);
			double rtime = Timer.elapsedMilliSeconds("cqmat");
			UIMan.verbose(2, Timer.elapsed("cqmat"));
			UIMan.verbose(2, "COST-RATIO = " + (db.estimatedCost/rtime) + " ; ROW-RATIO = " + 
					((double)db.estimatedRows/db.getLastUpdateRowCount()));
		}
	}

	public double estimatedCost = 0;
	public double estimatedRows = 0;
	public String estimateCost(String sql){
		String plan = explain(sql);
		if(plan == null){
			estimatedCost = Double.MAX_VALUE;
			estimatedRows = Double.MAX_VALUE;
			return null;
		}
		String rep = plan.split("\n")[0];
		String[] parts = rep.split(" ");
		for(String p : parts){
			if(p.startsWith("(cost=")){
				int i = p.indexOf("..") + 2;
				estimatedCost = Double.parseDouble(p.substring(i));
			}else if(p.startsWith("rows=")){
				estimatedRows = Double.parseDouble(p.substring(5));
			}
		}
		return rep;
	}

	/**
	 * Explain a SQL query with an execution plan.
	 * @param sql
	 */
	public String explain(String sql){

		try {

			//this.execute("EXPLAIN " + sql);
			//this.regExplainProc("expl");

			PreparedStatement ps = getPrepareStatement(
			"SELECT * FROM expl(cast(? as text))");
			ps.setString(1, sql);
			//System.out.println(sql);
			ResultSet rs = ps.executeQuery();

			StringBuilder sb = new StringBuilder();
			while(rs.next()){
				sb.append(rs.getString(1) + "\n");
			}
			return sb.toString();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();

			// dirty

			try {
				this.con.close();
				this.con = DriverManager.getConnection(db, user, password);
				if(this.schema != null){
					this.execute("SET SEARCH_PATH TO " + schema);
				}
				//return this.explain(sql);
				return null;
			} catch (SQLException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			return null;
		}

	}


	public void createTempTableIntList(String rel, Collection<Integer> vals){
		dropTable(rel);
		String sql = "CREATE TABLE " + rel + "(id INT)";
		update(sql);
		try {
			
			String loadingFile = Config.dir_working + "/createTempTableIntList";
			
			BufferedWriter bw = new BufferedWriter(new FileWriter(
					Config.dir_working + "/createTempTableIntList"));
			
			for(int pid : vals){
				bw.write(pid + "\n");
			}
			bw.close();
		
			ArrayList<String> cols = new ArrayList<String>();
			cols.add("id");
			FileInputStream in = new FileInputStream(loadingFile);
			PGConnection con = (PGConnection) this.getConnection();
			sql = "COPY " + rel + 
			StringMan.commaListParen(cols) + " FROM STDIN CSV";
			con.getCopyAPI().copyIn(sql, in);
			in.close();
			this.analyze(rel);
			FileMan.removeFile(loadingFile);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}


	/**
	 * Restore the auto-commit state saved by {@link RDB#disableAutoCommitForNow()}.
	 * 
	 * @see RDB#disableAutoCommitForNow()
	 */
	public void restoreAutoCommitState(){
		try {
			con.setAutoCommit(savedAutoCommit);
		} catch (SQLException e) {
			ExceptionMan.handle(e);
		}
	}

	/**
	 * Return the number of affected tuples from last update.
	 */
	public int getLastUpdateRowCount() {
		return lastUpdateRowCount;
	}

	/**
	 * Return the database connection.
	 */
	public Connection getConnection(){
		return con;
	}


	/**
	 * Dump a MAP world produced by MAP inference.
	 * 
	 * @param fout path of output file
	 */
	public void dumpTableToFile(Predicate p, String fout) {
		HashMap<Long,String> cmap = this.loadIdSymbolMapFromTable();
		try {
			BufferedWriter bufferedWriter = null;
			bufferedWriter = new BufferedWriter(new OutputStreamWriter
					(new FileOutputStream(fout),"UTF8"));
			String sql = "SELECT * FROM " + p.getRelName() +
			" WHERE truth OR itruth " +
			" ORDER BY " + StringMan.commaList(p.getArgs());
			ResultSet rs = this.query(sql);
			while(rs.next()) {
				String line = p.getName() + "(";
				ArrayList<String> cs = new ArrayList<String>();
				for(String a : p.getArgs()) {
					long c = rs.getLong(a);
					cs.add("\"" + StringMan.escapeJavaString(cmap.get(c)) + "\"");
				}
				line += StringMan.commaList(cs) + ")";
				bufferedWriter.append(line + "\n");
			}
			rs.close();
			bufferedWriter.close();
		} catch (Exception e) {
			ExceptionMan.handle(e);
		}
	}



	/**
	 * Attempt to establish the connection as specified in the 
	 * (deault) configuration.
	 */
	public static RDB getRDBbyConfig() {

		//TODO: why need so large
		//int nConnections = 1;

		//TODO: change nCores to # connect
		//if(allRDBs.size() < nConnections){
		RDB tmp  = new RDB(Config.db_url,
				Config.db_username, Config.db_password);

		tmp.db = Config.db_url;
		tmp.user = Config.db_username;
		tmp.password = Config.db_password;

		historyInstances.add(tmp);


		//	allRDBs.add(tmp);
		//	currentDBCounter = allRDBs.size() - 1;
		//}else{
		//	currentDBCounter = (currentDBCounter+1) % nConnections;
		//}

		//return allRDBs.get(currentDBCounter);
		return tmp;

	}

	public static RDB getRDBbyConfig(String schema) {

		RDB tmp  = new RDB(Config.db_url,
				Config.db_username, Config.db_password);

		tmp.db = Config.db_url;
		tmp.user = Config.db_username;
		tmp.password = Config.db_password;
		tmp.schema = schema;

		tmp.execute("SET search_path = " + schema);

		historyInstances.add(tmp);

		return tmp;

	}

	/**
	 * Register the JDBC driver.
	 */
	private void registerDrivers(){
		/*
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			System.err.println(e.getMessage());
			System.err.println("Failed to load PostgreSQL JDBC driver.");
		}
		 */
	}

	private void dumpSQL(String sql){
		UIMan.println("-----BEGIN:SQL-----");
		UIMan.println(sql);
		UIMan.println("-----END:SQL-----");
	}

	/**
	 * Execute an update SQL statement.
	 * 
	 * @return the number of tuples affected
	 */
	public int update(String sql){
		if(Config.exiting_mode) ExceptionMan.die("");
		try {
			Statement stmt = con.createStatement();
			currentlyRunningQuery = stmt;
			lastUpdateRowCount = stmt.executeUpdate(sql);
			stmt.close();
			currentlyRunningQuery = null;
		} catch (SQLException e) {
			UIMan.error(sql);
			ExceptionMan.handle(e);
			return 0;
		}
		return lastUpdateRowCount;
	}

	/**
	 * Execute a SQL statement (query/update).
	 */
	public void execute(String sql) {		
		//if(sql.contains("DELETE") || sql.contains("delete")) System.out.println(sql);
		if(Config.exiting_mode) ExceptionMan.die("");
		try {
			Statement stmt = con.createStatement();
			currentlyRunningQuery = stmt;
			stmt.execute(sql);
			stmt.close();
			currentlyRunningQuery = null;
		} catch (SQLException e) {
			dumpSQL(sql);
			e.printStackTrace();

			ExceptionMan.handle(e);
		}
	}

	public void executeWhatever(String sql) {
		try {
			Statement stmt = con.createStatement();
			stmt.execute(sql);
			stmt.close();
		} catch (SQLException e) {
			dumpSQL(sql);
		}
	}

	private void executeRaw(String sql) throws SQLException{
		Statement stmt = con.createStatement();
		stmt.execute(sql);
		stmt.close();
	}

	private void updateRaw(String sql) throws SQLException{
		this.commit();
		this.setAutoCommit(true);
		Statement stmt = con.createStatement();
		currentlyRunningQuery = stmt;
		stmt.executeUpdate(sql);
		stmt.close();
		currentlyRunningQuery = null;
	}

	/**
	 * Execute a set of update SQL statements as a batch.
	 * 
	 * @return true on success
	 */
	public boolean updateBatch(ArrayList<String> sqls) {
		try {
			Statement st = con.createStatement();
			currentlyRunningQuery = st;
			for(String s : sqls) {
				st.addBatch(s);
			}
			st.executeBatch();
			st.close();
			currentlyRunningQuery = null;
			return true;
		} catch (SQLException e) {
			ExceptionMan.handle(e);
		}
		return false;
	}

	/**
	 * Execute a SQL query.
	 * 
	 * @param sql the SQL statement
	 * @return the result set. remembe to close it afterwards.
	 */
	public ResultSet query(String sql){
		if(Config.exiting_mode) ExceptionMan.die("");
		try {
			Statement stmt = con.createStatement(ResultSet.HOLD_CURSORS_OVER_COMMIT, 1);
			currentlyRunningQuery = stmt;
			stmt.setFetchSize(100000);
			ResultSet rs = stmt.executeQuery(sql);
			currentlyRunningQuery = null;
			return rs;
		} catch (SQLException e) {
			UIMan.error(sql);
			ExceptionMan.handle(e);
			return null;
		}
	}

	/**
	 * Load the symbol table into a hash table mapping
	 * symbols to their IDs.
	 * 
	 * @see Config#relConstants
	 */
	public ConcurrentHashMap<String, Integer> loadSymbolIdMapFromTable() {
		ConcurrentHashMap<String, Integer> map =
			new ConcurrentHashMap<String, Integer>();
		String rel = Config.relConstants;
		String sql = "SELECT * FROM " + rel;
		ResultSet rs = query(sql);
		try {
			while(rs.next()) {
				String word = rs.getString("string");
				int id = rs.getInt("id");
				map.put(word, id);
			}
			rs.close();
		} catch (SQLException e) {
			ExceptionMan.handle(e);
		}
		return map;
	}

	/**
	 * Load the symbol table into a hash table mapping
	 * symbol IDs to the original symbols.
	 * 
	 * @see Config#relConstants
	 */
	public HashMap<Long,String> loadIdSymbolMapFromTable() {
		HashMap<Long,String> map =
			new HashMap<Long,String>();
		String sql = "SELECT * FROM " + Config.relConstants;
		ResultSet rs = query(sql);
		try {
			while(rs.next()) {
				String word = rs.getString("string");
				long id = rs.getLong("id");
				map.put(id, word);
			}
			rs.close();
		} catch (SQLException e) {
			ExceptionMan.handle(e);
		}
		return map;
	}

	/**
	 * Store the symbol-ID mapping into a symbol table.
	 * 
	 * @param mapConstantID the symbol-ID mapping
	 * @see Config#relConstants
	 */
	public void createConstantTable(Map<String, Integer> mapConstantID, String rel) {
		dropTable(rel);
		String sql = "CREATE TABLE " + rel +
		"(id bigint, string TEXT)";
		
		if (rel.equals(Config.relConstants)) {
			sql = "CREATE TABLE " + rel +
			"(id bigint PRIMARY KEY, string TEXT)";
		}
		
		//if(Config.using_greenplum){
		//	sql += " DISTRIBUTED BY (string)";
		//}
		
		update(sql);

		BufferedWriter writer = null;
		File loadingFile = new File(Config.getLoadingDir(), "loading_symbols_");
		try {
			writer = new BufferedWriter(new OutputStreamWriter
					(new FileOutputStream(loadingFile),"UTF8"));
		} catch (Exception e) {
			ExceptionMan.handle(e);
		}
		try {
			for(Map.Entry<String, Integer> pair : mapConstantID.entrySet()) {
				writer.append(pair.getValue().toString());
				writer.append("\t"); 
				writer.append(StringMan.escapeJavaString(pair.getKey()));
				writer.append("\n");
			}
			writer.close();
			FileInputStream in = new FileInputStream(loadingFile);
			PGConnection con = (PGConnection)this.getConnection();
			sql = "COPY " + rel + " FROM STDIN ";
			con.getCopyAPI().copyIn(sql, in);
			in.close();

			//sql = "CREATE INDEX " + rel + "_constants_index_id ON " + rel + "(id)";
			//this.execute(sql);

		}catch(Exception e) {
			ExceptionMan.handle(e);
		}

	}

	public void insertConstantTable(Map<String, Integer> mapConstantID) {
		String rel = Config.relConstants;
		String sql;
		BufferedWriter writer = null;
		File loadingFile = new File(Config.getLoadingDir(), "loading_symbols_");
		try {
			writer = new BufferedWriter(new OutputStreamWriter
					(new FileOutputStream(loadingFile),"UTF8"));
		} catch (Exception e) {
			ExceptionMan.handle(e);
		}
		try {
			for(Map.Entry<String, Integer> pair : mapConstantID.entrySet()) {
				writer.append(pair.getValue() + "\t" + 
						StringMan.escapeJavaString(pair.getKey()) + "\n");
			}
			writer.close();
			FileInputStream in = new FileInputStream(loadingFile);
			PGConnection con = (PGConnection)this.getConnection();
			sql = "COPY " + rel + " FROM STDIN ";
			con.getCopyAPI().copyIn(sql, in);
			in.close();

			//sql = "CREATE INDEX constants_index_id ON " + rel + "(id)";
			//this.execute(sql);

		}catch(Exception e) {
			ExceptionMan.handle(e);
		}
	}

	/**
	 * Create a table to store a set of integers
	 * @param rel the name of the table
	 * @param set the set of integers
	 */
	public void createSetTable(String rel, HashSet<Integer> set){
		dropTable(rel);
		String sql = "CREATE TEMPORARY TABLE " + rel +
		"(id INT)";
		update(sql);
		PreparedStatement ps = getPrepareStatement(
				"INSERT INTO " + rel + " VALUES(?)");
		try {
			for(int pid : set){
				ps.setInt(1, pid);
				ps.addBatch();
			}
			ps.executeBatch();
			ps.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Try to drop a table; remain silent if the specified
	 * table doesn't exist.
	 */
	public void dropTable(String rel){
		// System.out.println("####### drop " + rel);
		dropStuff("TABLE", rel);
	}

	/**
	 * Try to drop a schema; remain silent if the specified
	 * schema doesn't exist.
	 */
	public boolean dropSchema(String sch){
		return dropStuff("SCHEMA", sch + "");
	}

	/**
	 * Try to drop a sequence; remain silent if the specified
	 * sequence doesn't exist.
	 */
	public void dropSequence(String seq){
		dropStuff("SEQUENCE", seq);
	}

	public void dropView(String view){
		dropStuff("VIEW", view);
	}

	private boolean dropStuff(String type, String obj){
		String sql = "DROP " + type + " IF EXISTS " + obj + " CASCADE";
		String sql2 = "DROP " + type + " IF EXISTS " + obj + "";
		try {
			updateRaw(sql);
			return true;
		} catch (SQLException e) {

			// the target was not found; do nothing
			try{
				updateRaw(sql2);
				return true; 
			}catch(Exception e2){
				return false;
			}

		}
	}


	/**
	 * Return a prepared statement of the given SQL statement.
	 * A SQL statement with or without parameters can be pre-compiled 
	 * and stored in a PreparedStatement object. This object can then 
	 * be used to efficiently execute this statement multiple times. 
	 */
	public PreparedStatement getPrepareStatement(String sql) {
		PreparedStatement ps = null;
		try {
			ps = con.prepareStatement(sql,ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);
			ps.setFetchSize(100000);
		} catch (SQLException e) {
			ExceptionMan.handle(e);
		}
		return ps;
	}

	public boolean schemaExists(String name){

		ResultSet rs = this.query("SELECT * FROM information_schema.schemata WHERE schema_name = '" + name.toLowerCase() + "'");
		try {
			if(rs.next()){
				return true;
			}else{
				return false;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	public boolean tableExists(String tableName) {
		String sql = "SELECT * FROM " + tableName + " LIMIT 1";
		try {
			executeRaw(sql);
			return true;
		} catch (SQLException e){
			return false;
		}
	}
	
	public boolean tableExists(String schemaName, String tableName){
		
		String sql = "SELECT * FROM " + schemaName + "." + tableName + " LIMIT 1";
		try {
			executeRaw(sql);
			return true;
		} catch (SQLException e){
			return false;
		}
	}

	/**
	 * Reset the database schema that serves as Tuffy's workspace.
	 * 
	 * @see Config#db_schema
	 */
	public void resetSchema(String schema) {
		try {
			UIMan.verbose(3, "### Checking existence of " + schema);
			updateRaw("SET search_path = " + schema);
			UIMan.verbose(3, "### Reusing schema " + schema);
		} catch (SQLException e) {
			dropSchema(schema);
			UIMan.verbose(3, "### Creating schema " + schema);
			String sql = "CREATE SCHEMA " + schema + " AUTHORIZATION " + Config.db_username;
			update(sql);
			sql = "GRANT ALL ON SCHEMA " + schema + " TO " + Config.db_username;
			update(sql);
			execute("SET search_path = " + schema);
			execute("DROP TYPE IF EXISTS typeOfIntArray CASCADE");
			execute("CREATE TYPE typeOfIntArray AS ( a INT[] );");
			execute(SQLMan.sqlTypeConversions);
			execute(SQLMan.sqlIntArrayFuncReg);
			execute(SQLMan.sqlRandomAgg);
			execute(SQLMan.sqlFuncMisc);
			regExplainProc("expl");
		}
	}

	/**
	 * Copy the tuples of a table to another.
	 * Can be used to check out the content of a temporary table.
	 * 
	 * @param src name of the source table
	 * @param dest name the destination table; will be dropped if already exists
	 */
	public void copyTable(String src, String dest) {
		dropTable(dest);
		String sql = "CREATE TABLE "+dest+" AS " +
		"SELECT * FROM " + src;
		try {
			updateRaw(sql);
		} catch (SQLException e) {
			ExceptionMan.handle(e);
		}
	}

	/**
	 * Commit the previous actions.
	 * Useless when AutoCommit is on, which is so by default.
	 */
	public void commit() {
		try {
			con.commit();
		} catch (SQLException e) {
			ExceptionMan.handle(e);
		}
	}

	/**
	 * Specifies a JDBC connection.
	 */
	public RDB(String url, String user, String password){

		UIMan.verbose(1000, "------------------- Open a new DB " + this);

		registerDrivers();
		try {
			con = DriverManager.getConnection(url, user, password);
			con.setAutoCommit(true);
			//execute("SET work_mem = '100MB'");
			//execute("SET checkpoint_segments = 30");
			//execute("SET temp_buffers = '2000MB'");
			//execute("SET maintenance_work_mem = '100MB'");
			//execute("SET archive_mode = OFF");
			//execute("SET wal_buffers = '50MB'");
			//execute("SET shared_buffers = '500MB'");
			execute("set client_encoding='utf8'");
			/*
			if(Config.forceNestedLoop) {
				execute("SET enable_bitmapscan = 'off'");
				execute("SET enable_hashagg = 'off'");
				execute("SET enable_hashjoin = 'off'");
				execute("SET enable_indexscan = 'off'");
				execute("SET enable_mergejoin = 'off'");
				execute("SET enable_sort = 'off'");
				execute("SET enable_tidscan = 'off'");
			}
			if(Config.forceJoinOrder) {
				execute("SET join_collapse_limit = '1'");
			}
			 */    
			Runtime.getRuntime().addShutdownHook(new Thread() {
				public void run() {
					if(Config.exiting_mode) return;
					Config.exiting_mode = true;
					UIMan.setSilent(true);
					UIMan.setSilentErr(true);
					System.out.println("\n!!! Shutting down Tuffy !!!");
					if (currentlyRunningQuery != null){
						try {
							System.out.print("Cacelling currently running DB query...");
							currentlyRunningQuery.cancel();
							currentlyRunningQuery = null;
							System.out.println("Done.");
						} catch (SQLException e) {
							System.out.println("Failed.");
						}
					}

					System.out.print("Removing temporary dir '" + Config.getWorkingDir() + "'...");
					System.out.println(FileMan.removeDirectory(new File(Config.getWorkingDir()))?"OK" : "FAILED");

					if(!Config.keep_db_data){
						System.out.print("Removing database schema '" + Config.db_schema + "'...");
						System.out.println(dropSchema(Config.db_schema)?"OK" : "FAILED");
					}else{
						System.out.println("Data remains in schema '" + Config.db_schema + "'.");
					}
					try {
						if(con != null && !con.isClosed()) {
							con.close();
						}
					} catch (SQLException e) {
					}
				}
			});
		} catch (SQLException e) {
			System.err.println("Failed to connect to PostgreSQL!");
			System.err.println(e.getMessage());
			return;
		}
	}

	/**
	 * Set auto-commit state of this connection.
	 */
	public void setAutoCommit(boolean v){
		try {
			con.setAutoCommit(v);
		} catch (SQLException e) {
			System.err.println("Failed to set AutoCommit to " + v);
			System.err.println(e.getMessage());
		}
	}

	/**
	 * Read the current value of a sequence.
	 * 
	 * @param seq the name of the sequence
	 */
	public int getSequenceCurValue(String seq) {
		String s = "SELECT CURRVAL('"+seq+"')";
		ResultSet rs = query(s);
		if(rs == null) return -1;
		try {
			if(rs.next()) {
				return rs.getInt(1);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return -1;
	}

	/**
	 * Count the tuples in a table.
	 */
	public long countTuples(String table) {
		if(Config.exiting_mode) ExceptionMan.die("");
		String s = "SELECT COUNT(*) FROM " + table;
		ResultSet rs = query(s);
		if(rs == null) ExceptionMan.die("");
		try {
			if(rs.next()) {
				long c = rs.getLong(1);
				rs.close();
				return c;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return -1;
	}

	/**
	 * Close this connection.
	 */
	public void close() {
		
		try {
			if (con != null) {
				UIMan.verbose(1000, "------------------- Close a DB " + this);
				con.close();
				con = null;
			}
			
		} catch (SQLException e) {
			ExceptionMan.handle(e);
		}
		
	}

	/**
	 * Analyze a specific table.
	 * 
	 * @param rel name of the table
	 * 
	 * @see <a hef='http://www.postgresql.org/docs/current/static/sql-analyze.html'>
	 * the PostgreSQL doc</a>
	 */
	public void analyze(String rel) {
		String sql = "ANALYZE " + rel;
		this.update(sql);
	}

	/**
	 * Vacuum a specific table.
	 * 
	 * @param rel name of the table
	 * 
	 * @see <a hef='http://www.postgresql.org/docs/current/static/sql-vacuum.html'>
	 * the PostgreSQL doc</a>
	 */
	public void vacuum(String rel) {
		String sql = "VACUUM " + rel;
		this.update(sql);
	}

	/**
	 * Drop an index if it exists.
	 * 
	 * @param idx name of the index
	 */
	public void dropIndex(String idx){
		String sql = "DROP INDEX IF EXISTS " + idx;
		try {
			updateRaw(sql);
		} catch (SQLException e) {
			ExceptionMan.handle(e);
		}
	}

	/**
	 * Reset the value of a sequence to 1.
	 * 
	 * @param seq name of the sequence
	 */
	public void resetSequence(String seq) {
		String sql = "SELECT setval('" + seq +
		"', 1, false)";
		execute(sql);
	}

	/**
	 * Call a stored procedure that doesn't have any parameters.
	 * 
	 * @param proc name of the stored procedure
	 */
	public void callProcedure(String proc) {
		CallableStatement stmt = null;
		try {
			stmt = con.prepareCall("{call "+proc+"()}");
			stmt.execute();
			stmt.close();
		} catch (SQLException e) {
			ExceptionMan.handle(e);
		}
	}

	/**
	 * Call a function that returns a double.
	 * 
	 * @param func name of the function
	 * @param args arguments in the form of a string
	 * @return value returned by the function; null on error
	 */
	public Double callFunctionDouble(String func, String args) {
		CallableStatement stmt = null;
		try {
			if(args == null) args = "";
			stmt = con.prepareCall("{? = call "+func+"(" +args + ")}");
			stmt.registerOutParameter(1, java.sql.Types.DOUBLE);
			stmt.execute();
			double x = stmt.getDouble(1);
			stmt.close();
			return x;
		} catch (SQLException e) {
			ExceptionMan.handle(e);
		}
		return null;
	}

}
