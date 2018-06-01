/*
 * Copyright (c) 2018, Inversoft Inc., All Rights Reserved
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific
 * language governing permissions and limitations under the License.
 */
package org.primeframework.transformer.service

import org.testng.annotations.Test

import java.util.function.Supplier

/**
 * @author Tyler Scott
 */
class HTMLParserTest {

  // Using anonymous inner class to resolve lack of lambdas in groovy 2
  ParserAsserter asserter = new ParserAsserter(new Supplier<Parser>() {
    @Override
    Parser get() {
      return new HTMLParser()
    }
  })

  @Test
  void basic() {
    asserter.assertParse("<strong>Foo</strong>", [[0, 8], [11, 9]], []) {
      TagNode(name: "strong", start: 0, nameEnd: 7, bodyBegin: 8, bodyEnd: 11, end: 20) {
        TextNode(body: "Foo", start: 8, end: 11)
      }
    }
  }

  @Test
  void comment() {
    asserter.assertParse("<!-- Foo -->", [], []) {
      TextNode(body: "<!-- Foo -->", start: 0, end: 12)
    }
  }

  @Test
  void commentWithTagInside() {
    asserter.assertParse("<!-- Foo <bar></bar> -->", [], []) {
      TextNode(body: "<!-- Foo <bar></bar> -->", start: 0, end: 24)
    }
  }

  @Test
  void commentWithTextBeforeAndAfter() {
    asserter.assertParse("Some text<!-- Foo -->with some other text", [], []) {
      TextNode(body: "Some text<!-- Foo -->with some other text", start: 0, end: 41)
    }
  }

  @Test
  void advanced_attributeNewLines() {
    asserter.assertParse(
        """
<div class="someClass"
id="someId"
></div>
""", [[1, 36], [37, 6]], [[13, 9], [28, 6]]) {
      TextNode(body: "\n", start: 0, end: 1)
      TagNode(name: "div", start: 1, nameEnd: 5, bodyBegin: 37, bodyEnd: 37, end: 43,
              attributes: [class: "someClass", id: "someId"])
      TextNode(body: "\n", start: 43, end: 44)
    }
  }

  @Test
  void basic_withAttribute() {
    asserter.assertParse("""<div class="bold">Foo</div>""", [[0, 18], [21, 6]], [[12, 4]]) {
      TagNode(name: "div", start: 0, nameEnd: 4, bodyBegin: 18, bodyEnd: 21, end: 27, attributes: [class: "bold"]) {
        TextNode(body: "Foo", start: 18, end: 21)
      }
    }
  }

  @Test
  void basic_htmlIsNotEscapedWithBackslash() {
    // Keep BBCode separate from the html parser. HTML uses the &<codepoint>; escape style.
    asserter.assertParse("""<div class="bold">Foo\\</div>""", [[0, 18], [22, 6]], [[12, 4]]) {
      TagNode(name: "div", start: 0, nameEnd: 4, bodyBegin: 18, bodyEnd: 22, end: 28, attributes: [class: "bold"]) {
        TextNode(body: "Foo\\", start: 18, end: 22)
      }
    }
  }

  @Test
  void basic_selfClosingTag() {
    // While HTML5 stopped using the self close slash, they are still valid (but ignored)
    asserter.assertParse("""<img src="someImage.png" alt="some text"/>""", [[0, 42]], [[10, 13], [30, 9]]) {
      TagNode(name: "img", start: 0, nameEnd: 4, bodyBegin: 42, bodyEnd: 42, end: 42,
              attributes: [src: "someImage.png", alt: "some text"])
    }
  }

  @Test
  void basic_implicitlyClosedTag() {
    // Some tags are implicitly closed
    asserter.assertParse("""<img src="someImage.png" alt="some text">""", [[0, 41]], [[10, 13], [30, 9]]) {
      TagNode(name: "img", start: 0, nameEnd: 4, bodyBegin: 41, bodyEnd: 41, end: 41,
              attributes: [src: "someImage.png", alt: "some text"])
    }
  }

