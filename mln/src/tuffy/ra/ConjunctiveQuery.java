package tuffy.ra;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;


import tuffy.db.RDB;
import tuffy.db.SQLMan;
import tuffy.mln.Clause;
import tuffy.mln.Literal;
import tuffy.mln.Predicate;
import tuffy.mln.Term;
import tuffy.mln.Type;
import tuffy.util.Config;
import tuffy.util.DebugMan;
import tuffy.util.ExceptionMan;
import tuffy.util.StringMan;
import tuffy.util.Timer;
import tuffy.util.UIMan;

/**
 * A conjunctive query.
 * Used by Datalog and scoping rules.
 */
public class ConjunctiveQuery implements Cloneable{

	public String additionalWhereClause = " (1=1) ";

	private boolean isScopingRule = false;

	public boolean inverseEmbededWeight = false;

	public boolean isQueryScopingRule = false;
	public boolean updateNotInsert = false;

	public void setScopingRule(boolean isScopingRule) {
		this.isScopingRule = isScopingRule;
	}

	public boolean isScopingRule() {
		return isScopingRule;
	}

	private Double newTuplePrior = null;

	public void setNewTuplePrior(double prior){
		newTuplePrior = prior;
	}

	public Double getNetTuplePrior(){
		return newTuplePrior;
	}

	public boolean isCRFChainRule = false;

	public static HashSet<String> indexBuilt = new HashSet<String>();

	private static int idGen = 0;
	public boolean isView = false;
	public boolean isStatic = false;
	public boolean isFictitious = false;

	public String allFreeBinding = null;

	private static HashMap<Integer, ConjunctiveQuery> objMap = new HashMap<Integer, ConjunctiveQuery>();

	private int id = 0;

	public ConjunctiveQuery(){
		id = (++idGen);
		objMap.put(id, this);
	}

	public int getID()	{
		return id;
	}

	public static ConjunctiveQuery getCqById(int id){
		return objMap.get(id);
	}

	public Clause sourceClause = null;

	@SuppressWarnings("unchecked")
	public ConjunctiveQuery clone(){

		ConjunctiveQuery cloned = new ConjunctiveQuery();
		try{

			cloned.additionalWhereClause = this.additionalWhereClause;
			cloned.allFreeBinding = this.allFreeBinding;
			cloned.allVariable = (HashSet<String>) this.allVariable.clone();
			cloned.body = (ArrayList<Literal>) this.body.clone();
			cloned.constraints = (ArrayList<Expression>) this.constraints.clone();
			cloned.freeVars = (HashMap<String, Type>) this.freeVars.clone();
			cloned.head = (Literal) this.head.clone();
			cloned.inverseEmbededWeight = this.inverseEmbededWeight;
			cloned.isCRFChainRule = this.isCRFChainRule;
			cloned.isFictitious = this.isFictitious;
			cloned.isScopingRule = this.isScopingRule;
			cloned.isStatic = this.isStatic;
			cloned.isView = this.isView;
			cloned.newTuplePrior = this.newTuplePrior;
			cloned.psMap = (HashMap<String, PreparedStatement>) this.psMap.clone();
			cloned.sourceClause = this.sourceClause;
			cloned.type = this.type;
			cloned.weight = this.weight;
			cloned.updateNotInsert = this.updateNotInsert;
			cloned.isQueryScopingRule = this.isQueryScopingRule;
		}catch(Exception e){
			e.printStackTrace();
		}
		return cloned;
	}

	/**
	 * Maps from binding patterns to corresponding prepared statements. Here the binding pattern
	 * is a string like "11011", which means the third parameter need to be queried, while other
	 * four are provided.
	 */
	public HashMap<String, PreparedStatement> psMap = new HashMap<String, PreparedStatement>();

	public String getAllFreeBinding(){
		return this.allFreeBinding;
	}

	public static void clearIndexHistory(){
		indexBuilt = new  HashSet<String>();
	}

	private double weight = 0;

	public void setWeight(double w){
		weight = w;
	}

	public double getWeight(){
		return weight;
	}

	/**
	 * Type used by CC.
	 */
	public enum CLUSTERING_RULE_TYPE 
	{SOFT_COMPLETE, SOFT_INCOMPLETE, HARD, COULD_LINK_CLIQUE, MUST_LINK_CLIQUE, 
		COULD_LINK_PAIRWISE, NODE_LIST, NODE_CLASS, CLASS_TAGS, WOULD_LINK_CLIQUE, SOFT_NEGATIVE,
		HARD_NEGATIVE}

	/**
	 * Type used by CC.
	 */
	public CLUSTERING_RULE_TYPE type = null;

	public Literal head;
	public ArrayList<Literal> body = new ArrayList<Literal>();
	HashMap<String, Type> freeVars = new HashMap<String, Type>();

	private ArrayList<Expression> constraints = new ArrayList<Expression>();

	/**
	 * Add a constraint that must hold.
	 * @param e A bool expression that must be TRUE.
	 */
	public void addConstraint(Expression e){
		constraints.add(e);
	}

	public ArrayList<Expression> getConstraint(){
		return constraints;
	}

