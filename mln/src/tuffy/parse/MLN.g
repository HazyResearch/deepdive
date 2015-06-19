grammar MLN;
options {language=Java; output=AST; backtrack=true; memoize=true;}

@lexer::header {
package tuffy.parse;
}

@parser::header{
package tuffy.parse;
import tuffy.mln.*;
import tuffy.ra.*;
import tuffy.util.*;
}

@parser::members {
    MarkovLogicNetwork ml;
    private String clauseName = null;
    private boolean clauseLabelTrailing = false;
    private Predicate curPred = null;
    
    public long lineOffset = 0;
    
    private void die(String msg){
        ExceptionMan.die(msg);
    }
    
    protected Object recoverFromMismatchedToken(IntStream input,
                                            int ttype,
                                            BitSet follow)
		    throws RecognitionException
		{   
		    throw new MismatchedTokenException(ttype, input);
		}   
		
		public void emitErrorMessage(String msg) {
        die(msg);
    }
		
    
}

WS  :   ( ' '
        | '\t'
        | '\r'
        | '\n'
        ) {$channel=HIDDEN;}
    ;
  
COMMENT
    :   '//' ~('\n'|'\r')* '\r'? '\n' {$channel=HIDDEN;}
    |   '/*' ( options {greedy=false;} : . )* '*/' {$channel=HIDDEN;}
    ;

NOT 	:	'!';
PLUS   : '+';
MINUS   : '-';
ASTERISK:	'*';
PERIOD: '.';
EXIST	:	'EXIST' | 'Exist' | 'exist';
IMPLIES : '=>';

STRING
@init{StringBuilder lBuf = new StringBuilder();}
    :   
   '"'
   ( escaped=ESC {lBuf.append(getText());} | 
     normal=~('"'|'\\'|'\n'|'\r') {lBuf.appendCodePoint(normal);} )* 
   '"'
   {setText(lBuf.toString());}
    ;
    
fragment
ESC
    :   '\\'
        (       'n'    {setText("\n");}
        |       'r'    {setText("\r");}
        |       't'    {setText("\t");}
        |       'b'    {setText("\b");}
        |       'f'    {setText("\f");}
        |       '"'    {setText("\"");}
        |       '\''   {setText("\'");}
        |       '/'    {setText("/");}
        |       '\\'   {setText("\\");}
        | 'u' i=HEXDIGIT j=HEXDIGIT k=HEXDIGIT l=HEXDIGIT
         {
           String num = $i.getText() + $j.getText() + $k.getText() + $l.getText();
           char[] realc = new char[1];
           realc[0] = (char) Integer.valueOf(num, 16).intValue();
           setText(new String(realc));
         }
        | ~('u'|'r'|'n'|'t'|'b'|'f'|'"'|'\''|'/'|'\\')
        )
    ;
    
fragment 
HEXDIGIT
  : '0'..'9' | 'A'..'F' | 'a'..'f'
  ;
  
NUMBER :  INTEGER | FLOAT;
fragment
INTEGER : '0' | ('+'|'-')? '1'..'9' '0'..'9'*;
fragment 
EXPONENT : ('e'|'E') ('+'|'-')? ('0'..'9')+ ;
fragment 
FLOAT
    :   ('+'|'-')? ('0'..'9')+ '.' ('0'..'9')* EXPONENT?
    |   '.' ('0'..'9')+ EXPONENT?
    |   ('0'..'9')+ EXPONENT
    ;
  
ID
  :   ('a'..'z'|'A'..'Z')('a'..'z'|'A'..'Z'|'0'..'9'|'_'|'-')*
  ;
  
    
    
    
    
    
definitions : schemaList ruleList EOF;

schemaList : (schema | schemaConstraint)*;

schema : (du='+')? (aa='**'|a1=ASTERISK)? (vt='@')? pname=ID a2=ASTERISK?
	{
		//boolean cwa = ($a1 != null || $aa != null);
		boolean cwa = ($a1 != null);
    boolean eqrel = ($a2 != null);
		Predicate pred = new Predicate(ml, $pname.text, cwa);
		if($aa != null) pred.setCompeletelySpecified(true);
		//if(eqrel) pred.isDedupalogHead = true;
		if($vt != null){
		    //if($vt.text.equals("@")) pred.isInMem = true;
		}
    if($du != null){
        //if($du.text.equals("+")) pred.toDump = true;
    }
		curPred = pred;
	}
	'(' types+=predArg (',' types+=predArg)* ')'
	{
	    curPred.sealDefinition();
      ml.registerPred(curPred);
	}
	;