  @Test
  void basic_booleanAttribute() {
    asserter.assertParse("<input disabled>", [[0, 16]], [[15, 0]]) {
      TagNode(name: "input", start: 0, nameEnd: 6, bodyBegin: 16, bodyEnd: 16, end: 16, attributes: [disabled: "true"])
    }
  }

  @Test
  void control_embeddedTags() {
    asserter.assertParse("<div><span>foo</span></div>", [[0, 5], [5, 6], [14, 7], [21, 6]], []) {
      TagNode(name: "div", start: 0, nameEnd: 4, bodyBegin: 5, bodyEnd: 21, end: 27) {
        TagNode(name: "span", start: 5, nameEnd: 10, bodyBegin: 11, bodyEnd: 14, end: 21) {
          TextNode(body: "foo", start: 11, end: 14)
        }
      }
    }
  }

  @Test
  void notClosed() {
    asserter.assertParse("<div><span>foo</span>", [], []) {
      TextNode(body: "<div><span>foo</span>", start: 0, end: 21)
    }
  }

  @Test
  void extraListItem() {
    asserter.assertParse("<ul><li>foo</li><li></ul>", [[0, 4], [4, 4], [11, 5], [20, 5]], []) {
      TagNode(name: "ul", start: 0, nameEnd: 3, bodyBegin: 4, bodyEnd: 20, end: 25) {
        TagNode(name: "li", start: 4, nameEnd: 7, bodyBegin: 8, bodyEnd: 11, end: 16) {
          TextNode(body: "foo", start: 8, end: 11)
        }
        TextNode(body: "<li>", start: 16, end: 20)
      }
    }
  }

  @Test
  void unclosedStartTagAfterAttribute() {
    asserter.assertParse("<not a tag<a></a> <p>", [[10, 3], [13, 4]], []) {
      TextNode(body: "<not a tag", start: 0, end: 10)
      TagNode(start: 10, name: "a", nameEnd: 12, bodyBegin: 13, bodyEnd: 13, end: 17)
      TextNode(body: " <p>", start: 17, end: 21)
    }
  }

  @Test
  void unclosedStartTagInsideTagName() {
    asserter.assertParse("<no<a></a> <p>", [[3, 3], [6, 4]], []) {
      TextNode(body: "<no", start: 0, end: 3)
      TagNode(start: 3, name: "a", nameEnd: 5, bodyBegin: 6, bodyEnd: 6, end: 10)
      TextNode(body: " <p>", start: 10, end: 14)
    }
  }

  @Test
  void tagStartWhereTagNameGoes() {
    asserter.assertParse("<<a></a> <p>", [[1, 3], [4, 4]], []) {
      TextNode(body: "<", start: 0, end: 1)
      TagNode(start: 1, name: "a", nameEnd: 3, bodyBegin: 4, bodyEnd: 4, end: 8)
      TextNode(body: " <p>", start: 8, end: 12)
    }
  }

  @Test
  void unclosedStartTagInAttribute() {
    asserter.assertParse("<not tag=<a></a> <p>", [[9, 3], [12, 4]], []) {
      TextNode(body: "<not tag=", start: 0, end: 9)
      TagNode(start: 9, name: "a", nameEnd: 11, bodyBegin: 12, bodyEnd: 12, end: 16)
      TextNode(body: " <p>", start: 16, end: 20)
    }
  }

  @Test
  void unpairedCloseTag() {
    asserter.assertParse("</option>", [], []) {
      TextNode(body: "</option>", start: 0, end: 9)
    }
  }

  @Test
  void nestedUnpairedCloseTag() {
    asserter.assertParse("<div><div></option></div></div>", [], []) {
      TagNode(start: 0, name: "div", nameEnd: 4, bodyBegin: 5, bodyEnd: 25, end: 31) {
        TagNode(start: 5, name: "div", nameEnd: 9, bodyBegin: 10, bodyEnd: 19, end: 25) {
          TextNode(body: "</option>", start: 10, end: 19)
        }
      }
    }
  }
}
