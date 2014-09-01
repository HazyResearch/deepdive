package tuffy.ra;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;



import tuffy.db.SQLMan;
import tuffy.mln.Type;
import tuffy.util.ExceptionMan;
import tuffy.util.StringMan;

/**
 * An expression to a function is like a literal to a predicate.
 * The interesting part is that expressions can be nested.
 * The value of an expression can be numeric/string/boolean.
 * 
 * @author Feng Niu
 *
 */
final public class Expression implements Cloneable{
	private Function func_ = null;
	private ArrayList<Expression> args_ = new ArrayList<Expression>();
	private String val = null;
	private String valBinding = null;
	
	// TODO(ce) What's changeName for?
	public boolean changeName = true;
	
	public ArrayList<Expression> getArgs(){
		return args_;
	}
	
	public Function getFunction(){
		return func_;
	}
	
	/**
	 * Get the variables referenced by this expression.
	 * 
	 */
	public HashSet<String> getVars(){
		HashSet<String> set = new HashSet<String>();
		if(func_ == Function.VariableBinding){
			set.add(val);
		}else{
			for(Expression e : args_){
				set.addAll(e.getVars());
			}
		}
		return set;
	}
	
	/**
	 * For aesthetics, check if we need a pair of parenthses for this experession.
	 * 
	 */
	private boolean needEnclosure(){
		return func_.isOperator();
	}
	
	/**
	 * Construct a new expression based on the function {@link #func_}
	 * 
	 * @param func the underlying function
	 */
	public Expression(Function func){
		func_ = func;
	}
	
	/**
	 * Boolean negation
	 * @param e
	 * 
	 */
	public static Expression not(Expression e){
		if(e.func_.equals(Function.NOT)){
			return e.args_.get(0);
		}
		Expression ne = new Expression(Function.NOT);
		ne.addArgument(e);
		return ne;
	}

	/**
	 * Boolean AND
	 * @param e1
	 * @param e2
	 * 
	 */
	public static Expression and(Expression e1, Expression e2){
		Expression ne = new Expression(Function.AND);
		ne.addArgument(e1);
		ne.addArgument(e2);
		return ne;
	}

	/**
	 * Boolean OR
	 * @param e1
	 * @param e2
	 * 
	 */
	public static Expression or(Expression e1, Expression e2){
		Expression ne = new Expression(Function.OR);
		ne.addArgument(e1);
		ne.addArgument(e2);
		return ne;
	}
	
	/**
	 * Test if this expression returns a boolean value
	 * 
	 */
	public boolean isBoolean(){
		return func_.getRetType() == Type.Bool;
	}

	/**
	 * Test if this expression returns a string value
	 * 
	 */
	public boolean isString(){
		return func_.getRetType() == Type.String;
	}
	

	/**
	 * Test if this expression returns a numeric value
	 * 
	 */
	public boolean isNumeric(){
		return func_.getRetType() == Type.Integer
		|| func_.getRetType() == Type.Float;
	}
	
	/**
	 * Append an argument to the underlying function
	 * @param expr the new argument
	 */
	public void addArgument(Expression expr){
		if(args_.size() >= func_.arity()){
			ExceptionMan.die("Function " + func_.getName() + 
					" expected " + func_.arity() + " arguments, but " +
					"received more");
		}else{
			args_.add(expr);
		}
	}
	
	/**
	 * Atomic expression representing a constant integer
	 * @param n
	 * 
	 */
	public static Expression exprConstInteger(int n){
		Expression ex = new Expression(Function.ConstantNumber);
		ex.val = Integer.toString(n);
		return ex;
	}

	/**
	 * Atomic expression representing a constant number
	 * 
	 */
	public static Expression exprConstNum(double num){
		Expression ex = new Expression(Function.ConstantNumber);
		ex.val = Double.toString(num);
		return ex;
	}

	/**
	 * Atomic expression representing a constant string
	 * 
	 */
	public static Expression exprConstString(String str){
		Expression ex = new Expression(Function.ConstantString);
		ex.val = str;
		return ex;
	}