	public ArrayList<Expression> getConstraint(HashSet<String> allVariables){
		//		return constraints;
		ArrayList<Expression> ret = new ArrayList<Expression>();

		for(Expression e : constraints){
			int flag = 0;
			for(String v : e.getVars()){
				if(!allVariables.contains(v)){
					flag = 1;
				}
			}
			if(flag == 0){
				ret.add(e);
			}
		}

		return ret;

	}

	public void addConstraintAll(Collection<Expression> es){
		constraints.addAll(es);
	}

	public String toStringInOneLine(){

		String s = "";
		if(this.sourceClause != null && this.sourceClause.hasEmbeddedWeight()){
			s = "["+this.sourceClause.getVarWeight()+"] " + head.toString() + 
			(isScopingRule ? " :=\t" : " :-\t");
		}else{
			s = "["+this.getWeight()+"] " + head.toString() + 
			(isScopingRule ? " :=\t" : " :-\t");
		}

		ArrayList<String> a = new ArrayList<String>();
		for(Literal b : body){
			a.add(b.toString());
		}
		for(Expression e : this.constraints){
			boolean ori = e.changeName;
			e.changeName = true;
			a.add(e.toString());
			e.changeName = ori;
		}
		s += StringMan.join(", ", a);
		if(newTuplePrior != null){
			s += "\n### (new tuple prior = " + newTuplePrior + ")";
		}
		return s;

	}

	public String toString(){

		String s = "";
		if(this.sourceClause != null && this.sourceClause.hasEmbeddedWeight()){
			s = "["+this.sourceClause.getVarWeight()+"] " + head.toString() + 
			(isScopingRule ? " :=\n\t" : " :-\n\t");
		}else{
			s = "["+this.getWeight()+"] " + head.toString() + 
			(isScopingRule ? " :=\n\t" : " :-\n\t");
		}

		ArrayList<String> a = new ArrayList<String>();
		for(Literal b : body){
			a.add(b.toString());
		}
		for(Expression e : this.constraints){
			boolean ori = e.changeName;
			e.changeName = true;
			a.add(e.toString());
			e.changeName = ori;
		}
		s += StringMan.join(", \n\t", a);
		if(newTuplePrior != null){
			s += "\n### (new tuple prior = " + newTuplePrior + ")";
		}
		return s;
	}

