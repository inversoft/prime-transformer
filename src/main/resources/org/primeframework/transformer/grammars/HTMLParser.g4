
parser grammar HTMLParser;

options { tokenVocab=HTMLLexer; }

htmlDocument
  : (WHITESPACE)* htmlElements*
  ;

htmlElements
  : misc* htmlElement misc*
  ;

htmlElement
  : TAG_OPEN htmlTagName htmlAttribute* TAG_CLOSE htmlContent TAG_OPEN SLASH htmlTagName TAG_CLOSE
  | TAG_OPEN htmlTagName htmlAttribute* SLASH? TAG_CLOSE
  ;

htmlComment
  : HTML_COMMENT
  ;

htmlTagName
  : TAG_NAME
  ;

htmlAttribute
  : htmlAttributeName TAG_EQUALS htmlAttributeValue
  | htmlAttributeName
  ;

htmlAttributeName
  : ATTRIBUTE_NAME
  ;

htmlAttributeValue
  :
  ;

misc
  : htmlComment
  | WHITESPACE
  ;