	/**
	 * Atomic expression representing a variable binding
	 * 
	 */
	public static Expression exprVariableBinding(String var){
		Expression ex = new Expression(Function.VariableBinding);
		ex.val = var;
		return ex;
	}

	
	@SuppressWarnings("unchecked")
	public Expression clone(){
		Expression ret;
		
		if(this.val != null){
			ret = new Expression(this.func_);
			ret.val = this.val;
			ret.valBinding = this.valBinding;
			ret.args_ = (ArrayList<Expression>) this.args_.clone();
			return ret;
		}
		
		ret = new Expression(this.func_);
		for(Expression sub : this.args_){
			ret.addArgument(sub.clone());
		}
		ret.val = this.val;
		ret.valBinding = this.valBinding;
		ret.changeName = this.changeName;
		
		return ret;
	}
	
	/**
	 * Bind variable references to their values in the symbol table.
	 * Variable name in the clause -> attribute name in SQL representing
	 * the value column of the symbol table.
	 * 
	 * @param mapVarVar
	 */
	public String renameVariables(Map<String, String> mapVarVar){
		String es = null;
		if(func_ == Function.VariableBinding){
			String nvar = mapVarVar.get(val);
			if(nvar == null){
				return val;
			}else{
				val = nvar;
			}
		}else{
			for(Expression e : args_){
				String aaa = e.renameVariables(mapVarVar);
				if(aaa != null){
					return aaa;
				}
			}
		}
		return es;
	}
	
	
	/**
	 * Bind variable references to their values in the symbol table.
	 * Variable name in the clause -> attribute name in SQL representing
	 * the value column of the symbol table.
	 * 
	 * @param mapVarVal
	 */
	public void bindVariables(Map<String, String> mapVarVal){
		if(func_ == Function.VariableBinding){
			String attr = mapVarVal.get(val);
			if(attr == null){
				ExceptionMan.die("Encountered a dangling variable: " + val);
			}else{
				valBinding = attr;
			}
		}else{
			for(Expression e : args_){
				e.bindVariables(mapVarVal);
			}
		}
	}
	
	/**
	 * Get the SQL snippet for this expression
	 * 
	 */
	public String toSQL(){
		return getStringForm(true);
	}
	
