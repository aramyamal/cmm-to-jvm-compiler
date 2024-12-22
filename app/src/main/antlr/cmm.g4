grammar cmm;

// options {
//     package = cmm_grammar;
// }

// PARSER RULES
// a program is a list of definitions
program
    : def* 
    ;

// definitions have type, identifier, arguments and statements
def 
    : type Id '(' (arg (',' arg)*)? ')' '{' stm* '}'
    ;

// an argument is a type and identifier
arg
    : type Id
    ;

// statements can be the following
stm
    : exp ';'                           # ExpStm
    | type Id (',' Id)* ';'             # DeclsStm
    | type Id ()                        # InitStm
    | 'return' exp ';'                  # ReturnStm
    | 'while' '(' exp ')' stm           # WhileStm
    | '{' stm* '}'                      # BlockStm
    | 'if' '(' exp ')' stm 'else' stm   # IfElseStm
    ;

// expressions can be the following
exp
    : Id '=' exp                        # AssExp
    | exp '||' exp                      # OrExp
    | exp '&&' exp                      # AndExp
    | exp cmpOp exp                     # CmpExp
    | exp addOp exp                     # AddExp
    | exp mulOp exp                     # MulExp
    | boolLit                           # BoolExp
    | Integer                           # IntExp
    | Double                            # DoubleExp
    | Id                                # IdExp
    | Id '(' (exp (',' exp)*)? ')'      # AppExp
    | Id incDecOp                       # PostExp
    | incDecOp Id                       # PreExp
    ;

type: 'bool' | 'int' | 'double' | 'void';

boolLit: 'true' | 'false';

incDecOp: '++' | '--';
mulOp: '*' | '/';
addOp: '+' | '-';
cmpOp: '<' | '>' | '<=' | '>=' | '==' | '!=';

// LEXER RULES
Id: Letter (Letter | Digit | '_')*;
Integer: Digit+;
Double: Digit+ '.' Digit+ | Digit+ ('.' Digit+)? ('e' | 'E') ('+' | '-')? Digit+;

fragment Letter: [a-zA-Z];
fragment Digit: [0-9];

// skip whitespace and comments
WS: [ \t\r\n]+ -> skip;
LineComment: ('//' | '#') ~[\r\n]* -> skip;
BlockComment: '/*' .*? '*/' -> skip;
