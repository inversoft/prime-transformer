prime-transformer
=================

Prime Transformer is a markup parser and transformation library. Written initially for parsing BBCode and transforming to HTML, additional implementations may be added support additional source or target markup languages.


Prime transformer is designed to be able to parse any markup and transform to any other markup language. A DocumentSource object is created using the source markup, and passed to a Parser which returns a Document. The document object can then be passed to any Transformer implementation.

source markup -> DocumentSource -> Parser.buildDocument() -> Document -> Transformer.transform() -> TransformedResult.result -> transformed markup

Example:

```java
String source = "[b]Hello World![/b]";

// Document contains source markup and can be passed to any transformer.
Document document = new BBCodeParser().buildDocument(source);

// A transformer will take a Document and return a TransformedResult that contains the transformed markup in string form.
String html = new BBCodeToHTMLTransformer().transform(document).result;
```

Result:
```
Input: [b]Hello World![/b]
Output: <strong>Hello World!</strong>
```