	/**
	 * Execute this conjunctive query.
	 * @param db the DB connection
	 * @param truth the truth value for the newly materialized tuples (of the head predicate)
	 */
	public void materialize(RDB db, Boolean truth, ArrayList<String> orderBy){
		Predicate p = head.getPred();
		HashMap<String, String> mapVarAttr = new HashMap<String, String>();
		ArrayList<String> selList = new ArrayList<String>();
		ArrayList<String> fromList = new ArrayList<String>();
		ArrayList<String> whereList = new ArrayList<String>();
		whereList.add("1=1");

		// instantiate dangling variables with the type dict
		for(String v : freeVars.keySet()){
			Type t = freeVars.get(v);
			String tname = "type" + v;
			fromList.add(t.getRelName() + " " + tname);
			mapVarAttr.put(v, tname + ".constantID");
		}
		HashMap<String, Type> var2type = new HashMap<String, Type>();
		ArrayList<String> exceptList = new ArrayList<String>();
		// variable/constant binding
		for(int i=0; i<body.size();i++) {
			Literal lit = body.get(i);
			int idx = i;
			String relP = lit.getPred().getRelName();
			if(lit.getSense()){
				fromList.add(relP + " t" +idx);
			}
			if(!lit.coversAllMaterializedTuples()){
				if(lit.getSense()){
					//TODO
					//	whereList.add("t" + idx + ".truth = " + 
					//			(lit.getSense()?"TRUE" : "FALSE"));
				}
			}

			ArrayList<Term> terms = lit.getTerms();

			if(lit.getSense()){
				for(int j=0; j<terms.size(); j++) {
					Term t = terms.get(j);
					String var = t.var();

					var2type.put(var, lit.getPred().getTypeAt(j));

					String attr = "t" + idx + "."+lit.getPred().getArgs().get(j);
					if(t.isConstant()) {
						whereList.add(attr + "=" + SQLMan.escapeString(t.constantString()));
					}else {
						String cattr = mapVarAttr.get(var);
						if(cattr == null) {
							mapVarAttr.put(var, attr);
						}else {
							whereList.add(attr + "=" + cattr);
						}
					}
				}
			}else{
				ArrayList<String> args = new ArrayList<String>();
				for(int j=0; j<terms.size(); j++) {
					Term t = terms.get(j);
					if(t.isConstant()){
						args.add(SQLMan.escapeString(t.constantString()));
					}else{
						String var = t.var();
						var2type.put(var, lit.getPred().getTypeAt(j));
						String cattr = mapVarAttr.get(var);
						if(cattr == null) {
							//	ExceptionMan.die("unsafe rule:\n" + toString());
						}else {
							args.add(cattr);
						}
					}
				}

				exceptList.add("(" + StringMan.commaList(args) + ") NOT IN (SELECT " + 
						StringMan.commaList(lit.getPred().getArgs()) + 
						" FROM " + lit.getPred().getRelName() + ")");
			}
		}

		///////////////////////////////////
		whereList.addAll(exceptList);

		for(int i=0;i<this.head.getPred().arity();i++){
			Term t = this.head.getTerms().get(i);
			Type type = this.head.getPred().getTypeAt(i);
			var2type.put(t.toString(), type);
		}

		// express constraints in SQL
		HashSet<String> cvars = new HashSet<String>();
		int nChangeName = 0;
		for(Expression e : constraints){
			for(String var : e.getVars()){
				if(var2type.get(var).isNonSymbolicType()){
					e.changeName = false;
				}
			}
			if(e.changeName == true){
				nChangeName ++;
			}
			cvars.addAll(e.getVars());
		}

		HashMap<String, String> mapVarVal = new HashMap<String, String>();
		HashMap<String, String> mapVarValNotChangeName = new HashMap<String, String>();
		int idx = 0;
		for(String v : cvars){
			++ idx;
			String attr = mapVarAttr.get(v);
			if(attr == null){
				ExceptionMan.die("unsafe constraints in conjunctive query\n" + toString());
			}
			if(nChangeName > 0){
				fromList.add(var2type.get(v).getRelName() + " s" + idx);
				whereList.add("s" + idx + ".constantid = " + attr);
			}
			mapVarVal.put(v, "s" + idx + ".constantvalue");
			mapVarValNotChangeName.put(v, attr);
		}
		for(Expression e : constraints){
			if(e.changeName == true){
				e.bindVariables(mapVarVal);
				whereList.add(e.toSQL());
			}else{
				Expression tmpE = e.clone();
				tmpE.renameVariables(mapVarValNotChangeName);
				whereList.add(tmpE.toString());
			}
		}


		// select list
		HashMap<String, String> var2argMap = new HashMap<String, String>();
		ArrayList<String> selSigList = new ArrayList<String>();
		for(int i=0; i<p.arity(); i++){
			Term t = head.getTerms().get(i);
			String v = null;
			if(t.isConstant()){
				v = SQLMan.escapeString(t.constantString());
			}else{
				v = mapVarAttr.get(t.var());
			}
			selSigList.add(v);
			selList.add(v + " AS " + p.getArgs().get(i));
			var2argMap.put(t.toString(), p.getArgs().get(i));
		}

		String sql = "", sub;
		sub = "SELECT " + StringMan.commaList(selList) +
		(fromList.size() > 0 ? " FROM " + StringMan.commaList(fromList) : " ") +
		" WHERE " + SQLMan.andSelCond(whereList) ;
		if (!updateNotInsert) {
			if(!Config.using_greenplum){
			sub += 
				"\nEXCEPT\n" +
				" SELECT " + StringMan.commaList(p.getArgs()) +
				" FROM " + p.getRelName()
				;
			}
		}


		ArrayList<String> iargs = new ArrayList<String>();
		ArrayList<String> eargs = new ArrayList<String>();
		//iargs.add(p.nextTupleID());
		String club = "2"; //evidence
		if(p.hasQuery()) club = "3";
		if (isQueryScopingRule) {
			iargs.add("NULL");
			iargs.add("1");
			if(newTuplePrior != null && 
					newTuplePrior <= 1 && newTuplePrior >= 0){
				iargs.add(newTuplePrior.toString());
			}else{
				iargs.add("NULL");
			}
		} else if(truth == null || head.coversAllMaterializedTuples() 
				|| newTuplePrior != null){
			iargs.add("NULL");
			iargs.add("0");
			if(newTuplePrior != null && 
					newTuplePrior <= 1 && newTuplePrior >= 0){
				iargs.add(newTuplePrior.toString());
			}else{
				iargs.add("NULL");
			}
		}else if(truth){
			iargs.add("TRUE");
			iargs.add(club);
			iargs.add("NULL");
		}else{
			iargs.add("FALSE");
			iargs.add(club);
			iargs.add("NULL");
		}


		for(String a : p.getArgs()){
			iargs.add("nt." + a);
			eargs.add("nt." + a);
		}

		//p.isInMem = true;

		if (updateNotInsert) {
			ArrayList<String> conds = new ArrayList<String>();
			conds.addAll(whereList);
			for (int i=0; i < p.arity(); i++) {
				conds.add("tar." + p.getArgs().get(i) + " = " + selSigList.get(i));
			}
			sql = "UPDATE " + p.getRelName() + " tar " +
			" SET truth = " + iargs.get(0) + ", " +
			" club = " + iargs.get(1) + ", " +
			" prior = " + iargs.get(2) + 
			(fromList.size() > 0 ? " FROM " + StringMan.commaList(fromList) : " ") +
			" WHERE " +
			SQLMan.andSelCond(conds);

		} else {

			if(Config.using_greenplum){
				System.out.println("--- I used to be a bug ? ...");
				db.update("ALTER TABLE " + p.getRelName() 
						+ " SET DISTRIBUTED BY (" + p.getArgs().get(0) + ");");
			}
			
			//sql = "INSERT INTO " + p.getRelName() + "(id,truth,club,prior," 
			sql = "INSERT INTO " + p.getRelName() + "(truth,club,prior," 
			+ StringMan.commaList(p.getArgs()) + ")\n";
			sql += "SELECT " + StringMan.commaList(iargs) +
			" FROM (" + sub + ") nt ";

			if(orderBy.size() > 0){
				ArrayList<String> orders = new ArrayList<String>();
				for(String o : orderBy){
					if(var2argMap.containsKey(o)){
						orders.add(var2argMap.get(o));
					}
				}

				if(orders.size() > 0){
					sql += " ORDER BY (" + StringMan.commaList(orders) + ")";
				}
			}

		}
		
		
		UIMan.verbose(2, sql);
		db.update(sql);
		/*
		String ecost = db.estimateCost(sub);
		// DebugMan.verbose(2, ecost);
		DebugMan.verbose(2, sql);
		DebugMan.verbose(2, "ESTIMATED cost = " + db.estimatedCost + " ; rows = " + db.estimatedRows);
		Timer.start("cqmat");
		db.update(sql);
		double rtime = Timer.elapsedMilliSeconds("cqmat");
		DebugMan.verbose(2, Timer.elapsed("cqmat"));
		DebugMan.verbose(2, "COST-RATIO = " + (db.estimatedCost/rtime) + " ; ROW-RATIO = " + 
				((double)db.estimatedRows/db.getLastUpdateRowCount()));
		 */
	}

