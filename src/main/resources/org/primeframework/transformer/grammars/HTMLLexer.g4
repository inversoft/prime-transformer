
lexer grammar HTMLLexer;

COMMENT
  : '<!--' .*? '-->'
  ;

WHITESPACE
  : (' '|'\t'|'\r'? '\n')+
  ;

SCRIPT_OPEN
  : '<script' -> pushMode(SCRIPT), pushMode(TAG_ATTRIBUTES)
  ;

STYLE_OPEN
  : '<style'-> pushMode(STYLE), pushMode(TAG_ATTRIBUTES)
  ;

SLASH
  : '/'
  ;

mode SCRIPT;

SCRIPT_BODY
  : .*? '</script>' -> popMode
  ;

mode STYLE;

STYLE_BODY
  : .*? '</style>' -> popMode
  ;

mode tag;


