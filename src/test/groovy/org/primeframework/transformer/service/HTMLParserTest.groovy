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
    asserter.assertParse("""<div class="bold">Foo\\</div>""", [[0, 18], [22, 6]], [[12, 4]]) {
      TagNode(name: "div", start: 0, nameEnd: 4, bodyBegin: 18, bodyEnd: 22, end: 28, attributes: [class: "bold"]) {
        TextNode(body: "Foo\\", start: 18, end: 22)
      }
    }
  }

  @Test
  void basic_selfClosingTag() {
    asserter.assertParse("""<img src="someImage.png" alt="some text"/>""", [[0, 42]], [[10, 13], [30, 9]]) {
      TagNode(name: "img", start: 0, nameEnd: 4, bodyBegin: 42, bodyEnd: 42, end: 42,
              attributes: [src: "someImage.png", alt: "some text"])
    }
  }

  @Test
  void basic_implicitlyClosedTag() {
    asserter.assertParse("""<img src="someImage.png" alt="some text">""", [[0, 41]], [[10, 13], [30, 9]]) {
      TagNode(name: "img", start: 0, nameEnd: 4, bodyBegin: 41, bodyEnd: 41, end: 41,
              attributes: [src: "someImage.png", alt: "some text"])
    }
  }
}
