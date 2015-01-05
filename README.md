prime-transformer
=================

Prime Transformer is a markup parser and transformation library. Written initially for parsing BBCode and transforming to HTML, additional implementations may be added support additional source or target markup languages.


Prime transformer is designed to be able to parse any markup and transform to any other markup language. The document object can then be passed to any Transformer implementation.

Example:

```java
String source = "[b]Hello World![/b]";
Document document = new BBCodeParser().buildDocument(source);
String html = new BBCodeToHTMLTransformer().transform(document, null, null, null).result;
```

Result:
```
Input: [b]Hello World![/b]
Output: <strong>Hello World!</strong>
```


