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
  void invalidHtml() {
    asserter.assertParse("<not a tag<a></a> <p>", [], []) {
      TextNode(body: "<not a tag<a></a> <p>", start: 0, end: 21)
    }
  }
}
