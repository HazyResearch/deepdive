// $ANTLR 3.2 Sep 23, 2009 12:02:23 MLN.g 2012-05-09 08:57:44

package tuffy.parse;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

public class MLNLexer extends Lexer {
    public static final int EXPONENT=19;
    public static final int T__29=29;
    public static final int T__28=28;
    public static final int T__27=27;
    public static final int T__26=26;
    public static final int T__25=25;
    public static final int T__24=24;
    public static final int T__23=23;
    public static final int T__22=22;
    public static final int ESC=13;
    public static final int T__21=21;
    public static final int FLOAT=17;
    public static final int NOT=6;
    public static final int T__61=61;
    public static final int ID=20;
    public static final int T__60=60;
    public static final int EOF=-1;
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
    public static final int T__48=48;
    public static final int PERIOD=10;
    public static final int T__49=49;
    public static final int NUMBER=18;
    public static final int MINUS=8;
    public static final int T__30=30;
    public static final int T__31=31;
    public static final int T__32=32;
    public static final int T__33=33;
    public static final int WS=4;
    public static final int T__34=34;
    public static final int T__35=35;
    public static final int T__36=36;
    public static final int T__37=37;
    public static final int T__38=38;
    public static final int T__39=39;
    public static final int STRING=14;
    public static final int EXIST=11;
    public static final int HEXDIGIT=15;

    // delegates
    // delegators

    public MLNLexer() {;} 
    public MLNLexer(CharStream input) {
        this(input, new RecognizerSharedState());
    }
    public MLNLexer(CharStream input, RecognizerSharedState state) {
        super(input,state);

    }
    public String getGrammarFileName() { return "MLN.g"; }

