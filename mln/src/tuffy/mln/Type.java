package tuffy.mln;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Writer;
import java.util.HashMap;
import java.util.HashSet;

import org.postgresql.PGConnection;

import tuffy.db.RDB;
import tuffy.util.Config;
import tuffy.util.ExceptionMan;

/**
 * A domain/type of constants; i.e., a subset of constants.
 */
public class Type {
	
	/**
	 * Built-in types
	 */
	public static Type Generic = new Type("_GENERIC");
	public static Type Float = new Type("_FLOAT");
	public static Type Integer = new Type("_INTEGER");
	public static Type String = new Type("_STRING");
	public static Type Bool = new Type("_BOOL");
	
	static {
		Float.isNonSymbolicType = true;
		Integer.isNonSymbolicType = true;
		Bool.isNonSymbolicType = true;
		Float.nonSymbolicType = Float;
		Integer.nonSymbolicType = Integer;
		Bool.nonSymbolicType = Bool;
	}
	
	private boolean isNonSymbolicType = false;
	
	private Type nonSymbolicType = null;
	
	/**
	 * See if this type is non-symbolic.
	 * "Non-symbolic" means that the value of this type is directly
	 * stored in the predicate table, whereas values of a "symbolic" (default)
	 * type are represented by unique IDs as per the symbol table.
	 * @return
	 */
	public boolean isNonSymbolicType(){
		return isNonSymbolicType;
	}
	
	public Type getNonSymbolicType(){
		return nonSymbolicType;
	}
	
	public String getNonSymbolicTypeInSQL(){
		if(nonSymbolicType.name.equals("_FLOAT")){
			return "float";
		}else if (nonSymbolicType.name.equals("_STRING")){
			return "string";
		}else if (nonSymbolicType.name.equals("_INTEGER")){
			return "integer";
		}else if (nonSymbolicType.name.equals("_BOOL")){
			return "boolean";
		}else{
			return null;
		}
	}
	
	/**
	 * The domain of variable values. The members of a domain are
	 * named as integer.
	 */
	private HashSet<Integer> domain = new HashSet<Integer>();
	
	/**
	 * Name of this Type.
	 */
	public String name;
	
	/**
	 * Name of the relational table corresponding to this type.
	 * Here it is type_$name.
	 */
	private String relName;
	
	public boolean isProbArg = false;
	
	/**
	 * Constructor of Type.
	 * 
	 * @param name the name of this new type; it must be unique among all types
	 */
	public Type(String name){
		this.name = name;
		relName = "type_" + name;
		
		if(name.endsWith("_")){
			isNonSymbolicType = true;
			if(name.toLowerCase().startsWith("float")){
				nonSymbolicType = Float;
			}else if(name.toLowerCase().startsWith("double")){
				nonSymbolicType = Float;
			}else if(name.toLowerCase().startsWith("int")){
				nonSymbolicType = Integer;
			}else{
				isNonSymbolicType = false;
			}
		}
		
		if(name.endsWith("_p_")){
			this.isProbArg = true;
		}
		
	}
	
	/**
	 * Return the name of the DB relational table of this type.
	 */
	public String getRelName(){
		return relName;
	}
	
	/**
	 * Store the list of constants in a DB table.
	 * 
	 * @param db 
	 */
	public void storeConstantList(RDB db, boolean... onlyNonEmptyDomain){
		
		if(onlyNonEmptyDomain.length>0 && onlyNonEmptyDomain[0] == true && domain.size() == 0){
			return;
		}
		
		String sql;
		
		if(onlyNonEmptyDomain.length == 0){
			db.dropTable(relName);
			//String sql = "CREATE TEMPORARY TABLE " + relName + "(constantID INTEGER)\n";
			sql = "CREATE TABLE " + relName + "(constantID bigint, constantVALUE TEXT)";
			db.update(sql);
		}
		
		
		BufferedWriter writer = null;
		File loadingFile = new File(Config.getLoadingDir(), "loading_type_" + name);
		try {
			writer = new BufferedWriter(new OutputStreamWriter
					(new FileOutputStream(loadingFile),"UTF8"));
			for(int v : domain) {
				writer.append(v + "\n");
			}
			writer.close();
			
			FileInputStream in = new FileInputStream(loadingFile);
			PGConnection con = (PGConnection)db.getConnection();
			sql = "COPY " + relName + " (constantID) FROM STDIN ";
			con.getCopyAPI().copyIn(sql, in);
			in.close();
			
			sql = "UPDATE " + relName + " SET constantVALUE = t1.string FROM " + Config.relConstants + " t1 WHERE t1.id = constantID AND constantVALUE IS NULL";
			db.execute(sql);
			
			//domain.clear();
		}catch(Exception e) {
			ExceptionMan.handle(e);
		}
		
		db.analyze(relName);
	}
	
	/**
	 * Add a constant to this type.
	 * 
	 * @param con the constant to be added
	 */
	public void addConstant(int con) {
		domain.add(con);
	}
	
	public HashSet<Integer> getDomain(){
		return domain;
	}
	
	/**
	 * Return true if this type contains the constant x
	 */
	public boolean contains(int x){
		return domain.contains(x);
	}
	
	/**
	 * Return the number of constants in this type domain.
	 */
	public int size(){
		RDB db = RDB.getRDBbyConfig(Config.db_schema);
		int a = (int) db.countTuples(this.relName);
		db.close();
		return a;
		//	return domain.size();
	}
	
	/**
	 * Return the name of this type.
	 */
	public String name(){
		return name;
	}
}