predArg : type=ID (name=ID)? uni='!'?
  {
    Type t = ml.getOrCreateTypeByName($type.text);
    String nm = null;
    if($name != null) nm = $name.text;
    curPred.appendArgument(t, nm);
    if($uni != null){
      curPred.addDependentAttrPosition(curPred.arity() - 1);
    }
  }
  ;

schemaConstraint :
  functionalDependency;
  
functionalDependency :
  ('FUNCTIONAL DEPENDENCY' | 'FD') ':'
  functionalDependencyItem
  (';' functionalDependencyItem)*
  {
    UIMan.warn("The feature of functional dependency " +
      "is still in development; use at your own risk.");
  }
  ;

functionalDependencyItem:
  pa+=ID (',' pa+=ID)* '->' sa=ID
  {
      ArrayList<Token> det = (ArrayList<Token>)($pa);
      ArrayList<String> dets = new ArrayList<String>();
      for(Token t : det){
          String s = t.getText();
          dets.add(s);
      }
      String dep = sa.getText();
      curPred.addFunctionalDependency(dets, dep);
  }
  ;



ruleList : (mlnRule | scopingRule | datalogRule)* 
    ;

mlnRule
    :
    (
	    tag=('[Label]'|'[+Label]'|'[Label+]') STRING
	    {
	       clauseName = $STRING.getText();
	       String t = $tag.text;
	       if(t != null && t.contains("+")){
	          clauseLabelTrailing = true;
	       }else{
            clauseLabelTrailing = false;
	       }
	    }
    )?
    (softRule | hardRule)
    ;

softRule returns [Clause c]
    :   (du='@')? weight=NUMBER fc=foclause
    {
    $c = $fc.c; 
    $c.setWeight(Double.parseDouble($weight.text));
    ml.registerClause($c);
    $c.addSpecText($text);
      if(du != null){
          $c.isFixedWeight = true;
      }else{
          $c.isFixedWeight = false;
      }
    }
    |
    (du='@')? warg=ID ':' fc=foclause
    {
    $c = $fc.c; 
    $c.setWeight(1);
    $c.setVarWeight($warg.text);
    ml.registerClause($c);
    $c.addSpecText($text);
      if(du != null){
          $c.isFixedWeight = true;
      }else{
          $c.isFixedWeight = false;
      }
    }
    ;



hardRule returns [Clause c]
    :   fc=foclause PERIOD
    {
    $c = $fc.c; 
    $c.setHardWeight();
    ml.registerClause($c);
    $c.addSpecText($text);
    } 
    ;


functionalAtom returns [AtomEx cond]
    :
    func=ID 
    {
      Predicate p = ml.getPredByName($func.text);
      if(p==null) die("Line #" + $func.getLine() + ": undefined predicate: " + $func.text);
      $cond = new AtomEx(p);
    }
    '(' 
    e1=expression 
    {$cond.appendTerm($e1.expr);}
      (',' ep=expression
      {$cond.appendTerm($ep.expr);}
      )*
    ')'
    ;

datalogRule returns [ConjunctiveQuery cq = new ConjunctiveQuery();]
    :   (vt=('@'|'#'))? (st='$')? head=literal {$cq.setHead($head.lit);} ':-'
        body0=literal {
          if($body0.lit!=null){
            $cq.addBodyLit($body0.lit);
          }
        }
        (',' bodyp=literal {
          if($bodyp.lit!=null){
            $cq.addBodyLit($bodyp.lit);
          }
        })*
        (',' mc=mathComparison {
          $cq.addConstraint($mc.expr);
        })*
       (',' '['
           be=boolExpression {
            $cq.addConstraint($be.be);
           }
       ']')?
     PERIOD 
     {
        if($st != null){
            if($st.text.equals("$")) $cq.isView = true;
        }
		    if($vt != null){
		        if($vt.text.equals("@")) ml.registerPostprocRule($cq);
            else if($vt.text.equals("#")){
               // ml.registerDatalogRule($cq);
               ml.registerIntermediateRule($cq);
            }
		    }else{
		        ml.registerDatalogRule($cq);
		    }
     }
     ;

