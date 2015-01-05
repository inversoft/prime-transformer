/*
 * Copyright (c) 2015, Inversoft Inc., All Rights Reserved
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

import org.testng.Assert
import org.testng.annotations.Test

import static org.primeframework.transformer.service.ParserAssert.assertParse

/**
 * Tests the BBCode parser using the ParserAssert DSL.
 *
 * @author Brian Pontarelli
 */
class BBCodeParserTest {
  @Test
  void edgeCase_BadNesting() {
    Assert.fail("Need to figure out handling")
    assertParse("abc[list][[[def[/list]", {
      TextNode(body: "abc", start: 0, end: 3)
      TagNode(name: "list", start: 3, nameEnd: 8, bodyBegin: 9, bodyEnd: 15, end: 22) {
        TextNode(body: "[[[def", start: 9, end: 15)
      }
    })
  }

  @Test
  void edgeCase_Code() {
    assertParse("char[] ca = new char[1024]", {
      TextNode(body: "char", start: 0, end: 4)
      TextNode(body: "[] ca = new char", start: 4, end: 20)
      TextNode(body: "[1024]", start: 20, end: 26)
    })
  }

  @Test
  void edgeCase_SingleBracket() {
    assertParse("abc[def", {
      TextNode(body: "abc", start: 0, end: 3)
      TextNode(body: "[def", start: 3, end: 7)
    })
  }

  @Test
  void edgeCase_MultipleBrackets() {
    assertParse("abc[[[def", {
      TextNode(body: "abc", start: 0, end: 3)
      TextNode(body: "[[[def", start: 3, end: 9)
    })
  }

  @Test
  void edgeCase_GoodStart_ThenOpen() {
    Assert.fail("Need to figure out handling")
    assertParse("[foo][def", {
      TextNode(body: "[foo][def", start: 0, end: 9)
    })
  }

  @Test
  void edgeCase_GoodTag_ThenOpen() {
    assertParse("[b]foo[/b] abc[def", {
      TagNode(name: "b", start: 0, nameEnd: 2, bodyBegin: 3, bodyEnd: 6, end: 10) {
        TextNode(body: "foo", start: 3, end: 6)
      }
      TextNode(body: " abc", start: 10, end: 14)
      TextNode(body: "[def", start: 14, end: 18)
    })
  }
}
