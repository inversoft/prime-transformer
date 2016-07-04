## Prime Transformer ![semver 2.0.0 compliant](http://img.shields.io/badge/semver-2.0.0-brightgreen.svg?style=flat-square)

Prime Transformer is a BBCode parser and transformation library. Additional implementations may be added support additional source or target markup languages.
The parser returns a light weight document object model of the source BBCode consisting mostly of index values and offsets that can be utilized by the transformer.

Prime Transformer is actively developed and is being utilized in high performance commercial products.

##### Example:

```java
String source = "[b]Hello World![/b]";
Document document = new BBCodeParser().buildDocument(source, null);
String html = new BBCodeToHTMLTransformer().transform(document, (node) -> {
  // transform predicate, returning false will cause this node to not be transformed
  return true;
}, new HTMLTransformFunction(), null);

Assert.assertEquals(html, "<strong>Hello World!</strong>");
```

In the above example, we also provided a transform function as the third parameter. This parameter is optional, but most users will wish to escape HTML characters.
This function is provided in the library, the caller can provide their own implementation as well.

##### Features:
* Written in Java 8
* No regular expressions, implementation uses a finite state machine... it's fast
* Supports tag attributes
 * Tag does not require a closing tag. (e.g. [*])
 * Tag has a pre-formatted body (e.g. [code] or [noparse])
* Supports escape character.
 * Normal: ```[b]foo\[/b]``` --> **foo**
 * Escaped: ```\[b]foo\[/b]``` --> ```[b]foo[/b]```
 
 
**Note:** This project uses the Savant build tool. To compile using using Savant, follow these instructions:
 
```bash
$ mkdir ~/savant
$ cd ~/savant
$ wget http://savant.inversoft.org/org/savantbuild/savant-core/1.0.0/savant-1.0.0.tar.gz
$ tar xvfz savant-1.0.0.tar.gz
$ ln -s ./savant-1.0.0 current
$ export PATH=$PATH:~/savant/current/bin/
```

Then, perform an integration build of the project by running:
```bash
$ sb int
```

For more information, checkout [savantbuild.org](http://savantbuild.org/).