scopingRule returns [ConjunctiveQuery cq = new ConjunctiveQuery();]
    :   ('[' 'priorProb' '=' prior=NUMBER ']')? head=literal {$cq.setHead($head.lit);} ':='
        body0=literal {
          if($body0.lit!=null){
            $cq.addBodyLit($body0.lit);
          }
        }
        (',' bodyp=literal {
          if($bodyp.lit!=null){
            $cq.addBodyLit($bodyp.lit);
          }
        })*
        (',' mc=mathComparison {
          $cq.addConstraint($mc.expr);
        })*
       (',' '['
           be=boolExpression {
            $cq.addConstraint($be.be);
           }
       ']')?
     PERIOD 
     {
      if($prior != null){
        double pr = Double.parseDouble($prior.text);
        if(pr > 1 || pr < 0){
            die("Line #" + (lineOffset+$prior.getLine()) + ": " + $prior.text + " - probabilities of soft evidence should be in [0,1]");
        }else{
          $cq.setNewTuplePrior(pr);
        }
      }
     ml.registerScopingRule($cq);
     }
     ;

/****************
clusteringRule returns [ConjunctiveQuery cq = new ConjunctiveQuery();]
    :   weight=NUMBER? (et='~')?  (st='*')? (vt='$')? 
        head=literal {$cq.setHead($head.lit);} 
        conn=('<='|'<-'|'<->'|'<@'|'<~'|'<!-'|'<CLASS-'|'<TAG-'|'<@-'|'<@='|'<@~')
        body0=literal {
          if($body0.lit!=null){
            $cq.addBodyLit($body0.lit);
          }
        } 
        (',' bodyp=literal {
          if($bodyp.lit!=null){
            $cq.addBodyLit($bodyp.lit);
          }
        })*
        (',' mc=mathComparison {
          $cq.addConstraint($mc.expr);
        })*
       (',' '['
           be=boolExpression {
            $cq.addConstraint($be.be);
           }
       ']')?
     PERIOD 
     {
	     if($conn.text.equals("<=")){
	        $cq.type = ConjunctiveQuery.CLUSTERING_RULE_TYPE.HARD;
	     }else if($conn.text.equals("<-")){
	        $cq.type = ConjunctiveQuery.CLUSTERING_RULE_TYPE.SOFT_INCOMPLETE;
	     }else if($conn.text.equals("<->")){
	        $cq.type = ConjunctiveQuery.CLUSTERING_RULE_TYPE.SOFT_COMPLETE;
	     }else if($conn.text.equals("<@~")){
          $cq.type = ConjunctiveQuery.CLUSTERING_RULE_TYPE.COULD_LINK_CLIQUE;
       }else if($conn.text.equals("<@=")){
          $cq.type = ConjunctiveQuery.CLUSTERING_RULE_TYPE.MUST_LINK_CLIQUE;
       }else if($conn.text.equals("<@-")){
          $cq.type = ConjunctiveQuery.CLUSTERING_RULE_TYPE.WOULD_LINK_CLIQUE;
       }else if($conn.text.equals("<~")){
          $cq.type = ConjunctiveQuery.CLUSTERING_RULE_TYPE.COULD_LINK_PAIRWISE;
       }else if($conn.text.equals("<!-")){
          $cq.type = ConjunctiveQuery.CLUSTERING_RULE_TYPE.NODE_LIST;
       }else if($conn.text.equals("<CLASS-")){
          $cq.type = ConjunctiveQuery.CLUSTERING_RULE_TYPE.NODE_CLASS;
       }else if($conn.text.equals("<TAG-")){
          $cq.type = ConjunctiveQuery.CLUSTERING_RULE_TYPE.CLASS_TAGS;
       }
       if($weight.text != null){
        $cq.setWeight(Double.parseDouble($weight.text));
       }
      
	    if($vt != null){
	        if($vt.text.equals("$")) $cq.isView = true;
	    }
	    
      if($st != null){
          if($st.text.equals("*")) $cq.isStatic = true;
      }
	    
      if($et != null){
          if($et.text.equals("~")) $cq.isFictitious = true;
      }
      
	     ml.registerClusteringRule($cq);
     }
     ;
****/