	public String getJoinSQL(HashSet<String> whichToBound){
		Predicate p = head.getPred();

		HashMap<String, String> mapVarAttr = new HashMap<String, String>();
		ArrayList<String> selList = new ArrayList<String>();
		ArrayList<String> fromList = new ArrayList<String>();
		ArrayList<String> whereList = new ArrayList<String>();
		ArrayList<String> exceptList = new ArrayList<String>();
		whereList.add("1=1");

		// instantiate dangling variables with the type dict
		for(String v : freeVars.keySet()){
			Type t = freeVars.get(v);
			String tname = "type" + v;
			fromList.add(t.getRelName() + " " + tname);
			mapVarAttr.put(v, tname + ".constantID");
		}

		HashMap<String, Type> var2type = new HashMap<String, Type>();
		// variable/constant binding
		for(int i=0; i<body.size();i++) {
			Literal lit = body.get(i);
			int idx = i;
			String relP = lit.getPred().getRelName();
			if(lit.getSense()){
				fromList.add(relP + " t" +idx);
			}
			if(!lit.coversAllMaterializedTuples()){
				if(lit.getSense()){
					//	whereList.add("t" + idx + ".truth = " + 
					//			(lit.getSense()?"TRUE" : "FALSE"));
				}
			}

			ArrayList<Term> terms = lit.getTerms();

			if(lit.getSense()){
				for(int j=0; j<terms.size(); j++) {
					Term t = terms.get(j);
					String var = t.var();
					String attr = "t" + idx + "."+lit.getPred().getArgs().get(j);

					var2type.put(var, lit.getPred().getTypeAt(j));

					if(t.isConstant()) {
						whereList.add(attr + "=" + SQLMan.escapeString(t.constantString()));
					}else {
						String cattr = mapVarAttr.get(var);
						if(cattr == null) {
							mapVarAttr.put(var, attr);
						}else {

							String a1 = attr;
							a1 = a1.replaceAll("\\..*$", "");
							if(cattr.contains(a1 + ".")){
								continue;
							}

							whereList.add(attr + "=" + cattr);
						}
					}
				}
			}else{
				ArrayList<String> args = new ArrayList<String>();
				for(int j=0; j<terms.size(); j++) {
					Term t = terms.get(j);
					if(t.isConstant()){
						args.add(SQLMan.escapeString(t.constantString()));
					}else{
						String var = t.var();

						var2type.put(var, lit.getPred().getTypeAt(j));

						String cattr = mapVarAttr.get(var);
						if(cattr == null) {
							return null;
							//	ExceptionMan.die("write negative predicate in the last\n" + toString());
						}else {
							args.add(cattr);
						}
					}
				}

				exceptList.add("(" + StringMan.commaList(args) + ") NOT IN (SELECT " + 
						StringMan.commaList(lit.getPred().getArgs()) + 
						" FROM " + lit.getPred().getRelName() + ")");
			}
		}

		///////////////////////////////////
		whereList.addAll(exceptList);

		for(int i=0;i<this.head.getPred().arity();i++){
			Term t = this.head.getTerms().get(i);
			Type type = this.head.getPred().getTypeAt(i);
			var2type.put(t.toString(), type);
		}

		// express constraints in SQL
		HashSet<String> cvars = new HashSet<String>();
		int nChangeName = 0;
		for(Expression e : constraints){
			for(String var : e.getVars()){
				if(var2type.get(var).isNonSymbolicType()){
					e.changeName = false;
				}
			}
			if(e.changeName == true){
				nChangeName ++;
			}
			cvars.addAll(e.getVars());
		}

		HashMap<String, String> mapVarVal = new HashMap<String, String>();
		HashMap<String, String> mapVarValNotChangeName = new HashMap<String, String>();
		int idx = 0;
		for(String v : cvars){
			++ idx;
			String attr = mapVarAttr.get(v);
			if(attr == null){
				ExceptionMan.die("unsafe constraints in conjunctive query\n" + toString());
			}
			if(nChangeName > 0){
				fromList.add(var2type.get(v).getRelName() + " s" + idx);
				whereList.add("s" + idx + ".constantid = " + attr);
			}
			mapVarVal.put(v, "s" + idx + ".constantstring");
			mapVarValNotChangeName.put(v, attr);
		}
		for(Expression e : constraints){
			if(e.changeName == true){
				e.bindVariables(mapVarVal);
				whereList.add(e.toSQL());
			}else{
				Expression tmpE = e.clone();
				tmpE.renameVariables(mapVarValNotChangeName);
				whereList.add(tmpE.toString());
			}
		}


		// select list
		for(int i=0; i<p.arity(); i++){
			Term t = head.getTerms().get(i);
			String v = null;
			if(t.isConstant()){
				v = SQLMan.escapeString(t.constantString());
			}else{
				v = mapVarAttr.get(t.var());
			}
			selList.add(v + " AS " + p.getArgs().get(i));

			if( whichToBound.contains(head.getTerms().get(i).toString()) ){				
				//					head.getTerms().get(i).toString().equals( whichToBound ) ){
				whereList.add(v + " = " + "1");
			}

		}

		String sub;

		String sel = "SELECT ";
		if(this.type == CLUSTERING_RULE_TYPE.COULD_LINK_PAIRWISE
				|| this.type == CLUSTERING_RULE_TYPE.COULD_LINK_CLIQUE){
			sel += "DISTINCT ";
		}

		sub = sel + StringMan.commaList(selList) +
		" FROM " + StringMan.join(" JOIN ", fromList) +
		" ON " + SQLMan.andSelCond(whereList) ;
		return sub;
	}

