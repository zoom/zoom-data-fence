grammar ObjectName;

objectName
    : part (DOT part)* args? EOF
    ;

part
    : IDENTIFIER
    | QUOTED_IDENTIFIER
    | FUTURE_TYPE_OBJECT
    ;

args
    : OPEN_P (argument (COMMA argument)*)? CLOSE_P
    ;

argument
    : IDENTIFIER (argumentParams | args)?
    ;

argumentParams
    : OPEN_P INTEGER (COMMA INTEGER)* CLOSE_P
    ;

OPEN_P : '(' ;
CLOSE_P : ')' ;
DOT : '.' ;
COMMA : ',';
QUOTE : '"';

INTEGER
    : [0-9]+
    ;

IDENTIFIER
    : [a-zA-Z_][a-zA-Z0-9_]*
    ;


FUTURE_TYPE_OBJECT
    : '<'[A-Z][A-Z0-9_ ]+'>'
    ;

QUOTED_IDENTIFIER
    : '"' ('""' | ~["])* ('"')
    ;

WS
    : [ \t\r\n]+ -> skip
    ;