foclause returns [Clause c = new Clause()]
    :   exq=existQuan?  
     (
        ant0=literal {
          if($ant0.lit!=null){
            $ant0.lit.flipSense(); 
            $c.addLiteral($ant0.lit);
          }
        }
        (',' antp=literal {
          if($antp.lit!=null){
            $antp.lit.flipSense(); 
            $c.addLiteral($antp.lit);
          }
        })*
        (',' mc=mathComparison {
          $c.addConstraint($mc.expr);
        })*
	     (',' '['
	         be=boolExpression {
	          $c.addConstraint($be.be);
	         }
	     ']')?
        
        IMPLIES
     )?
      lit0=literal {$c.addLiteral($lit0.lit);}
     ('v' litp=literal {$c.addLiteral($litp.lit);})*
        ('v' mc=mathComparison {
          $c.addConstraint(Expression.not($mc.expr));
        })*
     
     ('v' '['
         sbe=boolExpression {
          $c.addConstraint(Expression.not($sbe.be));
         }
     ']')?
    {
      if(exq != null){
         for(String v : exq.vars){
           $c.addExistentialVariable(v);
         }
      }
      $c.addUserProvidedName(clauseName);
      if(!clauseLabelTrailing) clauseName = null;
    }
    ;

existQuan returns [ArrayList<String> vars = new ArrayList<String>()]
    :   EXIST
        v0=ID {$vars.add($v0.text);}
        (',' vp=ID {$vars.add($vp.text);})*;


expression returns [Expression expr]
    :
    be=mathExpression
    ;

boolExpression returns [Expression be]
    :
    c0=boolConjunction
    {
        $be = $c0.be;
    }
    (
        ('||' | 'OR') cp=boolConjunction
        {
	        Expression enew = new Expression(Function.OR);
	        enew.addArgument($be);
	        enew.addArgument($cp.be);
	        $be = enew;
        }
    )*
    ;

boolConjunction returns [Expression be]
    :
    c0=boolConjunctionElement
    {
        $be = $c0.be;
    }
    (
        ('&&' | 'AND') cp=boolConjunctionElement
        {
          Expression enew = new Expression(Function.AND);
          enew.addArgument($be);
          enew.addArgument($cp.be);
          $be = enew;
        }
    )*
    ;

boolConjunctionElement returns [Expression be]
    :
    negc=('!' | 'NOT')? 
      (
        mc=mathComparison{$be=$mc.expr;}
      | fe=funcExpression{$be=$fe.expr;}
      )
    {
        if($negc != null){
          Expression enew = new Expression(Function.NOT);
          enew.addArgument($be);
          $be = enew;
        }
    }
    | negb=('!' | 'NOT')? '(' b=boolExpression ')'
    {
        $be = $b.be;
        if($negb != null){
          Expression enew = new Expression(Function.NOT);
          enew.addArgument($be);
          $be = enew;
        }
    }
    ;


mathComparison returns [Expression expr]
    :
    e1=mathExpression op=('='|'<>'|'<'|'<='|'>'|'>='|'!=') e2=mathExpression
    {
        Function f = Function.getBuiltInFunctionByName($op.text);
        $expr = new Expression(f);
        $expr.addArgument($e1.expr);
        $expr.addArgument($e2.expr);
    }
    ;


mathExpression returns [Expression expr]
    :
    t0=mathTerm 
    {
      $expr = $t0.expr;
    }
    (op=('+'|'-'|'%') tp=mathTerm
      {
        Expression enew = null;
        if($op.text.equals("+")){
            enew = new Expression(Function.Add);
        }else if($op.text.equals("-")){
            enew = new Expression(Function.Subtract);
        }else if($op.text.equals("\%")){
            enew = new Expression(Function.Modulo);
        }
        enew.addArgument($expr);
        enew.addArgument($tp.expr);
        $expr = enew;
      }
    )*
    ;