	public String getBoundedSQL(HashSet<String> whichToBound){
		Predicate p = head.getPred();

		HashMap<String, String> mapVarAttr = new HashMap<String, String>();
		ArrayList<String> selList = new ArrayList<String>();
		ArrayList<String> fromList = new ArrayList<String>();
		ArrayList<String> whereList = new ArrayList<String>();
		ArrayList<String> exceptList = new ArrayList<String>();
		whereList.add("1=1");


		HashMap<String, Type> var2type = new HashMap<String, Type>();
		// instantiate dangling variables with the type dict
		for(String v : freeVars.keySet()){
			Type t = freeVars.get(v);
			String tname = "type" + v;
			fromList.add(t.getRelName() + " " + tname);
			mapVarAttr.put(v, tname + ".constantID");
		}

		// variable/constant binding
		for(int i=0; i<body.size();i++) {
			Literal lit = body.get(i);
			int idx = i;
			String relP = lit.getPred().getRelName();
			if(lit.getSense()){
				fromList.add(relP + " t" +idx);
			}
			if(!lit.coversAllMaterializedTuples()){
				if(lit.getSense()){
					whereList.add("t" + idx + ".truth = " + 
							(lit.getSense()?"'1'" : "'0'"));
				}
			}

			ArrayList<Term> terms = lit.getTerms();

			if(lit.getSense()){
				for(int j=0; j<terms.size(); j++) {
					Term t = terms.get(j);
					String var = t.var();
					var2type.put(var, lit.getPred().getTypeAt(j));
					String attr = "t" + idx + "."+lit.getPred().getArgs().get(j);
					if(t.isConstant()) {
						whereList.add(attr + "=" + SQLMan.escapeString(t.constantString()));
					}else {
						String cattr = mapVarAttr.get(var);
						if(cattr == null) {
							mapVarAttr.put(var, attr);
						}else {
							whereList.add(attr + "=" + cattr);
						}
					}
				}
			}else{
				ArrayList<String> args = new ArrayList<String>();
				for(int j=0; j<terms.size(); j++) {
					Term t = terms.get(j);

					if(t.isConstant()){
						if (Config.constants_as_raw_string) {
							args.add(SQLMan.escapeString(t.constantString()));
						} else {
							args.add(t.constantString());
						}
					}else{
						String var = t.var();

						var2type.put(var, lit.getPred().getTypeAt(j));

						String cattr = mapVarAttr.get(var);
						if(cattr == null) {
							return null;
							//	ExceptionMan.die("write negative predicate in the last\n" + toString());
						}else {
							args.add(cattr);
						}
					}
				}

				exceptList.add("(" + StringMan.commaList(args) + ") NOT IN (SELECT " + 
						StringMan.commaList(lit.getPred().getArgs()) + 
						" FROM " + lit.getPred().getRelName() + ")");
			}
		}

		for(int i=0;i<this.head.getPred().arity();i++){
			Term t = this.head.getTerms().get(i);
			Type type = this.head.getPred().getTypeAt(i);
			var2type.put(t.toString(), type);
		}

		///////////////////////////////////
		// express constraints in SQL
		HashSet<String> cvars = new HashSet<String>();
		int nChangeName = 0;
		for(Expression e : constraints){
			for(String var : e.getVars()){
				if(var2type.get(var) == null){
					// TODO(ce) WHAT'S THIS???
					// System.out.println();
				}
				if(var2type.get(var).isNonSymbolicType()){
					e.changeName = false;
				}
			}
			if(e.changeName == true){
				nChangeName ++;
			}
			cvars.addAll(e.getVars());
		}

		HashMap<String, String> mapVarVal = new HashMap<String, String>();
		HashMap<String, String> mapVarValNotChangeName = new HashMap<String, String>();
		int idx = 0;
		for(String v : cvars){
			++ idx;
			String attr = mapVarAttr.get(v);
			if(attr == null){
				ExceptionMan.die("unsafe constraints in conjunctive query\n" + toString());
			}
			if(nChangeName > 0){
				fromList.add(var2type.get(v).getRelName() + " s" + idx);
				whereList.add("s" + idx + ".constantid = " + attr);
			}
			mapVarVal.put(v, "s" + idx + ".constantvalue");
			mapVarValNotChangeName.put(v, attr);
		}
		for(Expression e : constraints){
			if(e.changeName == true){
				e.bindVariables(mapVarVal);
				whereList.add(e.toSQL());
			}else{
				Expression tmpE = e.clone();
				tmpE.renameVariables(mapVarValNotChangeName);
				whereList.add(tmpE.toString());
			}
		}


		// select list
		for(int i=0; i<p.arity(); i++){
			Term t = head.getTerms().get(i);
			String v = null;
			if(t.isConstant()){
				v = SQLMan.escapeString(t.constantString());
			}else{
				v = mapVarAttr.get(t.var());
			}
			selList.add(v + " AS " + p.getArgs().get(i));


			if( whichToBound.contains(head.getTerms().get(i).toString()) ){				
				whereList.add(v + " = " + "1");
			}
		}

		String sub;

		String sel = "SELECT ";
		if(this.type == CLUSTERING_RULE_TYPE.COULD_LINK_PAIRWISE
				|| this.type == CLUSTERING_RULE_TYPE.COULD_LINK_CLIQUE){
			sel += "DISTINCT ";
		}

		sub = sel + StringMan.commaList(selList) +
		(fromList.size() > 0 ? " FROM " + StringMan.commaList(fromList) : " ") +
		" WHERE " + SQLMan.andSelCond(whereList) ;
		return sub;
	}