	/**
	 * Compute the SQL representation of this expression.
	 * Call {@link #bindVariables(Map)} first.
	 * 
	 */
	private String getStringForm(boolean inSQL){
		// atomic
		if(func_ == Function.ConstantNumber){
			return val;
		}
		if(func_ == Function.VariableBinding){
			return inSQL ? valBinding : val;
		}
		if(func_ == Function.ConstantString){
			return inSQL ? SQLMan.escapeString(val) : 
				"\"" + StringMan.escapeJavaString(val) + "\"";
		}
		
		// cast argument into correct types
		ArrayList<String> sterms = new ArrayList<String>();
		
		if(inSQL){
			for(int i=0; i < func_.arity(); i++){
				Type t = func_.getArgTypes().get(i);
				String s = args_.get(i).toSQL();
				if(t == Type.Integer){
					s = "(CASE WHEN (" + s + ") IS NULL THEN NULL " +
							"ELSE (" + s + ")::INT END)";
				}else if(t == Type.Float){
					s = "(CASE WHEN (" + s + ") IS NULL THEN NULL " +
					"ELSE (" + s + ")::FLOAT END)";
				}else if(t == Type.String){
					s = "CAST(" + s + " AS TEXT)";
				}else if(t == Type.Bool){
					s = "(CASE WHEN (" + s + ") IS NULL THEN NULL " +
					"ELSE (" + s + ")::BOOL END)";
				}else{
					s= "(" + s + ")";
				}
				
				if(func_ == Function.Eq || func_ == Function.Neq){
					
					//TODO: NEQ'S BUG!!!! ENRON680_TUFFY
				//	if(func_.getArgTypes().get(0) != func_.getArgTypes().get(1)){
						sterms.add(" CAST(" + s + " AS TEXT) ");
				//	}else{
				//		sterms.add(s);
				//	}
					
				}else{
					sterms.add(s);
				}
			}
		}else{
			for(int i=0; i < func_.arity(); i++){
				String s = args_.get(i).toString();
				if(args_.get(i).needEnclosure()){
					s = "(" + s + ")";
				}
				sterms.add(s);
			}
		}
		
		// map to operators
		if(func_ == Function.BitNeg){
			return "~" + sterms.get(0);
		}

		if(func_ == Function.Factorial){
			return sterms.get(0) + "!";
		}
		
		if(func_ == Function.NOT){
			return "NOT " + sterms.get(0) + "";
		}

		if(func_ == Function.AND){
			return sterms.get(0) + " AND " + sterms.get(1);
		}

		if(func_ == Function.OR){
			return sterms.get(0) + " OR " + sterms.get(1);
		}
		
		if(func_ == Function.Add){
			return sterms.get(0) + " + " + sterms.get(1);
		}
		
		if(func_ == Function.Subtract){
			return sterms.get(0) + " - " + sterms.get(1);
		}

		if(func_ == Function.Multiply){
			return sterms.get(0) + " * " + sterms.get(1);
		}

		if(func_ == Function.Divide){
			return sterms.get(0) + " / " + sterms.get(1);
		}
		
		if(func_ == Function.Modulo){
			return sterms.get(0) + " % " + sterms.get(1);
		}

		if(func_ == Function.BitAnd){
			return sterms.get(0) + " & " + sterms.get(1);
		}

		if(func_ == Function.BitOr){
			return sterms.get(0) + " | " + sterms.get(1);
		}

		if(func_ == Function.BitXor){
			return sterms.get(0) + (inSQL ? " # " : " ^ ") + sterms.get(1);
		}

		if(func_ == Function.BitShiftLeft){
			return sterms.get(0) + " << " + sterms.get(1);
		}

		if(func_ == Function.BitShiftRight){
			return sterms.get(0) + " >> " + sterms.get(1);
		}

		if(func_ == Function.Concat){
			return sterms.get(0) + " || " + sterms.get(1);
		}
		
		if(func_ == Function.Eq){
			//System.err.println(sterms.get(0) + " = " + sterms.get(1));
			return sterms.get(0) + " = " + sterms.get(1);
		}
		
		if(func_ == Function.Neq){
			return sterms.get(0) + " <> " + sterms.get(1);
		}
		
		if(func_ == Function.GreaterThan){
			return sterms.get(0) + " > " + sterms.get(1);
		}
		
		if(func_ == Function.GreaterThanEq){
			return sterms.get(0) + " >= " + sterms.get(1);
		}
		
		if(func_ == Function.LessThan){
			return sterms.get(0) + " < " + sterms.get(1);
		}
		
		if(func_ == Function.LessThanEq){
			return sterms.get(0) + " <= " + sterms.get(1);
		}
		
		// map to complex expressions
		if(func_ == Function.StrContains){
			return "strpos(" + sterms.get(0) + ", " + sterms.get(1) + ") > 0";
		}

		if(func_ == Function.StrStartsWith){
			return "strpos(" + sterms.get(0) + ", " + sterms.get(1) + ") = 1";
		}

		if(func_ == Function.StrEndsWith){
			return "substr(" + sterms.get(0) + ", length(" + sterms.get(0) + 
				") - length(" + sterms.get(1) + ") + 1) = " + sterms.get(1);
		}
		
		// map to direct functions
		StringBuilder sb = new StringBuilder();
		sb.append((inSQL ? func_.getPgFunction() : func_.getName()) + "(");
		for(int i=0; i<func_.arity(); i++){
			sb.append(sterms.get(i));
			if(i != func_.arity() - 1){
				sb.append(", ");
			}
		}
		sb.append(")");
		
		
		return sb.toString();
	}
	
	public String toString(){
		return getStringForm(false);
	}
}
