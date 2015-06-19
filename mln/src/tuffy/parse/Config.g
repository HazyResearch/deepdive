grammar Config;
options {language=Java; output=AST; backtrack=false; memoize=false;}

@lexer::header {
package tuffy.parse;
}

@parser::header{
package tuffy.parse;
import java.util.Hashtable;
import tuffy.mln.*;
import tuffy.util.*;
}

@parser::members {
public Hashtable<String, String> map = new Hashtable<String, String>();
}

WS  :   (' ' 
        | '\r'
        | '\n'
        | '\t'
        ) {$channel=HIDDEN;}
    ;

COMMENT
    :   (('#' ~('\n'|'\r')* '\r'? '\n')) {$channel=HIDDEN;}
    ;

SPAN: ('a'..'z'|'A'..'Z'|'0'..'9'|'_'|'-'|':'|'/'|'\\'|'\.')+;

config :  (state)+ EOF;

state : id=SPAN '=' value=SPAN
      {
          map.put($id.text, $value.text);
      }
      ;