	public HashSet<String> allVariable = new HashSet<String>();

	/**
	 * Set the head of this query.
	 * @param lit
	 */
	public void setHead(Literal lit){
		head = lit;
		for(int i=0; i<lit.getPred().arity(); i++){
			Term t = lit.getTerms().get(i);
			if(t.isVariable() && !allVariable.contains(t.var())) freeVars.put(t.var(), lit.getPred().getTypeAt(i));
		}
	}

	/**
	 * Add a body literal.
	 * @param lit
	 */
	public void addBodyLit(Literal lit){
		body.add(lit);
		for(int i=0; i<lit.getPred().arity(); i++){
			Term t = lit.getTerms().get(i);
			if(lit.getSense()){
				if(t.isVariable()) freeVars.remove(t.var());
				allVariable.add(t.var());
			}
		}
	}

	public void buildIndexes(RDB db, Boolean truth, Set<Predicate> IDB, String tableName, 
			boolean addM1LessThanM2, ArrayList<String> additionalSel, boolean... forceBuild){

		Predicate p = head.getPred();

		HashMap<String, String> mapVarAttr = new HashMap<String, String>();
		ArrayList<String> selList = new ArrayList<String>();
		ArrayList<String> fromList = new ArrayList<String>();
		ArrayList<String> whereList = new ArrayList<String>();
		ArrayList<String> exceptList = new ArrayList<String>();
		whereList.add("1=1");

		HashSet<String> tables = new HashSet<String>();
		HashSet<String> indices = new HashSet<String>();
		HashMap<String, String> indicesAssistor = new HashMap<String, String>();

		// instantiate dangling variables with the type dict
		for(String v : freeVars.keySet()){
			Type t = freeVars.get(v);
			String tname = "type" + v;
			fromList.add(t.getRelName() + " " + tname);
			mapVarAttr.put(v, tname + ".constantID");
		}

		HashMap<String, Type> var2type = new HashMap<String, Type>();

		ArrayList<String> priors = new ArrayList<String>();
		// variable/constant binding
		for(int i=0; i<body.size();i++) {
			Literal lit = body.get(i);
			int idx = i;
			String relP = lit.getPred().getRelName();
			tables.add(relP);
			if(lit.getSense()){
				fromList.add(relP + " t" +idx);
			}
			if(!lit.coversAllMaterializedTuples()){
				if(lit.getSense()){
					whereList.add("t" + idx + ".truth <> FALSE ");
					//					whereList.add("t" + idx + ".truth = " + 
					//							(lit.getSense()?"TRUE" : "FALSE"));
				}
			}

			//if(lit.getPred().hasSoftEvidence()){
			//	//priors.add("t"+idx+".prior");
			//	priors.add(
			//			"(CASE WHEN (t" + idx + ".prior) IS NULL THEN 0 " +
			//			"ELSE (" +
			//			"(CASE WHEN t" + idx + ".prior >=1 THEN " + Config.hard_weight +
			//			" WHEN t" + idx + ".prior<=0 THEN -" + Config.hard_weight +
			//			" ELSE ln(t" + idx + ".prior / (1-t" + idx+ ".prior)) END) "
			//			+ ")::FLOAT END)"		
			//
			//	);
			//	whereList.add("t" + idx + ".prior > 0.5");
			//}

			ArrayList<Term> terms = lit.getTerms();

			if(lit.getSense()){
				for(int j=0; j<terms.size(); j++) {
					Term t = terms.get(j);
					String var = t.var();

					var2type.put(var, lit.getPred().getTypeAt(j));

					String attr = "t" + idx + "."+lit.getPred().getArgs().get(j);
					if(t.isConstant()) {
						whereList.add(attr + "=" + SQLMan.escapeString(t.constantString()));
					}else {
						String cattr = mapVarAttr.get(var);
						if(cattr == null) {
							if(!lit.getPred().isCurrentlyView) indicesAssistor.put(attr, lit.getPred().getRelName()+"("+lit.getPred().getArgs().get(j)+")");
							mapVarAttr.put(var, attr);
						}else {
							if(!lit.getPred().isCurrentlyView) indices.add(lit.getPred().getRelName()+"("+lit.getPred().getArgs().get(j)+")");
							if(indicesAssistor.containsKey(cattr)) indices.add(indicesAssistor.get(cattr));
							whereList.add(attr + "=" + cattr);
						}
					}
				}
			}else{
				ArrayList<String> args = new ArrayList<String>();
				for(int j=0; j<terms.size(); j++) {
					Term t = terms.get(j);
					if(t.isConstant()){
						args.add(t.constantString());
					}else{
						String var = t.var();
						var2type.put(var, lit.getPred().getTypeAt(j));
						String cattr = mapVarAttr.get(var);
						if(cattr == null) {
							//	ExceptionMan.die("write negative predicate in the last\n" + toString());
						}else {
							args.add(cattr);
						}
					}
				}

				///////////////////////////////////////////////////TODO://////////////////////////////////////////////////
				//
				// [10.0]  tmp_predicate_1143(mid, eid) :-
				// mcoref(mid, mid1), 
				// Mention(mid1, sentID, docID, w1), 
				// EntityFeature_WikiTitlePartBefore(eid, w1), 
				// MentionFeature_TextLength(mid, len), 
				// MentionFeature_TextLength(mid1, len1), 
				// !mcoref(mid1, mid2), 
				// len <= len1
				//

				exceptList.add("(" + StringMan.commaList(args) + ") NOT IN (SELECT " + 
						StringMan.commaList(lit.getPred().getArgs()) + 
						" FROM " + lit.getPred().getRelName() + ")");
			}
		}

		///////////////////////////////////
		whereList.addAll(exceptList);

		for(int i=0;i<this.head.getPred().arity();i++){
			Term t = this.head.getTerms().get(i);
			Type type = this.head.getPred().getTypeAt(i);
			var2type.put(t.toString(), type);
		}


		// express constraints in SQL
		HashSet<String> cvars = new HashSet<String>();
		int nChangeName = 0;
		for(Expression e : constraints){
			for(String var : e.getVars()){
				if(var2type.get(var).isNonSymbolicType()){
					e.changeName = false;
				}
			}
			if(e.changeName == true){
				nChangeName ++;
			}
			cvars.addAll(e.getVars());
		}

		HashMap<String, String> mapVarVal = new HashMap<String, String>();
		HashMap<String, String> mapVarValNotChangeName = new HashMap<String, String>();
		int idx = 0;
		for(String v : cvars){
			++ idx;
			String attr = mapVarAttr.get(v);
			if(attr == null){
				ExceptionMan.die("unsafe constraints in conjunctive query\n" + toString());
			}
			if(nChangeName > 0){
				fromList.add(var2type.get(v).getRelName() + " s" + idx);
				whereList.add("s" + idx + ".constantid = " + attr);
			}
			mapVarVal.put(v, "s" + idx + ".constantvalue");
			mapVarValNotChangeName.put(v, attr);
		}
		for(Expression e : constraints){
			if(e.changeName == true){
				e.bindVariables(mapVarVal);
				whereList.add(e.toSQL());
			}else{
				Expression tmpE = e.clone();
				tmpE.renameVariables(mapVarValNotChangeName);
				/////////bug
				whereList.add(tmpE.toString());
			}
		}

		// select list
		for(int i=0; i<p.arity(); i++){
			Term t = head.getTerms().get(i);
			String v = null;
			if(t.isConstant()){
				v = t.constantString();
			}else{
				v = mapVarAttr.get(t.var());
				if(!p.getTypeAt(i).isNonSymbolicType()){
					if(indicesAssistor.containsKey(v)) indices.add(indicesAssistor.get(v));
				}
			}
			selList.add(v + (p.getTypeAt(i).isNonSymbolicType() ? 
					"::" + p.getTypeAt(i).getNonSymbolicTypeInSQL() : "") + 
					" AS " + p.getArgs().get(i) );
		}


		ArrayList<String> iargs = new ArrayList<String>();
		for(String a : p.getArgs()){
			iargs.add("nt." + a);
		}

		for(String s : additionalSel){
			if(s.equals("weight")){

				String expoExp = StringMan.join("+", priors);
				expoExp = "(2*(exp(" + expoExp + ")/(1+exp(" + expoExp +"))) - 1)";

				if(priors.size()==0){
					expoExp = "1";
				}

				selList.add((sourceClause.hasEmbeddedWeight()? 
						(this.inverseEmbededWeight == true? "-" : "") + mapVarAttr.get(sourceClause.getVarWeight()) : 
							this.weight) + "*" + expoExp + " AS weight ");
				iargs.add("nt.weight::float");
			}else if(s.equals("prov")){
				iargs.add(this.sourceClause.getId() + "::int as prov");
			}else if(s.equals("deepprov")){
				String deepprov = "";
				ArrayList<String> smalls = new ArrayList<String>();
				for(String var : mapVarAttr.keySet()){
					smalls.add("'" + var + ":' || CAST(" + mapVarAttr.get(var) + " AS TEXT)");  
				}
				smalls.add("''");
				deepprov = "ARRAY[" + StringMan.commaList(smalls) + "]::TEXT[] AS deepprov";
				selList.add(deepprov);
				iargs.add("nt.deepprov::TEXT[]");
			}
		}

		String sql, sub;

		sql = "";

		sub = "SELECT " + StringMan.commaList(selList) +
		(fromList.size() > 0 ? " FROM " + StringMan.commaList(fromList) : " ") +
		" WHERE " + SQLMan.andSelCond(whereList) ;

		//build index;
		if(Config.evidDBSchema == null || (forceBuild.length > 0 && forceBuild[0] == true)){
			for(String column : indices){
				if(indexBuilt.contains(column)){
					continue;
				}
				indexBuilt.add(column);

				String indexName = column.replaceAll("\\(|\\)", "") + Config.getNextGlobalCounter();
				//String indexName = column.replaceAll("\\(|\\)", "") ;
				String indexsql = "DROP INDEX IF EXISTS inair_" + indexName; 
				db.execute(indexsql);
				indexsql = "CREATE INDEX inair_" + indexName + " ON " + column;
				UIMan.verbose(3, indexsql);
				db.execute(indexsql);
			}
		}

		if(!Config.using_greenplum){
			for(String table : tables){
				db.analyze(table);
			}
		}

		//generates all the binding patterns
		int intBindingPattern = 0;	// 0->variable, 1->constant
		int nBindings = (int) Math.pow(2, p.arity());
		int lBindings = p.arity();

		for(int i=0;i<nBindings;i++){
			String bindingPattern = Integer.toBinaryString(intBindingPattern);
			intBindingPattern++;
			if(bindingPattern.length() < lBindings){
				char[] tmp = new char[lBindings - bindingPattern.length()];
				Arrays.fill(tmp, '0');
				bindingPattern = String.valueOf(tmp) + bindingPattern;
			}

			if(!bindingPattern.contains("1")){
				this.allFreeBinding = bindingPattern;
			}

			ArrayList<String> bindingWhere = new ArrayList<String>();

			for(int j=0;j<lBindings;j++){
				if(bindingPattern.charAt(j) == '1'){
					Term t = head.getTerms().get(j);
					if(t.isVariable()){
						String v = mapVarAttr.get(t.var());
						bindingWhere.add("( " + v + " = ? ) ");
					}
				}
			}

			String newsub = sub;
			if(bindingWhere.size() > 0){
				newsub = sub + " AND " + StringMan.join(" AND ", bindingWhere);
			}

			if(tableName == null){
				sql = "SELECT " + StringMan.commaList(iargs) +
				" FROM (" + newsub + ") nt";
			}else{

				String dropsql = "DROP TABLE IF EXISTS " + tableName;
				db.update(dropsql);
				String schemasql = "CREATE TABLE " + tableName + " AS SELECT * FROM " + p.getRelName() + " WHERE 1=2";
				db.update(schemasql);

				sql = "SELECT " + StringMan.commaList(iargs) +
				" FROM (" + newsub + ") nt";
			}

			//System.err.println(sql);
			PreparedStatement ps = db.getPrepareStatement(sql);
			this.psMap.put(bindingPattern, ps);
		}


	}

	public class StringSet{
		public String sql;
		public ArrayList<String> arg = new ArrayList<String>();
		public ArrayList<String> as = new ArrayList<String>();
		public ArrayList<String> prevas = new ArrayList<String>();
		public ArrayList<String> headas = new ArrayList<String>();
		public HashMap<String, String> arg2as = new HashMap<String, String>();
		public String classAs;
		public String classAsO;
		public Predicate headPred;
	}

}