    // $ANTLR start "T__21"
    public final void mT__21() throws RecognitionException {
        try {
            int _type = T__21;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:11:7: ( '**' )
            // MLN.g:11:9: '**'
            {
            match("**"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__21"

    // $ANTLR start "T__22"
    public final void mT__22() throws RecognitionException {
        try {
            int _type = T__22;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:12:7: ( '@' )
            // MLN.g:12:9: '@'
            {
            match('@'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__22"

    // $ANTLR start "T__23"
    public final void mT__23() throws RecognitionException {
        try {
            int _type = T__23;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:13:7: ( '(' )
            // MLN.g:13:9: '('
            {
            match('('); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__23"

    // $ANTLR start "T__24"
    public final void mT__24() throws RecognitionException {
        try {
            int _type = T__24;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:14:7: ( ',' )
            // MLN.g:14:9: ','
            {
            match(','); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__24"

    // $ANTLR start "T__25"
    public final void mT__25() throws RecognitionException {
        try {
            int _type = T__25;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:15:7: ( ')' )
            // MLN.g:15:9: ')'
            {
            match(')'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__25"

    // $ANTLR start "T__26"
    public final void mT__26() throws RecognitionException {
        try {
            int _type = T__26;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:16:7: ( 'FUNCTIONAL DEPENDENCY' )
            // MLN.g:16:9: 'FUNCTIONAL DEPENDENCY'
            {
            match("FUNCTIONAL DEPENDENCY"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__26"

    // $ANTLR start "T__27"
    public final void mT__27() throws RecognitionException {
        try {
            int _type = T__27;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:17:7: ( 'FD' )
            // MLN.g:17:9: 'FD'
            {
            match("FD"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__27"

    // $ANTLR start "T__28"
    public final void mT__28() throws RecognitionException {
        try {
            int _type = T__28;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:18:7: ( ':' )
            // MLN.g:18:9: ':'
            {
            match(':'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__28"

    // $ANTLR start "T__29"
    public final void mT__29() throws RecognitionException {
        try {
            int _type = T__29;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:19:7: ( ';' )
            // MLN.g:19:9: ';'
            {
            match(';'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__29"

    // $ANTLR start "T__30"
    public final void mT__30() throws RecognitionException {
        try {
            int _type = T__30;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:20:7: ( '->' )
            // MLN.g:20:9: '->'
            {
            match("->"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__30"

    // $ANTLR start "T__31"
    public final void mT__31() throws RecognitionException {
        try {
            int _type = T__31;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:21:7: ( '[Label]' )
            // MLN.g:21:9: '[Label]'
            {
            match("[Label]"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__31"

    // $ANTLR start "T__32"
    public final void mT__32() throws RecognitionException {
        try {
            int _type = T__32;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:22:7: ( '[+Label]' )
            // MLN.g:22:9: '[+Label]'
            {
            match("[+Label]"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__32"

    // $ANTLR start "T__33"
    public final void mT__33() throws RecognitionException {
        try {
            int _type = T__33;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:23:7: ( '[Label+]' )
            // MLN.g:23:9: '[Label+]'
            {
            match("[Label+]"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__33"

    // $ANTLR start "T__34"
    public final void mT__34() throws RecognitionException {
        try {
            int _type = T__34;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:24:7: ( '#' )
            // MLN.g:24:9: '#'
            {
            match('#'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__34"

    // $ANTLR start "T__35"
    public final void mT__35() throws RecognitionException {
        try {
            int _type = T__35;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:25:7: ( '$' )
            // MLN.g:25:9: '$'
            {
            match('$'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__35"

    // $ANTLR start "T__36"
    public final void mT__36() throws RecognitionException {
        try {
            int _type = T__36;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:26:7: ( ':-' )
            // MLN.g:26:9: ':-'
            {
            match(":-"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__36"

    // $ANTLR start "T__37"
    public final void mT__37() throws RecognitionException {
        try {
            int _type = T__37;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:27:7: ( '[' )
            // MLN.g:27:9: '['
            {
            match('['); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__37"

    // $ANTLR start "T__38"
    public final void mT__38() throws RecognitionException {
        try {
            int _type = T__38;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:28:7: ( ']' )
            // MLN.g:28:9: ']'
            {
            match(']'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__38"

    // $ANTLR start "T__39"
    public final void mT__39() throws RecognitionException {
        try {
            int _type = T__39;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:29:7: ( 'priorProb' )
            // MLN.g:29:9: 'priorProb'
            {
            match("priorProb"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__39"

    // $ANTLR start "T__40"
    public final void mT__40() throws RecognitionException {
        try {
            int _type = T__40;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:30:7: ( '=' )
            // MLN.g:30:9: '='
            {
            match('='); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__40"

    // $ANTLR start "T__41"
    public final void mT__41() throws RecognitionException {
        try {
            int _type = T__41;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:31:7: ( ':=' )
            // MLN.g:31:9: ':='
            {
            match(":="); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__41"

    // $ANTLR start "T__42"
    public final void mT__42() throws RecognitionException {
        try {
            int _type = T__42;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:32:7: ( 'v' )
            // MLN.g:32:9: 'v'
            {
            match('v'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__42"

    // $ANTLR start "T__43"
    public final void mT__43() throws RecognitionException {
        try {
            int _type = T__43;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:33:7: ( '||' )
            // MLN.g:33:9: '||'
            {
            match("||"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__43"

    // $ANTLR start "T__44"
    public final void mT__44() throws RecognitionException {
        try {
            int _type = T__44;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:34:7: ( 'OR' )
            // MLN.g:34:9: 'OR'
            {
            match("OR"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__44"

    // $ANTLR start "T__45"
    public final void mT__45() throws RecognitionException {
        try {
            int _type = T__45;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:35:7: ( '&&' )
            // MLN.g:35:9: '&&'
            {
            match("&&"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__45"

    // $ANTLR start "T__46"
    public final void mT__46() throws RecognitionException {
        try {
            int _type = T__46;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:36:7: ( 'AND' )
            // MLN.g:36:9: 'AND'
            {
            match("AND"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__46"

    // $ANTLR start "T__47"
    public final void mT__47() throws RecognitionException {
        try {
            int _type = T__47;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:37:7: ( 'NOT' )
            // MLN.g:37:9: 'NOT'
            {
            match("NOT"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__47"

    // $ANTLR start "T__48"
    public final void mT__48() throws RecognitionException {
        try {
            int _type = T__48;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:38:7: ( '<>' )
            // MLN.g:38:9: '<>'
            {
            match("<>"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__48"

    // $ANTLR start "T__49"
    public final void mT__49() throws RecognitionException {
        try {
            int _type = T__49;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:39:7: ( '<' )
            // MLN.g:39:9: '<'
            {
            match('<'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__49"

    // $ANTLR start "T__50"
    public final void mT__50() throws RecognitionException {
        try {
            int _type = T__50;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:40:7: ( '<=' )
            // MLN.g:40:9: '<='
            {
            match("<="); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__50"

    // $ANTLR start "T__51"
    public final void mT__51() throws RecognitionException {
        try {
            int _type = T__51;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:41:7: ( '>' )
            // MLN.g:41:9: '>'
            {
            match('>'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__51"

    // $ANTLR start "T__52"
    public final void mT__52() throws RecognitionException {
        try {
            int _type = T__52;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:42:7: ( '>=' )
            // MLN.g:42:9: '>='
            {
            match(">="); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__52"

    // $ANTLR start "T__53"
    public final void mT__53() throws RecognitionException {
        try {
            int _type = T__53;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:43:7: ( '!=' )
            // MLN.g:43:9: '!='
            {
            match("!="); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__53"

    // $ANTLR start "T__54"
    public final void mT__54() throws RecognitionException {
        try {
            int _type = T__54;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:44:7: ( '%' )
            // MLN.g:44:9: '%'
            {
            match('%'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__54"

    // $ANTLR start "T__55"
    public final void mT__55() throws RecognitionException {
        try {
            int _type = T__55;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:45:7: ( '/' )
            // MLN.g:45:9: '/'
            {
            match('/'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__55"

    // $ANTLR start "T__56"
    public final void mT__56() throws RecognitionException {
        try {
            int _type = T__56;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:46:7: ( '&' )
            // MLN.g:46:9: '&'
            {
            match('&'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__56"

    // $ANTLR start "T__57"
    public final void mT__57() throws RecognitionException {
        try {
            int _type = T__57;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:47:7: ( '|' )
            // MLN.g:47:9: '|'
            {
            match('|'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__57"

    // $ANTLR start "T__58"
    public final void mT__58() throws RecognitionException {
        try {
            int _type = T__58;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:48:7: ( '^' )
            // MLN.g:48:9: '^'
            {
            match('^'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__58"

    // $ANTLR start "T__59"
    public final void mT__59() throws RecognitionException {
        try {
            int _type = T__59;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:49:7: ( '<<' )
            // MLN.g:49:9: '<<'
            {
            match("<<"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__59"

    // $ANTLR start "T__60"
    public final void mT__60() throws RecognitionException {
        try {
            int _type = T__60;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:50:7: ( '>>' )
            // MLN.g:50:9: '>>'
            {
            match(">>"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__60"

    // $ANTLR start "T__61"
    public final void mT__61() throws RecognitionException {
        try {
            int _type = T__61;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:51:7: ( '~' )
            // MLN.g:51:9: '~'
            {
            match('~'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__61"

    // $ANTLR start "WS"
    public final void mWS() throws RecognitionException {
        try {
            int _type = WS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:42:5: ( ( ' ' | '\\t' | '\\r' | '\\n' ) )
            // MLN.g:42:9: ( ' ' | '\\t' | '\\r' | '\\n' )
            {
            if ( (input.LA(1)>='\t' && input.LA(1)<='\n')||input.LA(1)=='\r'||input.LA(1)==' ' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            _channel=HIDDEN;

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "WS"

    // $ANTLR start "COMMENT"
    public final void mCOMMENT() throws RecognitionException {
        try {
            int _type = COMMENT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:50:5: ( '//' (~ ( '\\n' | '\\r' ) )* ( '\\r' )? '\\n' | '/*' ( options {greedy=false; } : . )* '*/' )
            int alt4=2;
            int LA4_0 = input.LA(1);

            if ( (LA4_0=='/') ) {
                int LA4_1 = input.LA(2);

                if ( (LA4_1=='/') ) {
                    alt4=1;
                }
                else if ( (LA4_1=='*') ) {
                    alt4=2;
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("", 4, 1, input);

                    throw nvae;
                }
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 4, 0, input);

                throw nvae;
            }
            switch (alt4) {
                case 1 :
                    // MLN.g:50:9: '//' (~ ( '\\n' | '\\r' ) )* ( '\\r' )? '\\n'
                    {
                    match("//"); 

                    // MLN.g:50:14: (~ ( '\\n' | '\\r' ) )*
                    loop1:
                    do {
                        int alt1=2;
                        int LA1_0 = input.LA(1);

                        if ( ((LA1_0>='\u0000' && LA1_0<='\t')||(LA1_0>='\u000B' && LA1_0<='\f')||(LA1_0>='\u000E' && LA1_0<='\uFFFF')) ) {
                            alt1=1;
                        }


                        switch (alt1) {
                    	case 1 :
                    	    // MLN.g:50:14: ~ ( '\\n' | '\\r' )
                    	    {
                    	    if ( (input.LA(1)>='\u0000' && input.LA(1)<='\t')||(input.LA(1)>='\u000B' && input.LA(1)<='\f')||(input.LA(1)>='\u000E' && input.LA(1)<='\uFFFF') ) {
                    	        input.consume();

                    	    }
                    	    else {
                    	        MismatchedSetException mse = new MismatchedSetException(null,input);
                    	        recover(mse);
                    	        throw mse;}


                    	    }
                    	    break;

                    	default :
                    	    break loop1;
                        }
                    } while (true);

                    // MLN.g:50:28: ( '\\r' )?
                    int alt2=2;
                    int LA2_0 = input.LA(1);

                    if ( (LA2_0=='\r') ) {
                        alt2=1;
                    }
                    switch (alt2) {
                        case 1 :
                            // MLN.g:50:28: '\\r'
                            {
                            match('\r'); 

                            }
                            break;

                    }

                    match('\n'); 
                    _channel=HIDDEN;

                    }
                    break;
                case 2 :
                    // MLN.g:51:9: '/*' ( options {greedy=false; } : . )* '*/'
                    {
                    match("/*"); 

                    // MLN.g:51:14: ( options {greedy=false; } : . )*
                    loop3:
                    do {
                        int alt3=2;
                        int LA3_0 = input.LA(1);

                        if ( (LA3_0=='*') ) {
                            int LA3_1 = input.LA(2);

                            if ( (LA3_1=='/') ) {
                                alt3=2;
                            }
                            else if ( ((LA3_1>='\u0000' && LA3_1<='.')||(LA3_1>='0' && LA3_1<='\uFFFF')) ) {
                                alt3=1;
                            }


                        }
                        else if ( ((LA3_0>='\u0000' && LA3_0<=')')||(LA3_0>='+' && LA3_0<='\uFFFF')) ) {
                            alt3=1;
                        }


                        switch (alt3) {
                    	case 1 :
                    	    // MLN.g:51:42: .
                    	    {
                    	    matchAny(); 

                    	    }
                    	    break;

                    	default :
                    	    break loop3;
                        }
                    } while (true);

                    match("*/"); 

                    _channel=HIDDEN;

                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "COMMENT"

    // $ANTLR start "NOT"
    public final void mNOT() throws RecognitionException {
        try {
            int _type = NOT;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:54:6: ( '!' )
            // MLN.g:54:8: '!'
            {
            match('!'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "NOT"

    // $ANTLR start "PLUS"
    public final void mPLUS() throws RecognitionException {
        try {
            int _type = PLUS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:55:8: ( '+' )
            // MLN.g:55:10: '+'
            {
            match('+'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "PLUS"

    // $ANTLR start "MINUS"
    public final void mMINUS() throws RecognitionException {
        try {
            int _type = MINUS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:56:9: ( '-' )
            // MLN.g:56:11: '-'
            {
            match('-'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "MINUS"

    // $ANTLR start "ASTERISK"
    public final void mASTERISK() throws RecognitionException {
        try {
            int _type = ASTERISK;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:57:9: ( '*' )
            // MLN.g:57:11: '*'
            {
            match('*'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ASTERISK"

    // $ANTLR start "PERIOD"
    public final void mPERIOD() throws RecognitionException {
        try {
            int _type = PERIOD;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:58:7: ( '.' )
            // MLN.g:58:9: '.'
            {
            match('.'); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "PERIOD"

    // $ANTLR start "EXIST"
    public final void mEXIST() throws RecognitionException {
        try {
            int _type = EXIST;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:59:7: ( 'EXIST' | 'Exist' | 'exist' )
            int alt5=3;
            int LA5_0 = input.LA(1);

            if ( (LA5_0=='E') ) {
                int LA5_1 = input.LA(2);

                if ( (LA5_1=='X') ) {
                    alt5=1;
                }
                else if ( (LA5_1=='x') ) {
                    alt5=2;
                }
                else {
                    NoViableAltException nvae =
                        new NoViableAltException("", 5, 1, input);

                    throw nvae;
                }
            }
            else if ( (LA5_0=='e') ) {
                alt5=3;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 5, 0, input);

                throw nvae;
            }
            switch (alt5) {
                case 1 :
                    // MLN.g:59:9: 'EXIST'
                    {
                    match("EXIST"); 


                    }
                    break;
                case 2 :
                    // MLN.g:59:19: 'Exist'
                    {
                    match("Exist"); 


                    }
                    break;
                case 3 :
                    // MLN.g:59:29: 'exist'
                    {
                    match("exist"); 


                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "EXIST"

    // $ANTLR start "IMPLIES"
    public final void mIMPLIES() throws RecognitionException {
        try {
            int _type = IMPLIES;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:60:9: ( '=>' )
            // MLN.g:60:11: '=>'
            {
            match("=>"); 


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "IMPLIES"

    // $ANTLR start "STRING"
    public final void mSTRING() throws RecognitionException {
        try {
            int _type = STRING;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            CommonToken escaped=null;
            int normal;

            StringBuilder lBuf = new StringBuilder();
            // MLN.g:64:5: ( '\"' (escaped= ESC | normal=~ ( '\"' | '\\\\' | '\\n' | '\\r' ) )* '\"' )
            // MLN.g:65:4: '\"' (escaped= ESC | normal=~ ( '\"' | '\\\\' | '\\n' | '\\r' ) )* '\"'
            {
            match('\"'); 
            // MLN.g:66:4: (escaped= ESC | normal=~ ( '\"' | '\\\\' | '\\n' | '\\r' ) )*
            loop6:
            do {
                int alt6=3;
                int LA6_0 = input.LA(1);

                if ( (LA6_0=='\\') ) {
                    alt6=1;
                }
                else if ( ((LA6_0>='\u0000' && LA6_0<='\t')||(LA6_0>='\u000B' && LA6_0<='\f')||(LA6_0>='\u000E' && LA6_0<='!')||(LA6_0>='#' && LA6_0<='[')||(LA6_0>=']' && LA6_0<='\uFFFF')) ) {
                    alt6=2;
                }


                switch (alt6) {
            	case 1 :
            	    // MLN.g:66:6: escaped= ESC
            	    {
            	    int escapedStart585 = getCharIndex();
            	    mESC(); 
            	    escaped = new CommonToken(input, Token.INVALID_TOKEN_TYPE, Token.DEFAULT_CHANNEL, escapedStart585, getCharIndex()-1);
            	    lBuf.append(getText());

            	    }
            	    break;
            	case 2 :
            	    // MLN.g:67:6: normal=~ ( '\"' | '\\\\' | '\\n' | '\\r' )
            	    {
            	    normal= input.LA(1);
            	    if ( (input.LA(1)>='\u0000' && input.LA(1)<='\t')||(input.LA(1)>='\u000B' && input.LA(1)<='\f')||(input.LA(1)>='\u000E' && input.LA(1)<='!')||(input.LA(1)>='#' && input.LA(1)<='[')||(input.LA(1)>=']' && input.LA(1)<='\uFFFF') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}

            	    lBuf.appendCodePoint(normal);

            	    }
            	    break;

            	default :
            	    break loop6;
                }
            } while (true);

            match('\"'); 
            setText(lBuf.toString());

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "STRING"

    // $ANTLR start "ESC"
    public final void mESC() throws RecognitionException {
        try {
            CommonToken i=null;
            CommonToken j=null;
            CommonToken k=null;
            CommonToken l=null;

            // MLN.g:74:5: ( '\\\\' ( 'n' | 'r' | 't' | 'b' | 'f' | '\"' | '\\'' | '/' | '\\\\' | 'u' i= HEXDIGIT j= HEXDIGIT k= HEXDIGIT l= HEXDIGIT | ~ ( 'u' | 'r' | 'n' | 't' | 'b' | 'f' | '\"' | '\\'' | '/' | '\\\\' ) ) )
            // MLN.g:74:9: '\\\\' ( 'n' | 'r' | 't' | 'b' | 'f' | '\"' | '\\'' | '/' | '\\\\' | 'u' i= HEXDIGIT j= HEXDIGIT k= HEXDIGIT l= HEXDIGIT | ~ ( 'u' | 'r' | 'n' | 't' | 'b' | 'f' | '\"' | '\\'' | '/' | '\\\\' ) )
            {
            match('\\'); 
            // MLN.g:75:9: ( 'n' | 'r' | 't' | 'b' | 'f' | '\"' | '\\'' | '/' | '\\\\' | 'u' i= HEXDIGIT j= HEXDIGIT k= HEXDIGIT l= HEXDIGIT | ~ ( 'u' | 'r' | 'n' | 't' | 'b' | 'f' | '\"' | '\\'' | '/' | '\\\\' ) )
            int alt7=11;
            int LA7_0 = input.LA(1);

            if ( (LA7_0=='n') ) {
                alt7=1;
            }
            else if ( (LA7_0=='r') ) {
                alt7=2;
            }
            else if ( (LA7_0=='t') ) {
                alt7=3;
            }
            else if ( (LA7_0=='b') ) {
                alt7=4;
            }
            else if ( (LA7_0=='f') ) {
                alt7=5;
            }
            else if ( (LA7_0=='\"') ) {
                alt7=6;
            }
            else if ( (LA7_0=='\'') ) {
                alt7=7;
            }
            else if ( (LA7_0=='/') ) {
                alt7=8;
            }
            else if ( (LA7_0=='\\') ) {
                alt7=9;
            }
            else if ( (LA7_0=='u') ) {
                alt7=10;
            }
            else if ( ((LA7_0>='\u0000' && LA7_0<='!')||(LA7_0>='#' && LA7_0<='&')||(LA7_0>='(' && LA7_0<='.')||(LA7_0>='0' && LA7_0<='[')||(LA7_0>=']' && LA7_0<='a')||(LA7_0>='c' && LA7_0<='e')||(LA7_0>='g' && LA7_0<='m')||(LA7_0>='o' && LA7_0<='q')||LA7_0=='s'||(LA7_0>='v' && LA7_0<='\uFFFF')) ) {
                alt7=11;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 7, 0, input);

                throw nvae;
            }
            switch (alt7) {
                case 1 :
                    // MLN.g:75:17: 'n'
                    {
                    match('n'); 
                    setText("\n");

                    }
                    break;
                case 2 :
                    // MLN.g:76:17: 'r'
                    {
                    match('r'); 
                    setText("\r");

                    }
                    break;
                case 3 :
                    // MLN.g:77:17: 't'
                    {
                    match('t'); 
                    setText("\t");

                    }
                    break;
                case 4 :
                    // MLN.g:78:17: 'b'
                    {
                    match('b'); 
                    setText("\b");

                    }
                    break;
                case 5 :
                    // MLN.g:79:17: 'f'
                    {
                    match('f'); 
                    setText("\f");

                    }
                    break;
                case 6 :
                    // MLN.g:80:17: '\"'
                    {
                    match('\"'); 
                    setText("\"");

                    }
                    break;
                case 7 :
                    // MLN.g:81:17: '\\''
                    {
                    match('\''); 
                    setText("\'");

                    }
                    break;
                case 8 :
                    // MLN.g:82:17: '/'
                    {
                    match('/'); 
                    setText("/");

                    }
                    break;
                case 9 :
                    // MLN.g:83:17: '\\\\'
                    {
                    match('\\'); 
                    setText("\\");

                    }
                    break;
                case 10 :
                    // MLN.g:84:11: 'u' i= HEXDIGIT j= HEXDIGIT k= HEXDIGIT l= HEXDIGIT
                    {
                    match('u'); 
                    int iStart871 = getCharIndex();
                    mHEXDIGIT(); 
                    i = new CommonToken(input, Token.INVALID_TOKEN_TYPE, Token.DEFAULT_CHANNEL, iStart871, getCharIndex()-1);
                    int jStart875 = getCharIndex();
                    mHEXDIGIT(); 
                    j = new CommonToken(input, Token.INVALID_TOKEN_TYPE, Token.DEFAULT_CHANNEL, jStart875, getCharIndex()-1);
                    int kStart879 = getCharIndex();
                    mHEXDIGIT(); 
                    k = new CommonToken(input, Token.INVALID_TOKEN_TYPE, Token.DEFAULT_CHANNEL, kStart879, getCharIndex()-1);
                    int lStart883 = getCharIndex();
                    mHEXDIGIT(); 
                    l = new CommonToken(input, Token.INVALID_TOKEN_TYPE, Token.DEFAULT_CHANNEL, lStart883, getCharIndex()-1);

                               String num = i.getText() + j.getText() + k.getText() + l.getText();
                               char[] realc = new char[1];
                               realc[0] = (char) Integer.valueOf(num, 16).intValue();
                               setText(new String(realc));
                             

                    }
                    break;
                case 11 :
                    // MLN.g:91:11: ~ ( 'u' | 'r' | 'n' | 't' | 'b' | 'f' | '\"' | '\\'' | '/' | '\\\\' )
                    {
                    if ( (input.LA(1)>='\u0000' && input.LA(1)<='!')||(input.LA(1)>='#' && input.LA(1)<='&')||(input.LA(1)>='(' && input.LA(1)<='.')||(input.LA(1)>='0' && input.LA(1)<='[')||(input.LA(1)>=']' && input.LA(1)<='a')||(input.LA(1)>='c' && input.LA(1)<='e')||(input.LA(1)>='g' && input.LA(1)<='m')||(input.LA(1)>='o' && input.LA(1)<='q')||input.LA(1)=='s'||(input.LA(1)>='v' && input.LA(1)<='\uFFFF') ) {
                        input.consume();

                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}


                    }
                    break;

            }


            }

        }
        finally {
        }
    }
    // $ANTLR end "ESC"

    // $ANTLR start "HEXDIGIT"
    public final void mHEXDIGIT() throws RecognitionException {
        try {
            // MLN.g:97:3: ( '0' .. '9' | 'A' .. 'F' | 'a' .. 'f' )
            // MLN.g:
            {
            if ( (input.LA(1)>='0' && input.LA(1)<='9')||(input.LA(1)>='A' && input.LA(1)<='F')||(input.LA(1)>='a' && input.LA(1)<='f') ) {
                input.consume();

            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}


            }

        }
        finally {
        }
    }
    // $ANTLR end "HEXDIGIT"

    // $ANTLR start "NUMBER"
    public final void mNUMBER() throws RecognitionException {
        try {
            int _type = NUMBER;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:100:8: ( INTEGER | FLOAT )
            int alt8=2;
            alt8 = dfa8.predict(input);
            switch (alt8) {
                case 1 :
                    // MLN.g:100:11: INTEGER
                    {
                    mINTEGER(); 

                    }
                    break;
                case 2 :
                    // MLN.g:100:21: FLOAT
                    {
                    mFLOAT(); 

                    }
                    break;

            }
            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "NUMBER"

    // $ANTLR start "INTEGER"
    public final void mINTEGER() throws RecognitionException {
        try {
            // MLN.g:102:9: ( '0' | ( '+' | '-' )? '1' .. '9' ( '0' .. '9' )* )
            int alt11=2;
            int LA11_0 = input.LA(1);

            if ( (LA11_0=='0') ) {
                alt11=1;
            }
            else if ( (LA11_0=='+'||LA11_0=='-'||(LA11_0>='1' && LA11_0<='9')) ) {
                alt11=2;
            }
            else {
                NoViableAltException nvae =
                    new NoViableAltException("", 11, 0, input);

                throw nvae;
            }
            switch (alt11) {
                case 1 :
                    // MLN.g:102:11: '0'
                    {
                    match('0'); 

                    }
                    break;
                case 2 :
                    // MLN.g:102:17: ( '+' | '-' )? '1' .. '9' ( '0' .. '9' )*
                    {
                    // MLN.g:102:17: ( '+' | '-' )?
                    int alt9=2;
                    int LA9_0 = input.LA(1);

                    if ( (LA9_0=='+'||LA9_0=='-') ) {
                        alt9=1;
                    }
                    switch (alt9) {
                        case 1 :
                            // MLN.g:
                            {
                            if ( input.LA(1)=='+'||input.LA(1)=='-' ) {
                                input.consume();

                            }
                            else {
                                MismatchedSetException mse = new MismatchedSetException(null,input);
                                recover(mse);
                                throw mse;}


                            }
                            break;

                    }

                    matchRange('1','9'); 
                    // MLN.g:102:37: ( '0' .. '9' )*
                    loop10:
                    do {
                        int alt10=2;
                        int LA10_0 = input.LA(1);

                        if ( ((LA10_0>='0' && LA10_0<='9')) ) {
                            alt10=1;
                        }


                        switch (alt10) {
                    	case 1 :
                    	    // MLN.g:102:37: '0' .. '9'
                    	    {
                    	    matchRange('0','9'); 

                    	    }
                    	    break;

                    	default :
                    	    break loop10;
                        }
                    } while (true);


                    }
                    break;

            }
        }
        finally {
        }
    }
    // $ANTLR end "INTEGER"

    // $ANTLR start "EXPONENT"
    public final void mEXPONENT() throws RecognitionException {
        try {
            // MLN.g:104:10: ( ( 'e' | 'E' ) ( '+' | '-' )? ( '0' .. '9' )+ )
            // MLN.g:104:12: ( 'e' | 'E' ) ( '+' | '-' )? ( '0' .. '9' )+
            {
            if ( input.LA(1)=='E'||input.LA(1)=='e' ) {
                input.consume();

            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            // MLN.g:104:22: ( '+' | '-' )?
            int alt12=2;
            int LA12_0 = input.LA(1);

            if ( (LA12_0=='+'||LA12_0=='-') ) {
                alt12=1;
            }
            switch (alt12) {
                case 1 :
                    // MLN.g:
                    {
                    if ( input.LA(1)=='+'||input.LA(1)=='-' ) {
                        input.consume();

                    }
                    else {
                        MismatchedSetException mse = new MismatchedSetException(null,input);
                        recover(mse);
                        throw mse;}


                    }
                    break;

            }

            // MLN.g:104:33: ( '0' .. '9' )+
            int cnt13=0;
            loop13:
            do {
                int alt13=2;
                int LA13_0 = input.LA(1);

                if ( ((LA13_0>='0' && LA13_0<='9')) ) {
                    alt13=1;
                }


                switch (alt13) {
            	case 1 :
            	    // MLN.g:104:34: '0' .. '9'
            	    {
            	    matchRange('0','9'); 

            	    }
            	    break;

            	default :
            	    if ( cnt13 >= 1 ) break loop13;
                        EarlyExitException eee =
                            new EarlyExitException(13, input);
                        throw eee;
                }
                cnt13++;
            } while (true);


            }

        }
        finally {
        }
    }
    // $ANTLR end "EXPONENT"

    // $ANTLR start "FLOAT"
    public final void mFLOAT() throws RecognitionException {
        try {
            // MLN.g:107:5: ( ( '+' | '-' )? ( '0' .. '9' )+ '.' ( '0' .. '9' )* ( EXPONENT )? | '.' ( '0' .. '9' )+ ( EXPONENT )? | ( '0' .. '9' )+ EXPONENT )
            int alt21=3;
            alt21 = dfa21.predict(input);
            switch (alt21) {
                case 1 :
                    // MLN.g:107:9: ( '+' | '-' )? ( '0' .. '9' )+ '.' ( '0' .. '9' )* ( EXPONENT )?
                    {
                    // MLN.g:107:9: ( '+' | '-' )?
                    int alt14=2;
                    int LA14_0 = input.LA(1);

                    if ( (LA14_0=='+'||LA14_0=='-') ) {
                        alt14=1;
                    }
                    switch (alt14) {
                        case 1 :
                            // MLN.g:
                            {
                            if ( input.LA(1)=='+'||input.LA(1)=='-' ) {
                                input.consume();

                            }
                            else {
                                MismatchedSetException mse = new MismatchedSetException(null,input);
                                recover(mse);
                                throw mse;}


                            }
                            break;

                    }

                    // MLN.g:107:20: ( '0' .. '9' )+
                    int cnt15=0;
                    loop15:
                    do {
                        int alt15=2;
                        int LA15_0 = input.LA(1);

                        if ( ((LA15_0>='0' && LA15_0<='9')) ) {
                            alt15=1;
                        }


                        switch (alt15) {
                    	case 1 :
                    	    // MLN.g:107:21: '0' .. '9'
                    	    {
                    	    matchRange('0','9'); 

                    	    }
                    	    break;

                    	default :
                    	    if ( cnt15 >= 1 ) break loop15;
                                EarlyExitException eee =
                                    new EarlyExitException(15, input);
                                throw eee;
                        }
                        cnt15++;
                    } while (true);

                    match('.'); 
                    // MLN.g:107:36: ( '0' .. '9' )*
                    loop16:
                    do {
                        int alt16=2;
                        int LA16_0 = input.LA(1);

                        if ( ((LA16_0>='0' && LA16_0<='9')) ) {
                            alt16=1;
                        }


                        switch (alt16) {
                    	case 1 :
                    	    // MLN.g:107:37: '0' .. '9'
                    	    {
                    	    matchRange('0','9'); 

                    	    }
                    	    break;

                    	default :
                    	    break loop16;
                        }
                    } while (true);

                    // MLN.g:107:48: ( EXPONENT )?
                    int alt17=2;
                    int LA17_0 = input.LA(1);

                    if ( (LA17_0=='E'||LA17_0=='e') ) {
                        alt17=1;
                    }
                    switch (alt17) {
                        case 1 :
                            // MLN.g:107:48: EXPONENT
                            {
                            mEXPONENT(); 

                            }
                            break;

                    }


                    }
                    break;
                case 2 :
                    // MLN.g:108:9: '.' ( '0' .. '9' )+ ( EXPONENT )?
                    {
                    match('.'); 
                    // MLN.g:108:13: ( '0' .. '9' )+
                    int cnt18=0;
                    loop18:
                    do {
                        int alt18=2;
                        int LA18_0 = input.LA(1);

                        if ( ((LA18_0>='0' && LA18_0<='9')) ) {
                            alt18=1;
                        }


                        switch (alt18) {
                    	case 1 :
                    	    // MLN.g:108:14: '0' .. '9'
                    	    {
                    	    matchRange('0','9'); 

                    	    }
                    	    break;

                    	default :
                    	    if ( cnt18 >= 1 ) break loop18;
                                EarlyExitException eee =
                                    new EarlyExitException(18, input);
                                throw eee;
                        }
                        cnt18++;
                    } while (true);

                    // MLN.g:108:25: ( EXPONENT )?
                    int alt19=2;
                    int LA19_0 = input.LA(1);

                    if ( (LA19_0=='E'||LA19_0=='e') ) {
                        alt19=1;
                    }
                    switch (alt19) {
                        case 1 :
                            // MLN.g:108:25: EXPONENT
                            {
                            mEXPONENT(); 

                            }
                            break;

                    }


                    }
                    break;
                case 3 :
                    // MLN.g:109:9: ( '0' .. '9' )+ EXPONENT
                    {
                    // MLN.g:109:9: ( '0' .. '9' )+
                    int cnt20=0;
                    loop20:
                    do {
                        int alt20=2;
                        int LA20_0 = input.LA(1);

                        if ( ((LA20_0>='0' && LA20_0<='9')) ) {
                            alt20=1;
                        }


                        switch (alt20) {
                    	case 1 :
                    	    // MLN.g:109:10: '0' .. '9'
                    	    {
                    	    matchRange('0','9'); 

                    	    }
                    	    break;

                    	default :
                    	    if ( cnt20 >= 1 ) break loop20;
                                EarlyExitException eee =
                                    new EarlyExitException(20, input);
                                throw eee;
                        }
                        cnt20++;
                    } while (true);

                    mEXPONENT(); 

                    }
                    break;

            }
        }
        finally {
        }
    }
    // $ANTLR end "FLOAT"

    // $ANTLR start "ID"
    public final void mID() throws RecognitionException {
        try {
            int _type = ID;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // MLN.g:113:3: ( ( 'a' .. 'z' | 'A' .. 'Z' ) ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' | '-' )* )
            // MLN.g:113:7: ( 'a' .. 'z' | 'A' .. 'Z' ) ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' | '-' )*
            {
            if ( (input.LA(1)>='A' && input.LA(1)<='Z')||(input.LA(1)>='a' && input.LA(1)<='z') ) {
                input.consume();

            }
            else {
                MismatchedSetException mse = new MismatchedSetException(null,input);
                recover(mse);
                throw mse;}

            // MLN.g:113:26: ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' | '-' )*
            loop22:
            do {
                int alt22=2;
                int LA22_0 = input.LA(1);

                if ( (LA22_0=='-'||(LA22_0>='0' && LA22_0<='9')||(LA22_0>='A' && LA22_0<='Z')||LA22_0=='_'||(LA22_0>='a' && LA22_0<='z')) ) {
                    alt22=1;
                }


                switch (alt22) {
            	case 1 :
            	    // MLN.g:
            	    {
            	    if ( input.LA(1)=='-'||(input.LA(1)>='0' && input.LA(1)<='9')||(input.LA(1)>='A' && input.LA(1)<='Z')||input.LA(1)=='_'||(input.LA(1)>='a' && input.LA(1)<='z') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    break loop22;
                }
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "ID"

    public void mTokens() throws RecognitionException {
        // MLN.g:1:8: ( T__21 | T__22 | T__23 | T__24 | T__25 | T__26 | T__27 | T__28 | T__29 | T__30 | T__31 | T__32 | T__33 | T__34 | T__35 | T__36 | T__37 | T__38 | T__39 | T__40 | T__41 | T__42 | T__43 | T__44 | T__45 | T__46 | T__47 | T__48 | T__49 | T__50 | T__51 | T__52 | T__53 | T__54 | T__55 | T__56 | T__57 | T__58 | T__59 | T__60 | T__61 | WS | COMMENT | NOT | PLUS | MINUS | ASTERISK | PERIOD | EXIST | IMPLIES | STRING | NUMBER | ID )
        int alt23=53;
        alt23 = dfa23.predict(input);
        switch (alt23) {
            case 1 :
                // MLN.g:1:10: T__21
                {
                mT__21(); 

                }
                break;
            case 2 :
                // MLN.g:1:16: T__22
                {
                mT__22(); 

                }
                break;
            case 3 :
                // MLN.g:1:22: T__23
                {
                mT__23(); 

                }
                break;
            case 4 :
                // MLN.g:1:28: T__24
                {
                mT__24(); 

                }
                break;
            case 5 :
                // MLN.g:1:34: T__25
                {
                mT__25(); 

                }
                break;
            case 6 :
                // MLN.g:1:40: T__26
                {
                mT__26(); 

                }
                break;
            case 7 :
                // MLN.g:1:46: T__27
                {
                mT__27(); 

                }
                break;
            case 8 :
                // MLN.g:1:52: T__28
                {
                mT__28(); 

                }
                break;
            case 9 :
                // MLN.g:1:58: T__29
                {
                mT__29(); 

                }
                break;
            case 10 :
                // MLN.g:1:64: T__30
                {
                mT__30(); 

                }
                break;
            case 11 :
                // MLN.g:1:70: T__31
                {
                mT__31(); 

                }
                break;
            case 12 :
                // MLN.g:1:76: T__32
                {
                mT__32(); 

                }
                break;
            case 13 :
                // MLN.g:1:82: T__33
                {
                mT__33(); 

                }
                break;
            case 14 :
                // MLN.g:1:88: T__34
                {
                mT__34(); 

                }
                break;
            case 15 :
                // MLN.g:1:94: T__35
                {
                mT__35(); 

                }
                break;
            case 16 :
                // MLN.g:1:100: T__36
                {
                mT__36(); 

                }
                break;
            case 17 :
                // MLN.g:1:106: T__37
                {
                mT__37(); 

                }
                break;
            case 18 :
                // MLN.g:1:112: T__38
                {
                mT__38(); 

                }
                break;
            case 19 :
                // MLN.g:1:118: T__39
                {
                mT__39(); 

                }
                break;
            case 20 :
                // MLN.g:1:124: T__40
                {
                mT__40(); 

                }
                break;
            case 21 :
                // MLN.g:1:130: T__41
                {
                mT__41(); 

                }
                break;
            case 22 :
                // MLN.g:1:136: T__42
                {
                mT__42(); 

                }
                break;
            case 23 :
                // MLN.g:1:142: T__43
                {
                mT__43(); 

                }
                break;
            case 24 :
                // MLN.g:1:148: T__44
                {
                mT__44(); 

                }
                break;
            case 25 :
                // MLN.g:1:154: T__45
                {
                mT__45(); 

                }
                break;
            case 26 :
                // MLN.g:1:160: T__46
                {
                mT__46(); 

                }
                break;
            case 27 :
                // MLN.g:1:166: T__47
                {
                mT__47(); 

                }
                break;
            case 28 :
                // MLN.g:1:172: T__48
                {
                mT__48(); 

                }
                break;
            case 29 :
                // MLN.g:1:178: T__49
                {
                mT__49(); 

                }
                break;
            case 30 :
                // MLN.g:1:184: T__50
                {
                mT__50(); 

                }
                break;
            case 31 :
                // MLN.g:1:190: T__51
                {
                mT__51(); 

                }
                break;
            case 32 :
                // MLN.g:1:196: T__52
                {
                mT__52(); 

                }
                break;
            case 33 :
                // MLN.g:1:202: T__53
                {
                mT__53(); 

                }
                break;
            case 34 :
                // MLN.g:1:208: T__54
                {
                mT__54(); 

                }
                break;
            case 35 :
                // MLN.g:1:214: T__55
                {
                mT__55(); 

                }
                break;
            case 36 :
                // MLN.g:1:220: T__56
                {
                mT__56(); 

                }
                break;
            case 37 :
                // MLN.g:1:226: T__57
                {
                mT__57(); 

                }
                break;
            case 38 :
                // MLN.g:1:232: T__58
                {
                mT__58(); 

                }
                break;
            case 39 :
                // MLN.g:1:238: T__59
                {
                mT__59(); 

                }
                break;
            case 40 :
                // MLN.g:1:244: T__60
                {
                mT__60(); 

                }
                break;
            case 41 :
                // MLN.g:1:250: T__61
                {
                mT__61(); 

                }
                break;
            case 42 :
                // MLN.g:1:256: WS
                {
                mWS(); 

                }
                break;
            case 43 :
                // MLN.g:1:259: COMMENT
                {
                mCOMMENT(); 

                }
                break;
            case 44 :
                // MLN.g:1:267: NOT
                {
                mNOT(); 

                }
                break;
            case 45 :
                // MLN.g:1:271: PLUS
                {
                mPLUS(); 

                }
                break;
            case 46 :
                // MLN.g:1:276: MINUS
                {
                mMINUS(); 

                }
                break;
            case 47 :
                // MLN.g:1:282: ASTERISK
                {
                mASTERISK(); 

                }
                break;
            case 48 :
                // MLN.g:1:291: PERIOD
                {
                mPERIOD(); 

                }
                break;
            case 49 :
                // MLN.g:1:298: EXIST
                {
                mEXIST(); 

                }
                break;
            case 50 :
                // MLN.g:1:304: IMPLIES
                {
                mIMPLIES(); 

                }
                break;
            case 51 :
                // MLN.g:1:312: STRING
                {
                mSTRING(); 

                }
                break;
            case 52 :
                // MLN.g:1:319: NUMBER
                {
                mNUMBER(); 

                }
                break;
            case 53 :
                // MLN.g:1:326: ID
                {
                mID(); 

                }
                break;

        }

    }


    protected DFA8 dfa8 = new DFA8(this);
    protected DFA21 dfa21 = new DFA21(this);
    protected DFA23 dfa23 = new DFA23(this);
    static final String DFA8_eotS =
        "\1\uffff\1\5\1\uffff\1\5\2\uffff\3\5";
    static final String DFA8_eofS =
        "\11\uffff";
    static final String DFA8_minS =
        "\1\53\1\56\1\60\1\56\2\uffff\3\56";
    static final String DFA8_maxS =
        "\1\71\1\145\1\71\1\145\2\uffff\1\71\1\145\1\71";
    static final String DFA8_acceptS =
        "\4\uffff\1\2\1\1\3\uffff";
    static final String DFA8_specialS =
        "\11\uffff}>";
    static final String[] DFA8_transitionS = {
            "\1\2\1\uffff\1\2\1\4\1\uffff\1\1\11\3",
            "\1\4\1\uffff\12\4\13\uffff\1\4\37\uffff\1\4",
            "\1\4\11\6",
            "\1\4\1\uffff\12\7\13\uffff\1\4\37\uffff\1\4",
            "",
            "",
            "\1\4\1\uffff\12\10",
            "\1\4\1\uffff\12\7\13\uffff\1\4\37\uffff\1\4",
            "\1\4\1\uffff\12\10"
    };

    static final short[] DFA8_eot = DFA.unpackEncodedString(DFA8_eotS);
    static final short[] DFA8_eof = DFA.unpackEncodedString(DFA8_eofS);
    static final char[] DFA8_min = DFA.unpackEncodedStringToUnsignedChars(DFA8_minS);
    static final char[] DFA8_max = DFA.unpackEncodedStringToUnsignedChars(DFA8_maxS);
    static final short[] DFA8_accept = DFA.unpackEncodedString(DFA8_acceptS);
    static final short[] DFA8_special = DFA.unpackEncodedString(DFA8_specialS);
    static final short[][] DFA8_transition;

    static {
        int numStates = DFA8_transitionS.length;
        DFA8_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA8_transition[i] = DFA.unpackEncodedString(DFA8_transitionS[i]);
        }
    }

    class DFA8 extends DFA {

        public DFA8(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 8;
            this.eot = DFA8_eot;
            this.eof = DFA8_eof;
            this.min = DFA8_min;
            this.max = DFA8_max;
            this.accept = DFA8_accept;
            this.special = DFA8_special;
            this.transition = DFA8_transition;
        }
        public String getDescription() {
            return "100:1: NUMBER : ( INTEGER | FLOAT );";
        }
    }
    static final String DFA21_eotS =
        "\5\uffff";
    static final String DFA21_eofS =
        "\5\uffff";
    static final String DFA21_minS =
        "\1\53\1\uffff\1\56\2\uffff";
    static final String DFA21_maxS =
        "\1\71\1\uffff\1\145\2\uffff";
    static final String DFA21_acceptS =
        "\1\uffff\1\1\1\uffff\1\2\1\3";
    static final String DFA21_specialS =
        "\5\uffff}>";
    static final String[] DFA21_transitionS = {
            "\1\1\1\uffff\1\1\1\3\1\uffff\12\2",
            "",
            "\1\1\1\uffff\12\2\13\uffff\1\4\37\uffff\1\4",
            "",
            ""
    };

    static final short[] DFA21_eot = DFA.unpackEncodedString(DFA21_eotS);
    static final short[] DFA21_eof = DFA.unpackEncodedString(DFA21_eofS);
    static final char[] DFA21_min = DFA.unpackEncodedStringToUnsignedChars(DFA21_minS);
    static final char[] DFA21_max = DFA.unpackEncodedStringToUnsignedChars(DFA21_maxS);
    static final short[] DFA21_accept = DFA.unpackEncodedString(DFA21_acceptS);
    static final short[] DFA21_special = DFA.unpackEncodedString(DFA21_specialS);
    static final short[][] DFA21_transition;

    static {
        int numStates = DFA21_transitionS.length;
        DFA21_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA21_transition[i] = DFA.unpackEncodedString(DFA21_transitionS[i]);
        }
    }

    class DFA21 extends DFA {

        public DFA21(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 21;
            this.eot = DFA21_eot;
            this.eof = DFA21_eof;
            this.min = DFA21_min;
            this.max = DFA21_max;
            this.accept = DFA21_accept;
            this.special = DFA21_special;
            this.transition = DFA21_transition;
        }
        public String getDescription() {
            return "105:1: fragment FLOAT : ( ( '+' | '-' )? ( '0' .. '9' )+ '.' ( '0' .. '9' )* ( EXPONENT )? | '.' ( '0' .. '9' )+ ( EXPONENT )? | ( '0' .. '9' )+ EXPONENT );";
        }
    }
    static final String DFA23_eotS =
        "\1\uffff\1\46\4\uffff\1\44\1\53\1\uffff\1\55\1\60\3\uffff\1\44\1"+
        "\63\1\64\1\66\1\44\1\71\2\44\1\77\1\102\1\104\1\uffff\1\106\3\uffff"+
        "\1\107\1\110\2\44\5\uffff\1\44\1\115\10\uffff\1\44\5\uffff\1\120"+
        "\2\uffff\2\44\15\uffff\4\44\2\uffff\1\44\1\uffff\1\131\1\132\4\44"+
        "\1\uffff\1\44\2\uffff\4\44\1\uffff\1\44\3\147\1\44\1\uffff\1\44"+
        "\1\uffff\1\44\2\uffff\4\44\1\161\1\44\2\uffff";
    static final String DFA23_eofS =
        "\163\uffff";
    static final String DFA23_minS =
        "\1\11\1\52\4\uffff\1\104\1\55\1\uffff\1\60\1\53\3\uffff\1\162\1"+
        "\76\1\55\1\174\1\122\1\46\1\116\1\117\1\74\2\75\1\uffff\1\52\3\uffff"+
        "\2\60\1\130\1\170\5\uffff\1\116\1\55\5\uffff\1\141\2\uffff\1\151"+
        "\5\uffff\1\55\2\uffff\1\104\1\124\15\uffff\1\111\2\151\1\103\1\uffff"+
        "\1\142\1\157\1\uffff\2\55\1\123\2\163\1\124\1\145\1\162\2\uffff"+
        "\1\124\2\164\1\111\1\154\1\120\3\55\1\117\1\53\1\162\1\uffff\1\116"+
        "\2\uffff\1\157\1\101\1\142\1\114\1\55\1\40\2\uffff";
    static final String DFA23_maxS =
        "\1\176\1\52\4\uffff\1\125\1\75\1\uffff\1\76\1\114\3\uffff\1\162"+
        "\1\76\1\172\1\174\1\122\1\46\1\116\1\117\2\76\1\75\1\uffff\1\57"+
        "\3\uffff\2\71\2\170\5\uffff\1\116\1\172\5\uffff\1\141\2\uffff\1"+
        "\151\5\uffff\1\172\2\uffff\1\104\1\124\15\uffff\1\111\2\151\1\103"+
        "\1\uffff\1\142\1\157\1\uffff\2\172\1\123\2\163\1\124\1\145\1\162"+
        "\2\uffff\1\124\2\164\1\111\1\154\1\120\3\172\1\117\1\135\1\162\1"+
        "\uffff\1\116\2\uffff\1\157\1\101\1\142\1\114\1\172\1\40\2\uffff";
    static final String DFA23_acceptS =
        "\2\uffff\1\2\1\3\1\4\1\5\2\uffff\1\11\2\uffff\1\16\1\17\1\22\13"+
        "\uffff\1\42\1\uffff\1\46\1\51\1\52\4\uffff\1\63\1\64\1\65\1\1\1"+
        "\57\2\uffff\1\20\1\25\1\10\1\12\1\56\1\uffff\1\14\1\21\1\uffff\1"+
        "\62\1\24\1\26\1\27\1\45\1\uffff\1\31\1\44\2\uffff\1\34\1\36\1\47"+
        "\1\35\1\40\1\50\1\37\1\41\1\54\1\53\1\43\1\55\1\60\4\uffff\1\7\2"+
        "\uffff\1\30\10\uffff\1\32\1\33\14\uffff\1\61\1\uffff\1\13\1\15\6"+
        "\uffff\1\23\1\6";
    static final String DFA23_specialS =
        "\163\uffff}>";
    static final String[] DFA23_transitionS = {
            "\2\35\2\uffff\1\35\22\uffff\1\35\1\30\1\42\1\13\1\14\1\31\1"+
            "\23\1\uffff\1\3\1\5\1\1\1\36\1\4\1\11\1\37\1\32\12\43\1\7\1"+
            "\10\1\26\1\17\1\27\1\uffff\1\2\1\24\3\44\1\40\1\6\7\44\1\25"+
            "\1\22\13\44\1\12\1\uffff\1\15\1\33\2\uffff\4\44\1\41\12\44\1"+
            "\16\5\44\1\20\4\44\1\uffff\1\21\1\uffff\1\34",
            "\1\45",
            "",
            "",
            "",
            "",
            "\1\50\20\uffff\1\47",
            "\1\51\17\uffff\1\52",
            "",
            "\12\43\4\uffff\1\54",
            "\1\57\40\uffff\1\56",
            "",
            "",
            "",
            "\1\61",
            "\1\62",
            "\1\44\2\uffff\12\44\7\uffff\32\44\4\uffff\1\44\1\uffff\32\44",
            "\1\65",
            "\1\67",
            "\1\70",
            "\1\72",
            "\1\73",
            "\1\76\1\75\1\74",
            "\1\100\1\101",
            "\1\103",
            "",
            "\1\105\4\uffff\1\105",
            "",
            "",
            "",
            "\12\43",
            "\12\43",
            "\1\111\37\uffff\1\112",
            "\1\113",
            "",
            "",
            "",
            "",
            "",
            "\1\114",
            "\1\44\2\uffff\12\44\7\uffff\32\44\4\uffff\1\44\1\uffff\32\44",
            "",
            "",
            "",
            "",
            "",
            "\1\116",
            "",
            "",
            "\1\117",
            "",
            "",
            "",
            "",
            "",
            "\1\44\2\uffff\12\44\7\uffff\32\44\4\uffff\1\44\1\uffff\32\44",
            "",
            "",
            "\1\121",
            "\1\122",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "",
            "\1\123",
            "\1\124",
            "\1\125",
            "\1\126",
            "",
            "\1\127",
            "\1\130",
            "",
            "\1\44\2\uffff\12\44\7\uffff\32\44\4\uffff\1\44\1\uffff\32\44",
            "\1\44\2\uffff\12\44\7\uffff\32\44\4\uffff\1\44\1\uffff\32\44",
            "\1\133",
            "\1\134",
            "\1\135",
            "\1\136",
            "\1\137",
            "\1\140",
            "",
            "",
            "\1\141",
            "\1\142",
            "\1\143",
            "\1\144",
            "\1\145",
            "\1\146",
            "\1\44\2\uffff\12\44\7\uffff\32\44\4\uffff\1\44\1\uffff\32\44",
            "\1\44\2\uffff\12\44\7\uffff\32\44\4\uffff\1\44\1\uffff\32\44",
            "\1\44\2\uffff\12\44\7\uffff\32\44\4\uffff\1\44\1\uffff\32\44",
            "\1\150",
            "\1\152\61\uffff\1\151",
            "\1\153",
            "",
            "\1\154",
            "",
            "",
            "\1\155",
            "\1\156",
            "\1\157",
            "\1\160",
            "\1\44\2\uffff\12\44\7\uffff\32\44\4\uffff\1\44\1\uffff\32\44",
            "\1\162",
            "",
            ""
    };

    static final short[] DFA23_eot = DFA.unpackEncodedString(DFA23_eotS);
    static final short[] DFA23_eof = DFA.unpackEncodedString(DFA23_eofS);
    static final char[] DFA23_min = DFA.unpackEncodedStringToUnsignedChars(DFA23_minS);
    static final char[] DFA23_max = DFA.unpackEncodedStringToUnsignedChars(DFA23_maxS);
    static final short[] DFA23_accept = DFA.unpackEncodedString(DFA23_acceptS);
    static final short[] DFA23_special = DFA.unpackEncodedString(DFA23_specialS);
    static final short[][] DFA23_transition;

    static {
        int numStates = DFA23_transitionS.length;
        DFA23_transition = new short[numStates][];
        for (int i=0; i<numStates; i++) {
            DFA23_transition[i] = DFA.unpackEncodedString(DFA23_transitionS[i]);
        }
    }

    class DFA23 extends DFA {

        public DFA23(BaseRecognizer recognizer) {
            this.recognizer = recognizer;
            this.decisionNumber = 23;
            this.eot = DFA23_eot;
            this.eof = DFA23_eof;
            this.min = DFA23_min;
            this.max = DFA23_max;
            this.accept = DFA23_accept;
            this.special = DFA23_special;
            this.transition = DFA23_transition;
        }
        public String getDescription() {
            return "1:1: Tokens : ( T__21 | T__22 | T__23 | T__24 | T__25 | T__26 | T__27 | T__28 | T__29 | T__30 | T__31 | T__32 | T__33 | T__34 | T__35 | T__36 | T__37 | T__38 | T__39 | T__40 | T__41 | T__42 | T__43 | T__44 | T__45 | T__46 | T__47 | T__48 | T__49 | T__50 | T__51 | T__52 | T__53 | T__54 | T__55 | T__56 | T__57 | T__58 | T__59 | T__60 | T__61 | WS | COMMENT | NOT | PLUS | MINUS | ASTERISK | PERIOD | EXIST | IMPLIES | STRING | NUMBER | ID );";
        }
    }
 

}