mathTerm returns [Expression expr]
    :   
    t0=mathFactor 
    {
      $expr = $t0.expr;
    }
    (op=('*'|'/'|'&'|'|'|'^'|'<<'|'>>') tp=mathFactor
      {
        Expression enew = null;
        if($op.text.equals("*")){
            enew = new Expression(Function.Multiply);
        }else if($op.text.equals("/")){
            enew = new Expression(Function.Divide);
        }else if($op.text.equals("&")){
            enew = new Expression(Function.BitAnd);
        }else if($op.text.equals("|")){
            enew = new Expression(Function.BitOr);
        }else if($op.text.equals("^")){
            enew = new Expression(Function.BitXor);
        }else if($op.text.equals("<<")){
            enew = new Expression(Function.BitShiftLeft);
        }else if($op.text.equals(">>")){
            enew = new Expression(Function.BitShiftRight);
        }
        enew.addArgument($expr);
        enew.addArgument($tp.expr);
        $expr = enew;
      }
    )*;
    
mathFactor returns [Expression expr]
    :
    (
		    fe=funcExpression
		    {
		      $expr = $fe.expr;
		    }
		    | ae=atomicExpression
		    {
		      $expr = $ae.expr;
		    }
		    | '(' ee=mathExpression ')'
		    {
		      $expr = $ee.expr;
		    }
		    | '~' mf=mathFactor
		    {
		      Expression enew = new Expression(Function.BitNeg);
		      enew.addArgument($mf.expr);
		      $expr = enew;
		    }
    ) fact='!'?
	    {
	      if($fact != null){
			      Expression enew = new Expression(Function.Factorial);
			      enew.addArgument($expr);
			      $expr = enew;
	      }
	    }
    ;

funcExpression returns [Expression expr]
    :
    func=ID 
	    {
	        Function f = ml.getFunctionByName($func.text);
	        if(f == null) die("Line #" + $func.getLine() + 
	        ": unknown function " + $func.text +
	        ". Are you putting a bool expression before a "+
	        "regular literal in a rule? (HINT: You shouldn't.)"
	        );
	        $expr = new Expression(f);
	    }
    '(' a0=funcArgument 
	    {
	        $expr.addArgument($a0.expr);
	    }
    (',' ap=funcArgument
	    {
	        $expr.addArgument($ap.expr);
	    }
    )* 
    ')'
    ;

funcArgument returns [Expression expr]
    :
    be=boolExpression
    {
      $expr = $be.be;
    }
    | me=mathExpression
    {
      $expr = $me.expr;
    }
    | fe=funcExpression
    {
      $expr = $fe.expr;
    }
    |ae=atomicExpression
    {
      $expr = $ae.expr;
    }
    ;
    

atomicExpression returns [Expression expr]
    :
    num=NUMBER
    {
      try{
        int n = Integer.parseInt($num.text);
        $expr = Expression.exprConstInteger(n);
      }catch(NumberFormatException e){
        $expr = Expression.exprConstNum(Double.parseDouble($num.text));
      }
    }
    | str=STRING
    {
      String s = $str.text;
      $expr = Expression.exprConstString(s);
    }
    | id=ID
    {
      String s = $id.text;
      String init = s.substring(0,1);
      if(init.equals(init.toUpperCase())) { // a constant
        $expr = Expression.exprConstString(s);
      }else{
        $expr = Expression.exprVariableBinding(s);
      }
    }
    ;
    

literal returns [Literal lit]
    :   pref=(PLUS|NOT)?  atom
    {
      $lit = $atom.lit;
      if($pref != null && $lit != null){
        if($pref.text.equals("!")) $lit.setSense(false);
        else $lit.setCoversAllMaterializedTuples(true);
       }
       
    }
    ;


term returns [Term t]
    :
    ID
    {
          String s = $ID.text;
          String init = s.substring(0,1);
          if(init.equals(init.toUpperCase())) { // a constant
            if(Config.constants_as_raw_string) {
              $t = new Term(s, true);
            } else {
			        Integer cid = ml.getSymbolID(s, null);
			        $t = new Term(cid);
			      }
			    }else{ // a variable
			       $t = new Term(s);
			    }
    }
    | d=(NUMBER|STRING)
    {
          String s = $d.text;
          if(Config.constants_as_raw_string) {
            $t = new Term(s, true);
          } else {
            Integer cid = ml.getSymbolID(s, null);
            $t = new Term(cid);
          }
    }
    ;

