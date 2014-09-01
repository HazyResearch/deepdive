// $ANTLR 3.2 Sep 23, 2009 12:02:23 MLN.g 2012-05-09 08:57:43

package tuffy.parse;
import tuffy.mln.*;
import tuffy.ra.*;
import tuffy.util.*;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.antlr.runtime.tree.*;

public class MLNParser extends Parser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "WS", "COMMENT", "NOT", "PLUS", "MINUS", "ASTERISK", "PERIOD", "EXIST", "IMPLIES", "ESC", "STRING", "HEXDIGIT", "INTEGER", "FLOAT", "NUMBER", "EXPONENT", "ID", "'**'", "'@'", "'('", "','", "')'", "'FUNCTIONAL DEPENDENCY'", "'FD'", "':'", "';'", "'->'", "'[Label]'", "'[+Label]'", "'[Label+]'", "'#'", "'$'", "':-'", "'['", "']'", "'priorProb'", "'='", "':='", "'v'", "'||'", "'OR'", "'&&'", "'AND'", "'NOT'", "'<>'", "'<'", "'<='", "'>'", "'>='", "'!='", "'%'", "'/'", "'&'", "'|'", "'^'", "'<<'", "'>>'", "'~'"
    };
    public static final int EXPONENT=19;
    public static final int T__29=29;
    public static final int T__28=28;
    public static final int T__27=27;
    public static final int T__26=26;
    public static final int T__25=25;
    public static final int T__24=24;
    public static final int T__23=23;
    public static final int ESC=13;
    public static final int T__22=22;
    public static final int T__21=21;
    public static final int FLOAT=17;
    public static final int NOT=6;
    public static final int ID=20;
    public static final int T__61=61;
    public static final int EOF=-1;
    public static final int T__60=60;
    public static final int ASTERISK=9;
    public static final int T__55=55;
    public static final int T__56=56;
    public static final int T__57=57;
    public static final int T__58=58;
    public static final int T__51=51;
    public static final int T__52=52;
    public static final int T__53=53;
    public static final int T__54=54;
    public static final int T__59=59;
    public static final int PLUS=7;
    public static final int COMMENT=5;
    public static final int T__50=50;
    public static final int INTEGER=16;
    public static final int T__42=42;
    public static final int T__43=43;
    public static final int T__40=40;
    public static final int T__41=41;
    public static final int T__46=46;
    public static final int IMPLIES=12;
    public static final int T__47=47;
    public static final int T__44=44;
    public static final int T__45=45;
    public static final int PERIOD=10;
    public static final int T__48=48;
    public static final int T__49=49;
    public static final int NUMBER=18;
    public static final int MINUS=8;
    public static final int T__30=30;
    public static final int T__31=31;
    public static final int T__32=32;
    public static final int WS=4;
    public static final int T__33=33;
    public static final int T__34=34;
    public static final int T__35=35;
    public static final int T__36=36;
    public static final int T__37=37;
    public static final int T__38=38;
    public static final int T__39=39;
    public static final int STRING=14;
    public static final int HEXDIGIT=15;
    public static final int EXIST=11;

    // delegates
    // delegators


        public MLNParser(TokenStream input) {
            this(input, new RecognizerSharedState());
        }
        public MLNParser(TokenStream input, RecognizerSharedState state) {
            super(input, state);
            this.state.ruleMemo = new HashMap[133+1];
             
             
        }
        
    protected TreeAdaptor adaptor = new CommonTreeAdaptor();

    public void setTreeAdaptor(TreeAdaptor adaptor) {
        this.adaptor = adaptor;
    }
    public TreeAdaptor getTreeAdaptor() {
        return adaptor;
    }

    public String[] getTokenNames() { return MLNParser.tokenNames; }
    public String getGrammarFileName() { return "MLN.g"; }


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
    		
        


    public static class definitions_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "definitions"
    // MLN.g:121:1: definitions : schemaList ruleList EOF ;
    public final MLNParser.definitions_return definitions() throws RecognitionException {
        MLNParser.definitions_return retval = new MLNParser.definitions_return();
        retval.start = input.LT(1);
        int definitions_StartIndex = input.index();
        Object root_0 = null;

        Token EOF3=null;
        MLNParser.schemaList_return schemaList1 = null;

        MLNParser.ruleList_return ruleList2 = null;


        Object EOF3_tree=null;

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 1) ) { return retval; }
            // MLN.g:121:13: ( schemaList ruleList EOF )
            // MLN.g:121:15: schemaList ruleList EOF
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_schemaList_in_definitions895);
            schemaList1=schemaList();

            state._fsp--;
            if (state.failed) return retval;
            if ( state.backtracking==0 ) adaptor.addChild(root_0, schemaList1.getTree());
            pushFollow(FOLLOW_ruleList_in_definitions897);
            ruleList2=ruleList();

            state._fsp--;
            if (state.failed) return retval;
            if ( state.backtracking==0 ) adaptor.addChild(root_0, ruleList2.getTree());
            EOF3=(Token)match(input,EOF,FOLLOW_EOF_in_definitions899); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            EOF3_tree = (Object)adaptor.create(EOF3);
            adaptor.addChild(root_0, EOF3_tree);
            }

            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 1, definitions_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "definitions"

    public static class schemaList_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "schemaList"
    // MLN.g:123:1: schemaList : ( schema | schemaConstraint )* ;
    public final MLNParser.schemaList_return schemaList() throws RecognitionException {
        MLNParser.schemaList_return retval = new MLNParser.schemaList_return();
        retval.start = input.LT(1);
        int schemaList_StartIndex = input.index();
        Object root_0 = null;

        MLNParser.schema_return schema4 = null;

        MLNParser.schemaConstraint_return schemaConstraint5 = null;



        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 2) ) { return retval; }
            // MLN.g:123:12: ( ( schema | schemaConstraint )* )
            // MLN.g:123:14: ( schema | schemaConstraint )*
            {
            root_0 = (Object)adaptor.nil();

            // MLN.g:123:14: ( schema | schemaConstraint )*
            loop1:
            do {
                int alt1=3;
                alt1 = dfa1.predict(input);
                switch (alt1) {
            	case 1 :
            	    // MLN.g:123:15: schema
            	    {
            	    pushFollow(FOLLOW_schema_in_schemaList908);
            	    schema4=schema();

            	    state._fsp--;
            	    if (state.failed) return retval;
            	    if ( state.backtracking==0 ) adaptor.addChild(root_0, schema4.getTree());

            	    }
            	    break;
            	case 2 :
            	    // MLN.g:123:24: schemaConstraint
            	    {
            	    pushFollow(FOLLOW_schemaConstraint_in_schemaList912);
            	    schemaConstraint5=schemaConstraint();

            	    state._fsp--;
            	    if (state.failed) return retval;
            	    if ( state.backtracking==0 ) adaptor.addChild(root_0, schemaConstraint5.getTree());

            	    }
            	    break;

            	default :
            	    break loop1;
                }
            } while (true);


            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 2, schemaList_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "schemaList"

    public static class schema_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "schema"
    // MLN.g:125:1: schema : (du= '+' )? (aa= '**' | a1= ASTERISK )? (vt= '@' )? pname= ID (a2= ASTERISK )? '(' types+= predArg ( ',' types+= predArg )* ')' ;
    public final MLNParser.schema_return schema() throws RecognitionException {
        MLNParser.schema_return retval = new MLNParser.schema_return();
        retval.start = input.LT(1);
        int schema_StartIndex = input.index();
        Object root_0 = null;

        Token du=null;
        Token aa=null;
        Token a1=null;
        Token vt=null;
        Token pname=null;
        Token a2=null;
        Token char_literal6=null;
        Token char_literal7=null;
        Token char_literal8=null;
        List list_types=null;
        RuleReturnScope types = null;
        Object du_tree=null;
        Object aa_tree=null;
        Object a1_tree=null;
        Object vt_tree=null;
        Object pname_tree=null;
        Object a2_tree=null;
        Object char_literal6_tree=null;
        Object char_literal7_tree=null;
        Object char_literal8_tree=null;

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 3) ) { return retval; }
            // MLN.g:125:8: ( (du= '+' )? (aa= '**' | a1= ASTERISK )? (vt= '@' )? pname= ID (a2= ASTERISK )? '(' types+= predArg ( ',' types+= predArg )* ')' )
            // MLN.g:125:10: (du= '+' )? (aa= '**' | a1= ASTERISK )? (vt= '@' )? pname= ID (a2= ASTERISK )? '(' types+= predArg ( ',' types+= predArg )* ')'
            {
            root_0 = (Object)adaptor.nil();

            // MLN.g:125:10: (du= '+' )?
            int alt2=2;
            int LA2_0 = input.LA(1);

            if ( (LA2_0==PLUS) ) {
                alt2=1;
            }
            switch (alt2) {
                case 1 :
                    // MLN.g:125:11: du= '+'
                    {
                    du=(Token)match(input,PLUS,FOLLOW_PLUS_in_schema925); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    du_tree = (Object)adaptor.create(du);
                    adaptor.addChild(root_0, du_tree);
                    }

                    }
                    break;

            }

            // MLN.g:125:20: (aa= '**' | a1= ASTERISK )?
            int alt3=3;
            int LA3_0 = input.LA(1);

            if ( (LA3_0==21) ) {
                alt3=1;
            }
            else if ( (LA3_0==ASTERISK) ) {
                alt3=2;
            }
            switch (alt3) {
                case 1 :
                    // MLN.g:125:21: aa= '**'
                    {
                    aa=(Token)match(input,21,FOLLOW_21_in_schema932); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    aa_tree = (Object)adaptor.create(aa);
                    adaptor.addChild(root_0, aa_tree);
                    }

                    }
                    break;
                case 2 :
                    // MLN.g:125:29: a1= ASTERISK
                    {
                    a1=(Token)match(input,ASTERISK,FOLLOW_ASTERISK_in_schema936); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    a1_tree = (Object)adaptor.create(a1);
                    adaptor.addChild(root_0, a1_tree);
                    }

                    }
                    break;

            }

            // MLN.g:125:43: (vt= '@' )?
            int alt4=2;
            int LA4_0 = input.LA(1);

            if ( (LA4_0==22) ) {
                alt4=1;
            }
            switch (alt4) {
                case 1 :
                    // MLN.g:125:44: vt= '@'
                    {
                    vt=(Token)match(input,22,FOLLOW_22_in_schema943); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    vt_tree = (Object)adaptor.create(vt);
                    adaptor.addChild(root_0, vt_tree);
                    }

                    }
                    break;

            }

            pname=(Token)match(input,ID,FOLLOW_ID_in_schema949); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            pname_tree = (Object)adaptor.create(pname);
            adaptor.addChild(root_0, pname_tree);
            }
            // MLN.g:125:64: (a2= ASTERISK )?
            int alt5=2;
            int LA5_0 = input.LA(1);

            if ( (LA5_0==ASTERISK) ) {
                alt5=1;
            }
            switch (alt5) {
                case 1 :
                    // MLN.g:0:0: a2= ASTERISK
                    {
                    a2=(Token)match(input,ASTERISK,FOLLOW_ASTERISK_in_schema953); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    a2_tree = (Object)adaptor.create(a2);
                    adaptor.addChild(root_0, a2_tree);
                    }

                    }
                    break;

            }

            if ( state.backtracking==0 ) {

              		//boolean cwa = (a1 != null || aa != null);
              		boolean cwa = (a1 != null);
                  boolean eqrel = (a2 != null);
              		Predicate pred = new Predicate(ml, (pname!=null?pname.getText():null), cwa);
              		if(aa != null) pred.setCompeletelySpecified(true);
              		//if(eqrel) pred.isDedupalogHead = true;
              		if(vt != null){
              		    //if((vt!=null?vt.getText():null).equals("@")) pred.isInMem = true;
              		}
                  if(du != null){
                      //if((du!=null?du.getText():null).equals("+")) pred.toDump = true;
                  }
              		curPred = pred;
              	
            }
            char_literal6=(Token)match(input,23,FOLLOW_23_in_schema960); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            char_literal6_tree = (Object)adaptor.create(char_literal6);
            adaptor.addChild(root_0, char_literal6_tree);
            }
            pushFollow(FOLLOW_predArg_in_schema964);
            types=predArg();

            state._fsp--;
            if (state.failed) return retval;
            if ( state.backtracking==0 ) adaptor.addChild(root_0, types.getTree());
            if (list_types==null) list_types=new ArrayList();
            list_types.add(types.getTree());

            // MLN.g:141:21: ( ',' types+= predArg )*
            loop6:
            do {
                int alt6=2;
                int LA6_0 = input.LA(1);

                if ( (LA6_0==24) ) {
                    alt6=1;
                }


                switch (alt6) {
            	case 1 :
            	    // MLN.g:141:22: ',' types+= predArg
            	    {
            	    char_literal7=(Token)match(input,24,FOLLOW_24_in_schema967); if (state.failed) return retval;
            	    if ( state.backtracking==0 ) {
            	    char_literal7_tree = (Object)adaptor.create(char_literal7);
            	    adaptor.addChild(root_0, char_literal7_tree);
            	    }
            	    pushFollow(FOLLOW_predArg_in_schema971);
            	    types=predArg();

            	    state._fsp--;
            	    if (state.failed) return retval;
            	    if ( state.backtracking==0 ) adaptor.addChild(root_0, types.getTree());
            	    if (list_types==null) list_types=new ArrayList();
            	    list_types.add(types.getTree());


            	    }
            	    break;

            	default :
            	    break loop6;
                }
            } while (true);

            char_literal8=(Token)match(input,25,FOLLOW_25_in_schema975); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            char_literal8_tree = (Object)adaptor.create(char_literal8);
            adaptor.addChild(root_0, char_literal8_tree);
            }
            if ( state.backtracking==0 ) {

              	    curPred.sealDefinition();
                    ml.registerPred(curPred);
              	
            }

            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 3, schema_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "schema"

    public static class predArg_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "predArg"
    // MLN.g:148:1: predArg : type= ID (name= ID )? (uni= '!' )? ;
    public final MLNParser.predArg_return predArg() throws RecognitionException {
        MLNParser.predArg_return retval = new MLNParser.predArg_return();
        retval.start = input.LT(1);
        int predArg_StartIndex = input.index();
        Object root_0 = null;

        Token type=null;
        Token name=null;
        Token uni=null;

        Object type_tree=null;
        Object name_tree=null;
        Object uni_tree=null;

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 4) ) { return retval; }
            // MLN.g:148:9: (type= ID (name= ID )? (uni= '!' )? )
            // MLN.g:148:11: type= ID (name= ID )? (uni= '!' )?
            {
            root_0 = (Object)adaptor.nil();

            type=(Token)match(input,ID,FOLLOW_ID_in_predArg990); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            type_tree = (Object)adaptor.create(type);
            adaptor.addChild(root_0, type_tree);
            }
            // MLN.g:148:19: (name= ID )?
            int alt7=2;
            int LA7_0 = input.LA(1);

            if ( (LA7_0==ID) ) {
                alt7=1;
            }
            switch (alt7) {
                case 1 :
                    // MLN.g:148:20: name= ID
                    {
                    name=(Token)match(input,ID,FOLLOW_ID_in_predArg995); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    name_tree = (Object)adaptor.create(name);
                    adaptor.addChild(root_0, name_tree);
                    }

                    }
                    break;

            }

            // MLN.g:148:33: (uni= '!' )?
            int alt8=2;
            int LA8_0 = input.LA(1);

            if ( (LA8_0==NOT) ) {
                alt8=1;
            }
            switch (alt8) {
                case 1 :
                    // MLN.g:0:0: uni= '!'
                    {
                    uni=(Token)match(input,NOT,FOLLOW_NOT_in_predArg1001); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    uni_tree = (Object)adaptor.create(uni);
                    adaptor.addChild(root_0, uni_tree);
                    }

                    }
                    break;

            }

            if ( state.backtracking==0 ) {

                  Type t = ml.getOrCreateTypeByName((type!=null?type.getText():null));
                  String nm = null;
                  if(name != null) nm = (name!=null?name.getText():null);
                  curPred.appendArgument(t, nm);
                  if(uni != null){
                    curPred.addDependentAttrPosition(curPred.arity() - 1);
                  }
                
            }

            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 4, predArg_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "predArg"

    public static class schemaConstraint_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "schemaConstraint"
    // MLN.g:160:1: schemaConstraint : functionalDependency ;
    public final MLNParser.schemaConstraint_return schemaConstraint() throws RecognitionException {
        MLNParser.schemaConstraint_return retval = new MLNParser.schemaConstraint_return();
        retval.start = input.LT(1);
        int schemaConstraint_StartIndex = input.index();
        Object root_0 = null;

        MLNParser.functionalDependency_return functionalDependency9 = null;



        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 5) ) { return retval; }
            // MLN.g:160:18: ( functionalDependency )
            // MLN.g:161:3: functionalDependency
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_functionalDependency_in_schemaConstraint1019);
            functionalDependency9=functionalDependency();

            state._fsp--;
            if (state.failed) return retval;
            if ( state.backtracking==0 ) adaptor.addChild(root_0, functionalDependency9.getTree());

            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 5, schemaConstraint_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "schemaConstraint"

    public static class functionalDependency_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "functionalDependency"
    // MLN.g:163:1: functionalDependency : ( 'FUNCTIONAL DEPENDENCY' | 'FD' ) ':' functionalDependencyItem ( ';' functionalDependencyItem )* ;
    public final MLNParser.functionalDependency_return functionalDependency() throws RecognitionException {
        MLNParser.functionalDependency_return retval = new MLNParser.functionalDependency_return();
        retval.start = input.LT(1);
        int functionalDependency_StartIndex = input.index();
        Object root_0 = null;

        Token set10=null;
        Token char_literal11=null;
        Token char_literal13=null;
        MLNParser.functionalDependencyItem_return functionalDependencyItem12 = null;

        MLNParser.functionalDependencyItem_return functionalDependencyItem14 = null;


        Object set10_tree=null;
        Object char_literal11_tree=null;
        Object char_literal13_tree=null;

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 6) ) { return retval; }
            // MLN.g:163:22: ( ( 'FUNCTIONAL DEPENDENCY' | 'FD' ) ':' functionalDependencyItem ( ';' functionalDependencyItem )* )
            // MLN.g:164:3: ( 'FUNCTIONAL DEPENDENCY' | 'FD' ) ':' functionalDependencyItem ( ';' functionalDependencyItem )*
            {
            root_0 = (Object)adaptor.nil();

            set10=(Token)input.LT(1);
            if ( (input.LA(1)>=26 && input.LA(1)<=27) ) {
                input.consume();
                if ( state.backtracking==0 ) adaptor.addChild(root_0, (Object)adaptor.create(set10));
                state.errorRecovery=false;state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return retval;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                throw mse;
            }

            char_literal11=(Token)match(input,28,FOLLOW_28_in_functionalDependency1039); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            char_literal11_tree = (Object)adaptor.create(char_literal11);
            adaptor.addChild(root_0, char_literal11_tree);
            }
            pushFollow(FOLLOW_functionalDependencyItem_in_functionalDependency1043);
            functionalDependencyItem12=functionalDependencyItem();

            state._fsp--;
            if (state.failed) return retval;
            if ( state.backtracking==0 ) adaptor.addChild(root_0, functionalDependencyItem12.getTree());
            // MLN.g:166:3: ( ';' functionalDependencyItem )*
            loop9:
            do {
                int alt9=2;
                int LA9_0 = input.LA(1);

                if ( (LA9_0==29) ) {
                    alt9=1;
                }


                switch (alt9) {
            	case 1 :
            	    // MLN.g:166:4: ';' functionalDependencyItem
            	    {
            	    char_literal13=(Token)match(input,29,FOLLOW_29_in_functionalDependency1048); if (state.failed) return retval;
            	    if ( state.backtracking==0 ) {
            	    char_literal13_tree = (Object)adaptor.create(char_literal13);
            	    adaptor.addChild(root_0, char_literal13_tree);
            	    }
            	    pushFollow(FOLLOW_functionalDependencyItem_in_functionalDependency1050);
            	    functionalDependencyItem14=functionalDependencyItem();

            	    state._fsp--;
            	    if (state.failed) return retval;
            	    if ( state.backtracking==0 ) adaptor.addChild(root_0, functionalDependencyItem14.getTree());

            	    }
            	    break;

            	default :
            	    break loop9;
                }
            } while (true);

            if ( state.backtracking==0 ) {

                  UIMan.warn("The feature of functional dependency " +
                    "is still in development; use at your own risk.");
                
            }

            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 6, functionalDependency_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "functionalDependency"

    public static class functionalDependencyItem_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "functionalDependencyItem"
    // MLN.g:173:1: functionalDependencyItem : pa+= ID ( ',' pa+= ID )* '->' sa= ID ;
    public final MLNParser.functionalDependencyItem_return functionalDependencyItem() throws RecognitionException {
        MLNParser.functionalDependencyItem_return retval = new MLNParser.functionalDependencyItem_return();
        retval.start = input.LT(1);
        int functionalDependencyItem_StartIndex = input.index();
        Object root_0 = null;

        Token sa=null;
        Token char_literal15=null;
        Token string_literal16=null;
        Token pa=null;
        List list_pa=null;

        Object sa_tree=null;
        Object char_literal15_tree=null;
        Object string_literal16_tree=null;
        Object pa_tree=null;

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 7) ) { return retval; }
            // MLN.g:173:25: (pa+= ID ( ',' pa+= ID )* '->' sa= ID )
            // MLN.g:174:3: pa+= ID ( ',' pa+= ID )* '->' sa= ID
            {
            root_0 = (Object)adaptor.nil();

            pa=(Token)match(input,ID,FOLLOW_ID_in_functionalDependencyItem1070); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            pa_tree = (Object)adaptor.create(pa);
            adaptor.addChild(root_0, pa_tree);
            }
            if (list_pa==null) list_pa=new ArrayList();
            list_pa.add(pa);

            // MLN.g:174:10: ( ',' pa+= ID )*
            loop10:
            do {
                int alt10=2;
                int LA10_0 = input.LA(1);

                if ( (LA10_0==24) ) {
                    alt10=1;
                }


                switch (alt10) {
            	case 1 :
            	    // MLN.g:174:11: ',' pa+= ID
            	    {
            	    char_literal15=(Token)match(input,24,FOLLOW_24_in_functionalDependencyItem1073); if (state.failed) return retval;
            	    if ( state.backtracking==0 ) {
            	    char_literal15_tree = (Object)adaptor.create(char_literal15);
            	    adaptor.addChild(root_0, char_literal15_tree);
            	    }
            	    pa=(Token)match(input,ID,FOLLOW_ID_in_functionalDependencyItem1077); if (state.failed) return retval;
            	    if ( state.backtracking==0 ) {
            	    pa_tree = (Object)adaptor.create(pa);
            	    adaptor.addChild(root_0, pa_tree);
            	    }
            	    if (list_pa==null) list_pa=new ArrayList();
            	    list_pa.add(pa);


            	    }
            	    break;

            	default :
            	    break loop10;
                }
            } while (true);

            string_literal16=(Token)match(input,30,FOLLOW_30_in_functionalDependencyItem1081); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            string_literal16_tree = (Object)adaptor.create(string_literal16);
            adaptor.addChild(root_0, string_literal16_tree);
            }
            sa=(Token)match(input,ID,FOLLOW_ID_in_functionalDependencyItem1085); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            sa_tree = (Object)adaptor.create(sa);
            adaptor.addChild(root_0, sa_tree);
            }
            if ( state.backtracking==0 ) {

                    ArrayList<Token> det = (ArrayList<Token>)(list_pa);
                    ArrayList<String> dets = new ArrayList<String>();
                    for(Token t : det){
                        String s = t.getText();
                        dets.add(s);
                    }
                    String dep = sa.getText();
                    curPred.addFunctionalDependency(dets, dep);
                
            }

            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 7, functionalDependencyItem_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "functionalDependencyItem"

    public static class ruleList_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "ruleList"
    // MLN.g:189:1: ruleList : ( mlnRule | scopingRule | datalogRule )* ;
    public final MLNParser.ruleList_return ruleList() throws RecognitionException {
        MLNParser.ruleList_return retval = new MLNParser.ruleList_return();
        retval.start = input.LT(1);
        int ruleList_StartIndex = input.index();
        Object root_0 = null;

        MLNParser.mlnRule_return mlnRule17 = null;

        MLNParser.scopingRule_return scopingRule18 = null;

        MLNParser.datalogRule_return datalogRule19 = null;



        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 8) ) { return retval; }
            // MLN.g:189:10: ( ( mlnRule | scopingRule | datalogRule )* )
            // MLN.g:189:12: ( mlnRule | scopingRule | datalogRule )*
            {
            root_0 = (Object)adaptor.nil();

            // MLN.g:189:12: ( mlnRule | scopingRule | datalogRule )*
            loop11:
            do {
                int alt11=4;
                alt11 = dfa11.predict(input);
                switch (alt11) {
            	case 1 :
            	    // MLN.g:189:13: mlnRule
            	    {
            	    pushFollow(FOLLOW_mlnRule_in_ruleList1103);
            	    mlnRule17=mlnRule();

            	    state._fsp--;
            	    if (state.failed) return retval;
            	    if ( state.backtracking==0 ) adaptor.addChild(root_0, mlnRule17.getTree());

            	    }
            	    break;
            	case 2 :
            	    // MLN.g:189:23: scopingRule
            	    {
            	    pushFollow(FOLLOW_scopingRule_in_ruleList1107);
            	    scopingRule18=scopingRule();

            	    state._fsp--;
            	    if (state.failed) return retval;
            	    if ( state.backtracking==0 ) adaptor.addChild(root_0, scopingRule18.getTree());

            	    }
            	    break;
            	case 3 :
            	    // MLN.g:189:37: datalogRule
            	    {
            	    pushFollow(FOLLOW_datalogRule_in_ruleList1111);
            	    datalogRule19=datalogRule();

            	    state._fsp--;
            	    if (state.failed) return retval;
            	    if ( state.backtracking==0 ) adaptor.addChild(root_0, datalogRule19.getTree());

            	    }
            	    break;

            	default :
            	    break loop11;
                }
            } while (true);


            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 8, ruleList_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "ruleList"

    public static class mlnRule_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "mlnRule"
    // MLN.g:192:1: mlnRule : (tag= ( '[Label]' | '[+Label]' | '[Label+]' ) STRING )? ( softRule | hardRule ) ;
    public final MLNParser.mlnRule_return mlnRule() throws RecognitionException {
        MLNParser.mlnRule_return retval = new MLNParser.mlnRule_return();
        retval.start = input.LT(1);
        int mlnRule_StartIndex = input.index();
        Object root_0 = null;

        Token tag=null;
        Token STRING20=null;
        MLNParser.softRule_return softRule21 = null;

        MLNParser.hardRule_return hardRule22 = null;


        Object tag_tree=null;
        Object STRING20_tree=null;

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 9) ) { return retval; }
            // MLN.g:193:5: ( (tag= ( '[Label]' | '[+Label]' | '[Label+]' ) STRING )? ( softRule | hardRule ) )
            // MLN.g:194:5: (tag= ( '[Label]' | '[+Label]' | '[Label+]' ) STRING )? ( softRule | hardRule )
            {
            root_0 = (Object)adaptor.nil();

            // MLN.g:194:5: (tag= ( '[Label]' | '[+Label]' | '[Label+]' ) STRING )?
            int alt12=2;
            int LA12_0 = input.LA(1);

            if ( ((LA12_0>=31 && LA12_0<=33)) ) {
                alt12=1;
            }
            switch (alt12) {
                case 1 :
                    // MLN.g:195:6: tag= ( '[Label]' | '[+Label]' | '[Label+]' ) STRING
                    {
                    tag=(Token)input.LT(1);
                    if ( (input.LA(1)>=31 && input.LA(1)<=33) ) {
                        input.consume();
                        if ( state.backtracking==0 ) adaptor.addChild(root_0, (Object)adaptor.create(tag));
                        state.errorRecovery=false;state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return retval;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        throw mse;
                    }

                    STRING20=(Token)match(input,STRING,FOLLOW_STRING_in_mlnRule1152); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    STRING20_tree = (Object)adaptor.create(STRING20);
                    adaptor.addChild(root_0, STRING20_tree);
                    }
                    if ( state.backtracking==0 ) {

                      	       clauseName = STRING20.getText();
                      	       String t = (tag!=null?tag.getText():null);
                      	       if(t != null && t.contains("+")){
                      	          clauseLabelTrailing = true;
                      	       }else{
                                  clauseLabelTrailing = false;
                      	       }
                      	    
                    }

                    }
                    break;

            }

            // MLN.g:206:5: ( softRule | hardRule )
            int alt13=2;
            switch ( input.LA(1) ) {
            case NUMBER:
            case 22:
                {
                alt13=1;
                }
                break;
            case ID:
                {
                int LA13_2 = input.LA(2);

                if ( (LA13_2==28) ) {
                    alt13=1;
                }
                else if ( (LA13_2==23) ) {
                    alt13=2;
                }
                else {
                    if (state.backtracking>0) {state.failed=true; return retval;}
                    NoViableAltException nvae =
                        new NoViableAltException("", 13, 2, input);

                    throw nvae;
                }
                }
                break;
            case NOT:
            case PLUS:
            case EXIST:
                {
                alt13=2;
                }
                break;
            default:
                if (state.backtracking>0) {state.failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("", 13, 0, input);

                throw nvae;
            }

            switch (alt13) {
                case 1 :
                    // MLN.g:206:6: softRule
                    {
                    pushFollow(FOLLOW_softRule_in_mlnRule1173);
                    softRule21=softRule();

                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, softRule21.getTree());

                    }
                    break;
                case 2 :
                    // MLN.g:206:17: hardRule
                    {
                    pushFollow(FOLLOW_hardRule_in_mlnRule1177);
                    hardRule22=hardRule();

                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, hardRule22.getTree());

                    }
                    break;

            }


            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 9, mlnRule_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "mlnRule"

    public static class softRule_return extends ParserRuleReturnScope {
        public Clause c;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "softRule"
    // MLN.g:209:1: softRule returns [Clause c] : ( (du= '@' )? weight= NUMBER fc= foclause | (du= '@' )? warg= ID ':' fc= foclause );
    public final MLNParser.softRule_return softRule() throws RecognitionException {
        MLNParser.softRule_return retval = new MLNParser.softRule_return();
        retval.start = input.LT(1);
        int softRule_StartIndex = input.index();
        Object root_0 = null;

        Token du=null;
        Token weight=null;
        Token warg=null;
        Token char_literal23=null;
        MLNParser.foclause_return fc = null;


        Object du_tree=null;
        Object weight_tree=null;
        Object warg_tree=null;
        Object char_literal23_tree=null;

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 10) ) { return retval; }
            // MLN.g:210:5: ( (du= '@' )? weight= NUMBER fc= foclause | (du= '@' )? warg= ID ':' fc= foclause )
            int alt16=2;
            switch ( input.LA(1) ) {
            case 22:
                {
                int LA16_1 = input.LA(2);

                if ( (LA16_1==NUMBER) ) {
                    alt16=1;
                }
                else if ( (LA16_1==ID) ) {
                    alt16=2;
                }
                else {
                    if (state.backtracking>0) {state.failed=true; return retval;}
                    NoViableAltException nvae =
                        new NoViableAltException("", 16, 1, input);

                    throw nvae;
                }
                }
                break;
            case NUMBER:
                {
                alt16=1;
                }
                break;
            case ID:
                {
                alt16=2;
                }
                break;
            default:
                if (state.backtracking>0) {state.failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("", 16, 0, input);

                throw nvae;
            }

            switch (alt16) {
                case 1 :
                    // MLN.g:210:9: (du= '@' )? weight= NUMBER fc= foclause
                    {
                    root_0 = (Object)adaptor.nil();

                    // MLN.g:210:9: (du= '@' )?
                    int alt14=2;
                    int LA14_0 = input.LA(1);

                    if ( (LA14_0==22) ) {
                        alt14=1;
                    }
                    switch (alt14) {
                        case 1 :
                            // MLN.g:210:10: du= '@'
                            {
                            du=(Token)match(input,22,FOLLOW_22_in_softRule1204); if (state.failed) return retval;
                            if ( state.backtracking==0 ) {
                            du_tree = (Object)adaptor.create(du);
                            adaptor.addChild(root_0, du_tree);
                            }

                            }
                            break;

                    }

                    weight=(Token)match(input,NUMBER,FOLLOW_NUMBER_in_softRule1210); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    weight_tree = (Object)adaptor.create(weight);
                    adaptor.addChild(root_0, weight_tree);
                    }
                    pushFollow(FOLLOW_foclause_in_softRule1214);
                    fc=foclause();

                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, fc.getTree());
                    if ( state.backtracking==0 ) {

                          retval.c = (fc!=null?fc.c:null); 
                          retval.c.setWeight(Double.parseDouble((weight!=null?weight.getText():null)));
                          ml.registerClause(retval.c);
                          retval.c.addSpecText(input.toString(retval.start,input.LT(-1)));
                            if(du != null){
                                retval.c.isFixedWeight = true;
                            }else{
                                retval.c.isFixedWeight = false;
                            }
                          
                    }

                    }
                    break;
                case 2 :
                    // MLN.g:223:5: (du= '@' )? warg= ID ':' fc= foclause
                    {
                    root_0 = (Object)adaptor.nil();

                    // MLN.g:223:5: (du= '@' )?
                    int alt15=2;
                    int LA15_0 = input.LA(1);

                    if ( (LA15_0==22) ) {
                        alt15=1;
                    }
                    switch (alt15) {
                        case 1 :
                            // MLN.g:223:6: du= '@'
                            {
                            du=(Token)match(input,22,FOLLOW_22_in_softRule1235); if (state.failed) return retval;
                            if ( state.backtracking==0 ) {
                            du_tree = (Object)adaptor.create(du);
                            adaptor.addChild(root_0, du_tree);
                            }

                            }
                            break;

                    }

                    warg=(Token)match(input,ID,FOLLOW_ID_in_softRule1241); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    warg_tree = (Object)adaptor.create(warg);
                    adaptor.addChild(root_0, warg_tree);
                    }
                    char_literal23=(Token)match(input,28,FOLLOW_28_in_softRule1243); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    char_literal23_tree = (Object)adaptor.create(char_literal23);
                    adaptor.addChild(root_0, char_literal23_tree);
                    }
                    pushFollow(FOLLOW_foclause_in_softRule1247);
                    fc=foclause();

                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, fc.getTree());
                    if ( state.backtracking==0 ) {

                          retval.c = (fc!=null?fc.c:null); 
                          retval.c.setWeight(1);
                          retval.c.setVarWeight((warg!=null?warg.getText():null));
                          ml.registerClause(retval.c);
                          retval.c.addSpecText(input.toString(retval.start,input.LT(-1)));
                            if(du != null){
                                retval.c.isFixedWeight = true;
                            }else{
                                retval.c.isFixedWeight = false;
                            }
                          
                    }

                    }
                    break;

            }
            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 10, softRule_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "softRule"

    public static class hardRule_return extends ParserRuleReturnScope {
        public Clause c;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "hardRule"
    // MLN.g:240:1: hardRule returns [Clause c] : fc= foclause PERIOD ;
    public final MLNParser.hardRule_return hardRule() throws RecognitionException {
        MLNParser.hardRule_return retval = new MLNParser.hardRule_return();
        retval.start = input.LT(1);
        int hardRule_StartIndex = input.index();
        Object root_0 = null;

        Token PERIOD24=null;
        MLNParser.foclause_return fc = null;


        Object PERIOD24_tree=null;

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 11) ) { return retval; }
            // MLN.g:241:5: (fc= foclause PERIOD )
            // MLN.g:241:9: fc= foclause PERIOD
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_foclause_in_hardRule1280);
            fc=foclause();

            state._fsp--;
            if (state.failed) return retval;
            if ( state.backtracking==0 ) adaptor.addChild(root_0, fc.getTree());
            PERIOD24=(Token)match(input,PERIOD,FOLLOW_PERIOD_in_hardRule1282); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            PERIOD24_tree = (Object)adaptor.create(PERIOD24);
            adaptor.addChild(root_0, PERIOD24_tree);
            }
            if ( state.backtracking==0 ) {

                  retval.c = (fc!=null?fc.c:null); 
                  retval.c.setHardWeight();
                  ml.registerClause(retval.c);
                  retval.c.addSpecText(input.toString(retval.start,input.LT(-1)));
                  
            }

            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 11, hardRule_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "hardRule"

    public static class functionalAtom_return extends ParserRuleReturnScope {
        public AtomEx cond;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "functionalAtom"
    // MLN.g:251:1: functionalAtom returns [AtomEx cond] : func= ID '(' e1= expression ( ',' ep= expression )* ')' ;
    public final MLNParser.functionalAtom_return functionalAtom() throws RecognitionException {
        MLNParser.functionalAtom_return retval = new MLNParser.functionalAtom_return();
        retval.start = input.LT(1);
        int functionalAtom_StartIndex = input.index();
        Object root_0 = null;

        Token func=null;
        Token char_literal25=null;
        Token char_literal26=null;
        Token char_literal27=null;
        MLNParser.expression_return e1 = null;

        MLNParser.expression_return ep = null;


        Object func_tree=null;
        Object char_literal25_tree=null;
        Object char_literal26_tree=null;
        Object char_literal27_tree=null;

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 12) ) { return retval; }
            // MLN.g:252:5: (func= ID '(' e1= expression ( ',' ep= expression )* ')' )
            // MLN.g:253:5: func= ID '(' e1= expression ( ',' ep= expression )* ')'
            {
            root_0 = (Object)adaptor.nil();

            func=(Token)match(input,ID,FOLLOW_ID_in_functionalAtom1317); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            func_tree = (Object)adaptor.create(func);
            adaptor.addChild(root_0, func_tree);
            }
            if ( state.backtracking==0 ) {

                    Predicate p = ml.getPredByName((func!=null?func.getText():null));
                    if(p==null) die("Line #" + func.getLine() + ": undefined predicate: " + (func!=null?func.getText():null));
                    retval.cond = new AtomEx(p);
                  
            }
            char_literal25=(Token)match(input,23,FOLLOW_23_in_functionalAtom1330); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            char_literal25_tree = (Object)adaptor.create(char_literal25);
            adaptor.addChild(root_0, char_literal25_tree);
            }
            pushFollow(FOLLOW_expression_in_functionalAtom1339);
            e1=expression();

            state._fsp--;
            if (state.failed) return retval;
            if ( state.backtracking==0 ) adaptor.addChild(root_0, e1.getTree());
            if ( state.backtracking==0 ) {
              retval.cond.appendTerm((e1!=null?e1.expr:null));
            }
            // MLN.g:262:7: ( ',' ep= expression )*
            loop17:
            do {
                int alt17=2;
                int LA17_0 = input.LA(1);

                if ( (LA17_0==24) ) {
                    alt17=1;
                }


                switch (alt17) {
            	case 1 :
            	    // MLN.g:262:8: ',' ep= expression
            	    {
            	    char_literal26=(Token)match(input,24,FOLLOW_24_in_functionalAtom1355); if (state.failed) return retval;
            	    if ( state.backtracking==0 ) {
            	    char_literal26_tree = (Object)adaptor.create(char_literal26);
            	    adaptor.addChild(root_0, char_literal26_tree);
            	    }
            	    pushFollow(FOLLOW_expression_in_functionalAtom1359);
            	    ep=expression();

            	    state._fsp--;
            	    if (state.failed) return retval;
            	    if ( state.backtracking==0 ) adaptor.addChild(root_0, ep.getTree());
            	    if ( state.backtracking==0 ) {
            	      retval.cond.appendTerm((ep!=null?ep.expr:null));
            	    }

            	    }
            	    break;

            	default :
            	    break loop17;
                }
            } while (true);

            char_literal27=(Token)match(input,25,FOLLOW_25_in_functionalAtom1382); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            char_literal27_tree = (Object)adaptor.create(char_literal27);
            adaptor.addChild(root_0, char_literal27_tree);
            }

            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 12, functionalAtom_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "functionalAtom"

    public static class datalogRule_return extends ParserRuleReturnScope {
        public ConjunctiveQuery cq = new ConjunctiveQuery();;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "datalogRule"
    // MLN.g:268:1: datalogRule returns [ConjunctiveQuery cq = new ConjunctiveQuery();] : (vt= ( '@' | '#' ) )? (st= '$' )? head= literal ':-' body0= literal ( ',' bodyp= literal )* ( ',' mc= mathComparison )* ( ',' '[' be= boolExpression ']' )? PERIOD ;
    public final MLNParser.datalogRule_return datalogRule() throws RecognitionException {
        MLNParser.datalogRule_return retval = new MLNParser.datalogRule_return();
        retval.start = input.LT(1);
        int datalogRule_StartIndex = input.index();
        Object root_0 = null;

        Token vt=null;
        Token st=null;
        Token string_literal28=null;
        Token char_literal29=null;
        Token char_literal30=null;
        Token char_literal31=null;
        Token char_literal32=null;
        Token char_literal33=null;
        Token PERIOD34=null;
        MLNParser.literal_return head = null;

        MLNParser.literal_return body0 = null;

        MLNParser.literal_return bodyp = null;

        MLNParser.mathComparison_return mc = null;

        MLNParser.boolExpression_return be = null;


        Object vt_tree=null;
        Object st_tree=null;
        Object string_literal28_tree=null;
        Object char_literal29_tree=null;
        Object char_literal30_tree=null;
        Object char_literal31_tree=null;
        Object char_literal32_tree=null;
        Object char_literal33_tree=null;
        Object PERIOD34_tree=null;

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 13) ) { return retval; }
            // MLN.g:269:5: ( (vt= ( '@' | '#' ) )? (st= '$' )? head= literal ':-' body0= literal ( ',' bodyp= literal )* ( ',' mc= mathComparison )* ( ',' '[' be= boolExpression ']' )? PERIOD )
            // MLN.g:269:9: (vt= ( '@' | '#' ) )? (st= '$' )? head= literal ':-' body0= literal ( ',' bodyp= literal )* ( ',' mc= mathComparison )* ( ',' '[' be= boolExpression ']' )? PERIOD
            {
            root_0 = (Object)adaptor.nil();

            // MLN.g:269:9: (vt= ( '@' | '#' ) )?
            int alt18=2;
            int LA18_0 = input.LA(1);

            if ( (LA18_0==22||LA18_0==34) ) {
                alt18=1;
            }
            switch (alt18) {
                case 1 :
                    // MLN.g:269:10: vt= ( '@' | '#' )
                    {
                    vt=(Token)input.LT(1);
                    if ( input.LA(1)==22||input.LA(1)==34 ) {
                        input.consume();
                        if ( state.backtracking==0 ) adaptor.addChild(root_0, (Object)adaptor.create(vt));
                        state.errorRecovery=false;state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return retval;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        throw mse;
                    }


                    }
                    break;

            }

            // MLN.g:269:25: (st= '$' )?
            int alt19=2;
            int LA19_0 = input.LA(1);

            if ( (LA19_0==35) ) {
                alt19=1;
            }
            switch (alt19) {
                case 1 :
                    // MLN.g:269:26: st= '$'
                    {
                    st=(Token)match(input,35,FOLLOW_35_in_datalogRule1419); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    st_tree = (Object)adaptor.create(st);
                    adaptor.addChild(root_0, st_tree);
                    }

                    }
                    break;

            }

            pushFollow(FOLLOW_literal_in_datalogRule1425);
            head=literal();

            state._fsp--;
            if (state.failed) return retval;
            if ( state.backtracking==0 ) adaptor.addChild(root_0, head.getTree());
            if ( state.backtracking==0 ) {
              retval.cq.setHead((head!=null?head.lit:null));
            }
            string_literal28=(Token)match(input,36,FOLLOW_36_in_datalogRule1429); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            string_literal28_tree = (Object)adaptor.create(string_literal28);
            adaptor.addChild(root_0, string_literal28_tree);
            }
            pushFollow(FOLLOW_literal_in_datalogRule1441);
            body0=literal();

            state._fsp--;
            if (state.failed) return retval;
            if ( state.backtracking==0 ) adaptor.addChild(root_0, body0.getTree());
            if ( state.backtracking==0 ) {

                        if((body0!=null?body0.lit:null)!=null){
                          retval.cq.addBodyLit((body0!=null?body0.lit:null));
                        }
                      
            }
            // MLN.g:275:9: ( ',' bodyp= literal )*
            loop20:
            do {
                int alt20=2;
                alt20 = dfa20.predict(input);
                switch (alt20) {
            	case 1 :
            	    // MLN.g:275:10: ',' bodyp= literal
            	    {
            	    char_literal29=(Token)match(input,24,FOLLOW_24_in_datalogRule1454); if (state.failed) return retval;
            	    if ( state.backtracking==0 ) {
            	    char_literal29_tree = (Object)adaptor.create(char_literal29);
            	    adaptor.addChild(root_0, char_literal29_tree);
            	    }
            	    pushFollow(FOLLOW_literal_in_datalogRule1458);
            	    bodyp=literal();

            	    state._fsp--;
            	    if (state.failed) return retval;
            	    if ( state.backtracking==0 ) adaptor.addChild(root_0, bodyp.getTree());
            	    if ( state.backtracking==0 ) {

            	                if((bodyp!=null?bodyp.lit:null)!=null){
            	                  retval.cq.addBodyLit((bodyp!=null?bodyp.lit:null));
            	                }
            	              
            	    }

            	    }
            	    break;

            	default :
            	    break loop20;
                }
            } while (true);

            // MLN.g:280:9: ( ',' mc= mathComparison )*
            loop21:
            do {
                int alt21=2;
                int LA21_0 = input.LA(1);

                if ( (LA21_0==24) ) {
                    int LA21_1 = input.LA(2);

                    if ( (LA21_1==STRING||LA21_1==NUMBER||LA21_1==ID||LA21_1==23||LA21_1==61) ) {
                        alt21=1;
                    }


                }


                switch (alt21) {
            	case 1 :
            	    // MLN.g:280:10: ',' mc= mathComparison
            	    {
            	    char_literal30=(Token)match(input,24,FOLLOW_24_in_datalogRule1473); if (state.failed) return retval;
            	    if ( state.backtracking==0 ) {
            	    char_literal30_tree = (Object)adaptor.create(char_literal30);
            	    adaptor.addChild(root_0, char_literal30_tree);
            	    }
            	    pushFollow(FOLLOW_mathComparison_in_datalogRule1477);
            	    mc=mathComparison();

            	    state._fsp--;
            	    if (state.failed) return retval;
            	    if ( state.backtracking==0 ) adaptor.addChild(root_0, mc.getTree());
            	    if ( state.backtracking==0 ) {

            	                retval.cq.addConstraint((mc!=null?mc.expr:null));
            	              
            	    }

            	    }
            	    break;

            	default :
            	    break loop21;
                }
            } while (true);

            // MLN.g:283:8: ( ',' '[' be= boolExpression ']' )?
            int alt22=2;
            int LA22_0 = input.LA(1);

            if ( (LA22_0==24) ) {
                alt22=1;
            }
            switch (alt22) {
                case 1 :
                    // MLN.g:283:9: ',' '[' be= boolExpression ']'
                    {
                    char_literal31=(Token)match(input,24,FOLLOW_24_in_datalogRule1491); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    char_literal31_tree = (Object)adaptor.create(char_literal31);
                    adaptor.addChild(root_0, char_literal31_tree);
                    }
                    char_literal32=(Token)match(input,37,FOLLOW_37_in_datalogRule1493); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    char_literal32_tree = (Object)adaptor.create(char_literal32);
                    adaptor.addChild(root_0, char_literal32_tree);
                    }
                    pushFollow(FOLLOW_boolExpression_in_datalogRule1508);
                    be=boolExpression();

                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, be.getTree());
                    if ( state.backtracking==0 ) {

                                  retval.cq.addConstraint((be!=null?be.be:null));
                                 
                    }
                    char_literal33=(Token)match(input,38,FOLLOW_38_in_datalogRule1519); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    char_literal33_tree = (Object)adaptor.create(char_literal33);
                    adaptor.addChild(root_0, char_literal33_tree);
                    }

                    }
                    break;

            }

            PERIOD34=(Token)match(input,PERIOD,FOLLOW_PERIOD_in_datalogRule1528); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            PERIOD34_tree = (Object)adaptor.create(PERIOD34);
            adaptor.addChild(root_0, PERIOD34_tree);
            }
            if ( state.backtracking==0 ) {

                      if(st != null){
                          if((st!=null?st.getText():null).equals("$")) retval.cq.isView = true;
                      }
              		    if(vt != null){
              		        if((vt!=null?vt.getText():null).equals("@")) ml.registerPostprocRule(retval.cq);
                          else if((vt!=null?vt.getText():null).equals("#")){
                             // ml.registerDatalogRule(retval.cq);
                             ml.registerIntermediateRule(retval.cq);
                          }
              		    }else{
              		        ml.registerDatalogRule(retval.cq);
              		    }
                   
            }

            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 13, datalogRule_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "datalogRule"

    public static class scopingRule_return extends ParserRuleReturnScope {
        public ConjunctiveQuery cq = new ConjunctiveQuery();;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "scopingRule"
    // MLN.g:305:1: scopingRule returns [ConjunctiveQuery cq = new ConjunctiveQuery();] : ( '[' 'priorProb' '=' prior= NUMBER ']' )? head= literal ':=' body0= literal ( ',' bodyp= literal )* ( ',' mc= mathComparison )* ( ',' '[' be= boolExpression ']' )? PERIOD ;
    public final MLNParser.scopingRule_return scopingRule() throws RecognitionException {
        MLNParser.scopingRule_return retval = new MLNParser.scopingRule_return();
        retval.start = input.LT(1);
        int scopingRule_StartIndex = input.index();
        Object root_0 = null;

        Token prior=null;
        Token char_literal35=null;
        Token string_literal36=null;
        Token char_literal37=null;
        Token char_literal38=null;
        Token string_literal39=null;
        Token char_literal40=null;
        Token char_literal41=null;
        Token char_literal42=null;
        Token char_literal43=null;
        Token char_literal44=null;
        Token PERIOD45=null;
        MLNParser.literal_return head = null;

        MLNParser.literal_return body0 = null;

        MLNParser.literal_return bodyp = null;

        MLNParser.mathComparison_return mc = null;

        MLNParser.boolExpression_return be = null;


        Object prior_tree=null;
        Object char_literal35_tree=null;
        Object string_literal36_tree=null;
        Object char_literal37_tree=null;
        Object char_literal38_tree=null;
        Object string_literal39_tree=null;
        Object char_literal40_tree=null;
        Object char_literal41_tree=null;
        Object char_literal42_tree=null;
        Object char_literal43_tree=null;
        Object char_literal44_tree=null;
        Object PERIOD45_tree=null;

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 14) ) { return retval; }
            // MLN.g:306:5: ( ( '[' 'priorProb' '=' prior= NUMBER ']' )? head= literal ':=' body0= literal ( ',' bodyp= literal )* ( ',' mc= mathComparison )* ( ',' '[' be= boolExpression ']' )? PERIOD )
            // MLN.g:306:9: ( '[' 'priorProb' '=' prior= NUMBER ']' )? head= literal ':=' body0= literal ( ',' bodyp= literal )* ( ',' mc= mathComparison )* ( ',' '[' be= boolExpression ']' )? PERIOD
            {
            root_0 = (Object)adaptor.nil();

            // MLN.g:306:9: ( '[' 'priorProb' '=' prior= NUMBER ']' )?
            int alt23=2;
            int LA23_0 = input.LA(1);

            if ( (LA23_0==37) ) {
                alt23=1;
            }
            switch (alt23) {
                case 1 :
                    // MLN.g:306:10: '[' 'priorProb' '=' prior= NUMBER ']'
                    {
                    char_literal35=(Token)match(input,37,FOLLOW_37_in_scopingRule1561); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    char_literal35_tree = (Object)adaptor.create(char_literal35);
                    adaptor.addChild(root_0, char_literal35_tree);
                    }
                    string_literal36=(Token)match(input,39,FOLLOW_39_in_scopingRule1563); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    string_literal36_tree = (Object)adaptor.create(string_literal36);
                    adaptor.addChild(root_0, string_literal36_tree);
                    }
                    char_literal37=(Token)match(input,40,FOLLOW_40_in_scopingRule1565); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    char_literal37_tree = (Object)adaptor.create(char_literal37);
                    adaptor.addChild(root_0, char_literal37_tree);
                    }
                    prior=(Token)match(input,NUMBER,FOLLOW_NUMBER_in_scopingRule1569); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    prior_tree = (Object)adaptor.create(prior);
                    adaptor.addChild(root_0, prior_tree);
                    }
                    char_literal38=(Token)match(input,38,FOLLOW_38_in_scopingRule1571); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    char_literal38_tree = (Object)adaptor.create(char_literal38);
                    adaptor.addChild(root_0, char_literal38_tree);
                    }

                    }
                    break;

            }

            pushFollow(FOLLOW_literal_in_scopingRule1577);
            head=literal();

            state._fsp--;
            if (state.failed) return retval;
            if ( state.backtracking==0 ) adaptor.addChild(root_0, head.getTree());
            if ( state.backtracking==0 ) {
              retval.cq.setHead((head!=null?head.lit:null));
            }
            string_literal39=(Token)match(input,41,FOLLOW_41_in_scopingRule1581); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            string_literal39_tree = (Object)adaptor.create(string_literal39);
            adaptor.addChild(root_0, string_literal39_tree);
            }
            pushFollow(FOLLOW_literal_in_scopingRule1593);
            body0=literal();

            state._fsp--;
            if (state.failed) return retval;
            if ( state.backtracking==0 ) adaptor.addChild(root_0, body0.getTree());
            if ( state.backtracking==0 ) {

                        if((body0!=null?body0.lit:null)!=null){
                          retval.cq.addBodyLit((body0!=null?body0.lit:null));
                        }
                      
            }
            // MLN.g:312:9: ( ',' bodyp= literal )*
            loop24:
            do {
                int alt24=2;
                alt24 = dfa24.predict(input);
                switch (alt24) {
            	case 1 :
            	    // MLN.g:312:10: ',' bodyp= literal
            	    {
            	    char_literal40=(Token)match(input,24,FOLLOW_24_in_scopingRule1606); if (state.failed) return retval;
            	    if ( state.backtracking==0 ) {
            	    char_literal40_tree = (Object)adaptor.create(char_literal40);
            	    adaptor.addChild(root_0, char_literal40_tree);
            	    }
            	    pushFollow(FOLLOW_literal_in_scopingRule1610);
            	    bodyp=literal();

            	    state._fsp--;
            	    if (state.failed) return retval;
            	    if ( state.backtracking==0 ) adaptor.addChild(root_0, bodyp.getTree());
            	    if ( state.backtracking==0 ) {

            	                if((bodyp!=null?bodyp.lit:null)!=null){
            	                  retval.cq.addBodyLit((bodyp!=null?bodyp.lit:null));
            	                }
            	              
            	    }

            	    }
            	    break;

            	default :
            	    break loop24;
                }
            } while (true);

            // MLN.g:317:9: ( ',' mc= mathComparison )*
            loop25:
            do {
                int alt25=2;
                int LA25_0 = input.LA(1);

                if ( (LA25_0==24) ) {
                    int LA25_1 = input.LA(2);

                    if ( (LA25_1==STRING||LA25_1==NUMBER||LA25_1==ID||LA25_1==23||LA25_1==61) ) {
                        alt25=1;
                    }


                }


                switch (alt25) {
            	case 1 :
            	    // MLN.g:317:10: ',' mc= mathComparison
            	    {
            	    char_literal41=(Token)match(input,24,FOLLOW_24_in_scopingRule1625); if (state.failed) return retval;
            	    if ( state.backtracking==0 ) {
            	    char_literal41_tree = (Object)adaptor.create(char_literal41);
            	    adaptor.addChild(root_0, char_literal41_tree);
            	    }
            	    pushFollow(FOLLOW_mathComparison_in_scopingRule1629);
            	    mc=mathComparison();

            	    state._fsp--;
            	    if (state.failed) return retval;
            	    if ( state.backtracking==0 ) adaptor.addChild(root_0, mc.getTree());
            	    if ( state.backtracking==0 ) {

            	                retval.cq.addConstraint((mc!=null?mc.expr:null));
            	              
            	    }

            	    }
            	    break;

            	default :
            	    break loop25;
                }
            } while (true);

            // MLN.g:320:8: ( ',' '[' be= boolExpression ']' )?
            int alt26=2;
            int LA26_0 = input.LA(1);

            if ( (LA26_0==24) ) {
                alt26=1;
            }
            switch (alt26) {
                case 1 :
                    // MLN.g:320:9: ',' '[' be= boolExpression ']'
                    {
                    char_literal42=(Token)match(input,24,FOLLOW_24_in_scopingRule1643); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    char_literal42_tree = (Object)adaptor.create(char_literal42);
                    adaptor.addChild(root_0, char_literal42_tree);
                    }
                    char_literal43=(Token)match(input,37,FOLLOW_37_in_scopingRule1645); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    char_literal43_tree = (Object)adaptor.create(char_literal43);
                    adaptor.addChild(root_0, char_literal43_tree);
                    }
                    pushFollow(FOLLOW_boolExpression_in_scopingRule1660);
                    be=boolExpression();

                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, be.getTree());
                    if ( state.backtracking==0 ) {

                                  retval.cq.addConstraint((be!=null?be.be:null));
                                 
                    }
                    char_literal44=(Token)match(input,38,FOLLOW_38_in_scopingRule1671); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    char_literal44_tree = (Object)adaptor.create(char_literal44);
                    adaptor.addChild(root_0, char_literal44_tree);
                    }

                    }
                    break;

            }

            PERIOD45=(Token)match(input,PERIOD,FOLLOW_PERIOD_in_scopingRule1680); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            PERIOD45_tree = (Object)adaptor.create(PERIOD45);
            adaptor.addChild(root_0, PERIOD45_tree);
            }
            if ( state.backtracking==0 ) {

                    if(prior != null){
                      double pr = Double.parseDouble((prior!=null?prior.getText():null));
                      if(pr > 1 || pr < 0){
                          die("Line #" + (lineOffset+prior.getLine()) + ": " + (prior!=null?prior.getText():null) + " - probabilities of soft evidence should be in [0,1]");
                      }else{
                        retval.cq.setNewTuplePrior(pr);
                      }
                    }
                   ml.registerScopingRule(retval.cq);
                   
            }

            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 14, scopingRule_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "scopingRule"

    public static class foclause_return extends ParserRuleReturnScope {
        public Clause c = new Clause();
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "foclause"
    // MLN.g:339:1: foclause returns [Clause c = new Clause()] : (exq= existQuan )? (ant0= literal ( ',' antp= literal )* ( ',' mc= mathComparison )* ( ',' '[' be= boolExpression ']' )? IMPLIES )? lit0= literal ( 'v' litp= literal )* ( 'v' mc= mathComparison )* ( 'v' '[' sbe= boolExpression ']' )? ;
    public final MLNParser.foclause_return foclause() throws RecognitionException {
        MLNParser.foclause_return retval = new MLNParser.foclause_return();
        retval.start = input.LT(1);
        int foclause_StartIndex = input.index();
        Object root_0 = null;

        Token char_literal46=null;
        Token char_literal47=null;
        Token char_literal48=null;
        Token char_literal49=null;
        Token char_literal50=null;
        Token IMPLIES51=null;
        Token char_literal52=null;
        Token char_literal53=null;
        Token char_literal54=null;
        Token char_literal55=null;
        Token char_literal56=null;
        MLNParser.existQuan_return exq = null;

        MLNParser.literal_return ant0 = null;

        MLNParser.literal_return antp = null;

        MLNParser.mathComparison_return mc = null;

        MLNParser.boolExpression_return be = null;

        MLNParser.literal_return lit0 = null;

        MLNParser.literal_return litp = null;

        MLNParser.boolExpression_return sbe = null;


        Object char_literal46_tree=null;
        Object char_literal47_tree=null;
        Object char_literal48_tree=null;
        Object char_literal49_tree=null;
        Object char_literal50_tree=null;
        Object IMPLIES51_tree=null;
        Object char_literal52_tree=null;
        Object char_literal53_tree=null;
        Object char_literal54_tree=null;
        Object char_literal55_tree=null;
        Object char_literal56_tree=null;

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 15) ) { return retval; }
            // MLN.g:407:5: ( (exq= existQuan )? (ant0= literal ( ',' antp= literal )* ( ',' mc= mathComparison )* ( ',' '[' be= boolExpression ']' )? IMPLIES )? lit0= literal ( 'v' litp= literal )* ( 'v' mc= mathComparison )* ( 'v' '[' sbe= boolExpression ']' )? )
            // MLN.g:407:9: (exq= existQuan )? (ant0= literal ( ',' antp= literal )* ( ',' mc= mathComparison )* ( ',' '[' be= boolExpression ']' )? IMPLIES )? lit0= literal ( 'v' litp= literal )* ( 'v' mc= mathComparison )* ( 'v' '[' sbe= boolExpression ']' )?
            {
            root_0 = (Object)adaptor.nil();

            // MLN.g:407:12: (exq= existQuan )?
            int alt27=2;
            int LA27_0 = input.LA(1);

            if ( (LA27_0==EXIST) ) {
                alt27=1;
            }
            switch (alt27) {
                case 1 :
                    // MLN.g:0:0: exq= existQuan
                    {
                    pushFollow(FOLLOW_existQuan_in_foclause1717);
                    exq=existQuan();

                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, exq.getTree());

                    }
                    break;

            }

            // MLN.g:408:6: (ant0= literal ( ',' antp= literal )* ( ',' mc= mathComparison )* ( ',' '[' be= boolExpression ']' )? IMPLIES )?
            int alt31=2;
            alt31 = dfa31.predict(input);
            switch (alt31) {
                case 1 :
                    // MLN.g:409:9: ant0= literal ( ',' antp= literal )* ( ',' mc= mathComparison )* ( ',' '[' be= boolExpression ']' )? IMPLIES
                    {
                    pushFollow(FOLLOW_literal_in_foclause1739);
                    ant0=literal();

                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, ant0.getTree());
                    if ( state.backtracking==0 ) {

                                if((ant0!=null?ant0.lit:null)!=null){
                                  (ant0!=null?ant0.lit:null).flipSense(); 
                                  retval.c.addLiteral((ant0!=null?ant0.lit:null));
                                }
                              
                    }
                    // MLN.g:415:9: ( ',' antp= literal )*
                    loop28:
                    do {
                        int alt28=2;
                        alt28 = dfa28.predict(input);
                        switch (alt28) {
                    	case 1 :
                    	    // MLN.g:415:10: ',' antp= literal
                    	    {
                    	    char_literal46=(Token)match(input,24,FOLLOW_24_in_foclause1752); if (state.failed) return retval;
                    	    if ( state.backtracking==0 ) {
                    	    char_literal46_tree = (Object)adaptor.create(char_literal46);
                    	    adaptor.addChild(root_0, char_literal46_tree);
                    	    }
                    	    pushFollow(FOLLOW_literal_in_foclause1756);
                    	    antp=literal();

                    	    state._fsp--;
                    	    if (state.failed) return retval;
                    	    if ( state.backtracking==0 ) adaptor.addChild(root_0, antp.getTree());
                    	    if ( state.backtracking==0 ) {

                    	                if((antp!=null?antp.lit:null)!=null){
                    	                  (antp!=null?antp.lit:null).flipSense(); 
                    	                  retval.c.addLiteral((antp!=null?antp.lit:null));
                    	                }
                    	              
                    	    }

                    	    }
                    	    break;

                    	default :
                    	    break loop28;
                        }
                    } while (true);

                    // MLN.g:421:9: ( ',' mc= mathComparison )*
                    loop29:
                    do {
                        int alt29=2;
                        int LA29_0 = input.LA(1);

                        if ( (LA29_0==24) ) {
                            int LA29_1 = input.LA(2);

                            if ( (LA29_1==STRING||LA29_1==NUMBER||LA29_1==ID||LA29_1==23||LA29_1==61) ) {
                                alt29=1;
                            }


                        }


                        switch (alt29) {
                    	case 1 :
                    	    // MLN.g:421:10: ',' mc= mathComparison
                    	    {
                    	    char_literal47=(Token)match(input,24,FOLLOW_24_in_foclause1771); if (state.failed) return retval;
                    	    if ( state.backtracking==0 ) {
                    	    char_literal47_tree = (Object)adaptor.create(char_literal47);
                    	    adaptor.addChild(root_0, char_literal47_tree);
                    	    }
                    	    pushFollow(FOLLOW_mathComparison_in_foclause1775);
                    	    mc=mathComparison();

                    	    state._fsp--;
                    	    if (state.failed) return retval;
                    	    if ( state.backtracking==0 ) adaptor.addChild(root_0, mc.getTree());
                    	    if ( state.backtracking==0 ) {

                    	                retval.c.addConstraint((mc!=null?mc.expr:null));
                    	              
                    	    }

                    	    }
                    	    break;

                    	default :
                    	    break loop29;
                        }
                    } while (true);

                    // MLN.g:424:7: ( ',' '[' be= boolExpression ']' )?
                    int alt30=2;
                    int LA30_0 = input.LA(1);

                    if ( (LA30_0==24) ) {
                        alt30=1;
                    }
                    switch (alt30) {
                        case 1 :
                            // MLN.g:424:8: ',' '[' be= boolExpression ']'
                            {
                            char_literal48=(Token)match(input,24,FOLLOW_24_in_foclause1788); if (state.failed) return retval;
                            if ( state.backtracking==0 ) {
                            char_literal48_tree = (Object)adaptor.create(char_literal48);
                            adaptor.addChild(root_0, char_literal48_tree);
                            }
                            char_literal49=(Token)match(input,37,FOLLOW_37_in_foclause1790); if (state.failed) return retval;
                            if ( state.backtracking==0 ) {
                            char_literal49_tree = (Object)adaptor.create(char_literal49);
                            adaptor.addChild(root_0, char_literal49_tree);
                            }
                            pushFollow(FOLLOW_boolExpression_in_foclause1804);
                            be=boolExpression();

                            state._fsp--;
                            if (state.failed) return retval;
                            if ( state.backtracking==0 ) adaptor.addChild(root_0, be.getTree());
                            if ( state.backtracking==0 ) {

                              	          retval.c.addConstraint((be!=null?be.be:null));
                              	         
                            }
                            char_literal50=(Token)match(input,38,FOLLOW_38_in_foclause1814); if (state.failed) return retval;
                            if ( state.backtracking==0 ) {
                            char_literal50_tree = (Object)adaptor.create(char_literal50);
                            adaptor.addChild(root_0, char_literal50_tree);
                            }

                            }
                            break;

                    }

                    IMPLIES51=(Token)match(input,IMPLIES,FOLLOW_IMPLIES_in_foclause1835); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    IMPLIES51_tree = (Object)adaptor.create(IMPLIES51);
                    adaptor.addChild(root_0, IMPLIES51_tree);
                    }

                    }
                    break;

            }

            pushFollow(FOLLOW_literal_in_foclause1853);
            lit0=literal();

            state._fsp--;
            if (state.failed) return retval;
            if ( state.backtracking==0 ) adaptor.addChild(root_0, lit0.getTree());
            if ( state.backtracking==0 ) {
              retval.c.addLiteral((lit0!=null?lit0.lit:null));
            }
            // MLN.g:433:6: ( 'v' litp= literal )*
            loop32:
            do {
                int alt32=2;
                alt32 = dfa32.predict(input);
                switch (alt32) {
            	case 1 :
            	    // MLN.g:433:7: 'v' litp= literal
            	    {
            	    char_literal52=(Token)match(input,42,FOLLOW_42_in_foclause1863); if (state.failed) return retval;
            	    if ( state.backtracking==0 ) {
            	    char_literal52_tree = (Object)adaptor.create(char_literal52);
            	    adaptor.addChild(root_0, char_literal52_tree);
            	    }
            	    pushFollow(FOLLOW_literal_in_foclause1867);
            	    litp=literal();

            	    state._fsp--;
            	    if (state.failed) return retval;
            	    if ( state.backtracking==0 ) adaptor.addChild(root_0, litp.getTree());
            	    if ( state.backtracking==0 ) {
            	      retval.c.addLiteral((litp!=null?litp.lit:null));
            	    }

            	    }
            	    break;

            	default :
            	    break loop32;
                }
            } while (true);

            // MLN.g:434:9: ( 'v' mc= mathComparison )*
            loop33:
            do {
                int alt33=2;
                int LA33_0 = input.LA(1);

                if ( (LA33_0==42) ) {
                    int LA33_1 = input.LA(2);

                    if ( (LA33_1==STRING||LA33_1==NUMBER||LA33_1==ID||LA33_1==23||LA33_1==61) ) {
                        alt33=1;
                    }


                }


                switch (alt33) {
            	case 1 :
            	    // MLN.g:434:10: 'v' mc= mathComparison
            	    {
            	    char_literal53=(Token)match(input,42,FOLLOW_42_in_foclause1882); if (state.failed) return retval;
            	    if ( state.backtracking==0 ) {
            	    char_literal53_tree = (Object)adaptor.create(char_literal53);
            	    adaptor.addChild(root_0, char_literal53_tree);
            	    }
            	    pushFollow(FOLLOW_mathComparison_in_foclause1886);
            	    mc=mathComparison();

            	    state._fsp--;
            	    if (state.failed) return retval;
            	    if ( state.backtracking==0 ) adaptor.addChild(root_0, mc.getTree());
            	    if ( state.backtracking==0 ) {

            	                retval.c.addConstraint(Expression.not((mc!=null?mc.expr:null)));
            	              
            	    }

            	    }
            	    break;

            	default :
            	    break loop33;
                }
            } while (true);

            // MLN.g:438:6: ( 'v' '[' sbe= boolExpression ']' )?
            int alt34=2;
            int LA34_0 = input.LA(1);

            if ( (LA34_0==42) ) {
                alt34=1;
            }
            switch (alt34) {
                case 1 :
                    // MLN.g:438:7: 'v' '[' sbe= boolExpression ']'
                    {
                    char_literal54=(Token)match(input,42,FOLLOW_42_in_foclause1904); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    char_literal54_tree = (Object)adaptor.create(char_literal54);
                    adaptor.addChild(root_0, char_literal54_tree);
                    }
                    char_literal55=(Token)match(input,37,FOLLOW_37_in_foclause1906); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    char_literal55_tree = (Object)adaptor.create(char_literal55);
                    adaptor.addChild(root_0, char_literal55_tree);
                    }
                    pushFollow(FOLLOW_boolExpression_in_foclause1919);
                    sbe=boolExpression();

                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, sbe.getTree());
                    if ( state.backtracking==0 ) {

                                retval.c.addConstraint(Expression.not((sbe!=null?sbe.be:null)));
                               
                    }
                    char_literal56=(Token)match(input,38,FOLLOW_38_in_foclause1928); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    char_literal56_tree = (Object)adaptor.create(char_literal56);
                    adaptor.addChild(root_0, char_literal56_tree);
                    }

                    }
                    break;

            }

            if ( state.backtracking==0 ) {

                    if(exq != null){
                       for(String v : exq.vars){
                         retval.c.addExistentialVariable(v);
                       }
                    }
                    retval.c.addUserProvidedName(clauseName);
                    if(!clauseLabelTrailing) clauseName = null;
                  
            }

            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 15, foclause_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "foclause"

    public static class existQuan_return extends ParserRuleReturnScope {
        public ArrayList<String> vars = new ArrayList<String>();
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "existQuan"
    // MLN.g:454:1: existQuan returns [ArrayList<String> vars = new ArrayList<String>()] : EXIST v0= ID ( ',' vp= ID )* ;
    public final MLNParser.existQuan_return existQuan() throws RecognitionException {
        MLNParser.existQuan_return retval = new MLNParser.existQuan_return();
        retval.start = input.LT(1);
        int existQuan_StartIndex = input.index();
        Object root_0 = null;

        Token v0=null;
        Token vp=null;
        Token EXIST57=null;
        Token char_literal58=null;

        Object v0_tree=null;
        Object vp_tree=null;
        Object EXIST57_tree=null;
        Object char_literal58_tree=null;

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 16) ) { return retval; }
            // MLN.g:455:5: ( EXIST v0= ID ( ',' vp= ID )* )
            // MLN.g:455:9: EXIST v0= ID ( ',' vp= ID )*
            {
            root_0 = (Object)adaptor.nil();

            EXIST57=(Token)match(input,EXIST,FOLLOW_EXIST_in_existQuan1959); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            EXIST57_tree = (Object)adaptor.create(EXIST57);
            adaptor.addChild(root_0, EXIST57_tree);
            }
            v0=(Token)match(input,ID,FOLLOW_ID_in_existQuan1971); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            v0_tree = (Object)adaptor.create(v0);
            adaptor.addChild(root_0, v0_tree);
            }
            if ( state.backtracking==0 ) {
              retval.vars.add((v0!=null?v0.getText():null));
            }
            // MLN.g:457:9: ( ',' vp= ID )*
            loop35:
            do {
                int alt35=2;
                int LA35_0 = input.LA(1);

                if ( (LA35_0==24) ) {
                    alt35=1;
                }


                switch (alt35) {
            	case 1 :
            	    // MLN.g:457:10: ',' vp= ID
            	    {
            	    char_literal58=(Token)match(input,24,FOLLOW_24_in_existQuan1984); if (state.failed) return retval;
            	    if ( state.backtracking==0 ) {
            	    char_literal58_tree = (Object)adaptor.create(char_literal58);
            	    adaptor.addChild(root_0, char_literal58_tree);
            	    }
            	    vp=(Token)match(input,ID,FOLLOW_ID_in_existQuan1988); if (state.failed) return retval;
            	    if ( state.backtracking==0 ) {
            	    vp_tree = (Object)adaptor.create(vp);
            	    adaptor.addChild(root_0, vp_tree);
            	    }
            	    if ( state.backtracking==0 ) {
            	      retval.vars.add((vp!=null?vp.getText():null));
            	    }

            	    }
            	    break;

            	default :
            	    break loop35;
                }
            } while (true);


            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 16, existQuan_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "existQuan"

    public static class expression_return extends ParserRuleReturnScope {
        public Expression expr;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "expression"
    // MLN.g:460:1: expression returns [Expression expr] : be= mathExpression ;
    public final MLNParser.expression_return expression() throws RecognitionException {
        MLNParser.expression_return retval = new MLNParser.expression_return();
        retval.start = input.LT(1);
        int expression_StartIndex = input.index();
        Object root_0 = null;

        MLNParser.mathExpression_return be = null;



        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 17) ) { return retval; }
            // MLN.g:461:5: (be= mathExpression )
            // MLN.g:462:5: be= mathExpression
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_mathExpression_in_expression2015);
            be=mathExpression();

            state._fsp--;
            if (state.failed) return retval;
            if ( state.backtracking==0 ) adaptor.addChild(root_0, be.getTree());

            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 17, expression_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "expression"

    public static class boolExpression_return extends ParserRuleReturnScope {
        public Expression be;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "boolExpression"
    // MLN.g:465:1: boolExpression returns [Expression be] : c0= boolConjunction ( ( '||' | 'OR' ) cp= boolConjunction )* ;
    public final MLNParser.boolExpression_return boolExpression() throws RecognitionException {
        MLNParser.boolExpression_return retval = new MLNParser.boolExpression_return();
        retval.start = input.LT(1);
        int boolExpression_StartIndex = input.index();
        Object root_0 = null;

        Token set59=null;
        MLNParser.boolConjunction_return c0 = null;

        MLNParser.boolConjunction_return cp = null;


        Object set59_tree=null;

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 18) ) { return retval; }
            // MLN.g:466:5: (c0= boolConjunction ( ( '||' | 'OR' ) cp= boolConjunction )* )
            // MLN.g:467:5: c0= boolConjunction ( ( '||' | 'OR' ) cp= boolConjunction )*
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_boolConjunction_in_boolExpression2042);
            c0=boolConjunction();

            state._fsp--;
            if (state.failed) return retval;
            if ( state.backtracking==0 ) adaptor.addChild(root_0, c0.getTree());
            if ( state.backtracking==0 ) {

                      retval.be = (c0!=null?c0.be:null);
                  
            }
            // MLN.g:471:5: ( ( '||' | 'OR' ) cp= boolConjunction )*
            loop36:
            do {
                int alt36=2;
                int LA36_0 = input.LA(1);

                if ( ((LA36_0>=43 && LA36_0<=44)) ) {
                    alt36=1;
                }


                switch (alt36) {
            	case 1 :
            	    // MLN.g:472:9: ( '||' | 'OR' ) cp= boolConjunction
            	    {
            	    set59=(Token)input.LT(1);
            	    if ( (input.LA(1)>=43 && input.LA(1)<=44) ) {
            	        input.consume();
            	        if ( state.backtracking==0 ) adaptor.addChild(root_0, (Object)adaptor.create(set59));
            	        state.errorRecovery=false;state.failed=false;
            	    }
            	    else {
            	        if (state.backtracking>0) {state.failed=true; return retval;}
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        throw mse;
            	    }

            	    pushFollow(FOLLOW_boolConjunction_in_boolExpression2074);
            	    cp=boolConjunction();

            	    state._fsp--;
            	    if (state.failed) return retval;
            	    if ( state.backtracking==0 ) adaptor.addChild(root_0, cp.getTree());
            	    if ( state.backtracking==0 ) {

            	      	        Expression enew = new Expression(Function.OR);
            	      	        enew.addArgument(retval.be);
            	      	        enew.addArgument((cp!=null?cp.be:null));
            	      	        retval.be = enew;
            	              
            	    }

            	    }
            	    break;

            	default :
            	    break loop36;
                }
            } while (true);


            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 18, boolExpression_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "boolExpression"

    public static class boolConjunction_return extends ParserRuleReturnScope {
        public Expression be;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "boolConjunction"
    // MLN.g:482:1: boolConjunction returns [Expression be] : c0= boolConjunctionElement ( ( '&&' | 'AND' ) cp= boolConjunctionElement )* ;
    public final MLNParser.boolConjunction_return boolConjunction() throws RecognitionException {
        MLNParser.boolConjunction_return retval = new MLNParser.boolConjunction_return();
        retval.start = input.LT(1);
        int boolConjunction_StartIndex = input.index();
        Object root_0 = null;

        Token set60=null;
        MLNParser.boolConjunctionElement_return c0 = null;

        MLNParser.boolConjunctionElement_return cp = null;


        Object set60_tree=null;

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 19) ) { return retval; }
            // MLN.g:483:5: (c0= boolConjunctionElement ( ( '&&' | 'AND' ) cp= boolConjunctionElement )* )
            // MLN.g:484:5: c0= boolConjunctionElement ( ( '&&' | 'AND' ) cp= boolConjunctionElement )*
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_boolConjunctionElement_in_boolConjunction2118);
            c0=boolConjunctionElement();

            state._fsp--;
            if (state.failed) return retval;
            if ( state.backtracking==0 ) adaptor.addChild(root_0, c0.getTree());
            if ( state.backtracking==0 ) {

                      retval.be = (c0!=null?c0.be:null);
                  
            }
            // MLN.g:488:5: ( ( '&&' | 'AND' ) cp= boolConjunctionElement )*
            loop37:
            do {
                int alt37=2;
                int LA37_0 = input.LA(1);

                if ( ((LA37_0>=45 && LA37_0<=46)) ) {
                    alt37=1;
                }


                switch (alt37) {
            	case 1 :
            	    // MLN.g:489:9: ( '&&' | 'AND' ) cp= boolConjunctionElement
            	    {
            	    set60=(Token)input.LT(1);
            	    if ( (input.LA(1)>=45 && input.LA(1)<=46) ) {
            	        input.consume();
            	        if ( state.backtracking==0 ) adaptor.addChild(root_0, (Object)adaptor.create(set60));
            	        state.errorRecovery=false;state.failed=false;
            	    }
            	    else {
            	        if (state.backtracking>0) {state.failed=true; return retval;}
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        throw mse;
            	    }

            	    pushFollow(FOLLOW_boolConjunctionElement_in_boolConjunction2150);
            	    cp=boolConjunctionElement();

            	    state._fsp--;
            	    if (state.failed) return retval;
            	    if ( state.backtracking==0 ) adaptor.addChild(root_0, cp.getTree());
            	    if ( state.backtracking==0 ) {

            	                Expression enew = new Expression(Function.AND);
            	                enew.addArgument(retval.be);
            	                enew.addArgument((cp!=null?cp.be:null));
            	                retval.be = enew;
            	              
            	    }

            	    }
            	    break;

            	default :
            	    break loop37;
                }
            } while (true);


            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 19, boolConjunction_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "boolConjunction"

    public static class boolConjunctionElement_return extends ParserRuleReturnScope {
        public Expression be;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "boolConjunctionElement"
    // MLN.g:499:1: boolConjunctionElement returns [Expression be] : ( (negc= ( '!' | 'NOT' ) )? (mc= mathComparison | fe= funcExpression ) | (negb= ( '!' | 'NOT' ) )? '(' b= boolExpression ')' );
    public final MLNParser.boolConjunctionElement_return boolConjunctionElement() throws RecognitionException {
        MLNParser.boolConjunctionElement_return retval = new MLNParser.boolConjunctionElement_return();
        retval.start = input.LT(1);
        int boolConjunctionElement_StartIndex = input.index();
        Object root_0 = null;

        Token negc=null;
        Token negb=null;
        Token char_literal61=null;
        Token char_literal62=null;
        MLNParser.mathComparison_return mc = null;

        MLNParser.funcExpression_return fe = null;

        MLNParser.boolExpression_return b = null;


        Object negc_tree=null;
        Object negb_tree=null;
        Object char_literal61_tree=null;
        Object char_literal62_tree=null;

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 20) ) { return retval; }
            // MLN.g:500:5: ( (negc= ( '!' | 'NOT' ) )? (mc= mathComparison | fe= funcExpression ) | (negb= ( '!' | 'NOT' ) )? '(' b= boolExpression ')' )
            int alt41=2;
            switch ( input.LA(1) ) {
            case NOT:
            case 47:
                {
                int LA41_1 = input.LA(2);

                if ( (synpred52_MLN()) ) {
                    alt41=1;
                }
                else if ( (true) ) {
                    alt41=2;
                }
                else {
                    if (state.backtracking>0) {state.failed=true; return retval;}
                    NoViableAltException nvae =
                        new NoViableAltException("", 41, 1, input);

                    throw nvae;
                }
                }
                break;
            case STRING:
            case NUMBER:
            case ID:
            case 61:
                {
                alt41=1;
                }
                break;
            case 23:
                {
                int LA41_5 = input.LA(2);

                if ( (synpred52_MLN()) ) {
                    alt41=1;
                }
                else if ( (true) ) {
                    alt41=2;
                }
                else {
                    if (state.backtracking>0) {state.failed=true; return retval;}
                    NoViableAltException nvae =
                        new NoViableAltException("", 41, 5, input);

                    throw nvae;
                }
                }
                break;
            default:
                if (state.backtracking>0) {state.failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("", 41, 0, input);

                throw nvae;
            }

            switch (alt41) {
                case 1 :
                    // MLN.g:501:5: (negc= ( '!' | 'NOT' ) )? (mc= mathComparison | fe= funcExpression )
                    {
                    root_0 = (Object)adaptor.nil();

                    // MLN.g:501:9: (negc= ( '!' | 'NOT' ) )?
                    int alt38=2;
                    int LA38_0 = input.LA(1);

                    if ( (LA38_0==NOT||LA38_0==47) ) {
                        alt38=1;
                    }
                    switch (alt38) {
                        case 1 :
                            // MLN.g:0:0: negc= ( '!' | 'NOT' )
                            {
                            negc=(Token)input.LT(1);
                            if ( input.LA(1)==NOT||input.LA(1)==47 ) {
                                input.consume();
                                if ( state.backtracking==0 ) adaptor.addChild(root_0, (Object)adaptor.create(negc));
                                state.errorRecovery=false;state.failed=false;
                            }
                            else {
                                if (state.backtracking>0) {state.failed=true; return retval;}
                                MismatchedSetException mse = new MismatchedSetException(null,input);
                                throw mse;
                            }


                            }
                            break;

                    }

                    // MLN.g:502:7: (mc= mathComparison | fe= funcExpression )
                    int alt39=2;
                    int LA39_0 = input.LA(1);

                    if ( (LA39_0==ID) ) {
                        int LA39_1 = input.LA(2);

                        if ( (synpred51_MLN()) ) {
                            alt39=1;
                        }
                        else if ( (true) ) {
                            alt39=2;
                        }
                        else {
                            if (state.backtracking>0) {state.failed=true; return retval;}
                            NoViableAltException nvae =
                                new NoViableAltException("", 39, 1, input);

                            throw nvae;
                        }
                    }
                    else if ( (LA39_0==STRING||LA39_0==NUMBER||LA39_0==23||LA39_0==61) ) {
                        alt39=1;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return retval;}
                        NoViableAltException nvae =
                            new NoViableAltException("", 39, 0, input);

                        throw nvae;
                    }
                    switch (alt39) {
                        case 1 :
                            // MLN.g:503:9: mc= mathComparison
                            {
                            pushFollow(FOLLOW_mathComparison_in_boolConjunctionElement2222);
                            mc=mathComparison();

                            state._fsp--;
                            if (state.failed) return retval;
                            if ( state.backtracking==0 ) adaptor.addChild(root_0, mc.getTree());
                            if ( state.backtracking==0 ) {
                              retval.be =(mc!=null?mc.expr:null);
                            }

                            }
                            break;
                        case 2 :
                            // MLN.g:504:9: fe= funcExpression
                            {
                            pushFollow(FOLLOW_funcExpression_in_boolConjunctionElement2235);
                            fe=funcExpression();

                            state._fsp--;
                            if (state.failed) return retval;
                            if ( state.backtracking==0 ) adaptor.addChild(root_0, fe.getTree());
                            if ( state.backtracking==0 ) {
                              retval.be =(fe!=null?fe.expr:null);
                            }

                            }
                            break;

                    }

                    if ( state.backtracking==0 ) {

                              if(negc != null){
                                Expression enew = new Expression(Function.NOT);
                                enew.addArgument(retval.be);
                                retval.be = enew;
                              }
                          
                    }

                    }
                    break;
                case 2 :
                    // MLN.g:513:7: (negb= ( '!' | 'NOT' ) )? '(' b= boolExpression ')'
                    {
                    root_0 = (Object)adaptor.nil();

                    // MLN.g:513:11: (negb= ( '!' | 'NOT' ) )?
                    int alt40=2;
                    int LA40_0 = input.LA(1);

                    if ( (LA40_0==NOT||LA40_0==47) ) {
                        alt40=1;
                    }
                    switch (alt40) {
                        case 1 :
                            // MLN.g:0:0: negb= ( '!' | 'NOT' )
                            {
                            negb=(Token)input.LT(1);
                            if ( input.LA(1)==NOT||input.LA(1)==47 ) {
                                input.consume();
                                if ( state.backtracking==0 ) adaptor.addChild(root_0, (Object)adaptor.create(negb));
                                state.errorRecovery=false;state.failed=false;
                            }
                            else {
                                if (state.backtracking>0) {state.failed=true; return retval;}
                                MismatchedSetException mse = new MismatchedSetException(null,input);
                                throw mse;
                            }


                            }
                            break;

                    }

                    char_literal61=(Token)match(input,23,FOLLOW_23_in_boolConjunctionElement2269); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    char_literal61_tree = (Object)adaptor.create(char_literal61);
                    adaptor.addChild(root_0, char_literal61_tree);
                    }
                    pushFollow(FOLLOW_boolExpression_in_boolConjunctionElement2273);
                    b=boolExpression();

                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, b.getTree());
                    char_literal62=(Token)match(input,25,FOLLOW_25_in_boolConjunctionElement2275); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    char_literal62_tree = (Object)adaptor.create(char_literal62);
                    adaptor.addChild(root_0, char_literal62_tree);
                    }
                    if ( state.backtracking==0 ) {

                              retval.be = (b!=null?b.be:null);
                              if(negb != null){
                                Expression enew = new Expression(Function.NOT);
                                enew.addArgument(retval.be);
                                retval.be = enew;
                              }
                          
                    }

                    }
                    break;

            }
            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 20, boolConjunctionElement_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "boolConjunctionElement"

    public static class mathComparison_return extends ParserRuleReturnScope {
        public Expression expr;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "mathComparison"
    // MLN.g:525:1: mathComparison returns [Expression expr] : e1= mathExpression op= ( '=' | '<>' | '<' | '<=' | '>' | '>=' | '!=' ) e2= mathExpression ;
    public final MLNParser.mathComparison_return mathComparison() throws RecognitionException {
        MLNParser.mathComparison_return retval = new MLNParser.mathComparison_return();
        retval.start = input.LT(1);
        int mathComparison_StartIndex = input.index();
        Object root_0 = null;

        Token op=null;
        MLNParser.mathExpression_return e1 = null;

        MLNParser.mathExpression_return e2 = null;


        Object op_tree=null;

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 21) ) { return retval; }
            // MLN.g:526:5: (e1= mathExpression op= ( '=' | '<>' | '<' | '<=' | '>' | '>=' | '!=' ) e2= mathExpression )
            // MLN.g:527:5: e1= mathExpression op= ( '=' | '<>' | '<' | '<=' | '>' | '>=' | '!=' ) e2= mathExpression
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_mathExpression_in_mathComparison2309);
            e1=mathExpression();

            state._fsp--;
            if (state.failed) return retval;
            if ( state.backtracking==0 ) adaptor.addChild(root_0, e1.getTree());
            op=(Token)input.LT(1);
            if ( input.LA(1)==40||(input.LA(1)>=48 && input.LA(1)<=53) ) {
                input.consume();
                if ( state.backtracking==0 ) adaptor.addChild(root_0, (Object)adaptor.create(op));
                state.errorRecovery=false;state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return retval;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                throw mse;
            }

            pushFollow(FOLLOW_mathExpression_in_mathComparison2331);
            e2=mathExpression();

            state._fsp--;
            if (state.failed) return retval;
            if ( state.backtracking==0 ) adaptor.addChild(root_0, e2.getTree());
            if ( state.backtracking==0 ) {

                      Function f = Function.getBuiltInFunctionByName((op!=null?op.getText():null));
                      retval.expr = new Expression(f);
                      retval.expr.addArgument((e1!=null?e1.expr:null));
                      retval.expr.addArgument((e2!=null?e2.expr:null));
                  
            }

            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 21, mathComparison_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "mathComparison"

    public static class mathExpression_return extends ParserRuleReturnScope {
        public Expression expr;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "mathExpression"
    // MLN.g:537:1: mathExpression returns [Expression expr] : t0= mathTerm (op= ( '+' | '-' | '%' ) tp= mathTerm )* ;
    public final MLNParser.mathExpression_return mathExpression() throws RecognitionException {
        MLNParser.mathExpression_return retval = new MLNParser.mathExpression_return();
        retval.start = input.LT(1);
        int mathExpression_StartIndex = input.index();
        Object root_0 = null;

        Token op=null;
        MLNParser.mathTerm_return t0 = null;

        MLNParser.mathTerm_return tp = null;


        Object op_tree=null;

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 22) ) { return retval; }
            // MLN.g:538:5: (t0= mathTerm (op= ( '+' | '-' | '%' ) tp= mathTerm )* )
            // MLN.g:539:5: t0= mathTerm (op= ( '+' | '-' | '%' ) tp= mathTerm )*
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_mathTerm_in_mathExpression2365);
            t0=mathTerm();

            state._fsp--;
            if (state.failed) return retval;
            if ( state.backtracking==0 ) adaptor.addChild(root_0, t0.getTree());
            if ( state.backtracking==0 ) {

                    retval.expr = (t0!=null?t0.expr:null);
                  
            }
            // MLN.g:543:5: (op= ( '+' | '-' | '%' ) tp= mathTerm )*
            loop42:
            do {
                int alt42=2;
                alt42 = dfa42.predict(input);
                switch (alt42) {
            	case 1 :
            	    // MLN.g:543:6: op= ( '+' | '-' | '%' ) tp= mathTerm
            	    {
            	    op=(Token)input.LT(1);
            	    if ( (input.LA(1)>=PLUS && input.LA(1)<=MINUS)||input.LA(1)==54 ) {
            	        input.consume();
            	        if ( state.backtracking==0 ) adaptor.addChild(root_0, (Object)adaptor.create(op));
            	        state.errorRecovery=false;state.failed=false;
            	    }
            	    else {
            	        if (state.backtracking>0) {state.failed=true; return retval;}
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        throw mse;
            	    }

            	    pushFollow(FOLLOW_mathTerm_in_mathExpression2391);
            	    tp=mathTerm();

            	    state._fsp--;
            	    if (state.failed) return retval;
            	    if ( state.backtracking==0 ) adaptor.addChild(root_0, tp.getTree());
            	    if ( state.backtracking==0 ) {

            	              Expression enew = null;
            	              if((op!=null?op.getText():null).equals("+")){
            	                  enew = new Expression(Function.Add);
            	              }else if((op!=null?op.getText():null).equals("-")){
            	                  enew = new Expression(Function.Subtract);
            	              }else if((op!=null?op.getText():null).equals("%")){
            	                  enew = new Expression(Function.Modulo);
            	              }
            	              enew.addArgument(retval.expr);
            	              enew.addArgument((tp!=null?tp.expr:null));
            	              retval.expr = enew;
            	            
            	    }

            	    }
            	    break;

            	default :
            	    break loop42;
                }
            } while (true);


            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 22, mathExpression_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "mathExpression"

    public static class mathTerm_return extends ParserRuleReturnScope {
        public Expression expr;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "mathTerm"
    // MLN.g:560:1: mathTerm returns [Expression expr] : t0= mathFactor (op= ( '*' | '/' | '&' | '|' | '^' | '<<' | '>>' ) tp= mathFactor )* ;
    public final MLNParser.mathTerm_return mathTerm() throws RecognitionException {
        MLNParser.mathTerm_return retval = new MLNParser.mathTerm_return();
        retval.start = input.LT(1);
        int mathTerm_StartIndex = input.index();
        Object root_0 = null;

        Token op=null;
        MLNParser.mathFactor_return t0 = null;

        MLNParser.mathFactor_return tp = null;


        Object op_tree=null;

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 23) ) { return retval; }
            // MLN.g:561:5: (t0= mathFactor (op= ( '*' | '/' | '&' | '|' | '^' | '<<' | '>>' ) tp= mathFactor )* )
            // MLN.g:562:5: t0= mathFactor (op= ( '*' | '/' | '&' | '|' | '^' | '<<' | '>>' ) tp= mathFactor )*
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_mathFactor_in_mathTerm2436);
            t0=mathFactor();

            state._fsp--;
            if (state.failed) return retval;
            if ( state.backtracking==0 ) adaptor.addChild(root_0, t0.getTree());
            if ( state.backtracking==0 ) {

                    retval.expr = (t0!=null?t0.expr:null);
                  
            }
            // MLN.g:566:5: (op= ( '*' | '/' | '&' | '|' | '^' | '<<' | '>>' ) tp= mathFactor )*
            loop43:
            do {
                int alt43=2;
                int LA43_0 = input.LA(1);

                if ( (LA43_0==ASTERISK||(LA43_0>=55 && LA43_0<=60)) ) {
                    alt43=1;
                }


                switch (alt43) {
            	case 1 :
            	    // MLN.g:566:6: op= ( '*' | '/' | '&' | '|' | '^' | '<<' | '>>' ) tp= mathFactor
            	    {
            	    op=(Token)input.LT(1);
            	    if ( input.LA(1)==ASTERISK||(input.LA(1)>=55 && input.LA(1)<=60) ) {
            	        input.consume();
            	        if ( state.backtracking==0 ) adaptor.addChild(root_0, (Object)adaptor.create(op));
            	        state.errorRecovery=false;state.failed=false;
            	    }
            	    else {
            	        if (state.backtracking>0) {state.failed=true; return retval;}
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        throw mse;
            	    }

            	    pushFollow(FOLLOW_mathFactor_in_mathTerm2470);
            	    tp=mathFactor();

            	    state._fsp--;
            	    if (state.failed) return retval;
            	    if ( state.backtracking==0 ) adaptor.addChild(root_0, tp.getTree());
            	    if ( state.backtracking==0 ) {

            	              Expression enew = null;
            	              if((op!=null?op.getText():null).equals("*")){
            	                  enew = new Expression(Function.Multiply);
            	              }else if((op!=null?op.getText():null).equals("/")){
            	                  enew = new Expression(Function.Divide);
            	              }else if((op!=null?op.getText():null).equals("&")){
            	                  enew = new Expression(Function.BitAnd);
            	              }else if((op!=null?op.getText():null).equals("|")){
            	                  enew = new Expression(Function.BitOr);
            	              }else if((op!=null?op.getText():null).equals("^")){
            	                  enew = new Expression(Function.BitXor);
            	              }else if((op!=null?op.getText():null).equals("<<")){
            	                  enew = new Expression(Function.BitShiftLeft);
            	              }else if((op!=null?op.getText():null).equals(">>")){
            	                  enew = new Expression(Function.BitShiftRight);
            	              }
            	              enew.addArgument(retval.expr);
            	              enew.addArgument((tp!=null?tp.expr:null));
            	              retval.expr = enew;
            	            
            	    }

            	    }
            	    break;

            	default :
            	    break loop43;
                }
            } while (true);


            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 23, mathTerm_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "mathTerm"

    public static class mathFactor_return extends ParserRuleReturnScope {
        public Expression expr;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "mathFactor"
    // MLN.g:590:1: mathFactor returns [Expression expr] : (fe= funcExpression | ae= atomicExpression | '(' ee= mathExpression ')' | '~' mf= mathFactor ) (fact= '!' )? ;
    public final MLNParser.mathFactor_return mathFactor() throws RecognitionException {
        MLNParser.mathFactor_return retval = new MLNParser.mathFactor_return();
        retval.start = input.LT(1);
        int mathFactor_StartIndex = input.index();
        Object root_0 = null;

        Token fact=null;
        Token char_literal63=null;
        Token char_literal64=null;
        Token char_literal65=null;
        MLNParser.funcExpression_return fe = null;

        MLNParser.atomicExpression_return ae = null;

        MLNParser.mathExpression_return ee = null;

        MLNParser.mathFactor_return mf = null;


        Object fact_tree=null;
        Object char_literal63_tree=null;
        Object char_literal64_tree=null;
        Object char_literal65_tree=null;

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 24) ) { return retval; }
            // MLN.g:591:5: ( (fe= funcExpression | ae= atomicExpression | '(' ee= mathExpression ')' | '~' mf= mathFactor ) (fact= '!' )? )
            // MLN.g:592:5: (fe= funcExpression | ae= atomicExpression | '(' ee= mathExpression ')' | '~' mf= mathFactor ) (fact= '!' )?
            {
            root_0 = (Object)adaptor.nil();

            // MLN.g:592:5: (fe= funcExpression | ae= atomicExpression | '(' ee= mathExpression ')' | '~' mf= mathFactor )
            int alt44=4;
            switch ( input.LA(1) ) {
            case ID:
                {
                int LA44_1 = input.LA(2);

                if ( (LA44_1==23) ) {
                    alt44=1;
                }
                else if ( (LA44_1==EOF||(LA44_1>=NOT && LA44_1<=IMPLIES)||LA44_1==NUMBER||LA44_1==ID||LA44_1==22||(LA44_1>=24 && LA44_1<=25)||(LA44_1>=31 && LA44_1<=35)||(LA44_1>=37 && LA44_1<=38)||LA44_1==40||(LA44_1>=42 && LA44_1<=46)||(LA44_1>=48 && LA44_1<=60)) ) {
                    alt44=2;
                }
                else {
                    if (state.backtracking>0) {state.failed=true; return retval;}
                    NoViableAltException nvae =
                        new NoViableAltException("", 44, 1, input);

                    throw nvae;
                }
                }
                break;
            case STRING:
            case NUMBER:
                {
                alt44=2;
                }
                break;
            case 23:
                {
                alt44=3;
                }
                break;
            case 61:
                {
                alt44=4;
                }
                break;
            default:
                if (state.backtracking>0) {state.failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("", 44, 0, input);

                throw nvae;
            }

            switch (alt44) {
                case 1 :
                    // MLN.g:593:7: fe= funcExpression
                    {
                    pushFollow(FOLLOW_funcExpression_in_mathFactor2519);
                    fe=funcExpression();

                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, fe.getTree());
                    if ( state.backtracking==0 ) {

                      		      retval.expr = (fe!=null?fe.expr:null);
                      		    
                    }

                    }
                    break;
                case 2 :
                    // MLN.g:597:9: ae= atomicExpression
                    {
                    pushFollow(FOLLOW_atomicExpression_in_mathFactor2539);
                    ae=atomicExpression();

                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, ae.getTree());
                    if ( state.backtracking==0 ) {

                      		      retval.expr = (ae!=null?ae.expr:null);
                      		    
                    }

                    }
                    break;
                case 3 :
                    // MLN.g:601:9: '(' ee= mathExpression ')'
                    {
                    char_literal63=(Token)match(input,23,FOLLOW_23_in_mathFactor2557); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    char_literal63_tree = (Object)adaptor.create(char_literal63);
                    adaptor.addChild(root_0, char_literal63_tree);
                    }
                    pushFollow(FOLLOW_mathExpression_in_mathFactor2561);
                    ee=mathExpression();

                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, ee.getTree());
                    char_literal64=(Token)match(input,25,FOLLOW_25_in_mathFactor2563); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    char_literal64_tree = (Object)adaptor.create(char_literal64);
                    adaptor.addChild(root_0, char_literal64_tree);
                    }
                    if ( state.backtracking==0 ) {

                      		      retval.expr = (ee!=null?ee.expr:null);
                      		    
                    }

                    }
                    break;
                case 4 :
                    // MLN.g:605:9: '~' mf= mathFactor
                    {
                    char_literal65=(Token)match(input,61,FOLLOW_61_in_mathFactor2581); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    char_literal65_tree = (Object)adaptor.create(char_literal65);
                    adaptor.addChild(root_0, char_literal65_tree);
                    }
                    pushFollow(FOLLOW_mathFactor_in_mathFactor2585);
                    mf=mathFactor();

                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, mf.getTree());
                    if ( state.backtracking==0 ) {

                      		      Expression enew = new Expression(Function.BitNeg);
                      		      enew.addArgument((mf!=null?mf.expr:null));
                      		      retval.expr = enew;
                      		    
                    }

                    }
                    break;

            }

            // MLN.g:611:11: (fact= '!' )?
            int alt45=2;
            int LA45_0 = input.LA(1);

            if ( (LA45_0==NOT) ) {
                int LA45_1 = input.LA(2);

                if ( (synpred75_MLN()) ) {
                    alt45=1;
                }
            }
            switch (alt45) {
                case 1 :
                    // MLN.g:0:0: fact= '!'
                    {
                    fact=(Token)match(input,NOT,FOLLOW_NOT_in_mathFactor2603); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    fact_tree = (Object)adaptor.create(fact);
                    adaptor.addChild(root_0, fact_tree);
                    }

                    }
                    break;

            }

            if ( state.backtracking==0 ) {

              	      if(fact != null){
              			      Expression enew = new Expression(Function.Factorial);
              			      enew.addArgument(retval.expr);
              			      retval.expr = enew;
              	      }
              	    
            }

            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 24, mathFactor_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "mathFactor"

    public static class funcExpression_return extends ParserRuleReturnScope {
        public Expression expr;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "funcExpression"
    // MLN.g:621:1: funcExpression returns [Expression expr] : func= ID '(' a0= funcArgument ( ',' ap= funcArgument )* ')' ;
    public final MLNParser.funcExpression_return funcExpression() throws RecognitionException {
        MLNParser.funcExpression_return retval = new MLNParser.funcExpression_return();
        retval.start = input.LT(1);
        int funcExpression_StartIndex = input.index();
        Object root_0 = null;

        Token func=null;
        Token char_literal66=null;
        Token char_literal67=null;
        Token char_literal68=null;
        MLNParser.funcArgument_return a0 = null;

        MLNParser.funcArgument_return ap = null;


        Object func_tree=null;
        Object char_literal66_tree=null;
        Object char_literal67_tree=null;
        Object char_literal68_tree=null;

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 25) ) { return retval; }
            // MLN.g:622:5: (func= ID '(' a0= funcArgument ( ',' ap= funcArgument )* ')' )
            // MLN.g:623:5: func= ID '(' a0= funcArgument ( ',' ap= funcArgument )* ')'
            {
            root_0 = (Object)adaptor.nil();

            func=(Token)match(input,ID,FOLLOW_ID_in_funcExpression2638); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            func_tree = (Object)adaptor.create(func);
            adaptor.addChild(root_0, func_tree);
            }
            if ( state.backtracking==0 ) {

              	        Function f = ml.getFunctionByName((func!=null?func.getText():null));
              	        if(f == null) die("Line #" + func.getLine() + 
              	        ": unknown function " + (func!=null?func.getText():null) +
              	        ". Are you putting a bool expression before a "+
              	        "regular literal in a rule? (HINT: You shouldn't.)"
              	        );
              	        retval.expr = new Expression(f);
              	    
            }
            char_literal66=(Token)match(input,23,FOLLOW_23_in_funcExpression2652); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            char_literal66_tree = (Object)adaptor.create(char_literal66);
            adaptor.addChild(root_0, char_literal66_tree);
            }
            pushFollow(FOLLOW_funcArgument_in_funcExpression2656);
            a0=funcArgument();

            state._fsp--;
            if (state.failed) return retval;
            if ( state.backtracking==0 ) adaptor.addChild(root_0, a0.getTree());
            if ( state.backtracking==0 ) {

              	        retval.expr.addArgument((a0!=null?a0.expr:null));
              	    
            }
            // MLN.g:637:5: ( ',' ap= funcArgument )*
            loop46:
            do {
                int alt46=2;
                int LA46_0 = input.LA(1);

                if ( (LA46_0==24) ) {
                    alt46=1;
                }


                switch (alt46) {
            	case 1 :
            	    // MLN.g:637:6: ',' ap= funcArgument
            	    {
            	    char_literal67=(Token)match(input,24,FOLLOW_24_in_funcExpression2671); if (state.failed) return retval;
            	    if ( state.backtracking==0 ) {
            	    char_literal67_tree = (Object)adaptor.create(char_literal67);
            	    adaptor.addChild(root_0, char_literal67_tree);
            	    }
            	    pushFollow(FOLLOW_funcArgument_in_funcExpression2675);
            	    ap=funcArgument();

            	    state._fsp--;
            	    if (state.failed) return retval;
            	    if ( state.backtracking==0 ) adaptor.addChild(root_0, ap.getTree());
            	    if ( state.backtracking==0 ) {

            	      	        retval.expr.addArgument((ap!=null?ap.expr:null));
            	      	    
            	    }

            	    }
            	    break;

            	default :
            	    break loop46;
                }
            } while (true);

            char_literal68=(Token)match(input,25,FOLLOW_25_in_funcExpression2696); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            char_literal68_tree = (Object)adaptor.create(char_literal68);
            adaptor.addChild(root_0, char_literal68_tree);
            }

            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 25, funcExpression_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "funcExpression"

    public static class funcArgument_return extends ParserRuleReturnScope {
        public Expression expr;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "funcArgument"
    // MLN.g:645:1: funcArgument returns [Expression expr] : (be= boolExpression | me= mathExpression | fe= funcExpression | ae= atomicExpression );
    public final MLNParser.funcArgument_return funcArgument() throws RecognitionException {
        MLNParser.funcArgument_return retval = new MLNParser.funcArgument_return();
        retval.start = input.LT(1);
        int funcArgument_StartIndex = input.index();
        Object root_0 = null;

        MLNParser.boolExpression_return be = null;

        MLNParser.mathExpression_return me = null;

        MLNParser.funcExpression_return fe = null;

        MLNParser.atomicExpression_return ae = null;



        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 26) ) { return retval; }
            // MLN.g:646:5: (be= boolExpression | me= mathExpression | fe= funcExpression | ae= atomicExpression )
            int alt47=4;
            alt47 = dfa47.predict(input);
            switch (alt47) {
                case 1 :
                    // MLN.g:647:5: be= boolExpression
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_boolExpression_in_funcArgument2723);
                    be=boolExpression();

                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, be.getTree());
                    if ( state.backtracking==0 ) {

                            retval.expr = (be!=null?be.be:null);
                          
                    }

                    }
                    break;
                case 2 :
                    // MLN.g:651:7: me= mathExpression
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_mathExpression_in_funcArgument2739);
                    me=mathExpression();

                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, me.getTree());
                    if ( state.backtracking==0 ) {

                            retval.expr = (me!=null?me.expr:null);
                          
                    }

                    }
                    break;
                case 3 :
                    // MLN.g:655:7: fe= funcExpression
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_funcExpression_in_funcArgument2755);
                    fe=funcExpression();

                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, fe.getTree());
                    if ( state.backtracking==0 ) {

                            retval.expr = (fe!=null?fe.expr:null);
                          
                    }

                    }
                    break;
                case 4 :
                    // MLN.g:659:6: ae= atomicExpression
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_atomicExpression_in_funcArgument2770);
                    ae=atomicExpression();

                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, ae.getTree());
                    if ( state.backtracking==0 ) {

                            retval.expr = (ae!=null?ae.expr:null);
                          
                    }

                    }
                    break;

            }
            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 26, funcArgument_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "funcArgument"

    public static class atomicExpression_return extends ParserRuleReturnScope {
        public Expression expr;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "atomicExpression"
    // MLN.g:666:1: atomicExpression returns [Expression expr] : (num= NUMBER | str= STRING | id= ID );
    public final MLNParser.atomicExpression_return atomicExpression() throws RecognitionException {
        MLNParser.atomicExpression_return retval = new MLNParser.atomicExpression_return();
        retval.start = input.LT(1);
        int atomicExpression_StartIndex = input.index();
        Object root_0 = null;

        Token num=null;
        Token str=null;
        Token id=null;

        Object num_tree=null;
        Object str_tree=null;
        Object id_tree=null;

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 27) ) { return retval; }
            // MLN.g:667:5: (num= NUMBER | str= STRING | id= ID )
            int alt48=3;
            switch ( input.LA(1) ) {
            case NUMBER:
                {
                alt48=1;
                }
                break;
            case STRING:
                {
                alt48=2;
                }
                break;
            case ID:
                {
                alt48=3;
                }
                break;
            default:
                if (state.backtracking>0) {state.failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("", 48, 0, input);

                throw nvae;
            }

            switch (alt48) {
                case 1 :
                    // MLN.g:668:5: num= NUMBER
                    {
                    root_0 = (Object)adaptor.nil();

                    num=(Token)match(input,NUMBER,FOLLOW_NUMBER_in_atomicExpression2808); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    num_tree = (Object)adaptor.create(num);
                    adaptor.addChild(root_0, num_tree);
                    }
                    if ( state.backtracking==0 ) {

                            try{
                              int n = Integer.parseInt((num!=null?num.getText():null));
                              retval.expr = Expression.exprConstInteger(n);
                            }catch(NumberFormatException e){
                              retval.expr = Expression.exprConstNum(Double.parseDouble((num!=null?num.getText():null)));
                            }
                          
                    }

                    }
                    break;
                case 2 :
                    // MLN.g:677:7: str= STRING
                    {
                    root_0 = (Object)adaptor.nil();

                    str=(Token)match(input,STRING,FOLLOW_STRING_in_atomicExpression2824); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    str_tree = (Object)adaptor.create(str);
                    adaptor.addChild(root_0, str_tree);
                    }
                    if ( state.backtracking==0 ) {

                            String s = (str!=null?str.getText():null);
                            retval.expr = Expression.exprConstString(s);
                          
                    }

                    }
                    break;
                case 3 :
                    // MLN.g:682:7: id= ID
                    {
                    root_0 = (Object)adaptor.nil();

                    id=(Token)match(input,ID,FOLLOW_ID_in_atomicExpression2840); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    id_tree = (Object)adaptor.create(id);
                    adaptor.addChild(root_0, id_tree);
                    }
                    if ( state.backtracking==0 ) {

                            String s = (id!=null?id.getText():null);
                            String init = s.substring(0,1);
                            if(init.equals(init.toUpperCase())) { // a constant
                              retval.expr = Expression.exprConstString(s);
                            }else{
                              retval.expr = Expression.exprVariableBinding(s);
                            }
                          
                    }

                    }
                    break;

            }
            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 27, atomicExpression_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "atomicExpression"

    public static class literal_return extends ParserRuleReturnScope {
        public Literal lit;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "literal"
    // MLN.g:695:1: literal returns [Literal lit] : (pref= ( PLUS | NOT ) )? atom ;
    public final MLNParser.literal_return literal() throws RecognitionException {
        MLNParser.literal_return retval = new MLNParser.literal_return();
        retval.start = input.LT(1);
        int literal_StartIndex = input.index();
        Object root_0 = null;

        Token pref=null;
        MLNParser.atom_return atom69 = null;


        Object pref_tree=null;

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 28) ) { return retval; }
            // MLN.g:696:5: ( (pref= ( PLUS | NOT ) )? atom )
            // MLN.g:696:9: (pref= ( PLUS | NOT ) )? atom
            {
            root_0 = (Object)adaptor.nil();

            // MLN.g:696:13: (pref= ( PLUS | NOT ) )?
            int alt49=2;
            int LA49_0 = input.LA(1);

            if ( ((LA49_0>=NOT && LA49_0<=PLUS)) ) {
                alt49=1;
            }
            switch (alt49) {
                case 1 :
                    // MLN.g:0:0: pref= ( PLUS | NOT )
                    {
                    pref=(Token)input.LT(1);
                    if ( (input.LA(1)>=NOT && input.LA(1)<=PLUS) ) {
                        input.consume();
                        if ( state.backtracking==0 ) adaptor.addChild(root_0, (Object)adaptor.create(pref));
                        state.errorRecovery=false;state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return retval;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        throw mse;
                    }


                    }
                    break;

            }

            pushFollow(FOLLOW_atom_in_literal2884);
            atom69=atom();

            state._fsp--;
            if (state.failed) return retval;
            if ( state.backtracking==0 ) adaptor.addChild(root_0, atom69.getTree());
            if ( state.backtracking==0 ) {

                    retval.lit = (atom69!=null?atom69.lit:null);
                    if(pref != null && retval.lit != null){
                      if((pref!=null?pref.getText():null).equals("!")) retval.lit.setSense(false);
                      else retval.lit.setCoversAllMaterializedTuples(true);
                     }
                     
                  
            }

            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 28, literal_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "literal"

    public static class term_return extends ParserRuleReturnScope {
        public Term t;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "term"
    // MLN.g:708:1: term returns [Term t] : ( ID | d= ( NUMBER | STRING ) );
    public final MLNParser.term_return term() throws RecognitionException {
        MLNParser.term_return retval = new MLNParser.term_return();
        retval.start = input.LT(1);
        int term_StartIndex = input.index();
        Object root_0 = null;

        Token d=null;
        Token ID70=null;

        Object d_tree=null;
        Object ID70_tree=null;

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 29) ) { return retval; }
            // MLN.g:709:5: ( ID | d= ( NUMBER | STRING ) )
            int alt50=2;
            int LA50_0 = input.LA(1);

            if ( (LA50_0==ID) ) {
                alt50=1;
            }
            else if ( (LA50_0==STRING||LA50_0==NUMBER) ) {
                alt50=2;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("", 50, 0, input);

                throw nvae;
            }
            switch (alt50) {
                case 1 :
                    // MLN.g:710:5: ID
                    {
                    root_0 = (Object)adaptor.nil();

                    ID70=(Token)match(input,ID,FOLLOW_ID_in_term2916); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    ID70_tree = (Object)adaptor.create(ID70);
                    adaptor.addChild(root_0, ID70_tree);
                    }
                    if ( state.backtracking==0 ) {

                                String s = (ID70!=null?ID70.getText():null);
                                String init = s.substring(0,1);
                                if(init.equals(init.toUpperCase())) { // a constant
                                  if(Config.constants_as_raw_string) {
                                    retval.t = new Term(s, true);
                                  } else {
                      			        Integer cid = ml.getSymbolID(s, null);
                      			        retval.t = new Term(cid);
                      			      }
                      			    }else{ // a variable
                      			       retval.t = new Term(s);
                      			    }
                          
                    }

                    }
                    break;
                case 2 :
                    // MLN.g:725:7: d= ( NUMBER | STRING )
                    {
                    root_0 = (Object)adaptor.nil();

                    d=(Token)input.LT(1);
                    if ( input.LA(1)==STRING||input.LA(1)==NUMBER ) {
                        input.consume();
                        if ( state.backtracking==0 ) adaptor.addChild(root_0, (Object)adaptor.create(d));
                        state.errorRecovery=false;state.failed=false;
                    }
                    else {
                        if (state.backtracking>0) {state.failed=true; return retval;}
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        throw mse;
                    }

                    if ( state.backtracking==0 ) {

                                String s = (d!=null?d.getText():null);
                                if(Config.constants_as_raw_string) {
                                  retval.t = new Term(s, true);
                                } else {
                                  Integer cid = ml.getSymbolID(s, null);
                                  retval.t = new Term(cid);
                                }
                          
                    }

                    }
                    break;

            }
            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 29, term_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "term"

    public static class atom_return extends ParserRuleReturnScope {
        public Literal lit;
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "atom"
    // MLN.g:737:1: atom returns [Literal lit] : pred= ID '(' term1= term ( ',' termp= term )* ')' ;
    public final MLNParser.atom_return atom() throws RecognitionException {
        MLNParser.atom_return retval = new MLNParser.atom_return();
        retval.start = input.LT(1);
        int atom_StartIndex = input.index();
        Object root_0 = null;

        Token pred=null;
        Token char_literal71=null;
        Token char_literal72=null;
        Token char_literal73=null;
        MLNParser.term_return term1 = null;

        MLNParser.term_return termp = null;


        Object pred_tree=null;
        Object char_literal71_tree=null;
        Object char_literal72_tree=null;
        Object char_literal73_tree=null;

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 30) ) { return retval; }
            // MLN.g:738:5: (pred= ID '(' term1= term ( ',' termp= term )* ')' )
            // MLN.g:738:9: pred= ID '(' term1= term ( ',' termp= term )* ')'
            {
            root_0 = (Object)adaptor.nil();

            pred=(Token)match(input,ID,FOLLOW_ID_in_atom2967); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            pred_tree = (Object)adaptor.create(pred);
            adaptor.addChild(root_0, pred_tree);
            }
            char_literal71=(Token)match(input,23,FOLLOW_23_in_atom2969); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            char_literal71_tree = (Object)adaptor.create(char_literal71);
            adaptor.addChild(root_0, char_literal71_tree);
            }
            if ( state.backtracking==0 ) {

                      Predicate p = ml.getPredByName((pred!=null?pred.getText():null));
                      if(p == null) die("Line #" + pred.getLine() + ": unknown predicate name - " + (pred!=null?pred.getText():null));
                      retval.lit = new Literal(p, true);
                  
            }
            pushFollow(FOLLOW_term_in_atom2984);
            term1=term();

            state._fsp--;
            if (state.failed) return retval;
            if ( state.backtracking==0 ) adaptor.addChild(root_0, term1.getTree());
            if ( state.backtracking==0 ) {

                      if((term1!=null?term1.t:null).isConstant()) retval.lit.getPred().getTypeAt(retval.lit.getTerms().size()).addConstant((term1!=null?term1.t:null).constant());
                      retval.lit.appendTerm((term1!=null?term1.t:null));
                  
            }
            // MLN.g:749:5: ( ',' termp= term )*
            loop51:
            do {
                int alt51=2;
                int LA51_0 = input.LA(1);

                if ( (LA51_0==24) ) {
                    alt51=1;
                }


                switch (alt51) {
            	case 1 :
            	    // MLN.g:749:6: ',' termp= term
            	    {
            	    char_literal72=(Token)match(input,24,FOLLOW_24_in_atom2998); if (state.failed) return retval;
            	    if ( state.backtracking==0 ) {
            	    char_literal72_tree = (Object)adaptor.create(char_literal72);
            	    adaptor.addChild(root_0, char_literal72_tree);
            	    }
            	    pushFollow(FOLLOW_term_in_atom3007);
            	    termp=term();

            	    state._fsp--;
            	    if (state.failed) return retval;
            	    if ( state.backtracking==0 ) adaptor.addChild(root_0, termp.getTree());
            	    if ( state.backtracking==0 ) {

            	              if((termp!=null?termp.t:null).isConstant()) retval.lit.getPred().getTypeAt(retval.lit.getTerms().size()).addConstant((termp!=null?termp.t:null).constant());
            	              retval.lit.appendTerm((termp!=null?termp.t:null));
            	          
            	    }

            	    }
            	    break;

            	default :
            	    break loop51;
                }
            } while (true);

            char_literal73=(Token)match(input,25,FOLLOW_25_in_atom3022); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            char_literal73_tree = (Object)adaptor.create(char_literal73);
            adaptor.addChild(root_0, char_literal73_tree);
            }
            if ( state.backtracking==0 ) {

                      Predicate p = retval.lit.getPred();
                      if(retval.lit.getTerms().size() != p.arity()){
                        die("Line #" + pred.getLine() + 
                        ": incorrect # of args (read " + retval.lit.getTerms().size() +
                        " but expected " + p.arity() + ")" +
                        " for pred " + 
                        p.getName());
                      }
                      // Register constants (to types) that only appear in the program
                      if (!Config.constants_as_raw_string) {
              	        for(int i=0; i<p.arity(); i++){
              	          Type t = p.getTypeAt(i);
              	          Term term = retval.lit.getTerms().get(i);
              	          if(term.isConstant()){
              	            t.addConstant(term.constant());
              	          }
              	        }
                      }
                  
            }

            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 30, atom_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "atom"

    public static class queryList_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "queryList"
    // MLN.g:778:1: queryList : ( query )+ EOF ;
    public final MLNParser.queryList_return queryList() throws RecognitionException {
        MLNParser.queryList_return retval = new MLNParser.queryList_return();
        retval.start = input.LT(1);
        int queryList_StartIndex = input.index();
        Object root_0 = null;

        Token EOF75=null;
        MLNParser.query_return query74 = null;


        Object EOF75_tree=null;

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 31) ) { return retval; }
            // MLN.g:778:11: ( ( query )+ EOF )
            // MLN.g:778:13: ( query )+ EOF
            {
            root_0 = (Object)adaptor.nil();

            // MLN.g:778:13: ( query )+
            int cnt52=0;
            loop52:
            do {
                int alt52=2;
                int LA52_0 = input.LA(1);

                if ( (LA52_0==ID) ) {
                    alt52=1;
                }


                switch (alt52) {
            	case 1 :
            	    // MLN.g:0:0: query
            	    {
            	    pushFollow(FOLLOW_query_in_queryList3041);
            	    query74=query();

            	    state._fsp--;
            	    if (state.failed) return retval;
            	    if ( state.backtracking==0 ) adaptor.addChild(root_0, query74.getTree());

            	    }
            	    break;

            	default :
            	    if ( cnt52 >= 1 ) break loop52;
            	    if (state.backtracking>0) {state.failed=true; return retval;}
                        EarlyExitException eee =
                            new EarlyExitException(52, input);
                        throw eee;
                }
                cnt52++;
            } while (true);

            EOF75=(Token)match(input,EOF,FOLLOW_EOF_in_queryList3044); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            EOF75_tree = (Object)adaptor.create(EOF75);
            adaptor.addChild(root_0, EOF75_tree);
            }

            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 31, queryList_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "queryList"

    public static class query_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "query"
    // MLN.g:781:1: query : ( atom | ID );
    public final MLNParser.query_return query() throws RecognitionException {
        MLNParser.query_return retval = new MLNParser.query_return();
        retval.start = input.LT(1);
        int query_StartIndex = input.index();
        Object root_0 = null;

        Token ID77=null;
        MLNParser.atom_return atom76 = null;


        Object ID77_tree=null;

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 32) ) { return retval; }
            // MLN.g:781:8: ( atom | ID )
            int alt53=2;
            int LA53_0 = input.LA(1);

            if ( (LA53_0==ID) ) {
                int LA53_1 = input.LA(2);

                if ( (LA53_1==23) ) {
                    alt53=1;
                }
                else if ( (LA53_1==EOF||LA53_1==ID||LA53_1==24) ) {
                    alt53=2;
                }
                else {
                    if (state.backtracking>0) {state.failed=true; return retval;}
                    NoViableAltException nvae =
                        new NoViableAltException("", 53, 1, input);

                    throw nvae;
                }
            }
            else {
                if (state.backtracking>0) {state.failed=true; return retval;}
                NoViableAltException nvae =
                    new NoViableAltException("", 53, 0, input);

                throw nvae;
            }
            switch (alt53) {
                case 1 :
                    // MLN.g:782:3: atom
                    {
                    root_0 = (Object)adaptor.nil();

                    pushFollow(FOLLOW_atom_in_query3060);
                    atom76=atom();

                    state._fsp--;
                    if (state.failed) return retval;
                    if ( state.backtracking==0 ) adaptor.addChild(root_0, atom76.getTree());
                    if ( state.backtracking==0 ) {

                      		  Atom q = (atom76!=null?atom76.lit:null).toAtom(Atom.AtomType.QUERY);
                      		  (atom76!=null?atom76.lit:null).getPred().addQuery(q);
                        
                    }

                    }
                    break;
                case 2 :
                    // MLN.g:787:5: ID
                    {
                    root_0 = (Object)adaptor.nil();

                    ID77=(Token)match(input,ID,FOLLOW_ID_in_query3070); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    ID77_tree = (Object)adaptor.create(ID77);
                    adaptor.addChild(root_0, ID77_tree);
                    }
                    if ( state.backtracking==0 ) {

                            Predicate p = ml.getPredByName((ID77!=null?ID77.getText():null));
                            if(p == null) die("Line #" + ID77.getLine() + ": unknown predicate name - " + (ID77!=null?ID77.getText():null));
                            p.setAllQuery();
                        
                    }

                    }
                    break;

            }
            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 32, query_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "query"

    public static class queryCommaList_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "queryCommaList"
    // MLN.g:795:1: queryCommaList : query ( ',' query )* EOF ;
    public final MLNParser.queryCommaList_return queryCommaList() throws RecognitionException {
        MLNParser.queryCommaList_return retval = new MLNParser.queryCommaList_return();
        retval.start = input.LT(1);
        int queryCommaList_StartIndex = input.index();
        Object root_0 = null;

        Token char_literal79=null;
        Token EOF81=null;
        MLNParser.query_return query78 = null;

        MLNParser.query_return query80 = null;


        Object char_literal79_tree=null;
        Object EOF81_tree=null;

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 33) ) { return retval; }
            // MLN.g:795:16: ( query ( ',' query )* EOF )
            // MLN.g:795:18: query ( ',' query )* EOF
            {
            root_0 = (Object)adaptor.nil();

            pushFollow(FOLLOW_query_in_queryCommaList3085);
            query78=query();

            state._fsp--;
            if (state.failed) return retval;
            if ( state.backtracking==0 ) adaptor.addChild(root_0, query78.getTree());
            // MLN.g:795:24: ( ',' query )*
            loop54:
            do {
                int alt54=2;
                int LA54_0 = input.LA(1);

                if ( (LA54_0==24) ) {
                    alt54=1;
                }


                switch (alt54) {
            	case 1 :
            	    // MLN.g:795:25: ',' query
            	    {
            	    char_literal79=(Token)match(input,24,FOLLOW_24_in_queryCommaList3088); if (state.failed) return retval;
            	    if ( state.backtracking==0 ) {
            	    char_literal79_tree = (Object)adaptor.create(char_literal79);
            	    adaptor.addChild(root_0, char_literal79_tree);
            	    }
            	    pushFollow(FOLLOW_query_in_queryCommaList3090);
            	    query80=query();

            	    state._fsp--;
            	    if (state.failed) return retval;
            	    if ( state.backtracking==0 ) adaptor.addChild(root_0, query80.getTree());

            	    }
            	    break;

            	default :
            	    break loop54;
                }
            } while (true);

            EOF81=(Token)match(input,EOF,FOLLOW_EOF_in_queryCommaList3094); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            EOF81_tree = (Object)adaptor.create(EOF81);
            adaptor.addChild(root_0, EOF81_tree);
            }

            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 33, queryCommaList_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "queryCommaList"

    public static class evidenceList_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "evidenceList"
    // MLN.g:798:1: evidenceList : ( evidence )+ EOF ;
    public final MLNParser.evidenceList_return evidenceList() throws RecognitionException {
        MLNParser.evidenceList_return retval = new MLNParser.evidenceList_return();
        retval.start = input.LT(1);
        int evidenceList_StartIndex = input.index();
        Object root_0 = null;

        Token EOF83=null;
        MLNParser.evidence_return evidence82 = null;


        Object EOF83_tree=null;

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 34) ) { return retval; }
            // MLN.g:798:14: ( ( evidence )+ EOF )
            // MLN.g:798:16: ( evidence )+ EOF
            {
            root_0 = (Object)adaptor.nil();

            // MLN.g:798:16: ( evidence )+
            int cnt55=0;
            loop55:
            do {
                int alt55=2;
                int LA55_0 = input.LA(1);

                if ( (LA55_0==NOT||LA55_0==NUMBER||LA55_0==ID) ) {
                    alt55=1;
                }


                switch (alt55) {
            	case 1 :
            	    // MLN.g:0:0: evidence
            	    {
            	    pushFollow(FOLLOW_evidence_in_evidenceList3103);
            	    evidence82=evidence();

            	    state._fsp--;
            	    if (state.failed) return retval;
            	    if ( state.backtracking==0 ) adaptor.addChild(root_0, evidence82.getTree());

            	    }
            	    break;

            	default :
            	    if ( cnt55 >= 1 ) break loop55;
            	    if (state.backtracking>0) {state.failed=true; return retval;}
                        EarlyExitException eee =
                            new EarlyExitException(55, input);
                        throw eee;
                }
                cnt55++;
            } while (true);

            EOF83=(Token)match(input,EOF,FOLLOW_EOF_in_evidenceList3106); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            EOF83_tree = (Object)adaptor.create(EOF83);
            adaptor.addChild(root_0, EOF83_tree);
            }

            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 34, evidenceList_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "evidenceList"

    public static class evidence_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "evidence"
    // MLN.g:801:1: evidence : (prior= NUMBER )? ( NOT )? pred= ID '(' terms+= ( ID | NUMBER | STRING ) ( ',' terms+= ( ID | NUMBER | STRING ) )* ')' ;
    public final MLNParser.evidence_return evidence() throws RecognitionException {
        MLNParser.evidence_return retval = new MLNParser.evidence_return();
        retval.start = input.LT(1);
        int evidence_StartIndex = input.index();
        Object root_0 = null;

        Token prior=null;
        Token pred=null;
        Token NOT84=null;
        Token char_literal85=null;
        Token char_literal86=null;
        Token char_literal87=null;
        Token terms=null;
        List list_terms=null;

        Object prior_tree=null;
        Object pred_tree=null;
        Object NOT84_tree=null;
        Object char_literal85_tree=null;
        Object char_literal86_tree=null;
        Object char_literal87_tree=null;
        Object terms_tree=null;

        try {
            if ( state.backtracking>0 && alreadyParsedRule(input, 35) ) { return retval; }
            // MLN.g:801:10: ( (prior= NUMBER )? ( NOT )? pred= ID '(' terms+= ( ID | NUMBER | STRING ) ( ',' terms+= ( ID | NUMBER | STRING ) )* ')' )
            // MLN.g:801:12: (prior= NUMBER )? ( NOT )? pred= ID '(' terms+= ( ID | NUMBER | STRING ) ( ',' terms+= ( ID | NUMBER | STRING ) )* ')'
            {
            root_0 = (Object)adaptor.nil();

            // MLN.g:801:17: (prior= NUMBER )?
            int alt56=2;
            int LA56_0 = input.LA(1);

            if ( (LA56_0==NUMBER) ) {
                alt56=1;
            }
            switch (alt56) {
                case 1 :
                    // MLN.g:0:0: prior= NUMBER
                    {
                    prior=(Token)match(input,NUMBER,FOLLOW_NUMBER_in_evidence3117); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    prior_tree = (Object)adaptor.create(prior);
                    adaptor.addChild(root_0, prior_tree);
                    }

                    }
                    break;

            }

            // MLN.g:801:26: ( NOT )?
            int alt57=2;
            int LA57_0 = input.LA(1);

            if ( (LA57_0==NOT) ) {
                alt57=1;
            }
            switch (alt57) {
                case 1 :
                    // MLN.g:0:0: NOT
                    {
                    NOT84=(Token)match(input,NOT,FOLLOW_NOT_in_evidence3120); if (state.failed) return retval;
                    if ( state.backtracking==0 ) {
                    NOT84_tree = (Object)adaptor.create(NOT84);
                    adaptor.addChild(root_0, NOT84_tree);
                    }

                    }
                    break;

            }

            pred=(Token)match(input,ID,FOLLOW_ID_in_evidence3125); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            pred_tree = (Object)adaptor.create(pred);
            adaptor.addChild(root_0, pred_tree);
            }
            char_literal85=(Token)match(input,23,FOLLOW_23_in_evidence3127); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            char_literal85_tree = (Object)adaptor.create(char_literal85);
            adaptor.addChild(root_0, char_literal85_tree);
            }
            terms=(Token)input.LT(1);
            if ( input.LA(1)==STRING||input.LA(1)==NUMBER||input.LA(1)==ID ) {
                input.consume();
                if ( state.backtracking==0 ) adaptor.addChild(root_0, (Object)adaptor.create(terms));
                state.errorRecovery=false;state.failed=false;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return retval;}
                MismatchedSetException mse = new MismatchedSetException(null,input);
                throw mse;
            }

            if (list_terms==null) list_terms=new ArrayList();
            list_terms.add(terms);

            // MLN.g:801:69: ( ',' terms+= ( ID | NUMBER | STRING ) )*
            loop58:
            do {
                int alt58=2;
                int LA58_0 = input.LA(1);

                if ( (LA58_0==24) ) {
                    alt58=1;
                }


                switch (alt58) {
            	case 1 :
            	    // MLN.g:801:70: ',' terms+= ( ID | NUMBER | STRING )
            	    {
            	    char_literal86=(Token)match(input,24,FOLLOW_24_in_evidence3140); if (state.failed) return retval;
            	    if ( state.backtracking==0 ) {
            	    char_literal86_tree = (Object)adaptor.create(char_literal86);
            	    adaptor.addChild(root_0, char_literal86_tree);
            	    }
            	    terms=(Token)input.LT(1);
            	    if ( input.LA(1)==STRING||input.LA(1)==NUMBER||input.LA(1)==ID ) {
            	        input.consume();
            	        if ( state.backtracking==0 ) adaptor.addChild(root_0, (Object)adaptor.create(terms));
            	        state.errorRecovery=false;state.failed=false;
            	    }
            	    else {
            	        if (state.backtracking>0) {state.failed=true; return retval;}
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        throw mse;
            	    }

            	    if (list_terms==null) list_terms=new ArrayList();
            	    list_terms.add(terms);


            	    }
            	    break;

            	default :
            	    break loop58;
                }
            } while (true);

            char_literal87=(Token)match(input,25,FOLLOW_25_in_evidence3154); if (state.failed) return retval;
            if ( state.backtracking==0 ) {
            char_literal87_tree = (Object)adaptor.create(char_literal87);
            adaptor.addChild(root_0, char_literal87_tree);
            }
            if ( state.backtracking==0 ) {

                    Boolean truth = null;
                    Double pr = null;
                    if(prior != null){
              	      pr = Double.parseDouble((prior!=null?prior.getText():null));
              	      if(pr > 1 || pr < 0){
                          die("Line #" + (lineOffset+pred.getLine()) + ": " + (prior!=null?prior.getText():null) + " - probabilities of soft evidence should be in [0,1]");
              	      }
                      if(NOT84 != null) pr = 1 - pr;
                      if(pr == 0 || pr == 1){
                        if(pr == 0) truth = false;
                        else truth = true;
                        pr = null;
                      }
              	    }else{
              	      truth = (NOT84 == null);
              	    }
              	    Predicate p = ml.getPredByName((pred!=null?pred.getText():null));
              	    if(p == null) die("Line #" + (lineOffset+pred.getLine()) + ": unknown predicate name - " + (pred!=null?pred.getText():null));
              	    ArrayList<String> args = new ArrayList<String>();
              	    ArrayList<Token> ts = (ArrayList<Token>)(list_terms);
                    if(ts.size() != p.arity()) die("Line #" + (lineOffset+pred.getLine()) + ": incorrect # args - " + input.toString(retval.start,input.LT(-1)));
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

            }

            retval.stop = input.LT(-1);

            if ( state.backtracking==0 ) {

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);
            }
        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
            if ( state.backtracking>0 ) { memoize(input, 35, evidence_StartIndex); }
        }
        return retval;
    }
    // $ANTLR end "evidence"

    // $ANTLR start synpred51_MLN
    public final void synpred51_MLN_fragment() throws RecognitionException {   
        MLNParser.mathComparison_return mc = null;


        // MLN.g:503:9: (mc= mathComparison )
        // MLN.g:503:9: mc= mathComparison
        {
        pushFollow(FOLLOW_mathComparison_in_synpred51_MLN2222);
        mc=mathComparison();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred51_MLN

    // $ANTLR start synpred52_MLN
    public final void synpred52_MLN_fragment() throws RecognitionException {   
        Token negc=null;
        MLNParser.mathComparison_return mc = null;

        MLNParser.funcExpression_return fe = null;


        // MLN.g:501:5: ( (negc= ( '!' | 'NOT' ) )? (mc= mathComparison | fe= funcExpression ) )
        // MLN.g:501:5: (negc= ( '!' | 'NOT' ) )? (mc= mathComparison | fe= funcExpression )
        {
        // MLN.g:501:9: (negc= ( '!' | 'NOT' ) )?
        int alt63=2;
        int LA63_0 = input.LA(1);

        if ( (LA63_0==NOT||LA63_0==47) ) {
            alt63=1;
        }
        switch (alt63) {
            case 1 :
                // MLN.g:0:0: negc= ( '!' | 'NOT' )
                {
                negc=(Token)input.LT(1);
                if ( input.LA(1)==NOT||input.LA(1)==47 ) {
                    input.consume();
                    state.errorRecovery=false;state.failed=false;
                }
                else {
                    if (state.backtracking>0) {state.failed=true; return ;}
                    MismatchedSetException mse = new MismatchedSetException(null,input);
                    throw mse;
                }


                }
                break;

        }

        // MLN.g:502:7: (mc= mathComparison | fe= funcExpression )
        int alt64=2;
        int LA64_0 = input.LA(1);

        if ( (LA64_0==ID) ) {
            int LA64_1 = input.LA(2);

            if ( (synpred51_MLN()) ) {
                alt64=1;
            }
            else if ( (true) ) {
                alt64=2;
            }
            else {
                if (state.backtracking>0) {state.failed=true; return ;}
                NoViableAltException nvae =
                    new NoViableAltException("", 64, 1, input);

                throw nvae;
            }
        }
        else if ( (LA64_0==STRING||LA64_0==NUMBER||LA64_0==23||LA64_0==61) ) {
            alt64=1;
        }
        else {
            if (state.backtracking>0) {state.failed=true; return ;}
            NoViableAltException nvae =
                new NoViableAltException("", 64, 0, input);

            throw nvae;
        }
        switch (alt64) {
            case 1 :
                // MLN.g:503:9: mc= mathComparison
                {
                pushFollow(FOLLOW_mathComparison_in_synpred52_MLN2222);
                mc=mathComparison();

                state._fsp--;
                if (state.failed) return ;

                }
                break;
            case 2 :
                // MLN.g:504:9: fe= funcExpression
                {
                pushFollow(FOLLOW_funcExpression_in_synpred52_MLN2235);
                fe=funcExpression();

                state._fsp--;
                if (state.failed) return ;

                }
                break;

        }


        }
    }
    // $ANTLR end synpred52_MLN

    // $ANTLR start synpred64_MLN
    public final void synpred64_MLN_fragment() throws RecognitionException {   
        Token op=null;
        MLNParser.mathTerm_return tp = null;


        // MLN.g:543:6: (op= ( '+' | '-' | '%' ) tp= mathTerm )
        // MLN.g:543:6: op= ( '+' | '-' | '%' ) tp= mathTerm
        {
        op=(Token)input.LT(1);
        if ( (input.LA(1)>=PLUS && input.LA(1)<=MINUS)||input.LA(1)==54 ) {
            input.consume();
            state.errorRecovery=false;state.failed=false;
        }
        else {
            if (state.backtracking>0) {state.failed=true; return ;}
            MismatchedSetException mse = new MismatchedSetException(null,input);
            throw mse;
        }

        pushFollow(FOLLOW_mathTerm_in_synpred64_MLN2391);
        tp=mathTerm();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred64_MLN

    // $ANTLR start synpred75_MLN
    public final void synpred75_MLN_fragment() throws RecognitionException {   
        Token fact=null;

        // MLN.g:611:11: (fact= '!' )
        // MLN.g:611:11: fact= '!'
        {
        fact=(Token)match(input,NOT,FOLLOW_NOT_in_synpred75_MLN2603); if (state.failed) return ;

        }
    }
    // $ANTLR end synpred75_MLN

    // $ANTLR start synpred77_MLN
    public final void synpred77_MLN_fragment() throws RecognitionException {   
        MLNParser.boolExpression_return be = null;


        // MLN.g:647:5: (be= boolExpression )
        // MLN.g:647:5: be= boolExpression
        {
        pushFollow(FOLLOW_boolExpression_in_synpred77_MLN2723);
        be=boolExpression();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred77_MLN

    // $ANTLR start synpred78_MLN
    public final void synpred78_MLN_fragment() throws RecognitionException {   
        MLNParser.mathExpression_return me = null;


        // MLN.g:651:7: (me= mathExpression )
        // MLN.g:651:7: me= mathExpression
        {
        pushFollow(FOLLOW_mathExpression_in_synpred78_MLN2739);
        me=mathExpression();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred78_MLN

    // $ANTLR start synpred79_MLN
    public final void synpred79_MLN_fragment() throws RecognitionException {   
        MLNParser.funcExpression_return fe = null;


        // MLN.g:655:7: (fe= funcExpression )
        // MLN.g:655:7: fe= funcExpression
        {
        pushFollow(FOLLOW_funcExpression_in_synpred79_MLN2755);
        fe=funcExpression();

        state._fsp--;
        if (state.failed) return ;

        }
    }
    // $ANTLR end synpred79_MLN

    // Delegated rules

    public final boolean synpred75_MLN() {
        state.backtracking++;
        int start = input.mark();
        try {
            synpred75_MLN_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred51_MLN() {
        state.backtracking++;
        int start = input.mark();
        try {
            synpred51_MLN_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred78_MLN() {
        state.backtracking++;
        int start = input.mark();
        try {
            synpred78_MLN_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred52_MLN() {
        state.backtracking++;
        int start = input.mark();
        try {
            synpred52_MLN_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred77_MLN() {
        state.backtracking++;
        int start = input.mark();
        try {
            synpred77_MLN_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred64_MLN() {
        state.backtracking++;
        int start = input.mark();
        try {
            synpred64_MLN_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        state.backtracking--;
        state.failed=false;
        return success;
    }
    public final boolean synpred79_MLN() {
        state.backtracking++;
        int start = input.mark();
        try {
            synpred79_MLN_fragment(); // can never throw exception
        } catch (RecognitionException re) {
            System.err.println("impossible: "+re);
        }
        boolean success = !state.failed;
        input.rewind(start);
        state.backtracking--;
        state.failed=false;
        return success;
    }


    protected DFA1 dfa1 = new DFA1(this);
    protected DFA11 dfa11 = new DFA11(this);
    protected DFA20 dfa20 = new DFA20(this);
    protected DFA24 dfa24 = new DFA24(this);
    protected DFA31 dfa31 = new DFA31(this);
    protected DFA28 dfa28 = new DFA28(this);
    protected DFA32 dfa32 = new DFA32(this);
    protected DFA42 dfa42 = new DFA42(this);
    protected DFA47 dfa47 = new DFA47(this);
    static final String DFA1_eotS =
        "\23\uffff";
    static final String DFA1_eofS =
        "\1\1\15\uffff\1\5\1\uffff\1\5\2\uffff";
    static final String DFA1_minS =
        "\1\6\1\uffff\1\6\2\11\2\uffff\1\11\1\16\1\11\1\16\2\6\1\16\1\6\1"+
        "\16\3\6";
    static final String DFA1_maxS =
        "\1\45\1\uffff\1\43\1\34\1\26\2\uffff\1\34\1\24\1\27\1\24\2\31\1"+
        "\24\1\52\1\24\1\45\2\31";
    static final String DFA1_acceptS =
        "\1\uffff\1\3\3\uffff\1\1\1\2\14\uffff";
    static final String DFA1_specialS =
        "\23\uffff}>";
    static final String[] DFA1_transitionS = {
            "\1\1\1\4\1\uffff\1\5\1\uffff\1\1\6\uffff\1\1\1\uffff\1\3\1\5"+
            "\1\2\3\uffff\2\6\3\uffff\5\1\1\uffff\1\1",
            "",
            "\2\1\12\uffff\1\1\1\uffff\1\7\16\uffff\1\1",
            "\1\5\15\uffff\1\10\4\uffff\1\1",
            "\1\5\12\uffff\1\11\2\5",
            "",
            "",
            "\1\5\15\uffff\1\12\4\uffff\1\1",
            "\1\1\3\uffff\1\1\1\uffff\1\13",
            "\1\5\15\uffff\1\10",
            "\1\1\3\uffff\1\1\1\uffff\1\14",
            "\1\5\15\uffff\1\5\3\uffff\1\15\1\16",
            "\1\5\15\uffff\1\5\3\uffff\1\17\1\20",
            "\1\1\3\uffff\1\1\1\uffff\1\21",
            "\2\5\1\uffff\1\5\1\1\1\5\1\1\5\uffff\1\5\1\uffff\3\5\1\uffff"+
            "\1\1\1\uffff\2\5\3\uffff\5\5\1\1\1\5\3\uffff\2\1",
            "\1\1\3\uffff\1\1\1\uffff\1\22",
            "\2\5\1\uffff\1\5\1\uffff\1\5\6\uffff\1\5\1\uffff\3\5\3\uffff"+
            "\2\5\3\uffff\5\5\1\1\1\5",
            "\1\5\15\uffff\1\5\3\uffff\1\15\1\16",
            "\1\5\15\uffff\1\5\3\uffff\1\17\1\20"
    };

    static final short[] DFA1_eot = DFA.unpackEncodedString(DFA1_eotS);
    static final short[] DFA1_eof = DFA.unpackEncodedString(DFA1_eofS);
    static final char[] DFA1_min = DFA.unpackEncodedStringToUnsignedChars(DFA1_minS);
    static final char[] DFA1_max = DFA.unpackEncodedStringToUnsignedChars(DFA1_maxS);
    static final short[] DFA1_accept = DFA.unpackEncodedString(DFA1_acceptS);
    static final short[] DFA1_special = DFA.unpackEncodedString(DFA1_specialS);
    static final short[][] DFA1_transition;

    static {
        int numStates = DFA1_transitionS.length;
        DFA1_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA1_transition[i] = DFA.unpackEncodedString(DFA1_transitionS[i]);
        }
    }

    class DFA1 extends DFA {

        public DFA1(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 1;
            this.eot = DFA1_eot;
            this.eof = DFA1_eof;
            this.min = DFA1_min;
            this.max = DFA1_max;
            this.accept = DFA1_accept;
            this.special = DFA1_special;
            this.transition = DFA1_transition;
        }
        public String getDescription() {
            return "()* loopback of 123:14: ( schema | schemaConstraint )*";
        }
    }
    static final String DFA11_eotS =
        "\21\uffff";
    static final String DFA11_eofS =
        "\1\1\20\uffff";
    static final String DFA11_minS =
        "\1\6\2\uffff\1\6\1\27\1\24\2\uffff\1\27\1\16\1\27\2\30\1\16\1\12"+
        "\2\30";
    static final String DFA11_maxS =
        "\1\45\2\uffff\1\43\1\34\1\24\2\uffff\1\34\1\24\1\27\2\31\1\24\1"+
        "\52\2\31";
    static final String DFA11_acceptS =
        "\1\uffff\1\4\1\1\3\uffff\1\2\1\3\11\uffff";
    static final String DFA11_specialS =
        "\21\uffff}>";
    static final String[] DFA11_transitionS = {
            "\2\5\3\uffff\1\2\6\uffff\1\2\1\uffff\1\4\1\uffff\1\3\10\uffff"+
            "\3\2\2\7\1\uffff\1\6",
            "",
            "",
            "\2\7\12\uffff\1\2\1\uffff\1\10\16\uffff\1\7",
            "\1\11\4\uffff\1\2",
            "\1\12",
            "",
            "",
            "\1\7\4\uffff\1\2",
            "\1\14\3\uffff\1\14\1\uffff\1\13",
            "\1\11",
            "\1\15\1\16",
            "\1\15\1\16",
            "\1\20\3\uffff\1\20\1\uffff\1\17",
            "\1\2\1\uffff\1\2\13\uffff\1\2\13\uffff\1\7\4\uffff\1\6\1\2",
            "\1\15\1\16",
            "\1\15\1\16"
    };

    static final short[] DFA11_eot = DFA.unpackEncodedString(DFA11_eotS);
    static final short[] DFA11_eof = DFA.unpackEncodedString(DFA11_eofS);
    static final char[] DFA11_min = DFA.unpackEncodedStringToUnsignedChars(DFA11_minS);
    static final char[] DFA11_max = DFA.unpackEncodedStringToUnsignedChars(DFA11_maxS);
    static final short[] DFA11_accept = DFA.unpackEncodedString(DFA11_acceptS);
    static final short[] DFA11_special = DFA.unpackEncodedString(DFA11_specialS);
    static final short[][] DFA11_transition;

    static {
        int numStates = DFA11_transitionS.length;
        DFA11_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA11_transition[i] = DFA.unpackEncodedString(DFA11_transitionS[i]);
        }
    }

    class DFA11 extends DFA {

        public DFA11(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 11;
            this.eot = DFA11_eot;
            this.eof = DFA11_eof;
            this.min = DFA11_min;
            this.max = DFA11_max;
            this.accept = DFA11_accept;
            this.special = DFA11_special;
            this.transition = DFA11_transition;
        }
        public String getDescription() {
            return "()* loopback of 189:12: ( mlnRule | scopingRule | datalogRule )*";
        }
    }
    static final String DFA20_eotS =
        "\16\uffff";
    static final String DFA20_eofS =
        "\16\uffff";
    static final String DFA20_minS =
        "\1\12\1\6\2\uffff\12\6";
    static final String DFA20_maxS =
        "\1\30\1\75\2\uffff\1\74\1\75\3\74\1\75\4\74";
    static final String DFA20_acceptS =
        "\2\uffff\1\2\1\1\12\uffff";
    static final String DFA20_specialS =
        "\16\uffff}>";
    static final String[] DFA20_transitionS = {
            "\1\2\15\uffff\1\1",
            "\2\3\6\uffff\1\2\3\uffff\1\2\1\uffff\1\4\2\uffff\1\2\15\uffff"+
            "\1\2\27\uffff\1\2",
            "",
            "",
            "\4\2\15\uffff\1\5\20\uffff\1\2\7\uffff\15\2",
            "\1\2\7\uffff\1\10\3\uffff\1\7\1\uffff\1\6\2\uffff\1\2\27\uffff"+
            "\1\2\15\uffff\1\2",
            "\4\2\15\uffff\1\2\1\11\1\12\16\uffff\1\2\7\uffff\15\2",
            "\4\2\16\uffff\1\11\1\12\16\uffff\1\2\7\uffff\15\2",
            "\4\2\16\uffff\1\11\1\12\16\uffff\1\2\7\uffff\15\2",
            "\1\2\7\uffff\1\15\3\uffff\1\14\1\uffff\1\13\2\uffff\1\2\27"+
            "\uffff\1\2\15\uffff\1\2",
            "\4\2\1\3\15\uffff\1\3\17\uffff\1\2\7\uffff\15\2",
            "\4\2\15\uffff\1\2\1\11\1\12\16\uffff\1\2\7\uffff\15\2",
            "\4\2\16\uffff\1\11\1\12\16\uffff\1\2\7\uffff\15\2",
            "\4\2\16\uffff\1\11\1\12\16\uffff\1\2\7\uffff\15\2"
    };

    static final short[] DFA20_eot = DFA.unpackEncodedString(DFA20_eotS);
    static final short[] DFA20_eof = DFA.unpackEncodedString(DFA20_eofS);
    static final char[] DFA20_min = DFA.unpackEncodedStringToUnsignedChars(DFA20_minS);
    static final char[] DFA20_max = DFA.unpackEncodedStringToUnsignedChars(DFA20_maxS);
    static final short[] DFA20_accept = DFA.unpackEncodedString(DFA20_acceptS);
    static final short[] DFA20_special = DFA.unpackEncodedString(DFA20_specialS);
    static final short[][] DFA20_transition;

    static {
        int numStates = DFA20_transitionS.length;
        DFA20_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA20_transition[i] = DFA.unpackEncodedString(DFA20_transitionS[i]);
        }
    }

    class DFA20 extends DFA {

        public DFA20(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 20;
            this.eot = DFA20_eot;
            this.eof = DFA20_eof;
            this.min = DFA20_min;
            this.max = DFA20_max;
            this.accept = DFA20_accept;
            this.special = DFA20_special;
            this.transition = DFA20_transition;
        }
        public String getDescription() {
            return "()* loopback of 275:9: ( ',' bodyp= literal )*";
        }
    }
    static final String DFA24_eotS =
        "\16\uffff";
    static final String DFA24_eofS =
        "\16\uffff";
    static final String DFA24_minS =
        "\1\12\1\6\1\uffff\1\6\1\uffff\11\6";
    static final String DFA24_maxS =
        "\1\30\1\75\1\uffff\1\74\1\uffff\1\75\3\74\1\75\4\74";
    static final String DFA24_acceptS =
        "\2\uffff\1\2\1\uffff\1\1\11\uffff";
    static final String DFA24_specialS =
        "\16\uffff}>";
    static final String[] DFA24_transitionS = {
            "\1\2\15\uffff\1\1",
            "\2\4\6\uffff\1\2\3\uffff\1\2\1\uffff\1\3\2\uffff\1\2\15\uffff"+
            "\1\2\27\uffff\1\2",
            "",
            "\4\2\15\uffff\1\5\20\uffff\1\2\7\uffff\15\2",
            "",
            "\1\2\7\uffff\1\10\3\uffff\1\7\1\uffff\1\6\2\uffff\1\2\27\uffff"+
            "\1\2\15\uffff\1\2",
            "\4\2\15\uffff\1\2\1\11\1\12\16\uffff\1\2\7\uffff\15\2",
            "\4\2\16\uffff\1\11\1\12\16\uffff\1\2\7\uffff\15\2",
            "\4\2\16\uffff\1\11\1\12\16\uffff\1\2\7\uffff\15\2",
            "\1\2\7\uffff\1\15\3\uffff\1\14\1\uffff\1\13\2\uffff\1\2\27"+
            "\uffff\1\2\15\uffff\1\2",
            "\4\2\1\4\15\uffff\1\4\17\uffff\1\2\7\uffff\15\2",
            "\4\2\15\uffff\1\2\1\11\1\12\16\uffff\1\2\7\uffff\15\2",
            "\4\2\16\uffff\1\11\1\12\16\uffff\1\2\7\uffff\15\2",
            "\4\2\16\uffff\1\11\1\12\16\uffff\1\2\7\uffff\15\2"
    };

    static final short[] DFA24_eot = DFA.unpackEncodedString(DFA24_eotS);
    static final short[] DFA24_eof = DFA.unpackEncodedString(DFA24_eofS);
    static final char[] DFA24_min = DFA.unpackEncodedStringToUnsignedChars(DFA24_minS);
    static final char[] DFA24_max = DFA.unpackEncodedStringToUnsignedChars(DFA24_maxS);
    static final short[] DFA24_accept = DFA.unpackEncodedString(DFA24_acceptS);
    static final short[] DFA24_special = DFA.unpackEncodedString(DFA24_specialS);
    static final short[][] DFA24_transition;

    static {
        int numStates = DFA24_transitionS.length;
        DFA24_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA24_transition[i] = DFA.unpackEncodedString(DFA24_transitionS[i]);
        }
    }

    class DFA24 extends DFA {

        public DFA24(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 24;
            this.eot = DFA24_eot;
            this.eof = DFA24_eof;
            this.min = DFA24_min;
            this.max = DFA24_max;
            this.accept = DFA24_accept;
            this.special = DFA24_special;
            this.transition = DFA24_transition;
        }
        public String getDescription() {
            return "()* loopback of 312:9: ( ',' bodyp= literal )*";
        }
    }
    static final String DFA31_eotS =
        "\14\uffff";
    static final String DFA31_eofS =
        "\7\uffff\1\12\4\uffff";
    static final String DFA31_minS =
        "\1\6\1\24\1\27\1\16\2\30\1\16\1\6\2\30\2\uffff";
    static final String DFA31_maxS =
        "\2\24\1\27\1\24\2\31\1\24\1\52\2\31\2\uffff";
    static final String DFA31_acceptS =
        "\12\uffff\1\2\1\1";
    static final String DFA31_specialS =
        "\14\uffff}>";
    static final String[] DFA31_transitionS = {
            "\2\1\14\uffff\1\2",
            "\1\2",
            "\1\3",
            "\1\5\3\uffff\1\5\1\uffff\1\4",
            "\1\6\1\7",
            "\1\6\1\7",
            "\1\11\3\uffff\1\11\1\uffff\1\10",
            "\2\12\2\uffff\2\12\1\13\5\uffff\1\12\1\uffff\1\12\1\uffff\1"+
            "\12\1\uffff\1\13\6\uffff\5\12\1\uffff\1\12\4\uffff\1\12",
            "\1\6\1\7",
            "\1\6\1\7",
            "",
            ""
    };

    static final short[] DFA31_eot = DFA.unpackEncodedString(DFA31_eotS);
    static final short[] DFA31_eof = DFA.unpackEncodedString(DFA31_eofS);
    static final char[] DFA31_min = DFA.unpackEncodedStringToUnsignedChars(DFA31_minS);
    static final char[] DFA31_max = DFA.unpackEncodedStringToUnsignedChars(DFA31_maxS);
    static final short[] DFA31_accept = DFA.unpackEncodedString(DFA31_acceptS);
    static final short[] DFA31_special = DFA.unpackEncodedString(DFA31_specialS);
    static final short[][] DFA31_transition;

    static {
        int numStates = DFA31_transitionS.length;
        DFA31_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA31_transition[i] = DFA.unpackEncodedString(DFA31_transitionS[i]);
        }
    }

    class DFA31 extends DFA {

        public DFA31(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 31;
            this.eot = DFA31_eot;
            this.eof = DFA31_eof;
            this.min = DFA31_min;
            this.max = DFA31_max;
            this.accept = DFA31_accept;
            this.special = DFA31_special;
            this.transition = DFA31_transition;
        }
        public String getDescription() {
            return "408:6: (ant0= literal ( ',' antp= literal )* ( ',' mc= mathComparison )* ( ',' '[' be= boolExpression ']' )? IMPLIES )?";
        }
    }
    static final String DFA28_eotS =
        "\16\uffff";
    static final String DFA28_eofS =
        "\16\uffff";
    static final String DFA28_minS =
        "\1\14\1\6\2\uffff\12\6";
    static final String DFA28_maxS =
        "\1\30\1\75\2\uffff\1\74\1\75\3\74\1\75\4\74";
    static final String DFA28_acceptS =
        "\2\uffff\1\2\1\1\12\uffff";
    static final String DFA28_specialS =
        "\16\uffff}>";
    static final String[] DFA28_transitionS = {
            "\1\2\13\uffff\1\1",
            "\2\3\6\uffff\1\2\3\uffff\1\2\1\uffff\1\4\2\uffff\1\2\15\uffff"+
            "\1\2\27\uffff\1\2",
            "",
            "",
            "\4\2\15\uffff\1\5\20\uffff\1\2\7\uffff\15\2",
            "\1\2\7\uffff\1\10\3\uffff\1\7\1\uffff\1\6\2\uffff\1\2\27\uffff"+
            "\1\2\15\uffff\1\2",
            "\4\2\15\uffff\1\2\1\11\1\12\16\uffff\1\2\7\uffff\15\2",
            "\4\2\16\uffff\1\11\1\12\16\uffff\1\2\7\uffff\15\2",
            "\4\2\16\uffff\1\11\1\12\16\uffff\1\2\7\uffff\15\2",
            "\1\2\7\uffff\1\15\3\uffff\1\14\1\uffff\1\13\2\uffff\1\2\27"+
            "\uffff\1\2\15\uffff\1\2",
            "\4\2\2\uffff\1\3\13\uffff\1\3\17\uffff\1\2\7\uffff\15\2",
            "\4\2\15\uffff\1\2\1\11\1\12\16\uffff\1\2\7\uffff\15\2",
            "\4\2\16\uffff\1\11\1\12\16\uffff\1\2\7\uffff\15\2",
            "\4\2\16\uffff\1\11\1\12\16\uffff\1\2\7\uffff\15\2"
    };

    static final short[] DFA28_eot = DFA.unpackEncodedString(DFA28_eotS);
    static final short[] DFA28_eof = DFA.unpackEncodedString(DFA28_eofS);
    static final char[] DFA28_min = DFA.unpackEncodedStringToUnsignedChars(DFA28_minS);
    static final char[] DFA28_max = DFA.unpackEncodedStringToUnsignedChars(DFA28_maxS);
    static final short[] DFA28_accept = DFA.unpackEncodedString(DFA28_acceptS);
    static final short[] DFA28_special = DFA.unpackEncodedString(DFA28_specialS);
    static final short[][] DFA28_transition;

    static {
        int numStates = DFA28_transitionS.length;
        DFA28_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA28_transition[i] = DFA.unpackEncodedString(DFA28_transitionS[i]);
        }
    }

    class DFA28 extends DFA {

        public DFA28(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 28;
            this.eot = DFA28_eot;
            this.eof = DFA28_eof;
            this.min = DFA28_min;
            this.max = DFA28_max;
            this.accept = DFA28_accept;
            this.special = DFA28_special;
            this.transition = DFA28_transition;
        }
        public String getDescription() {
            return "()* loopback of 415:9: ( ',' antp= literal )*";
        }
    }
    static final String DFA32_eotS =
        "\32\uffff";
    static final String DFA32_eofS =
        "\1\2\11\uffff\1\3\17\uffff";
    static final String DFA32_minS =
        "\2\6\2\uffff\12\6\1\7\1\16\12\6";
    static final String DFA32_maxS =
        "\1\52\1\75\2\uffff\1\74\1\75\3\74\1\75\5\74\1\75\1\74\1\75\3\74"+
        "\1\75\4\74";
    static final String DFA32_acceptS =
        "\2\uffff\1\2\1\1\26\uffff";
    static final String DFA32_specialS =
        "\32\uffff}>";
    static final String[] DFA32_transitionS = {
            "\2\2\2\uffff\2\2\6\uffff\1\2\1\uffff\1\2\1\uffff\1\2\10\uffff"+
            "\5\2\1\uffff\1\2\4\uffff\1\1",
            "\2\3\6\uffff\1\2\3\uffff\1\2\1\uffff\1\4\2\uffff\1\2\15\uffff"+
            "\1\2\27\uffff\1\2",
            "",
            "",
            "\4\2\15\uffff\1\5\20\uffff\1\2\7\uffff\15\2",
            "\1\2\7\uffff\1\10\3\uffff\1\7\1\uffff\1\6\2\uffff\1\2\27\uffff"+
            "\1\2\15\uffff\1\2",
            "\4\2\15\uffff\1\2\1\11\1\12\16\uffff\1\2\7\uffff\15\2",
            "\4\2\16\uffff\1\11\1\12\16\uffff\1\2\7\uffff\15\2",
            "\4\2\16\uffff\1\11\1\12\16\uffff\1\2\7\uffff\15\2",
            "\1\2\7\uffff\1\15\3\uffff\1\14\1\uffff\1\13\2\uffff\1\2\27"+
            "\uffff\1\2\15\uffff\1\2",
            "\1\16\1\17\2\2\2\3\6\uffff\1\3\1\uffff\1\3\1\uffff\1\3\10\uffff"+
            "\5\3\1\uffff\1\3\2\uffff\1\2\1\uffff\1\3\5\uffff\15\2",
            "\4\2\15\uffff\1\2\1\11\1\12\16\uffff\1\2\7\uffff\15\2",
            "\4\2\16\uffff\1\11\1\12\16\uffff\1\2\7\uffff\15\2",
            "\4\2\16\uffff\1\11\1\12\16\uffff\1\2\7\uffff\15\2",
            "\3\2\12\uffff\1\3\23\uffff\1\2\7\uffff\15\2",
            "\1\2\3\uffff\1\2\1\uffff\1\20\2\uffff\1\2\45\uffff\1\2",
            "\4\2\15\uffff\1\21\20\uffff\1\2\7\uffff\15\2",
            "\1\2\7\uffff\1\24\3\uffff\1\23\1\uffff\1\22\2\uffff\1\2\27"+
            "\uffff\1\2\15\uffff\1\2",
            "\4\2\15\uffff\1\2\1\25\1\26\16\uffff\1\2\7\uffff\15\2",
            "\4\2\16\uffff\1\25\1\26\16\uffff\1\2\7\uffff\15\2",
            "\4\2\16\uffff\1\25\1\26\16\uffff\1\2\7\uffff\15\2",
            "\1\2\7\uffff\1\31\3\uffff\1\30\1\uffff\1\27\2\uffff\1\2\27"+
            "\uffff\1\2\15\uffff\1\2",
            "\4\2\1\3\1\uffff\1\3\13\uffff\1\3\13\uffff\1\3\3\uffff\1\2"+
            "\2\3\5\uffff\15\2",
            "\4\2\15\uffff\1\2\1\25\1\26\16\uffff\1\2\7\uffff\15\2",
            "\4\2\16\uffff\1\25\1\26\16\uffff\1\2\7\uffff\15\2",
            "\4\2\16\uffff\1\25\1\26\16\uffff\1\2\7\uffff\15\2"
    };

    static final short[] DFA32_eot = DFA.unpackEncodedString(DFA32_eotS);
    static final short[] DFA32_eof = DFA.unpackEncodedString(DFA32_eofS);
    static final char[] DFA32_min = DFA.unpackEncodedStringToUnsignedChars(DFA32_minS);
    static final char[] DFA32_max = DFA.unpackEncodedStringToUnsignedChars(DFA32_maxS);
    static final short[] DFA32_accept = DFA.unpackEncodedString(DFA32_acceptS);
    static final short[] DFA32_special = DFA.unpackEncodedString(DFA32_specialS);
    static final short[][] DFA32_transition;

    static {
        int numStates = DFA32_transitionS.length;
        DFA32_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA32_transition[i] = DFA.unpackEncodedString(DFA32_transitionS[i]);
        }
    }

    class DFA32 extends DFA {

        public DFA32(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 32;
            this.eot = DFA32_eot;
            this.eof = DFA32_eof;
            this.min = DFA32_min;
            this.max = DFA32_max;
            this.accept = DFA32_accept;
            this.special = DFA32_special;
            this.transition = DFA32_transition;
        }
        public String getDescription() {
            return "()* loopback of 433:6: ( 'v' litp= literal )*";
        }
    }
    static final String DFA42_eotS =
        "\16\uffff";
    static final String DFA42_eofS =
        "\1\1\3\uffff\1\3\11\uffff";
    static final String DFA42_minS =
        "\1\6\1\uffff\1\16\1\uffff\6\6\1\0\3\6";
    static final String DFA42_maxS =
        "\1\66\1\uffff\1\75\1\uffff\1\74\1\75\3\74\1\75\1\0\3\74";
    static final String DFA42_acceptS =
        "\1\uffff\1\2\1\uffff\1\1\12\uffff";
    static final String DFA42_specialS =
        "\12\uffff\1\0\3\uffff}>";
    static final String[] DFA42_transitionS = {
            "\1\1\1\2\1\3\1\uffff\3\1\5\uffff\1\1\1\uffff\1\1\1\uffff\1\1"+
            "\1\uffff\2\1\5\uffff\5\1\1\uffff\2\1\1\uffff\1\1\1\uffff\5\1"+
            "\1\uffff\6\1\1\3",
            "",
            "\1\3\3\uffff\1\3\1\uffff\1\4\2\uffff\1\3\45\uffff\1\3",
            "",
            "\7\3\5\uffff\1\3\1\uffff\1\3\1\uffff\1\3\1\5\2\3\5\uffff\5"+
            "\3\1\uffff\2\3\1\uffff\1\3\1\uffff\5\3\1\uffff\15\3",
            "\1\3\7\uffff\1\10\3\uffff\1\7\1\uffff\1\6\2\uffff\1\3\27\uffff"+
            "\1\3\15\uffff\1\3",
            "\4\3\15\uffff\1\3\1\11\1\12\16\uffff\1\3\7\uffff\15\3",
            "\4\3\16\uffff\1\11\1\12\16\uffff\1\3\7\uffff\15\3",
            "\4\3\16\uffff\1\11\1\12\16\uffff\1\3\7\uffff\15\3",
            "\1\3\7\uffff\1\15\3\uffff\1\14\1\uffff\1\13\2\uffff\1\3\27"+
            "\uffff\1\3\15\uffff\1\3",
            "\1\uffff",
            "\4\3\15\uffff\1\3\1\11\1\12\16\uffff\1\3\7\uffff\15\3",
            "\4\3\16\uffff\1\11\1\12\16\uffff\1\3\7\uffff\15\3",
            "\4\3\16\uffff\1\11\1\12\16\uffff\1\3\7\uffff\15\3"
    };

    static final short[] DFA42_eot = DFA.unpackEncodedString(DFA42_eotS);
    static final short[] DFA42_eof = DFA.unpackEncodedString(DFA42_eofS);
    static final char[] DFA42_min = DFA.unpackEncodedStringToUnsignedChars(DFA42_minS);
    static final char[] DFA42_max = DFA.unpackEncodedStringToUnsignedChars(DFA42_maxS);
    static final short[] DFA42_accept = DFA.unpackEncodedString(DFA42_acceptS);
    static final short[] DFA42_special = DFA.unpackEncodedString(DFA42_specialS);
    static final short[][] DFA42_transition;

    static {
        int numStates = DFA42_transitionS.length;
        DFA42_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA42_transition[i] = DFA.unpackEncodedString(DFA42_transitionS[i]);
        }
    }

    class DFA42 extends DFA {

        public DFA42(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 42;
            this.eot = DFA42_eot;
            this.eof = DFA42_eof;
            this.min = DFA42_min;
            this.max = DFA42_max;
            this.accept = DFA42_accept;
            this.special = DFA42_special;
            this.transition = DFA42_transition;
        }
        public String getDescription() {
            return "()* loopback of 543:5: (op= ( '+' | '-' | '%' ) tp= mathTerm )*";
        }
        public int specialStateTransition(int s, IntStream _input) throws NoViableAltException {
            TokenStream input = (TokenStream)_input;
        	int _s = s;
            switch ( s ) {
                    case 0 : 
                        int LA42_10 = input.LA(1);

                         
                        int index42_10 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred64_MLN()) ) {s = 3;}

                        else if ( (true) ) {s = 1;}

                         
                        input.seek(index42_10);
                        if ( s>=0 ) return s;
                        break;
            }
            if (state.backtracking>0) {state.failed=true; return -1;}
            NoViableAltException nvae =
                new NoViableAltException(getDescription(), 42, _s, input);
            error(nvae);
            throw nvae;
        }
    }
    static final String DFA47_eotS =
        "\12\uffff";
    static final String DFA47_eofS =
        "\12\uffff";
    static final String DFA47_minS =
        "\1\6\1\uffff\5\0\3\uffff";
    static final String DFA47_maxS =
        "\1\75\1\uffff\5\0\3\uffff";
    static final String DFA47_acceptS =
        "\1\uffff\1\1\5\uffff\1\2\1\3\1\4";
    static final String DFA47_specialS =
        "\2\uffff\1\0\1\1\1\2\1\3\1\4\3\uffff}>";
    static final String[] DFA47_transitionS = {
            "\1\1\7\uffff\1\4\3\uffff\1\3\1\uffff\1\2\2\uffff\1\5\27\uffff"+
            "\1\1\15\uffff\1\6",
            "",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "\1\uffff",
            "",
            "",
            ""
    };

    static final short[] DFA47_eot = DFA.unpackEncodedString(DFA47_eotS);
    static final short[] DFA47_eof = DFA.unpackEncodedString(DFA47_eofS);
    static final char[] DFA47_min = DFA.unpackEncodedStringToUnsignedChars(DFA47_minS);
    static final char[] DFA47_max = DFA.unpackEncodedStringToUnsignedChars(DFA47_maxS);
    static final short[] DFA47_accept = DFA.unpackEncodedString(DFA47_acceptS);
    static final short[] DFA47_special = DFA.unpackEncodedString(DFA47_specialS);
    static final short[][] DFA47_transition;

    static {
        int numStates = DFA47_transitionS.length;
        DFA47_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA47_transition[i] = DFA.unpackEncodedString(DFA47_transitionS[i]);
        }
    }

    class DFA47 extends DFA {

        public DFA47(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 47;
            this.eot = DFA47_eot;
            this.eof = DFA47_eof;
            this.min = DFA47_min;
            this.max = DFA47_max;
            this.accept = DFA47_accept;
            this.special = DFA47_special;
            this.transition = DFA47_transition;
        }
        public String getDescription() {
            return "645:1: funcArgument returns [Expression expr] : (be= boolExpression | me= mathExpression | fe= funcExpression | ae= atomicExpression );";
        }
        public int specialStateTransition(int s, IntStream _input) throws NoViableAltException {
            TokenStream input = (TokenStream)_input;
        	int _s = s;
            switch ( s ) {
                    case 0 : 
                        int LA47_2 = input.LA(1);

                         
                        int index47_2 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred77_MLN()) ) {s = 1;}

                        else if ( (synpred78_MLN()) ) {s = 7;}

                        else if ( (synpred79_MLN()) ) {s = 8;}

                        else if ( (true) ) {s = 9;}

                         
                        input.seek(index47_2);
                        if ( s>=0 ) return s;
                        break;
                    case 1 : 
                        int LA47_3 = input.LA(1);

                         
                        int index47_3 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred77_MLN()) ) {s = 1;}

                        else if ( (synpred78_MLN()) ) {s = 7;}

                        else if ( (true) ) {s = 9;}

                         
                        input.seek(index47_3);
                        if ( s>=0 ) return s;
                        break;
                    case 2 : 
                        int LA47_4 = input.LA(1);

                         
                        int index47_4 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred77_MLN()) ) {s = 1;}

                        else if ( (synpred78_MLN()) ) {s = 7;}

                        else if ( (true) ) {s = 9;}

                         
                        input.seek(index47_4);
                        if ( s>=0 ) return s;
                        break;
                    case 3 : 
                        int LA47_5 = input.LA(1);

                         
                        int index47_5 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred77_MLN()) ) {s = 1;}

                        else if ( (synpred78_MLN()) ) {s = 7;}

                         
                        input.seek(index47_5);
                        if ( s>=0 ) return s;
                        break;
                    case 4 : 
                        int LA47_6 = input.LA(1);

                         
                        int index47_6 = input.index();
                        input.rewind();
                        s = -1;
                        if ( (synpred77_MLN()) ) {s = 1;}

                        else if ( (synpred78_MLN()) ) {s = 7;}

                         
                        input.seek(index47_6);
                        if ( s>=0 ) return s;
                        break;
            }
            if (state.backtracking>0) {state.failed=true; return -1;}
            NoViableAltException nvae =
                new NoViableAltException(getDescription(), 47, _s, input);
            error(nvae);
            throw nvae;
        }
    }
 

    public static final BitSet FOLLOW_schemaList_in_definitions895 = new BitSet(new long[]{0x0000002F805408C0L});
    public static final BitSet FOLLOW_ruleList_in_definitions897 = new BitSet(new long[]{0x0000000000000000L});
    public static final BitSet FOLLOW_EOF_in_definitions899 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_schema_in_schemaList908 = new BitSet(new long[]{0x000000000C700282L});
    public static final BitSet FOLLOW_schemaConstraint_in_schemaList912 = new BitSet(new long[]{0x000000000C700282L});
    public static final BitSet FOLLOW_PLUS_in_schema925 = new BitSet(new long[]{0x0000000000700200L});
    public static final BitSet FOLLOW_21_in_schema932 = new BitSet(new long[]{0x0000000000500000L});
    public static final BitSet FOLLOW_ASTERISK_in_schema936 = new BitSet(new long[]{0x0000000000500000L});
    public static final BitSet FOLLOW_22_in_schema943 = new BitSet(new long[]{0x0000000000100000L});
    public static final BitSet FOLLOW_ID_in_schema949 = new BitSet(new long[]{0x0000000000800200L});
    public static final BitSet FOLLOW_ASTERISK_in_schema953 = new BitSet(new long[]{0x0000000000800000L});
    public static final BitSet FOLLOW_23_in_schema960 = new BitSet(new long[]{0x0000000000100000L});
    public static final BitSet FOLLOW_predArg_in_schema964 = new BitSet(new long[]{0x0000000003000000L});
    public static final BitSet FOLLOW_24_in_schema967 = new BitSet(new long[]{0x0000000000100000L});
    public static final BitSet FOLLOW_predArg_in_schema971 = new BitSet(new long[]{0x0000000003000000L});
    public static final BitSet FOLLOW_25_in_schema975 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_predArg990 = new BitSet(new long[]{0x0000000000100042L});
    public static final BitSet FOLLOW_ID_in_predArg995 = new BitSet(new long[]{0x0000000000000042L});
    public static final BitSet FOLLOW_NOT_in_predArg1001 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_functionalDependency_in_schemaConstraint1019 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_set_in_functionalDependency1031 = new BitSet(new long[]{0x0000000010000000L});
    public static final BitSet FOLLOW_28_in_functionalDependency1039 = new BitSet(new long[]{0x0000000000100000L});
    public static final BitSet FOLLOW_functionalDependencyItem_in_functionalDependency1043 = new BitSet(new long[]{0x0000000020000002L});
    public static final BitSet FOLLOW_29_in_functionalDependency1048 = new BitSet(new long[]{0x0000000000100000L});
    public static final BitSet FOLLOW_functionalDependencyItem_in_functionalDependency1050 = new BitSet(new long[]{0x0000000020000002L});
    public static final BitSet FOLLOW_ID_in_functionalDependencyItem1070 = new BitSet(new long[]{0x0000000041000000L});
    public static final BitSet FOLLOW_24_in_functionalDependencyItem1073 = new BitSet(new long[]{0x0000000000100000L});
    public static final BitSet FOLLOW_ID_in_functionalDependencyItem1077 = new BitSet(new long[]{0x0000000041000000L});
    public static final BitSet FOLLOW_30_in_functionalDependencyItem1081 = new BitSet(new long[]{0x0000000000100000L});
    public static final BitSet FOLLOW_ID_in_functionalDependencyItem1085 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_mlnRule_in_ruleList1103 = new BitSet(new long[]{0x0000002F805408C2L});
    public static final BitSet FOLLOW_scopingRule_in_ruleList1107 = new BitSet(new long[]{0x0000002F805408C2L});
    public static final BitSet FOLLOW_datalogRule_in_ruleList1111 = new BitSet(new long[]{0x0000002F805408C2L});
    public static final BitSet FOLLOW_set_in_mlnRule1144 = new BitSet(new long[]{0x0000000000004000L});
    public static final BitSet FOLLOW_STRING_in_mlnRule1152 = new BitSet(new long[]{0x00000003805408C0L});
    public static final BitSet FOLLOW_softRule_in_mlnRule1173 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_hardRule_in_mlnRule1177 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_22_in_softRule1204 = new BitSet(new long[]{0x0000000000040000L});
    public static final BitSet FOLLOW_NUMBER_in_softRule1210 = new BitSet(new long[]{0x00000003805408C0L});
    public static final BitSet FOLLOW_foclause_in_softRule1214 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_22_in_softRule1235 = new BitSet(new long[]{0x0000000000100000L});
    public static final BitSet FOLLOW_ID_in_softRule1241 = new BitSet(new long[]{0x0000000010000000L});
    public static final BitSet FOLLOW_28_in_softRule1243 = new BitSet(new long[]{0x00000003805408C0L});
    public static final BitSet FOLLOW_foclause_in_softRule1247 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_foclause_in_hardRule1280 = new BitSet(new long[]{0x0000000000000400L});
    public static final BitSet FOLLOW_PERIOD_in_hardRule1282 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_functionalAtom1317 = new BitSet(new long[]{0x0000000000800000L});
    public static final BitSet FOLLOW_23_in_functionalAtom1330 = new BitSet(new long[]{0x2000000000944000L});
    public static final BitSet FOLLOW_expression_in_functionalAtom1339 = new BitSet(new long[]{0x0000000003000000L});
    public static final BitSet FOLLOW_24_in_functionalAtom1355 = new BitSet(new long[]{0x2000000000944000L});
    public static final BitSet FOLLOW_expression_in_functionalAtom1359 = new BitSet(new long[]{0x0000000003000000L});
    public static final BitSet FOLLOW_25_in_functionalAtom1382 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_set_in_datalogRule1408 = new BitSet(new long[]{0x00000008001000C0L});
    public static final BitSet FOLLOW_35_in_datalogRule1419 = new BitSet(new long[]{0x00000000001000C0L});
    public static final BitSet FOLLOW_literal_in_datalogRule1425 = new BitSet(new long[]{0x0000001000000000L});
    public static final BitSet FOLLOW_36_in_datalogRule1429 = new BitSet(new long[]{0x00000000001000C0L});
    public static final BitSet FOLLOW_literal_in_datalogRule1441 = new BitSet(new long[]{0x0000000001000400L});
    public static final BitSet FOLLOW_24_in_datalogRule1454 = new BitSet(new long[]{0x00000000001000C0L});
    public static final BitSet FOLLOW_literal_in_datalogRule1458 = new BitSet(new long[]{0x0000000001000400L});
    public static final BitSet FOLLOW_24_in_datalogRule1473 = new BitSet(new long[]{0x2000000000944000L});
    public static final BitSet FOLLOW_mathComparison_in_datalogRule1477 = new BitSet(new long[]{0x0000000001000400L});
    public static final BitSet FOLLOW_24_in_datalogRule1491 = new BitSet(new long[]{0x0000002000000000L});
    public static final BitSet FOLLOW_37_in_datalogRule1493 = new BitSet(new long[]{0x2000800000944040L});
    public static final BitSet FOLLOW_boolExpression_in_datalogRule1508 = new BitSet(new long[]{0x0000004000000000L});
    public static final BitSet FOLLOW_38_in_datalogRule1519 = new BitSet(new long[]{0x0000000000000400L});
    public static final BitSet FOLLOW_PERIOD_in_datalogRule1528 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_37_in_scopingRule1561 = new BitSet(new long[]{0x0000008000000000L});
    public static final BitSet FOLLOW_39_in_scopingRule1563 = new BitSet(new long[]{0x0000010000000000L});
    public static final BitSet FOLLOW_40_in_scopingRule1565 = new BitSet(new long[]{0x0000000000040000L});
    public static final BitSet FOLLOW_NUMBER_in_scopingRule1569 = new BitSet(new long[]{0x0000004000000000L});
    public static final BitSet FOLLOW_38_in_scopingRule1571 = new BitSet(new long[]{0x00000000001000C0L});
    public static final BitSet FOLLOW_literal_in_scopingRule1577 = new BitSet(new long[]{0x0000020000000000L});
    public static final BitSet FOLLOW_41_in_scopingRule1581 = new BitSet(new long[]{0x00000000001000C0L});
    public static final BitSet FOLLOW_literal_in_scopingRule1593 = new BitSet(new long[]{0x0000000001000400L});
    public static final BitSet FOLLOW_24_in_scopingRule1606 = new BitSet(new long[]{0x00000000001000C0L});
    public static final BitSet FOLLOW_literal_in_scopingRule1610 = new BitSet(new long[]{0x0000000001000400L});
    public static final BitSet FOLLOW_24_in_scopingRule1625 = new BitSet(new long[]{0x2000000000944000L});
    public static final BitSet FOLLOW_mathComparison_in_scopingRule1629 = new BitSet(new long[]{0x0000000001000400L});
    public static final BitSet FOLLOW_24_in_scopingRule1643 = new BitSet(new long[]{0x0000002000000000L});
    public static final BitSet FOLLOW_37_in_scopingRule1645 = new BitSet(new long[]{0x2000800000944040L});
    public static final BitSet FOLLOW_boolExpression_in_scopingRule1660 = new BitSet(new long[]{0x0000004000000000L});
    public static final BitSet FOLLOW_38_in_scopingRule1671 = new BitSet(new long[]{0x0000000000000400L});
    public static final BitSet FOLLOW_PERIOD_in_scopingRule1680 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_existQuan_in_foclause1717 = new BitSet(new long[]{0x00000000001000C0L});
    public static final BitSet FOLLOW_literal_in_foclause1739 = new BitSet(new long[]{0x0000000001001000L});
    public static final BitSet FOLLOW_24_in_foclause1752 = new BitSet(new long[]{0x00000000001000C0L});
    public static final BitSet FOLLOW_literal_in_foclause1756 = new BitSet(new long[]{0x0000000001001000L});
    public static final BitSet FOLLOW_24_in_foclause1771 = new BitSet(new long[]{0x2000000000944000L});
    public static final BitSet FOLLOW_mathComparison_in_foclause1775 = new BitSet(new long[]{0x0000000001001000L});
    public static final BitSet FOLLOW_24_in_foclause1788 = new BitSet(new long[]{0x0000002000000000L});
    public static final BitSet FOLLOW_37_in_foclause1790 = new BitSet(new long[]{0x2000800000944040L});
    public static final BitSet FOLLOW_boolExpression_in_foclause1804 = new BitSet(new long[]{0x0000004000000000L});
    public static final BitSet FOLLOW_38_in_foclause1814 = new BitSet(new long[]{0x0000000000001000L});
    public static final BitSet FOLLOW_IMPLIES_in_foclause1835 = new BitSet(new long[]{0x00000000001000C0L});
    public static final BitSet FOLLOW_literal_in_foclause1853 = new BitSet(new long[]{0x0000040000000002L});
    public static final BitSet FOLLOW_42_in_foclause1863 = new BitSet(new long[]{0x00000000001000C0L});
    public static final BitSet FOLLOW_literal_in_foclause1867 = new BitSet(new long[]{0x0000040000000002L});
    public static final BitSet FOLLOW_42_in_foclause1882 = new BitSet(new long[]{0x2000000000944000L});
    public static final BitSet FOLLOW_mathComparison_in_foclause1886 = new BitSet(new long[]{0x0000040000000002L});
    public static final BitSet FOLLOW_42_in_foclause1904 = new BitSet(new long[]{0x0000002000000000L});
    public static final BitSet FOLLOW_37_in_foclause1906 = new BitSet(new long[]{0x2000800000944040L});
    public static final BitSet FOLLOW_boolExpression_in_foclause1919 = new BitSet(new long[]{0x0000004000000000L});
    public static final BitSet FOLLOW_38_in_foclause1928 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_EXIST_in_existQuan1959 = new BitSet(new long[]{0x0000000000100000L});
    public static final BitSet FOLLOW_ID_in_existQuan1971 = new BitSet(new long[]{0x0000000001000002L});
    public static final BitSet FOLLOW_24_in_existQuan1984 = new BitSet(new long[]{0x0000000000100000L});
    public static final BitSet FOLLOW_ID_in_existQuan1988 = new BitSet(new long[]{0x0000000001000002L});
    public static final BitSet FOLLOW_mathExpression_in_expression2015 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_boolConjunction_in_boolExpression2042 = new BitSet(new long[]{0x0000180000000002L});
    public static final BitSet FOLLOW_set_in_boolExpression2064 = new BitSet(new long[]{0x2000800000944040L});
    public static final BitSet FOLLOW_boolConjunction_in_boolExpression2074 = new BitSet(new long[]{0x0000180000000002L});
    public static final BitSet FOLLOW_boolConjunctionElement_in_boolConjunction2118 = new BitSet(new long[]{0x0000600000000002L});
    public static final BitSet FOLLOW_set_in_boolConjunction2140 = new BitSet(new long[]{0x2000800000944040L});
    public static final BitSet FOLLOW_boolConjunctionElement_in_boolConjunction2150 = new BitSet(new long[]{0x0000600000000002L});
    public static final BitSet FOLLOW_set_in_boolConjunctionElement2194 = new BitSet(new long[]{0x2000000000944000L});
    public static final BitSet FOLLOW_mathComparison_in_boolConjunctionElement2222 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_funcExpression_in_boolConjunctionElement2235 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_set_in_boolConjunctionElement2260 = new BitSet(new long[]{0x0000000000800000L});
    public static final BitSet FOLLOW_23_in_boolConjunctionElement2269 = new BitSet(new long[]{0x2000800000944040L});
    public static final BitSet FOLLOW_boolExpression_in_boolConjunctionElement2273 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_25_in_boolConjunctionElement2275 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_mathExpression_in_mathComparison2309 = new BitSet(new long[]{0x003F010000000000L});
    public static final BitSet FOLLOW_set_in_mathComparison2313 = new BitSet(new long[]{0x2000000000944000L});
    public static final BitSet FOLLOW_mathExpression_in_mathComparison2331 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_mathTerm_in_mathExpression2365 = new BitSet(new long[]{0x0040000000000182L});
    public static final BitSet FOLLOW_set_in_mathExpression2381 = new BitSet(new long[]{0x2000000000944000L});
    public static final BitSet FOLLOW_mathTerm_in_mathExpression2391 = new BitSet(new long[]{0x0040000000000182L});
    public static final BitSet FOLLOW_mathFactor_in_mathTerm2436 = new BitSet(new long[]{0x1F80000000000202L});
    public static final BitSet FOLLOW_set_in_mathTerm2452 = new BitSet(new long[]{0x2000000000944000L});
    public static final BitSet FOLLOW_mathFactor_in_mathTerm2470 = new BitSet(new long[]{0x1F80000000000202L});
    public static final BitSet FOLLOW_funcExpression_in_mathFactor2519 = new BitSet(new long[]{0x0000000000000042L});
    public static final BitSet FOLLOW_atomicExpression_in_mathFactor2539 = new BitSet(new long[]{0x0000000000000042L});
    public static final BitSet FOLLOW_23_in_mathFactor2557 = new BitSet(new long[]{0x2000000000944000L});
    public static final BitSet FOLLOW_mathExpression_in_mathFactor2561 = new BitSet(new long[]{0x0000000002000000L});
    public static final BitSet FOLLOW_25_in_mathFactor2563 = new BitSet(new long[]{0x0000000000000042L});
    public static final BitSet FOLLOW_61_in_mathFactor2581 = new BitSet(new long[]{0x2000000000944000L});
    public static final BitSet FOLLOW_mathFactor_in_mathFactor2585 = new BitSet(new long[]{0x0000000000000042L});
    public static final BitSet FOLLOW_NOT_in_mathFactor2603 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_funcExpression2638 = new BitSet(new long[]{0x0000000000800000L});
    public static final BitSet FOLLOW_23_in_funcExpression2652 = new BitSet(new long[]{0x2000800000944040L});
    public static final BitSet FOLLOW_funcArgument_in_funcExpression2656 = new BitSet(new long[]{0x0000000003000000L});
    public static final BitSet FOLLOW_24_in_funcExpression2671 = new BitSet(new long[]{0x2000800000944040L});
    public static final BitSet FOLLOW_funcArgument_in_funcExpression2675 = new BitSet(new long[]{0x0000000003000000L});
    public static final BitSet FOLLOW_25_in_funcExpression2696 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_boolExpression_in_funcArgument2723 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_mathExpression_in_funcArgument2739 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_funcExpression_in_funcArgument2755 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_atomicExpression_in_funcArgument2770 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NUMBER_in_atomicExpression2808 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_STRING_in_atomicExpression2824 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_atomicExpression2840 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_set_in_literal2876 = new BitSet(new long[]{0x00000000001000C0L});
    public static final BitSet FOLLOW_atom_in_literal2884 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_term2916 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_set_in_term2932 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_atom2967 = new BitSet(new long[]{0x0000000000800000L});
    public static final BitSet FOLLOW_23_in_atom2969 = new BitSet(new long[]{0x0000000000144000L});
    public static final BitSet FOLLOW_term_in_atom2984 = new BitSet(new long[]{0x0000000003000000L});
    public static final BitSet FOLLOW_24_in_atom2998 = new BitSet(new long[]{0x0000000000144000L});
    public static final BitSet FOLLOW_term_in_atom3007 = new BitSet(new long[]{0x0000000003000000L});
    public static final BitSet FOLLOW_25_in_atom3022 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_query_in_queryList3041 = new BitSet(new long[]{0x00000000001000C0L});
    public static final BitSet FOLLOW_EOF_in_queryList3044 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_atom_in_query3060 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_ID_in_query3070 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_query_in_queryCommaList3085 = new BitSet(new long[]{0x0000000001000000L});
    public static final BitSet FOLLOW_24_in_queryCommaList3088 = new BitSet(new long[]{0x00000000001000C0L});
    public static final BitSet FOLLOW_query_in_queryCommaList3090 = new BitSet(new long[]{0x0000000001000000L});
    public static final BitSet FOLLOW_EOF_in_queryCommaList3094 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_evidence_in_evidenceList3103 = new BitSet(new long[]{0x0000000000140040L});
    public static final BitSet FOLLOW_EOF_in_evidenceList3106 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NUMBER_in_evidence3117 = new BitSet(new long[]{0x0000000000100040L});
    public static final BitSet FOLLOW_NOT_in_evidence3120 = new BitSet(new long[]{0x0000000000100000L});
    public static final BitSet FOLLOW_ID_in_evidence3125 = new BitSet(new long[]{0x0000000000800000L});
    public static final BitSet FOLLOW_23_in_evidence3127 = new BitSet(new long[]{0x0000000000144000L});
    public static final BitSet FOLLOW_set_in_evidence3131 = new BitSet(new long[]{0x0000000003000000L});
    public static final BitSet FOLLOW_24_in_evidence3140 = new BitSet(new long[]{0x0000000000144000L});
    public static final BitSet FOLLOW_set_in_evidence3144 = new BitSet(new long[]{0x0000000003000000L});
    public static final BitSet FOLLOW_25_in_evidence3154 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_mathComparison_in_synpred51_MLN2222 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_set_in_synpred52_MLN2194 = new BitSet(new long[]{0x2000000000944000L});
    public static final BitSet FOLLOW_mathComparison_in_synpred52_MLN2222 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_funcExpression_in_synpred52_MLN2235 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_set_in_synpred64_MLN2381 = new BitSet(new long[]{0x2000000000944000L});
    public static final BitSet FOLLOW_mathTerm_in_synpred64_MLN2391 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_NOT_in_synpred75_MLN2603 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_boolExpression_in_synpred77_MLN2723 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_mathExpression_in_synpred78_MLN2739 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_funcExpression_in_synpred79_MLN2755 = new BitSet(new long[]{0x0000000000000002L});

}