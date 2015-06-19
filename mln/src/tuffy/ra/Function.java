package tuffy.ra;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import tuffy.mln.Type;


/**
 * Bool, numberic, and string functions; user-defined functions.
 * 
 * @author Feng Niu
 *
 */

public class Function {
	public static HashMap<String, Function> builtInMap = 
		new HashMap<String, Function>();

	public static Function getBuiltInFunctionByName(String name){
		return builtInMap.get(name);
	}
	
	// atomic functions
	public static Function ConstantNumber = null;
	public static Function ConstantString = null;
	public static Function VariableBinding = null;

	static{
		ConstantNumber = new Function("_constNum", Type.Float);
		ConstantNumber.isBuiltIn_ = true;
		ConstantNumber.addArgument(Type.Float);
		builtInMap.put("_constNum", ConstantNumber);

		ConstantString = new Function("_constStr", Type.String);
		ConstantString.isBuiltIn_ = true;
		ConstantString.addArgument(Type.String);
		builtInMap.put("_constStr", ConstantString);

		VariableBinding = new Function("_var", Type.Generic);
		VariableBinding.isBuiltIn_ = true;
		VariableBinding.addArgument(Type.Generic);
		builtInMap.put("_var", VariableBinding);
	}
	
	
	
	// boolean functions
	public static Function NOT = null;
	public static Function OR = null;
	public static Function AND = null;
	
	public static Function Eq = null;
	public static Function Neq = null;
	public static Function LessThan = null;
	public static Function LessThanEq = null;
	public static Function GreaterThan = null;
	public static Function GreaterThanEq = null;
	
	public static Function StrContains = null;
	public static Function StrStartsWith = null;
	public static Function StrEndsWith = null;
	
	static{
		Function f = null;

		// logical operators
		
		f = new Function("NOT", Type.Bool);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Bool);
		builtInMap.put("NOT", f);
		builtInMap.put("!", f);
		NOT = f;

