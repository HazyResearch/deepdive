// $ANTLR 3.2 Sep 23, 2009 12:02:23 /scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g 2012-02-17 15:34:25

package tuffy.parse;
import java.util.Hashtable;
import tuffy.mln.*;
import tuffy.util.*;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;


import org.antlr.runtime.tree.*;

public class ConfigParser extends Parser {
    public static final String[] tokenNames = new String[] {
        "<invalid>", "<EOR>", "<DOWN>", "<UP>", "WS", "COMMENT", "SPAN", "'='"
    };
    public static final int WS=4;
    public static final int SPAN=6;
    public static final int COMMENT=5;
    public static final int EOF=-1;
    public static final int T__7=7;

    // delegates
    // delegators


        public ConfigParser(TokenStream input) {
            this(input, new RecognizerSharedState());
        }
        public ConfigParser(TokenStream input, RecognizerSharedState state) {
            super(input, state);
             
        }
        
    protected TreeAdaptor adaptor = new CommonTreeAdaptor();

    public void setTreeAdaptor(TreeAdaptor adaptor) {
        this.adaptor = adaptor;
    }
    public TreeAdaptor getTreeAdaptor() {
        return adaptor;
    }

    public String[] getTokenNames() { return ConfigParser.tokenNames; }
    public String getGrammarFileName() { return "/scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g"; }


    public Hashtable<String, String> map = new Hashtable<String, String>();


    public static class config_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "config"
    // /scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g:32:1: config : ( state )+ EOF ;
    public final ConfigParser.config_return config() throws RecognitionException {
        ConfigParser.config_return retval = new ConfigParser.config_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token EOF2=null;
        ConfigParser.state_return state1 = null;


        Object EOF2_tree=null;

        try {
            // /scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g:32:8: ( ( state )+ EOF )
            // /scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g:32:11: ( state )+ EOF
            {
            root_0 = (Object)adaptor.nil();

            // /scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g:32:11: ( state )+
            int cnt1=0;
            loop1:
            do {
                int alt1=2;
                int LA1_0 = input.LA(1);

                if ( (LA1_0==SPAN) ) {
                    alt1=1;
                }


                switch (alt1) {
            	case 1 :
            	    // /scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g:32:12: state
            	    {
            	    pushFollow(FOLLOW_state_in_config196);
            	    state1=state();

            	    state._fsp--;

            	    adaptor.addChild(root_0, state1.getTree());

            	    }
            	    break;

            	default :
            	    if ( cnt1 >= 1 ) break loop1;
                        EarlyExitException eee =
                            new EarlyExitException(1, input);
                        throw eee;
                }
                cnt1++;
            } while (true);

            EOF2=(Token)match(input,EOF,FOLLOW_EOF_in_config200); 
            EOF2_tree = (Object)adaptor.create(EOF2);
            adaptor.addChild(root_0, EOF2_tree);


            }

            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "config"

    public static class state_return extends ParserRuleReturnScope {
        Object tree;
        public Object getTree() { return tree; }
    };

    // $ANTLR start "state"
    // /scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g:34:1: state : id= SPAN '=' value= SPAN ;
    public final ConfigParser.state_return state() throws RecognitionException {
        ConfigParser.state_return retval = new ConfigParser.state_return();
        retval.start = input.LT(1);

        Object root_0 = null;

        Token id=null;
        Token value=null;
        Token char_literal3=null;

        Object id_tree=null;
        Object value_tree=null;
        Object char_literal3_tree=null;

        try {
            // /scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g:34:7: (id= SPAN '=' value= SPAN )
            // /scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g:34:9: id= SPAN '=' value= SPAN
            {
            root_0 = (Object)adaptor.nil();

            id=(Token)match(input,SPAN,FOLLOW_SPAN_in_state210); 
            id_tree = (Object)adaptor.create(id);
            adaptor.addChild(root_0, id_tree);

            char_literal3=(Token)match(input,7,FOLLOW_7_in_state212); 
            char_literal3_tree = (Object)adaptor.create(char_literal3);
            adaptor.addChild(root_0, char_literal3_tree);

            value=(Token)match(input,SPAN,FOLLOW_SPAN_in_state216); 
            value_tree = (Object)adaptor.create(value);
            adaptor.addChild(root_0, value_tree);


                      map.put((id!=null?id.getText():null), (value!=null?value.getText():null));
                  

            }

            retval.stop = input.LT(-1);

            retval.tree = (Object)adaptor.rulePostProcessing(root_0);
            adaptor.setTokenBoundaries(retval.tree, retval.start, retval.stop);

        }
        catch (RecognitionException re) {
            reportError(re);
            recover(input,re);
    	retval.tree = (Object)adaptor.errorNode(input, retval.start, input.LT(-1), re);

        }
        finally {
        }
        return retval;
    }
    // $ANTLR end "state"

    // Delegated rules


 

    public static final BitSet FOLLOW_state_in_config196 = new BitSet(new long[]{0x0000000000000040L});
    public static final BitSet FOLLOW_EOF_in_config200 = new BitSet(new long[]{0x0000000000000002L});
    public static final BitSet FOLLOW_SPAN_in_state210 = new BitSet(new long[]{0x0000000000000080L});
    public static final BitSet FOLLOW_7_in_state212 = new BitSet(new long[]{0x0000000000000040L});
    public static final BitSet FOLLOW_SPAN_in_state216 = new BitSet(new long[]{0x0000000000000002L});

}