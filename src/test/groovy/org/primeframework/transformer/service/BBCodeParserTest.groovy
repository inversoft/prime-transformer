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
      TextNode("abc", 0, 3)
      TagNode("list", 3, 8, -1, 9, 15, 22) {
        TextNode("[[[def", 9, 15)
      }
    })
  }

  @Test
  void edgeCase_Code() {
    assertParse("char[] ca = new char[1024]", {
      TextNode("char", 0, 4)
      TextNode("[] ca = new char", 4, 20)
      TextNode("[1024]", 20, 26)
    })
  }

  @Test
  void edgeCase_SingleBracket() {
    assertParse("abc[def", {
      TextNode("abc", 0, 3)
      TextNode("[def", 3, 7)
    })
  }

  @Test
  void edgeCase_MultipleBrackets() {
    assertParse("abc[[[def", {
      TextNode("abc", 0, 3)
      TextNode("[[[def", 3, 9)
    })
  }

  @Test
  void edgeCase_GoodStart_ThenOpen() {
    Assert.fail("Need to figure out handling")
    assertParse("[foo][def", {
      TextNode("[foo][def", 0, 9)
    })
  }

  @Test
  void edgeCase_GoodTag_ThenOpen() {
    assertParse("[b]foo[/b] abc[def", {
      TagNode("b", 0, 2, -1, 3, 6, 10) {
        TextNode("foo", 3, 6)
      }
      TextNode(" abc", 10, 14)
      TextNode("[def", 14, 18)
    })
  }
}