		f = new Function("OR", Type.Bool);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Bool);
		f.addArgument(Type.Bool);
		builtInMap.put("OR", f);
		f.isOperator_ = true;
		OR = f;

		f = new Function("AND", Type.Bool);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Bool);
		f.addArgument(Type.Bool);
		builtInMap.put("AND", f);
		f.isOperator_ = true;
		AND = f;
		
		
		// numeric bool functions
		
		f = new Function("_eq", Type.Bool);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Generic);
		f.addArgument(Type.Generic);
		builtInMap.put("_eq", f);
		builtInMap.put("=", f);
		f.isOperator_ = true;
		Eq = f;
		
		f = new Function("_neq", Type.Bool);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Generic);
		f.addArgument(Type.Generic);
		builtInMap.put("_neq", f);
		builtInMap.put("!=", f);
		builtInMap.put("<>", f);
		f.isOperator_ = true;
		Neq = f;

		f = new Function("_less", Type.Bool);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		f.addArgument(Type.Float);
		builtInMap.put("_less", f);
		builtInMap.put("<", f);
		f.isOperator_ = true;
		LessThan = f;

		f = new Function("_lessEq", Type.Bool);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		f.addArgument(Type.Float);
		builtInMap.put("_lessEq", f);
		builtInMap.put("<=", f);
		f.isOperator_ = true;
		LessThanEq = f;

		f = new Function("_greater", Type.Bool);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		f.addArgument(Type.Float);
		builtInMap.put("_greater", f);
		builtInMap.put(">", f);
		f.isOperator_ = true;
		GreaterThan = f;

		f = new Function("_greaterEq", Type.Bool);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		f.addArgument(Type.Float);
		builtInMap.put("_greaterEq", f);
		builtInMap.put(">=", f);
		f.isOperator_ = true;
		GreaterThanEq = f;
		
		// string bool functions

		f = new Function("contains", Type.Bool);
		f.isBuiltIn_ = true;
		f.addArgument(Type.String);
		f.addArgument(Type.String);
		builtInMap.put("contains", f);
		StrContains = f;

		f = new Function("startsWith", Type.Bool);
		f.isBuiltIn_ = true;
		f.addArgument(Type.String);
		f.addArgument(Type.String);
		builtInMap.put("startsWith", f);
		StrStartsWith = f;

		f = new Function("endsWith", Type.Bool);
		f.isBuiltIn_ = true;
		f.addArgument(Type.String);
		f.addArgument(Type.String);
		builtInMap.put("endsWith", f);
		StrEndsWith = f;
	}
	
	
	// math functions, unary
	public static Function Sign = null;
	public static Function Abs = null;
	public static Function Exp = null;
	public static Function Ceil = null;
	public static Function Floor = null;
	public static Function Trunc = null;
	public static Function Round = null;
	public static Function Ln = null; // base-e
	public static Function Lg = null; // base-10
	public static Function Sin = null;
	public static Function Cos = null;
	public static Function Tan = null;
	public static Function Sqrt = null;
	public static Function Factorial = null;
	
	static{
		Function f = null;
		
		f = new Function("sign", Type.Integer);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		builtInMap.put(f.name_, f);
		Sign = f;

		f = new Function("abs", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		builtInMap.put(f.name_, f);
		Abs = f;

		f = new Function("exp", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		builtInMap.put(f.name_, f);
		Exp = f;

		f = new Function("ceil", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		builtInMap.put(f.name_, f);
		builtInMap.put("ceiling", f);
		Ceil = f;

		f = new Function("floor", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		builtInMap.put(f.name_, f);
		Floor = f;

		f = new Function("trunc", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		builtInMap.put(f.name_, f);
		Trunc = f;

		f = new Function("round", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		builtInMap.put(f.name_, f);
		Round = f;

		f = new Function("ln", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		builtInMap.put(f.name_, f);
		Ln = f;

		f = new Function("lg", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		builtInMap.put(f.name_, f);
		f.setPgFunction("log");
		Lg = f;

		f = new Function("sin", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		builtInMap.put(f.name_, f);
		Sin = f;

		f = new Function("cos", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		builtInMap.put(f.name_, f);
		Cos = f;

		f = new Function("tan", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		builtInMap.put(f.name_, f);
		Tan = f;

		f = new Function("sqrt", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		builtInMap.put(f.name_, f);
		Sqrt = f;

		f = new Function("factorial", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		builtInMap.put(f.name_, f);
		Factorial = f;
		
	}
	
	
	// math functions, binary
	public static Function Add = null;
	public static Function Subtract = null;
	public static Function Multiply = null;
	public static Function Divide = null;
	public static Function Modulo = null;
	public static Function Power = null;
	public static Function Log = null;
	public static Function BitAnd = null;
	public static Function BitOr = null;
	public static Function BitXor = null;
	public static Function BitNeg = null;
	public static Function BitShiftLeft = null;
	public static Function BitShiftRight = null;
	
	static{
		Function f = null;
		f = new Function("add", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		f.addArgument(Type.Float);
		builtInMap.put("add", f);
		builtInMap.put("+", f);
		f.isOperator_ = true;
		Add = f;

		f = new Function("subtract", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		f.addArgument(Type.Float);
		builtInMap.put("subtract", f);
		builtInMap.put("-", f);
		f.isOperator_ = true;
		Subtract = f;

		f = new Function("multiply", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		f.addArgument(Type.Float);
		builtInMap.put("multiply", f);
		builtInMap.put("*", f);
		f.isOperator_ = true;
		Multiply = f;

		f = new Function("divide", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		f.addArgument(Type.Float);
		builtInMap.put("divide", f);
		builtInMap.put("/", f);
		f.isOperator_ = true;
		Divide = f;

		f = new Function("mod", Type.Integer);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Integer);
		f.addArgument(Type.Integer);
		builtInMap.put("mod", f);
		builtInMap.put("%", f);
		f.isOperator_ = true;
		Modulo = f;

		f = new Function("pow", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		f.addArgument(Type.Float);
		builtInMap.put("pow", f);
		f.isOperator_ = false;
		Power = f;

		f = new Function("log", Type.Float);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Float);
		f.addArgument(Type.Float);
		builtInMap.put("log", f);
		f.isOperator_ = false;
		Log = f;

		f = new Function("bitand", Type.Integer);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Integer);
		f.addArgument(Type.Integer);
		builtInMap.put(f.name_, f);
		builtInMap.put("&", f);
		f.isOperator_ = true;
		BitAnd = f;

		f = new Function("bitor", Type.Integer);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Integer);
		f.addArgument(Type.Integer);
		builtInMap.put(f.name_, f);
		builtInMap.put("|", f);
		f.isOperator_ = true;
		BitOr = f;

		f = new Function("bitxor", Type.Integer);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Integer);
		f.addArgument(Type.Integer);
		builtInMap.put(f.name_, f);
		builtInMap.put("^", f);
		f.isOperator_ = true;
		BitXor = f;

		f = new Function("bitneg", Type.Integer);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Integer);
		f.addArgument(Type.Integer);
		builtInMap.put(f.name_, f);
		builtInMap.put("~", f);
		f.isOperator_ = true;
		BitNeg = f;

		f = new Function("bitShiftLeft", Type.Integer);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Integer);
		f.addArgument(Type.Integer);
		builtInMap.put(f.name_, f);
		builtInMap.put("<<", f);
		f.isOperator_ = true;
		BitShiftLeft = f;

		f = new Function("bitShiftRight", Type.Integer);
		f.isBuiltIn_ = true;
		f.addArgument(Type.Integer);
		f.addArgument(Type.Integer);
		builtInMap.put(f.name_, f);
		builtInMap.put(">>", f);
		f.isOperator_ = true;
		BitShiftRight = f;
		
	}
	
	
	
	// string functions, unary
	public static Function Length = null;
	public static Function UpperCase = null;
	public static Function LowerCase = null;
	public static Function Trim = null;
	public static Function InitCap = null;
	public static Function MD5 = null;

	static{
		Function f = null;
		
		f = new Function("length", Type.Integer);
		f.isBuiltIn_ = true;
		f.addArgument(Type.String);
		builtInMap.put("length", f);
		builtInMap.put("strlen", f);
		builtInMap.put("len", f);
		Length = f;

		f = new Function("upper", Type.String);
		f.isBuiltIn_ = true;
		f.addArgument(Type.String);
		builtInMap.put(f.name_, f);
		UpperCase = f;

		f = new Function("lower", Type.String);
		f.isBuiltIn_ = true;
		f.addArgument(Type.String);
		builtInMap.put(f.name_, f);
		LowerCase = f;

		f = new Function("trim", Type.String);
		f.isBuiltIn_ = true;
		f.addArgument(Type.String);
		builtInMap.put(f.name_, f);
		Trim = f;

		f = new Function("initcap", Type.String);
		f.isBuiltIn_ = true;
		f.addArgument(Type.String);
		builtInMap.put(f.name_, f);
		InitCap = f;

		f = new Function("md5", Type.String);
		f.isBuiltIn_ = true;
		f.addArgument(Type.String);
		builtInMap.put(f.name_, f);
		MD5 = f;
	}
	
	
	
	// string functions, binary
	public static Function Concat = null;
	public static Function StrPos = null;
	public static Function Repeat = null;

	static{
		Function f = null;
		
		f = new Function("concat", Type.String);
		f.isBuiltIn_ = true;
		f.addArgument(Type.String);
		f.addArgument(Type.String);
		builtInMap.put("concat", f);
		builtInMap.put("||", f);
		Concat = f;

		f = new Function("strpos", Type.Integer);
		f.isBuiltIn_ = true;
		f.addArgument(Type.String);
		f.addArgument(Type.String);
		builtInMap.put(f.name_, f);
		StrPos = f;

		f = new Function("repeat", Type.String);
		f.isBuiltIn_ = true;
		f.addArgument(Type.String);
		f.addArgument(Type.Integer);
		builtInMap.put(f.name_, f);
		Repeat = f;
		
	}
	
	
	
	// string functions, ternary
	public static Function Substr = null;
	public static Function Replace = null;
	public static Function SplitPart = null;
	public static Function RegexReplace = null;
	
	static{
		Function f = null;
		
		f = new Function("substr", Type.String);
		f.isBuiltIn_ = true;
		f.addArgument(Type.String);
		f.addArgument(Type.Integer);
		f.addArgument(Type.Integer);
		builtInMap.put(f.name_, f);
		Substr = f;

		f = new Function("replace", Type.String);
		f.isBuiltIn_ = true;
		f.addArgument(Type.String);
		f.addArgument(Type.String);
		f.addArgument(Type.String);
		builtInMap.put(f.name_, f);
		Replace = f;

		f = new Function("split_part", Type.String);
		f.isBuiltIn_ = true;
		f.addArgument(Type.String);
		f.addArgument(Type.String);
		f.addArgument(Type.Integer);
		builtInMap.put(f.name_, f);
		SplitPart = f;

		f = new Function("regex_replace", Type.String);
		f.isBuiltIn_ = true;
		f.addArgument(Type.String);
		f.addArgument(Type.String);
		f.addArgument(Type.String);
		f.setPgFunction("regexp_replace");
		builtInMap.put(f.name_, f);
		RegexReplace = f;
		
	}
	

	
	/**
	 * Name of this function.
	 */
	private String name_;
	private String pgfun_ = null;
	
	private boolean isOperator_ = false;
	public boolean isOperator(){
		return isOperator_;
	}
	
	/**
	 * List of argument types and return type of this function. 
	 */
	private ArrayList<Type> argTypes_ = new ArrayList<Type>();
	private Type retType_ = null;
	
	private boolean isBuiltIn_ = false;
	
	public boolean isBuiltIn(){
		return isBuiltIn_;
	}
	
	/**
	 * Get the corresponding function name inside PgSQL.
	 * 
	 */
	public String getPgFunction(){
		return pgfun_;
	}

	/**
	 * Set the corresponding function name inside PgSQL.
	 * 
	 */
	public void setPgFunction(String fun){
		pgfun_ = fun;
	}
	
	public Function(String name, Type retType){
		name_ = name;
		retType_ = retType;
		pgfun_ = name_;
	}
	
	public void addArgument(Type type){
		argTypes_.add(type);
	}
	
	public int arity(){
		return argTypes_.size();
	}
	
	public String getName(){
		return name_;
	}
	
	/**
	 * Get return type
	 * 
	 */
	public Type getRetType(){
		return retType_;
	}
	
	public List<Type> getArgTypes(){
		return argTypes_;
	}
}
