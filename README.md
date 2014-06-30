prime-transformer
=================

Prime Transformer is a markup transformation library. Written initially for transforming BBCode to HTML, additional implementations may be added support additional source or target markup languages.

h4. Currently supported transformations

BBCode --> Freemarker templates
                --> HTML

Prime transformer is designed to be able to parse any markup and transform to any other markup language. A DocumentSource object is created using the source markup, and passed to a Parser which returns a Document. The document object can then be passed to an Transformer implementation.


Example:

```java
String bbCode = ""[b]Hello World![/b]";
DocumentSource documentSource = new DocumentSource(bbCode);
Document document = new BBCodeParser().buildDocument(documentSource);

String html = new BBCodeToHTMLTransformer().transform(document);

```

Result:
```
Input: [b]Hello World![/b]
Output: <strong>Hello World!</strong>
```


