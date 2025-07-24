grammar ShowGrantsProcedureName;

showGrantsProcedureName
    : part DOT part DOT callableName EOF
    ;

part
    : STANDARD_UPPERCASE_IDENTIFIER
    | quotedIdentifier
    ;

callableName
    : QUOTE unquotedIdentifier expandedArgs (COLON dataType)? returnArgs? QUOTE
    ;

returnArgs
    : COLON SPACE expandedArgs
    ;

unquotedIdentifier
    : STANDARD_UPPERCASE_IDENTIFIER|NONSTANDARD_IDENTIFIER_UNQUOTED
    ;

expandedArgs
    : OPEN_P (expandedArgument (COMMA SPACE expandedArgument)*)? CLOSE_P
    ;

expandedArgument
    : unquotedIdentifier SPACE dataType
    ;

columnTypes
    : OPEN_P (dataType (COMMA SPACE dataType)*)? CLOSE_P
    ;

dataType
    : STANDARD_UPPERCASE_IDENTIFIER ( expandedArgs | columnTypes | dataTypeParams)?
    ;

dataTypeParams
    : OPEN_P INTEGER (COMMA INTEGER)* CLOSE_P
    ;
quotedIdentifier
    : QUOTE NONSTANDARD_IDENTIFIER_UNQUOTED QUOTE
    ;


OPEN_P : '(' ;
CLOSE_P : ')' ;
DOT : '.' ;
COMMA : ',';
QUOTE : '"';
SPACE : ' ';
COLON : ':';
NEWLINE : [\n];
STANDARD_UPPERCASE_IDENTIFIER
    : [A-Z_][A-Z0-9_]*
    ;
INTEGER
    : [0-9]+
    ;

NONSTANDARD_IDENTIFIER_UNQUOTED
    : ('""' | ~["() \\.\n\t\r,:])+
    ;
