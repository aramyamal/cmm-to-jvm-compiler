grammar cmm;

options {
    package = cmm_grammar;
}

// PARSER RULES
// a program is a list of definitions
program
    : def* 
    ;

// definitions have type, identifier, arguments and statements
def 
    : type Ident '(' (arg (',' arg)*)? ')' '{' stm* '}'
    ;

// an argument is a type and identifier
arg
    : type Ident
    ;

// statements can be the following
stm
    : exp ';'                           # ExpStm
    | type Ident (',' Ident)* ';'       # DeclsStm
    | type Ident '=' exp ';'            # InitStm
    | 'return' exp ';'                  # ReturnStm
    | 'while' '(' exp ')' stm           # WhileStm
    | '{' stm* '}'                      # BlockStm
    | 'if' '(' exp ')' stm 'else' stm   # IfElseStm
    ;

// expressions can be the following
exp
    : '(' exp ')'                       # ParenExp
    | boolLit                           # BoolExp
    | Integer                           # IntExp
    | Double                            # DoubleExp
    | Ident                             # IdentExp
    | Ident '(' (exp (',' exp)*)? ')'   # FuncExp
    | Ident incDecOp                    # PostExp
    | incDecOp Ident                    # PreExp
    | exp mulOp exp                     # MulExp
    | exp addOp exp                     # AddExp
    | exp cmpOp exp                     # CmpExp
    | exp '&&' exp                      # AndExp
    | exp '||' exp                      # OrExp
    | <assoc=right> Ident '=' exp       # AssignExp
    ;

boolType: 'bool';
intType: 'int';
doubleType: 'double';
voidType: 'void';
type: boolType | intType | doubleType | voidType;

boolLit
    : 'true'                            #TrueLit
    | 'false'                           #FalseLit
    ;

incDecOp
    : '++'                              #Inc
    | '--'                              #Dec
    ;

mulOp
    : '*'                               #Mul
    | '/'                               #Div
    ;

addOp
    : '+'                               #Add
    | '-'                               #Sub
    ;

cmpOp
    : '<'                               #LTh
    | '>'                               #GTh
    | '<='                              #LTE 
    | '>='                              #GTE
    | '=='                              #Equ
    | '!='                              #NEq
    ;

// LEXER RULES
Ident: Letter (Letter | Digit | '_')*;
Integer: Digit+;
Double: Digit+ '.' Digit+ | Digit+ ('.' Digit+)? ('e' | 'E') ('+' | '-')? Digit+;

fragment Letter: [a-zA-Z];
fragment Digit: [0-9];

// skip whitespace and comments
WS: [ \t\r\n]+ -> skip;
LineComment: ('//' | '#') ~[\r\n]* -> skip;
BlockComment: '/*' .*? '*/' -> skip;
