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

import org.testng.annotations.DataProvider
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
    assertParse("abc[list][[[def[/list]") {
      TextNode(body: "abc", start: 0, end: 3)
      TagNode(name: "list", start: 3, nameEnd: 8, bodyBegin: 9, bodyEnd: 15, end: 22) {
        TextNode(body: "[[[def", start: 9, end: 15)
      }
    }
  }

  @Test
  void edgeCase_Code() {
    assertParse("char[] ca = new char[1024]") {
      TextNode(body: "char[] ca = new char[1024]", start: 0, end: 26)
    }
  }

  @Test
  void edgeCase_SingleBracket() {
    assertParse("abc[def") {
      TextNode(body: "abc[def", start: 0, end: 7)
    }
  }

  @Test
  void edgeCase_MultipleBrackets() {
    assertParse("abc[[[def") {
      TextNode(body: "abc[[[def", start: 0, end: 9)
    }
  }

  @Test
  void edgeCase_GoodStart_ThenOpen() {
    assertParse("[foo][def") {
      TextNode(body: "[foo][def", start: 0, end: 9)
    }
  }

  @Test
  void edgeCase_GoodTag_ThenOpen() {
    assertParse("[b]foo[/b] abc[def") {
      TagNode(name: "b", start: 0, nameEnd: 2, bodyBegin: 3, bodyEnd: 6, end: 10) {
        TextNode(body: "foo", start: 3, end: 6)
      }
      TextNode(body: " abc[def", start: 10, end: 18)
    }
  }

  @Test
  void edgeCase_OnlyClosingTag() {
    assertParse("[/b] foo") {
      TextNode(body: "[/b] foo", start: 0, end: 8)
    }
  }

  @Test
  void edgeCase_BadTagName() {
    assertParse("[b[b[b[b] foo") {
      TextNode(body: "[b[b[b[b] foo", start: 0, end: 13)
    }
  }

  @Test
  void edgeCase_TagOpeningEndBracketInAttributeWithQuote() {
    assertParse("[font size=']']foo[/font]") {
      TagNode(name: "font", start: 0, nameEnd: 5, attributesBegin: 6, bodyBegin: 15, bodyEnd: 18, end: 25,
              attributes: [size: "]"]) {
        TextNode(body: "foo", start: 15, end: 18)
      }
    }
  }

  @Test
  void edgeCase_TagOpeningEndBracketInAttributeWithoutQuote() {
    assertParse("[font size=]]foo[/font]") {
      TagNode(name: "font", start: 0, nameEnd: 5, attributesBegin: 6, bodyBegin: 12, bodyEnd: 16, end: 23,
              attributes: [size: ""]) {
        TextNode(body: "]foo", start: 12, end: 16)
      }
    }
  }

  @Test
  void edgeCase_MismatchedOpenAndCloseTags() {
    assertParse("[font size=12]foo[/b]") {
      TextNode(body: "[font size=12]foo[/b]", start: 0, end: 21)
    }
  }

  @Test
  void edgeCase_CloseTagNoName() {
    assertParse("[font size=12]foo[/]") {
      TextNode(body: "[font size=12]foo[/]", start: 0, end: 20)
    }
  }

  @Test
  void edgeCase_StrangeAttributeName() {
    assertParse("[font ,;_!=;_l;!]foo[/font]") {
      TagNode(name: "font", start: 0, nameEnd: 5, attributesBegin: 6, bodyBegin: 17, bodyEnd: 20, end: 27,
              attributes: [",;_!": ";_l;!"]) {
        TextNode(body: "foo", start: 17, end: 20)
      }
    }
  }

  @Test
  void edgeCase_StrangeAttributeNameWithQuote() {
    assertParse("[font ,;'!=;l;!]foo[/font]") {
      TagNode(name: "font", start: 0, nameEnd: 5, attributesBegin: 6, bodyBegin: 16, bodyEnd: 19, end: 26,
              attributes: [",;'!": ";l;!"]) {
        TextNode(body: "foo", start: 16, end: 19)
      }
    }
  }

  @Test
  void edgeCase_BadAttributeValueWithQuote() {
    assertParse("[font foo=values{'bar'}]foo[/font]") {
      TagNode(name: "font", start: 0, nameEnd: 5, attributesBegin: 6, bodyBegin: 24, bodyEnd: 27, end: 34,
              attributes: ["foo": "values{'bar'}"]) {
        TextNode(body: "foo", start: 24, end: 27)
      }
    }
  }

  @Test
  void edgeCase_BadAttributeValueWithMismatchedQuotes() {
    assertParse("[font foo='bar\"]foo[/font]") {
      TextNode(body: "[font foo='bar\"]foo[/font]", start: 0, end: 26)
    }
  }

  @Test
  void edgeCase_AttributeValueWithQuotes() {
    assertParse("[font foo='\"bar\"']foo[/font]") {
      TagNode(name: "font", start: 0, nameEnd: 5, attributesBegin: 6, bodyBegin: 18, bodyEnd: 21, end: 28,
              attributes: ["foo": "\"bar\""]) {
        TextNode(body: "foo", start: 18, end: 21)
      }
    }
  }

  @Test
  void edgeCase_AttributeNameWithQuotes() {
    assertParse("[font attr{'foo'}='bar']foo[/font]") {
      TagNode(name: "font", start: 0, nameEnd: 5, attributesBegin: 6, bodyBegin: 24, bodyEnd: 27, end: 34,
              attributes: ["attr{'foo'}": "bar"]) {
        TextNode(body: "foo", start: 24, end: 27)
      }
    }
  }

  @Test
  void edgeCase_SimpleAttributeNoQuotes() {
    assertParse("[font=12]foo[/font]", [[0, 9], [12, 7]], [[6, 2]]) {
      TagNode(name: "font", start: 0, nameEnd: 5, attributesBegin: 6, bodyBegin: 9, bodyEnd: 12, end: 19,
              attribute: "12") {
        TextNode(body: "foo", start: 9, end: 12)
      }
    }
  }

  @Test
  void edgeCase_SimpleAttributeSpacesAfter() {
    assertParse("[font=12   ]foo[/font]", [[0, 12], [15, 7]], [[6, 2]]) {
      TagNode(name: "font", start: 0, nameEnd: 5, attributesBegin: 6, bodyBegin: 12, bodyEnd: 15, end: 22,
              attribute: "12") {
        TextNode(body: "foo", start: 12, end: 15)
      }
    }
  }

  @Test
  void edgeCase_SimpleAttributeQuotesContainsBracket() {
    assertParse("[font='values[\"size\"]']foo[/font]") {
      TagNode(name: "font", start: 0, nameEnd: 5, attributesBegin: 7, bodyBegin: 23, bodyEnd: 26, end: 33,
              attribute: "values[\"size\"]") {
        TextNode(body: "foo", start: 23, end: 26)
      }
    }
  }

  @Test
  void TagDoesNotRequireClosingTagHasParent() {
    assertParse("[list][*]item1[*]item2[/list]") {
      TagNode(name: "list", start: 0, nameEnd: 5, attributesBegin: -1, bodyBegin: 6, bodyEnd: 22, end: 29) {
        TagNode(name: "*", start: 6, nameEnd: 8, bodyBegin: 9, bodyEnd: 14, end: 14) {
          TextNode(body: "item1", start: 9, end: 14)
        }
        TagNode(name: "*", start: 14, nameEnd: 16, bodyBegin: 17, bodyEnd: 22, end: 22) {
          TextNode(body: "item2", start: 17, end: 22)
        }
      }
    }
  }

  @Test
  void edgeCase_TagDoesNotRequireClosingTagNoParent() {
    assertParse("[*]item1[*]item2") {
      TagNode(name: "*", start: 0, nameEnd: 2, bodyBegin: 3, bodyEnd: 8, end: 8) {
        TextNode(body: "item1", start: 3, end: 8)
      }
      TagNode(name: "*", start: 8, nameEnd: 10, bodyBegin: 11, bodyEnd: 16, end: 16) {
        TextNode(body: "item2", start: 11, end: 16)
      }
    }
  }

  @Test
  void edgeCase_SimpleAttributeQuotesInside() {
    assertParse("[font=values{'12'}]foo[/font]") {
      TagNode(name: "font", start: 0, nameEnd: 5, attributesBegin: 6, bodyBegin: 19, bodyEnd: 22, end: 29,
              attribute: "values{'12'}") {
        TextNode(body: "foo", start: 19, end: 22)
      }
    }
  }

  @Test
  public void attributes() {
    assertParse("[d testattr=33]xyz[/d]") {
      TagNode(name: "d", start: 0, nameEnd: 2, attributesBegin: 3, bodyBegin: 15, bodyEnd: 18, end: 22,
              attributes: ["testattr": "33"]) {
        TextNode(body: "xyz", start: 15, end: 18)
      }
    }
  }

  @DataProvider
  public Object[][] noParseData() {
    return [
        ["[noparse]Example: [noparse]foo[/noparse][/noparse]", "Example: [noparse]foo[/noparse]"],
        ["[noparse]Example: [noparse []foo[/noparse][/noparse]", "Example: [noparse []foo[/noparse]"],
    ]
  }

  @Test(dataProvider = "noParseData")
  void edgeCase_noparseEmbeddedNoParse(String str, String body) {
    assertParse(str, [[0, 9], [str.length() - 10, 10]]) {
      TagNode(name: "noparse", start: 0, nameEnd: 8, bodyBegin: 9, bodyEnd: str.length() - 10, end: str.length()) {
        TextNode(body: body, start: 9, end: str.length() - 10)
      }
    }
  }

  @Test
  void tagWithoutClosingTagContainingEmbeddedTags() {
    assertParse("[list][*][b]Test[/b][*][i]Test[/i][/list]",
                [[0, 6], [6, 3], [9, 3], [16, 4], [20, 3], [23, 3], [30, 4], [34, 7]]) {
      TagNode(name: "list", start: 0, nameEnd: 5, bodyBegin: 6, bodyEnd: 34, end: 41) {
        TagNode(name: "*", start: 6, nameEnd: 8, bodyBegin: 9, bodyEnd: 20, end: 20) {
          TagNode(name: "b", start: 9, nameEnd: 11, bodyBegin: 12, bodyEnd: 16, end: 20) {
            TextNode(body: "Test", start: 12, end: 16)
          }
        }
        TagNode(name: "*", start: 20, nameEnd: 22, bodyBegin: 23, bodyEnd: 34, end: 34) {
          TagNode(name: "i", start: 23, nameEnd: 25, bodyBegin: 26, bodyEnd: 30, end: 34) {
            TextNode(body: "Test", start: 26, end: 30)
          }
        }
      }
    }
  }

  @Test
  public void offsets() {
    assertParse("z [b]abc defg [/b]hijk [ul][*]lmn opqr[*][/ul]",
                [[2, 3], [14, 4], [23, 4], [27, 3], [38, 3], [41, 5]]) {
      TextNode(body: "z ", start: 0, end: 2)
      TagNode(name: "b", start: 2, nameEnd: 4, bodyBegin: 5, bodyEnd: 14, end: 18) {
        TextNode(body: "abc defg ", start: 5, end: 14)
      }
      TextNode(body: "hijk ", start: 18, end: 23)
      TagNode(name: "ul", start: 23, nameEnd: 26, bodyBegin: 27, bodyEnd: 41, end: 46) {
        TagNode(name: "*", start: 27, nameEnd: 29, bodyBegin: 30, bodyEnd: 38, end: 38) {
          TextNode(body: "lmn opqr", start: 30, end: 38)
        }
        TagNode(name: "*", start: 38, nameEnd: 40, bodyBegin: 41, bodyEnd: 41, end: 41)
      }
    }

    assertParse(
        "Example [code type=\"see the java oo\" syntax=\"java\"] System.out.println(\"Hello World!\"); [/code] ",
        [[8, 43], [88, 7]]) {
      TextNode(body: "Example ", start: 0, end: 8)
      TagNode(name: "code", start: 8, nameEnd: 13, attributesBegin: 14, bodyBegin: 51, bodyEnd: 88, end: 95,
              attributes: ["type": "see the java oo", "syntax": "java"]) {
        TextNode(body: " System.out.println(\"Hello World!\"); ", start: 51, end: 88)
      }
      TextNode(body: " ", start: 95, end: 96)
    }
  }
}
