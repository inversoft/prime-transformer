prime-transformer
=================

Prime Transformer is a markup transformation library. Written initially for transforming BBCode to HTML, additional implementations may be added support additional source or target markup languages.

h4. Currently supported transformations

BBCode --> Freemarker templates (HTML)

Prime transformer is designed to be able to parse any markup and transform to any other markup language. A DocumentSource object is created using the source markup, and passed to a Parser which returns a Document. The document object can then be passed to an Transformer implementation.

input -> DocumentSource -> Parser.buildDocument() -> Document -> Transformer.transform() -> result

Example:

```java
Document document = new BBCodeParser().buildDocument(new DocumentSource("[b]Hello World![/b]"));
String html = new BBCodeToHTMLTransformer().transform(document);
```

Result:
```
Input: [b]Hello World![/b]
Output: <strong>Hello World!</strong>
```


