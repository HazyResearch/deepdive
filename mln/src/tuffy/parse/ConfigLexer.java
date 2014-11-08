// $ANTLR 3.2 Sep 23, 2009 12:02:23 /scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g 2012-02-17 15:34:25

package tuffy.parse;


import org.antlr.runtime.*;
import java.util.Stack;
import java.util.List;
import java.util.ArrayList;

public class ConfigLexer extends Lexer {
    public static final int WS=4;
    public static final int SPAN=6;
    public static final int COMMENT=5;
    public static final int EOF=-1;
    public static final int T__7=7;

    // delegates
    // delegators

    public ConfigLexer() {;} 
    public ConfigLexer(CharStream input) {
        this(input, new RecognizerSharedState());
    }
    public ConfigLexer(CharStream input, RecognizerSharedState state) {
        super(input,state);

    }
    public String getGrammarFileName() { return "/scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g"; }

    // $ANTLR start "T__7"
    public final void mT__7() throws RecognitionException {
        try {
            int _type = T__7;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g:11:6: ( '=' )
            // /scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g:11:8: '='
            {
            match('='); 

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "T__7"

    // $ANTLR start "WS"
    public final void mWS() throws RecognitionException {
        try {
            int _type = WS;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g:19:5: ( ( ' ' | '\\r' | '\\n' | '\\t' ) )
            // /scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g:19:9: ( ' ' | '\\r' | '\\n' | '\\t' )
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
            // /scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g:27:5: ( ( ( '#' (~ ( '\\n' | '\\r' ) )* ( '\\r' )? '\\n' ) ) )
            // /scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g:27:9: ( ( '#' (~ ( '\\n' | '\\r' ) )* ( '\\r' )? '\\n' ) )
            {
            // /scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g:27:9: ( ( '#' (~ ( '\\n' | '\\r' ) )* ( '\\r' )? '\\n' ) )
            // /scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g:27:10: ( '#' (~ ( '\\n' | '\\r' ) )* ( '\\r' )? '\\n' )
            {
            // /scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g:27:10: ( '#' (~ ( '\\n' | '\\r' ) )* ( '\\r' )? '\\n' )
            // /scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g:27:11: '#' (~ ( '\\n' | '\\r' ) )* ( '\\r' )? '\\n'
            {
            match('#'); 
            // /scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g:27:15: (~ ( '\\n' | '\\r' ) )*
            loop1:
            do {
                int alt1=2;
                int LA1_0 = input.LA(1);

                if ( ((LA1_0>='\u0000' && LA1_0<='\t')||(LA1_0>='\u000B' && LA1_0<='\f')||(LA1_0>='\u000E' && LA1_0<='\uFFFF')) ) {
                    alt1=1;
                }


                switch (alt1) {
            	case 1 :
            	    // /scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g:27:15: ~ ( '\\n' | '\\r' )
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

            // /scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g:27:29: ( '\\r' )?
            int alt2=2;
            int LA2_0 = input.LA(1);

            if ( (LA2_0=='\r') ) {
                alt2=1;
            }
            switch (alt2) {
                case 1 :
                    // /scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g:27:29: '\\r'
                    {
                    match('\r'); 

                    }
                    break;

            }

            match('\n'); 

            }


            }

            _channel=HIDDEN;

            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "COMMENT"

    // $ANTLR start "SPAN"
    public final void mSPAN() throws RecognitionException {
        try {
            int _type = SPAN;
            int _channel = DEFAULT_TOKEN_CHANNEL;
            // /scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g:30:5: ( ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' | '-' | ':' | '/' | '\\\\' | '\\.' )+ )
            // /scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g:30:7: ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' | '-' | ':' | '/' | '\\\\' | '\\.' )+
            {
            // /scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g:30:7: ( 'a' .. 'z' | 'A' .. 'Z' | '0' .. '9' | '_' | '-' | ':' | '/' | '\\\\' | '\\.' )+
            int cnt3=0;
            loop3:
            do {
                int alt3=2;
                int LA3_0 = input.LA(1);

                if ( ((LA3_0>='-' && LA3_0<=':')||(LA3_0>='A' && LA3_0<='Z')||LA3_0=='\\'||LA3_0=='_'||(LA3_0>='a' && LA3_0<='z')) ) {
                    alt3=1;
                }


                switch (alt3) {
            	case 1 :
            	    // /scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g:
            	    {
            	    if ( (input.LA(1)>='-' && input.LA(1)<=':')||(input.LA(1)>='A' && input.LA(1)<='Z')||input.LA(1)=='\\'||input.LA(1)=='_'||(input.LA(1)>='a' && input.LA(1)<='z') ) {
            	        input.consume();

            	    }
            	    else {
            	        MismatchedSetException mse = new MismatchedSetException(null,input);
            	        recover(mse);
            	        throw mse;}


            	    }
            	    break;

            	default :
            	    if ( cnt3 >= 1 ) break loop3;
                        EarlyExitException eee =
                            new EarlyExitException(3, input);
                        throw eee;
                }
                cnt3++;
            } while (true);


            }

            state.type = _type;
            state.channel = _channel;
        }
        finally {
        }
    }
    // $ANTLR end "SPAN"

    public void mTokens() throws RecognitionException {
        // /scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g:1:8: ( T__7 | WS | COMMENT | SPAN )
        int alt4=4;
        switch ( input.LA(1) ) {
        case '=':
            {
            alt4=1;
            }
            break;
        case '\t':
        case '\n':
        case '\r':
        case ' ':
            {
            alt4=2;
            }
            break;
        case '#':
            {
            alt4=3;
            }
            break;
        case '-':
        case '.':
        case '/':
        case '0':
        case '1':
        case '2':
        case '3':
        case '4':
        case '5':
        case '6':
        case '7':
        case '8':
        case '9':
        case ':':
        case 'A':
        case 'B':
        case 'C':
        case 'D':
        case 'E':
        case 'F':
        case 'G':
        case 'H':
        case 'I':
        case 'J':
        case 'K':
        case 'L':
        case 'M':
        case 'N':
        case 'O':
        case 'P':
        case 'Q':
        case 'R':
        case 'S':
        case 'T':
        case 'U':
        case 'V':
        case 'W':
        case 'X':
        case 'Y':
        case 'Z':
        case '\\':
        case '_':
        case 'a':
        case 'b':
        case 'c':
        case 'd':
        case 'e':
        case 'f':
        case 'g':
        case 'h':
        case 'i':
        case 'j':
        case 'k':
        case 'l':
        case 'm':
        case 'n':
        case 'o':
        case 'p':
        case 'q':
        case 'r':
        case 's':
        case 't':
        case 'u':
        case 'v':
        case 'w':
        case 'x':
        case 'y':
        case 'z':
            {
            alt4=4;
            }
            break;
        default:
            NoViableAltException nvae =
                new NoViableAltException("", 4, 0, input);

            throw nvae;
        }

        switch (alt4) {
            case 1 :
                // /scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g:1:10: T__7
                {
                mT__7(); 

                }
                break;
            case 2 :
                // /scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g:1:15: WS
                {
                mWS(); 

                }
                break;
            case 3 :
                // /scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g:1:18: COMMENT
                {
                mCOMMENT(); 

                }
                break;
            case 4 :
                // /scratch/leonn/workspace/tuffy/src/tuffy/parse/Config.g:1:26: SPAN
                {
                mSPAN(); 

                }
                break;

        }

    }


 

}