atom returns [Literal lit]
    :   pred=ID '(' 
    {
        Predicate p = ml.getPredByName($pred.text);
        if(p == null) die("Line #" + $pred.getLine() + ": unknown predicate name - " + $pred.text);
        $lit = new Literal(p, true);
    }
    term1=term 
    {
        if($term1.t.isConstant()) $lit.getPred().getTypeAt($lit.getTerms().size()).addConstant($term1.t.constant());
        $lit.appendTerm($term1.t);
    }
    (',' 
    termp=term
    {
        if($termp.t.isConstant()) $lit.getPred().getTypeAt($lit.getTerms().size()).addConstant($termp.t.constant());
        $lit.appendTerm($termp.t);
    }
    )* ')'
    {
        Predicate p = $lit.getPred();
        if($lit.getTerms().size() != p.arity()){
          die("Line #" + $pred.getLine() + 
          ": incorrect # of args (read " + $lit.getTerms().size() +
          " but expected " + p.arity() + ")" +
          " for pred " + 
          p.getName());
        }
        // Register constants (to types) that only appear in the program
        if (!Config.constants_as_raw_string) {
	        for(int i=0; i<p.arity(); i++){
	          Type t = p.getTypeAt(i);
	          Term term = $lit.getTerms().get(i);
	          if(term.isConstant()){
	            t.addConstant(term.constant());
	          }
	        }
        }
    }
    ;

queryList : query+ EOF
  ;

query  :  
  atom
  {
		  Atom q = $atom.lit.toAtom(Atom.AtomType.QUERY);
		  $atom.lit.getPred().addQuery(q);
  }
  | ID
  {
      Predicate p = ml.getPredByName($ID.text);
      if(p == null) die("Line #" + $ID.getLine() + ": unknown predicate name - " + $ID.text);
      p.setAllQuery();
  }
  ;

queryCommaList : query (',' query)* EOF;


evidenceList : evidence+ EOF;


evidence : prior=NUMBER? NOT? pred=ID '(' terms+=(ID|NUMBER|STRING) (',' terms+=(ID|NUMBER|STRING))* ')'
    {
      Boolean truth = null;
      Double pr = null;
      if($prior != null){
	      pr = Double.parseDouble($prior.text);
	      if(pr > 1 || pr < 0){
            die("Line #" + (lineOffset+$pred.getLine()) + ": " + $prior.text + " - probabilities of soft evidence should be in [0,1]");
	      }
        if($NOT != null) pr = 1 - pr;
        if(pr == 0 || pr == 1){
          if(pr == 0) truth = false;
          else truth = true;
          pr = null;
        }
	    }else{
	      truth = ($NOT == null);
	    }
	    Predicate p = ml.getPredByName($pred.text);
	    if(p == null) die("Line #" + (lineOffset+$pred.getLine()) + ": unknown predicate name - " + $pred.text);
	    ArrayList<String> args = new ArrayList<String>();
	    ArrayList<Token> ts = (ArrayList<Token>)($terms);
      if(ts.size() != p.arity()) die("Line #" + (lineOffset+$pred.getLine()) + ": incorrect # args - " + $evidence.text);
      for(int i=0; i<p.arity(); i++){
          Token t = ts.get(i);
          Type type = p.getTypeAt(i);
	    	  String s = t.getText();
	    	  if(type.isNonSymbolicType()){
	    	    args.add(s);
	    	  }else{
            if(Config.constants_as_raw_string) {
               args.add(s);
            } else {
               Integer sid = ml.getSymbolID(s,type);
			         args.add(sid.toString());
			      }
			    }
	    }
		  Atom gp = null;
		  if(pr == null) {
		    gp = new Atom(args, truth);
		  }else{
		    gp = new Atom(args, pr);
		  }
	    p.addEvidence(gp);
	    gp = null;
    }
    ;
