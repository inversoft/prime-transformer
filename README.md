prime-transformer
=================

Prime Transformer is a BBCode parser and transformation library. Additional implementations may be added support additional source or target markup languages.
The parser returns a light weight document object model of the source BBCode consisting mostly of index values and offsets that can be utilized by the transformer.

Example:

```java
String source = "[b]Hello World![/b]";
Document document = new BBCodeParser().buildDocument(source);
String html = new BBCodeToHTMLTransformer().transform(document, (node) -> {
  // transform predicate, returning false will cause this node to not be transformed
  return true;
}, new HTMLEscapeTransformFunction(), null).result;
```

Result:
```
Input: [b]Hello World![/b]
Output: <strong>Hello World!</strong>
```

In the above example, we also provided a transform function as the third parameter. This parameter is optional, but most users will wish to escape HTML characters.
This function is provided in the library, the caller can provide their own implementation as well.

Features:
* Written in Java 8
* No regular expressions, implementation uses a finite state machine... it's fast
* Supports passing tag attributes to identify tags with a pre-formatted body or not requiring a closing tag
* Supports escape character.
 * Normal: ```[b]foo\[/b]``` --> **foo**
 * Escaped: ```\[b]foo\[/b]``` --> ```[b]foo[/b]```