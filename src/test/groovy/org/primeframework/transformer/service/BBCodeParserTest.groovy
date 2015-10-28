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

import org.primeframework.transformer.domain.TagAttributes
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
    assertParse("abc[list][[[def[/list]", [[3, 6], [15, 7]], []) {
      TextNode(body: "abc", start: 0, end: 3)
      TagNode(name: "list", start: 3, nameEnd: 8, bodyBegin: 9, bodyEnd: 15, end: 22) {
        TextNode(body: "[[[def", start: 9, end: 15)
      }
    }
  }

  @Test
  void edgeCase_BadNesting_TrailingText() {
    assertParse("abc[list][[[def[/list] ", [[3, 6], [15, 7]], []) {
      TextNode(body: "abc", start: 0, end: 3)
      TagNode(name: "list", start: 3, nameEnd: 8, bodyBegin: 9, bodyEnd: 15, end: 22) {
        TextNode(body: "[[[def", start: 9, end: 15)
      }
      TextNode(body: " ", start: 22, end: 23)
    }
  }

  @Test
  void edgeCase_Code() {
    assertParse("char[] ca = new char[1024]", [], []) {
      TextNode(body: "char[] ca = new char[1024]", start: 0, end: 26)
    }
  }

  @Test
  void edgeCase_SingleBracket() {
    assertParse("abc[def", [], []) {
      TextNode(body: "abc[def", start: 0, end: 7)
    }
  }

  @Test
  void edgeCase_MultipleBrackets() {
    assertParse("abc[[[def", [], []) {
      TextNode(body: "abc[[[def", start: 0, end: 9)
    }
  }

  @Test
  void edgeCase_GoodStart_ThenOpen() {
    assertParse("[foo][def", [], []) {
      TextNode(body: "[foo][def", start: 0, end: 9)
    }
  }

  @Test
  void noParseBasic() {
    assertParse("[noparse][b]Bold[/b][/noparse]", [[0, 9], [20, 10]], []) {
      TagNode(name: "noparse", start: 0, nameEnd: 8, bodyBegin: 9, bodyEnd: 20, end: 30) {
        TextNode(body: "[b]Bold[/b]", start: 9, end: 20)
      }
    }
  }

  @Test
  void noParseBasic_TrailingText() {
    assertParse("[noparse][b]Bold[/b][/noparse] ", [[0, 9], [20, 10]], []) {
      TagNode(name: "noparse", start: 0, nameEnd: 8, bodyBegin: 9, bodyEnd: 20, end: 30) {
        TextNode(body: "[b]Bold[/b]", start: 9, end: 20)
      }
      TextNode(body: " ", start: 30, end: 31)
    }
  }

  @Test
  void noParseBasic_TrailingOpenCharacter() {
    assertParse("[noparse][b]Bold[/b][/noparse][", [[0, 9], [20, 10]], []) {
      TagNode(name: "noparse", start: 0, nameEnd: 8, bodyBegin: 9, bodyEnd: 20, end: 30) {
        TextNode(body: "[b]Bold[/b]", start: 9, end: 20)
      }
      TextNode(body: "[", start: 30, end: 31)
    }
  }

  @Test
  void noParseBasic_TrailingCloseCharacter() {
    assertParse("[noparse][b]Bold[/b][/noparse]]", [[0, 9], [20, 10]], []) {
      TagNode(name: "noparse", start: 0, nameEnd: 8, bodyBegin: 9, bodyEnd: 20, end: 30) {
        TextNode(body: "[b]Bold[/b]", start: 9, end: 20)
      }
      TextNode(body: "]", start: 30, end: 31)
    }
  }

  @Test
  void noParse_WithAttribute() {
    assertParse("[noparse][size=5]Bold[/size][/noparse]", [[0, 9], [28, 10]], []) {
      TagNode(name: "noparse", start: 0, nameEnd: 8, bodyBegin: 9, bodyEnd: 28, end: 38) {
        TextNode(body: "[size=5]Bold[/size]", start: 9, end: 28)
      }
    }
  }

  @Test
  void noParse_WithComplexAttributes() {
    assertParse("[noparse][size foo=5 bar=\"g\"]Bold[/size][/noparse]", [[0, 9], [40, 10]], []) {
      TagNode(name: "noparse", start: 0, nameEnd: 8, bodyBegin: 9, bodyEnd: 40, end: 50) {
        TextNode(body: "[size foo=5 bar=\"g\"]Bold[/size]", start: 9, end: 40)
      }
    }
  }

  @Test
  void noParse_Nested() {
    assertParse("[b][noparse][size foo=5 bar=\"g\"]Bold[/size][/noparse][/b]", [[0, 3], [3, 9], [43, 10], [53, 4]],
                []) {
      TagNode(name: "b", start: 0, nameEnd: 2, bodyBegin: 3, bodyEnd: 53, end: 57) {
        TagNode(name: "noparse", start: 3, nameEnd: 11, bodyBegin: 12, bodyEnd: 43, end: 53) {
          TextNode(body: "[size foo=5 bar=\"g\"]Bold[/size]", start: 12, end: 43)
        }
      }
    }
  }

  @Test
  void edgeCase_GoodTag_ThenOpen() {
    assertParse("[b]foo[/b] abc[def", [[0, 3], [6, 4]], []) {
      TagNode(name: "b", start: 0, nameEnd: 2, bodyBegin: 3, bodyEnd: 6, end: 10) {
        TextNode(body: "foo", start: 3, end: 6)
      }
      TextNode(body: " abc[def", start: 10, end: 18)
    }
  }

  @Test
  void edgeCase_OnlyClosingTag() {
    assertParse("[/b] foo", [], []) {
      TextNode(body: "[/b] foo", start: 0, end: 8)
    }
  }

  @Test
  void edgeCase_wrongClosingTagTag() {
    assertParse("[foo]bar[/foo][b]foo[b]bar[/b][/i]", [[0, 5], [8, 6]], []) {
      TagNode(name: "foo", start: 0, nameEnd: 4, bodyBegin: 5, bodyEnd: 8, end: 14) {
        TextNode(body: "bar", start: 5, end: 8)
      }
      TextNode(body: "[b]foo[b]bar[/b][/i]", start: 14, end: 34)
    }
  }

  @Test
  void edgeCase_BadTagName() {
    assertParse("[b[b[b[b] foo", [], []) {
      TextNode(body: "[b[b[b[b] foo", start: 0, end: 13)
    }
  }

  @Test
  void edgeCase_MultipleOpens() {
    assertParse("[b[b[b[b] foo[/b]", [[6, 3], [13, 4]], []) {
      TextNode(body: "[b[b[b", start: 0, end: 6)
      TagNode(name: "b", start: 6, nameEnd: 8, bodyBegin: 9, bodyEnd: 13, end: 17) {
        TextNode(body: " foo", start: 9, end: 13)
      }
    }
  }

  @Test
  void edgeCase_mixedCaseTags() {
    assertParse("[b]foo[/B][I]bar[/i]", [[0, 3], [6, 4], [10, 3], [16, 4]], []) {
      TagNode(name: "b", start: 0, nameEnd: 2, bodyBegin: 3, bodyEnd: 6, end: 10) {
        TextNode(body: "foo", start: 3, end: 6)
      }
      TagNode(name: "I", start: 10, nameEnd: 12, bodyBegin: 13, bodyEnd: 16, end: 20) {
        TextNode(body: "bar", start: 13, end: 16)
      }
    }
  }

  @Test
  void edgeCase_TagOpeningEndBracketInAttributeWithQuote() {
    assertParse("[font size=']']foo[/font]", [[0, 15], [18, 7]], [[12, 1]]) {
      TagNode(name: "font", start: 0, nameEnd: 5, attributesBegin: 6, bodyBegin: 15, bodyEnd: 18, end: 25,
              attributes: [size: "]"]) {
        TextNode(body: "foo", start: 15, end: 18)
      }
    }
  }

  @Test
  void edgeCase_TagOpeningEndBracketInAttributeWithoutQuote() {
    assertParse("[font size=]]foo[/font]", [[0, 12], [16, 7]], [[11, 0]]) {
      TagNode(name: "font", start: 0, nameEnd: 5, attributesBegin: 6, bodyBegin: 12, bodyEnd: 16, end: 23,
              attributes: [size: ""]) {
        TextNode(body: "]foo", start: 12, end: 16)
      }
    }
  }

  @Test
  void edgeCase_MismatchedOpenAndCloseTags() {
    assertParse("[font size=12]foo[/b]", [], []) {
      TextNode(body: "[font size=12]foo[/b]", start: 0, end: 21)
    }
  }

  @Test
  void edgeCase_CloseTagNoName() {
    assertParse("[font size=12]foo[/]", [], []) {
      TextNode(body: "[font size=12]foo[/]", start: 0, end: 20)
    }
  }

  @Test
  void edgeCase_StrangeAttributeName() {
    assertParse("[font ,;_!=;_l;!]foo[/font]", [[0, 17], [20, 7]], [[11, 5]]) {
      TagNode(name: "font", start: 0, nameEnd: 5, attributesBegin: 6, bodyBegin: 17, bodyEnd: 20, end: 27,
              attributes: [",;_!": ";_l;!"]) {
        TextNode(body: "foo", start: 17, end: 20)
      }
    }
  }

  @Test
  void edgeCase_StrangeAttributeNameWithQuote() {
    assertParse("[font ,;'!=;l;!]foo[/font]", [[0, 16], [19, 7]], [[11, 4]]) {
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
  void edgeCase_complexAttributeSpacesBeforeTwoDigits() {
    assertParse("[font      size=55]blah[/font]", [[0, 19], [23, 7]], [[16, 2]]) {
      TagNode(name: "font", start: 0, nameEnd: 5, attributesBegin: 11, bodyBegin: 19, bodyEnd: 23, end: 30,
              attributes: ["size": "55"]) {
        TextNode(body: "blah", start: 19, end: 23)
      }
    }
  }

  @Test
  void edgeCase_complexAttributeDoubleQuotedValue() {
    assertParse("[font      size=\"55\"]blah[/font]", [[0, 21], [25, 7]], [[17, 2]]) {
      TagNode(name: "font", start: 0, nameEnd: 5, attributesBegin: 11, bodyBegin: 21, bodyEnd: 25, end: 32,
              attributes: ["size": "55"]) {
        TextNode(body: "blah", start: 21, end: 25)
      }
    }
  }

  @Test
  void edgeCase_complexAttributeSingleQuotedValue() {
    assertParse("[font      size='55']blah[/font]", [[0, 21], [25, 7]], [[17, 2]]) {
      TagNode(name: "font", start: 0, nameEnd: 5, attributesBegin: 11, bodyBegin: 21, bodyEnd: 25, end: 32,
              attributes: ["size": "55"]) {
        TextNode(body: "blah", start: 21, end: 25)
      }
    }
  }

  @Test
  void edgeCase_SimpleAttributeQuotesContainsBracket() {
    assertParse("[font='values[\"size\"]']foo[/font]", [[0, 23], [26, 7]], [[7, 14]]) {
      TagNode(name: "font", start: 0, nameEnd: 5, attributesBegin: 7, bodyBegin: 23, bodyEnd: 26, end: 33,
              attribute: "values[\"size\"]") {
        TextNode(body: "foo", start: 23, end: 26)
      }
    }
  }

  @Test
  void normal_list() {
    assertParse("[list][*]item1[*]item2[/list]", [[0, 6], [6, 3], [14, 3], [22, 7]], []) {
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
  void normal_list_b() { // something other than [list]
    assertParse("[b][*]item1[*]item2[/b]", [[0, 3], [3, 3], [11, 3], [19, 4]], []) {
      TagNode(name: "b", start: 0, nameEnd: 2, attributesBegin: -1, bodyBegin: 3, bodyEnd: 19, end: 23) {
        TagNode(name: "*", start: 3, nameEnd: 5, bodyBegin: 6, bodyEnd: 11, end: 11) {
          TextNode(body: "item1", start: 6, end: 11)
        }
        TagNode(name: "*", start: 11, nameEnd: 13, bodyBegin: 14, bodyEnd: 19, end: 19) {
          TextNode(body: "item2", start: 14, end: 19)
        }
      }
    }
  }

  @Test
  void edgeCase_SimpleAttributeQuotesInside() {
    assertParse("[font=values{'12'}]foo[/font]", [[0, 19], [22, 7]], [[6, 12]]) {
      TagNode(name: "font", start: 0, nameEnd: 5, attributesBegin: 6, bodyBegin: 19, bodyEnd: 22, end: 29,
              attribute: "values{'12'}") {
        TextNode(body: "foo", start: 19, end: 22)
      }
    }
  }

  @Test
  void attributes() {
    assertParse("[d testattr=33]xyz[/d]", [[0, 15], [18, 4]], [[12, 2]]) {
      TagNode(name: "d", start: 0, nameEnd: 2, attributesBegin: 3, bodyBegin: 15, bodyEnd: 18, end: 22,
              attributes: ["testattr": "33"]) {
        TextNode(body: "xyz", start: 15, end: 18)
      }
    }
  }

  @DataProvider
  public Object[][] noParseData() {
    return [
        ["[noparse] System.out.println(\"Hello World!\"); [/noparse]", " System.out.println(\"Hello World!\"); "]
        , ["[noparse] [b]b[/i] [xyz] [foo] [] b] [bar '''''' [[[ ]]] [/noparse]", " [b]b[/i] [xyz] [foo] [] b] [bar '''''' [[[ ]]] "]
        , ["[noparse] [b][u][/i] ** && == ++ [foo] [] b] [bar \"\"\" ]] [/noparse]", " [b][u][/i] ** && == ++ [foo] [] b] [bar \"\"\" ]] "]
        , ["[noparse]am.getProcessMemoryInfo([mySinglePID]);[/noparse]", "am.getProcessMemoryInfo([mySinglePID]);"]
    ]
  }

  @Test(dataProvider = "noParseData")
  void edgeCase_noparseEmbeddedBadBBCode(String str, String body) {
    assertParse(str, [[0, 9], [str.length() - 10, 10]]) {
      TagNode(name: "noparse", start: 0, nameEnd: 8, bodyBegin: 9, bodyEnd: str.length() - 10, end: str.length()) {
        TextNode(body: body, start: 9, end: str.length() - 10)
      }
    }
  }

  @DataProvider
  public Object[][] codeData() {
    return [
        ["[code]Example: [code]foo[/code]", "Example: [code]foo"]
        , ["[code] System.out.println(\"Hello World!\"); [/code]", " System.out.println(\"Hello World!\"); "]
        , ["[code]  [b]b[/i] [xyz] [foo] [] b] [bar '''''' [[[ ]]] [/code]", "  [b]b[/i] [xyz] [foo] [] b] [bar '''''' [[[ ]]] "]
        , ["[code] [b][u][/i] ** && == ++ [foo] [] b] [bar \"\"\" ]] [/code]", " [b][u][/i] ** && == ++ [foo] [] b] [bar \"\"\" ]] "]
        , ["[code]am.getProcessMemoryInfo([mySinglePID]);[/code]", "am.getProcessMemoryInfo([mySinglePID]);"]
        , ["[code]new <type>[] { <list of values>};[/code]", "new <type>[] { <list of values>};"]
        , ["[code]// Comment[/code]", "// Comment"]
        , ["[code] // Comment[/code]", " // Comment"]
        , ["[code]<script> window.location.replace(\"http://foo.bar\"); </script> [/code]", "<script> window.location.replace(\"http://foo.bar\"); </script> "]
        , ["[code] [noparse] System.out.println(\"<script> window.location.replace('http://foo.bar');</script>\"); [/noparse] [/code]", " [noparse] System.out.println(\"<script> window.location.replace('http://foo.bar');</script>\"); [/noparse] "]
    ]
  }

  @Test(dataProvider = "codeData")
  void edgeCase_codeEmbeddedCode(String str, String body) {
    assertParse(str, [[0, 6], [str.length() - 7, 7]]) {
      TagNode(name: "code", start: 0, nameEnd: 5, bodyBegin: 6, bodyEnd: str.length() - 7, end: str.length()) {
        TextNode(body: body, start: 6, end: str.length() - 7)
      }
    }
  }

  @Test
  void edgeCase_codeWithLineReturnsOutside() {
    assertParse("\n[b]foo[/b]", [[1, 3], [7, 4]], []) {
      TextNode(body: "\n", start: 0, end: 1)
      TagNode(name: "b", start: 1, nameEnd: 3, bodyBegin: 4, bodyEnd: 7, end: 11) {
        TextNode(body: "foo", start: 4, end: 7)
      }
    }
  }

  @Test
  void edgeCase_codeWithLineReturnInBetweenTags() {
    assertParse(
        "\n[code]new <type>[] { <list of values>};[/code]\n[code]am.getProcessMemoryInfo([mySinglePID]);[/code]",
        [[1, 6], [40, 7], [48, 6], [93, 7]], []) {
      TextNode(body: "\n", start: 0, end: 1)
      TagNode(name: "code", start: 1, nameEnd: 6, bodyBegin: 7, bodyEnd: 40, end: 47) {
        TextNode(body: "new <type>[] { <list of values>};", start: 7, end: 40)
      }
      TextNode(body: "\n", start: 47, end: 48)
      TagNode(name: "code", start: 48, nameEnd: 53, bodyBegin: 54, bodyEnd: 93, end: 100) {
        TextNode(body: "am.getProcessMemoryInfo([mySinglePID]);", start: 54, end: 93)
      }
    }
  }

  @Test
  void edgeCase_codeWithLineReturnInBetweenTagsAndEmbeddedScript() {
    assertParse(
        "\n[code]am.getProcessMemoryInfo([mySinglePID]);[/code]\n[code]<script> window.location.replace(\"http://foo.bar\"); </script> [/code]",
        [[1, 6], [46, 7], [54, 6], [122, 7]],
        []) {
      TextNode(body: "\n", start: 0, end: 1)
      TagNode(name: "code", start: 1, nameEnd: 6, bodyBegin: 7, bodyEnd: 46, end: 53) {
        TextNode(body: "am.getProcessMemoryInfo([mySinglePID]);", start: 7, end: 46)
      }
      TextNode(body: "\n", start: 53, end: 54)
      TagNode(name: "code", start: 54, nameEnd: 59, bodyBegin: 60, bodyEnd: 122, end: 129) {
        TextNode(body: "<script> window.location.replace(\"http://foo.bar\"); </script> ", start: 60, end: 122)
      }
    }
  }

  @Test
  void edgeCase_badTable() {
    assertParse(
        "[table][tr][td]1[/td][/tr][/tr][/td][tr][td]2[/td][/tr][/table]",
        [[0, 7], [7, 4], [11, 4], [16, 5], [21, 5], [36, 4], [40, 4], [45, 5], [50, 5], [55, 8]],
        []) {
      TagNode(name: "table", start: 0, nameEnd: 6, bodyBegin: 7, bodyEnd: 55, end: 63) {
        TagNode(name: "tr", start: 7, nameEnd: 10, bodyBegin: 11, bodyEnd: 21, end: 26) {
          TagNode(name: "td", start: 11, nameEnd: 14, bodyBegin: 15, bodyEnd: 16, end: 21) {
            TextNode(body: "1", start: 15, end: 16)
          }
        }
        TagNode(name: "tr", start: 36, nameEnd: 39, bodyBegin: 40, bodyEnd: 50, end: 55) {
          TagNode(name: "td", start: 40, nameEnd: 43, bodyBegin: 44, bodyEnd: 45, end: 50) {
            TextNode(body: "2", start: 44, end: 45)
          }
        }
      }
    }
  }

  @Test
  void edgeCase_noparseEmbeddedMalformedNoParse() {
    assertParse("[noparse]Example: [noparse []foo[/noparse][/noparse]",
                [[0, 9], [32, 10]],
                []) {
      TagNode(name: "noparse", start: 0, nameEnd: 8, bodyBegin: 9, bodyEnd: 32, end: 42) {
        TextNode(body: "Example: [noparse []foo", start: 9, end: 32)
      }
      TextNode(body: "[/noparse]", start: 42, end: 52)
    }
  }

  @Test
  void edgeCase_DoubleEmbedded() {
    assertParse(
        "[size=5][color=rgb(0,176,80)][size=5][color=rgb(0,176,80)]SEASON 1 WINNER: LUXUSS!![/color][/size][/color][/size]",
        [[0, 8], [8, 21], [29, 8], [37, 21], [83, 8], [91, 7], [98, 8], [106, 7]],
        [[6, 1], [15, 13], [35, 1], [44, 13]]) {
      TagNode(name: "size", start: 0, nameEnd: 5, bodyBegin: 8, bodyEnd: 106, end: 113, attribute: "5") {
        TagNode(name: "color", start: 8, nameEnd: 14, bodyBegin: 29, bodyEnd: 98, end: 106,
                attribute: "rgb(0,176,80)") {
          TagNode(name: "size", start: 29, nameEnd: 34, bodyBegin: 37, bodyEnd: 91, end: 98, attribute: "5") {
            TagNode(name: "color", start: 37, nameEnd: 43, bodyBegin: 58, bodyEnd: 83, end: 91,
                    attribute: "rgb(0,176,80)") {
              TextNode(body: "SEASON 1 WINNER: LUXUSS!!", start: 58, end: 83)
            }
          }
        }
      }
    }
  }

  @Test
  void edgeCase_noparseClosingCodeTag() {
    assertParse("[noparse][b]foo[/b][/code]",
                [],
                []) {
      TextNode(body: "[noparse][b]foo[/b][/code]", start: 0, end: 26)
    }
  }

  @Test
  void edgeCase_noparseClosingCodeTag_closingTagNotRequired() {
    assertParse([noparse: new TagAttributes(true, true, false, true)], // no closing tag, pre-formatted
                "[noparse][b]foo[/b][/code]",
                [[0, 9]],
                []) {
      TagNode(name: "noparse", start: 0, nameEnd: 8, bodyBegin: 9, bodyEnd: 26, end: 26) {
        TextNode(body: "[b]foo[/b][/code]", start: 9, end: 26)
      }
    }
  }

  @Test
  void edgeCase_tagWithoutClosingTagWithBodyWithoutClosingParent() {
    assertParse("[list][*]abc[*] def ",
                [],
                []) {
      TextNode(body: "[list][*]abc[*] def ", start: 0, end: 20)
    }
  }

  @Test
  void edgeCase_tagWithoutClosingTagWithBodyWithoutClosingParent_withAttributes() {
    assertParse(['*' : new TagAttributes(true, false, false, true), // does not require closing tag, normal body
                 list: new TagAttributes(true, false, false, true)], // does not require closing tag, normal body
                "[list][*]abc[*] def ",
                [[0, 6], [6, 3], [12, 3]],
                []) {
      TagNode(name: "list", start: 0, nameEnd: 5, bodyBegin: 6, end: 6)
      TagNode(name: "*", start: 6, nameEnd: 8, bodyBegin: 9, bodyEnd: 12, end: 12) {
        TextNode(body: "abc", start: 9, end: 12)
      }
      TagNode(name: "*", start: 12, nameEnd: 14, bodyBegin: 15, bodyEnd: 20, end: 20) {
        TextNode(body: " def ", start: 15, end: 20)
      }
    }
  }

  @Test
  void edgeCase_unclosedTags() {
    assertParse("[code] abc123 [baz] xyz456",
                [], []) {
      TextNode(body: "[code] abc123 [baz] xyz456", start: 0, end: 26)
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
  void edge_tagWithoutClosingTagContainingEmbeddedTags_oneClosed() {
    assertParse("[list][*][b]Test[/b][/*][*][i]Test[/i][/list]",
                [[0, 6], [6, 3], [9, 3], [16, 4], [20, 4], [24, 3], [27, 3], [34, 4], [38, 7]],
                []) {
      TagNode(name: "list", start: 0, nameEnd: 5, bodyBegin: 6, bodyEnd: 38, end: 45) {
        TagNode(name: "*", start: 6, nameEnd: 8, bodyBegin: 9, bodyEnd: 20, end: 24) {
          TagNode(name: "b", start: 9, nameEnd: 11, bodyBegin: 12, bodyEnd: 16, end: 20) {
            TextNode(body: "Test", start: 12, end: 16)
          }
        }
        TagNode(name: "*", start: 24, nameEnd: 26, bodyBegin: 27, bodyEnd: 38, end: 38) {
          TagNode(name: "i", start: 27, nameEnd: 29, bodyBegin: 30, bodyEnd: 34, end: 38) {
            TextNode(body: "Test", start: 30, end: 34)
          }
        }
      }
    }
  }

  @Test
  void edge_tagWithoutClosingTagContainingEmbeddedTags_bothClosed() {
    assertParse("[list][*][b]Test[/b][/*][*][i]Test[/i][/*][/list]",
                [[0, 6], [6, 3], [9, 3], [16, 4], [20, 4], [24, 3], [27, 3], [34, 4], [38, 4], [42, 7]],
                []) {
      TagNode(name: "list", start: 0, nameEnd: 5, bodyBegin: 6, bodyEnd: 42, end: 49) {
        TagNode(name: "*", start: 6, nameEnd: 8, bodyBegin: 9, bodyEnd: 20, end: 24) {
          TagNode(name: "b", start: 9, nameEnd: 11, bodyBegin: 12, bodyEnd: 16, end: 20) {
            TextNode(body: "Test", start: 12, end: 16)
          }
        }
        TagNode(name: "*", start: 24, nameEnd: 26, bodyBegin: 27, bodyEnd: 38, end: 42) {
          TagNode(name: "i", start: 27, nameEnd: 29, bodyBegin: 30, bodyEnd: 34, end: 38) {
            TextNode(body: "Test", start: 30, end: 34)
          }
        }
      }
    }
  }

  @Test
  void edge_tagWithoutClosingTagContainingEmbeddedTags() {
    assertParse("[*]item1[*]item2", [[0, 3], [8, 3]], []) {
      TagNode(name: "*", start: 0, nameEnd: 2, bodyBegin: 3, bodyEnd: 8, end: 8) {
        TextNode(body: "item1", start: 3, end: 8)
      }
      TagNode(name: "*", start: 8, nameEnd: 10, bodyBegin: 11, bodyEnd: 16, end: 16) {
        TextNode(body: "item2", start: 11, end: 16)
      }
    }
  }

  @Test
  void edge_noTagAttribute() {
    assertParse([:], // Empty attribute map, parser has no knowledge of these tag attributes
                "[list][*]item[*]item[/list]",
                [], []) {
      TextNode(body: "[list][*]item[*]item[/list]", start: 0, end: 27)
    }
  }

  @Test
  void edge_TagAttributes_preFormattedWithAllDoNotRequireClosingTag() {
    assertParse([list: new TagAttributes(true, true, false, true), // do not require closing tag, pre-formatted body
                 '*' : new TagAttributes(true, false, false, true)], // do not require closing tag, normal body
                "[list][*]item[*]item",
                [[0, 6]], []) {
      TagNode(name: "list", start: 0, nameEnd: 5, bodyBegin: 6, bodyEnd: 20, end: 20) {
        TextNode(body: "[*]item[*]item", start: 6, end: 20)
      }
    }
  }

  @Test
  void edge_unexpectedTagAttributes_allDoNotRequireClosingTagNoBody() {
    assertParse([list: new TagAttributes(true, false, false, true), // do not require closing tag, normal body
                 '*' : new TagAttributes(true, false, false, true)], // do not require closing tag, normal body
                "[list][*][*]",
                [[0, 6], [6, 3], [9, 3]], []) {
      TagNode(name: "list", start: 0, nameEnd: 5, bodyBegin: 6, bodyEnd: 6, end: 6)
      TagNode(name: "*", start: 6, nameEnd: 8, bodyBegin: 9, bodyEnd: 9, end: 9)
      TagNode(name: "*", start: 9, nameEnd: 11, bodyBegin: 12, bodyEnd: 12, end: 12)
    }
  }

  @Test
  void edge_unexpectedTagAttributes_NestedLists() {
    assertParse("[list][*]one[*]two[list][*]three[*]four[/list][/list]",
                [[0, 6], [6, 3], [12, 3], [18, 6], [24, 3], [32, 3], [39, 7], [46, 7]], []) {
      TagNode(name: "list", start: 0, nameEnd: 5, bodyBegin: 6, bodyEnd: 46, end: 53) {
        TagNode(name: "*", start: 6, nameEnd: 8, bodyBegin: 9, bodyEnd: 12, end: 12) {
          TextNode(body: "one", start: 9, end: 12)
        }
        TagNode(name: "*", start: 12, nameEnd: 14, bodyBegin: 15, bodyEnd: 46, end: 46) {
          TextNode(body: "two", start: 15, end: 18)
          TagNode(name: "list", start: 18, nameEnd: 23, bodyBegin: 24, bodyEnd: 39, end: 46) {
            TagNode(name: "*", start: 24, nameEnd: 26, bodyBegin: 27, bodyEnd: 32, end: 32) {
              TextNode(body: "three", start: 27, end: 32)
            }
            TagNode(name: "*", start: 32, nameEnd: 34, bodyBegin: 35, bodyEnd: 39, end: 39) {
              TextNode(body: "four", start: 35, end: 39)
            }
          }
        }
      }
    }
  }

  @Test
  void edge_unexpectedTagAttribute_doNotRequireClosingTagsButIncludeThem() {
    assertParse([b: new TagAttributes(true, false, false, true),
                 i: new TagAttributes(true, false, false, true)], // set both nodes to not require closing tag
                "[b][i]italic[/i][/b]",
                [[0, 3], [3, 3], [12, 4], [16, 4]], []) {
      TagNode(name: "b", start: 0, nameEnd: 2, bodyBegin: 3, bodyEnd: 16, end: 20) {
        TagNode(name: "i", start: 3, nameEnd: 5, bodyBegin: 6, bodyEnd: 12, end: 16) {
          TextNode(body: "italic", start: 6, end: 12)
        }
      }
    }
  }

  @Test
  void edge_unexpectedTagAttribute_preFormatted_b_tag() {
    assertParse([b: new TagAttributes(true, true, false, true), // do not require closing tag, pre-formatted body
                 i: new TagAttributes(true, false, false, true)], // do not require closing tag, normal body
                "[b][i]italic[/i][/b]",
                [[0, 3], [16, 4]], []) {
      TagNode(name: "b", start: 0, nameEnd: 2, bodyBegin: 3, bodyEnd: 16, end: 20) {
        TextNode(body: "[i]italic[/i]", start: 3, end: 16)
      }
    }
  }

  @Test
  void embeddedTags_quote() {
    assertParse("[quote][quote][quote]Hot pocket in a hot pocket[/quote][/quote][/quote]",
                [[0, 7], [7, 7], [14, 7], [47, 8], [55, 8], [63, 8]], []) {
      TagNode(name: "quote", start: 0, nameEnd: 6, bodyBegin: 7, bodyEnd: 63, end: 71) {
        TagNode(name: "quote", start: 7, nameEnd: 13, bodyBegin: 14, bodyEnd: 55, end: 63) {
          TagNode(name: "quote", start: 14, nameEnd: 20, bodyBegin: 21, bodyEnd: 47, end: 55) {
            TextNode(body: "Hot pocket in a hot pocket", start: 21, end: 47)
          }
        }
      }
    }
  }

  @Test
  void escape_OpenTagAndCloseTags() {
    assertParse("Example BBCode: \\[code] foo \\[/code]", [], []) {
      TextNode(body: "Example BBCode: ", start: 0, end: 16)
      TextNode(body: "[code] foo ", start: 17, end: 28)
      TextNode(body: "[/code]", start: 29, end: 36)
    }
  }

  @Test
  void escape_OpenTagWithUnescapedCloseTag() {
    assertParse("Example BBCode: \\[code] foo [/code]", [], []) {
      TextNode(body: "Example BBCode: ", start: 0, end: 16)
      TextNode(body: "[code] foo [/code]", start: 17, end: 35)
    }
  }

  @Test
  void escape_OpenTag() {
    assertParse("Example BBCode: \\[test]", [], []) {
      TextNode(body: "Example BBCode: ", start: 0, end: 16)
      TextNode(body: "[test]", start: 17, end: 23)
    }
  }

  @Test
  void escape_Tags() {
    assertParse("\\[b] foo \\[/b]", [], []) {
      TextNode(body: "[b] foo ", start: 1, end: 9)
      TextNode(body: "[/b]", start: 10, end: 14)
    }
  }

  @Test
  void escape_Tags_WithTrailingText() {
    assertParse("\\[b] foo \\[/b] ", [], []) {
      TextNode(body: "[b] foo ", start: 1, end: 9)
      TextNode(body: "[/b] ", start: 10, end: 15)
    }
  }

  @Test
  void noClosingTag_offsetsCleared() {
    assertParse("[foo] [font=verdana]foo[/font] [b]bar[/b] [color=red] red[/color]", [], []) {
      TextNode(body: "[foo] [font=verdana]foo[/font] [b]bar[/b] [color=red] red[/color]", start: 0, end: 65)
    }
  }

  @Test
  void unexpectedCloseTag_offsetsCleared() {
    assertParse("[foo] abc [b]def[/b] [/bar]", [], []) {
      TextNode(body: "[foo] abc [b]def[/b] [/bar]", start: 0, end: 27)
    }
  }

  @Test
  void preFormattedTag_offsetsCleared() {
    assertParse("[code] [font=verdana]foo[/font] [/code]", [[0, 6], [32, 7]], []) {
      TagNode(name: "code", start: 0, nameEnd: 5, bodyBegin: 6, bodyEnd: 32, end: 39) {
        TextNode(body: " [font=verdana]foo[/font] ", start: 6, end: 32)
      }
    }
  }

  @Test
  void standalone_tags() {
    assertParse("[emoji][b]Have a nice day![/b]", [[0, 7], [7, 3], [26, 4]], []) {
      TagNode(name: "emoji", start: 0, nameEnd: 6, bodyBegin: 7, bodyEnd: 7, end: 7)
      TagNode(name: "b", start: 7, nameEnd: 9, bodyBegin: 10, bodyEnd: 26, end: 30) {
        TextNode(body: "Have a nice day!", start: 10, end: 26)
      }
    }
  }

  @Test
  void standalone_tagsWithText() {
    assertParse("[emoji] [b]Have a nice day![/b]", [[0, 7], [8, 3], [27, 4]], []) {
      TagNode(name: "emoji", start: 0, nameEnd: 6, bodyBegin: 7, bodyEnd: 7, end: 7)
      TextNode(body: " ", start: 7, end: 8)
      TagNode(name: "b", start: 8, nameEnd: 10, bodyBegin: 11, bodyEnd: 27, end: 31) {
        TextNode(body: "Have a nice day!", start: 11, end: 27)
      }
    }
  }

  @Test
  void standalone_tagsWithBadTagStart() {
    assertParse("[emoji][[b]Have a nice day![/b]", [[0, 7], [8, 3], [27, 4]], []) {
      TagNode(name: "emoji", start: 0, nameEnd: 6, bodyBegin: 7, bodyEnd: 7, end: 7)
      TextNode(body: "[", start: 7, end: 8)
      TagNode(name: "b", start: 8, nameEnd: 10, bodyBegin: 11, bodyEnd: 27, end: 31) {
        TextNode(body: "Have a nice day!", start: 11, end: 27)
      }
    }
  }

  @Test
  void standalone_NestedInsideUnclosedTag() {
    assertParse("[list][*]one[emoji]two[/list]", [[0, 6], [6, 3], [12, 7], [22, 7]], []) {
      TagNode(name: "list", start: 0, nameEnd: 5, bodyBegin: 6, bodyEnd: 22, end: 29) {
        TagNode(name: "*", start: 6, nameEnd: 8, bodyBegin: 9, bodyEnd: 22, end: 22) {
          TextNode(body: "one", start: 9, end: 12)
          TagNode(name: "emoji", start: 12, nameEnd: 18, bodyBegin: 19, bodyEnd: 19, end: 19)
          TextNode(body: "two", start: 19, end: 22)
        }
      }
    }
  }

  @Test
  void standalone_lastTag() {
    assertParse("[b]Have a nice day![/b][emoji]", [[0, 3], [19, 4], [23, 7]], []) {
      TagNode(name: "b", start: 0, nameEnd: 2, bodyBegin: 3, bodyEnd: 19, end: 23) {
        TextNode(body: "Have a nice day!", start: 3, end: 19)
      }
      TagNode(name: "emoji", start: 23, nameEnd: 29, bodyBegin: 30, bodyEnd: 30, end: 30)
    }
  }

  @Test
  void containsHTML() {
    assertParse(
        "[size=5][b]Pricing Guide:[/b][/size]<br><br><br>[font=Roboto]Phantom Force Cards[/font]<br><br>[font=Roboto]Phantom Forces Pokemon EX[/font]",
        [[0, 8], [8, 3], [25, 4], [29, 7], [48, 13], [80, 7], [95, 13], [133, 7]], [[6, 1], [54, 6], [101, 6]]) {
      TagNode(name: "size", start: 0, nameEnd: 5, bodyBegin: 8, bodyEnd: 29, end: 36, attribute: "5") {
        TagNode(name: "b", start: 8, nameEnd: 10, bodyBegin: 11, bodyEnd: 25, end: 29) {
          TextNode(body: "Pricing Guide:", start: 11, end: 25)
        }
      }
      TextNode(body: "<br><br><br>", start: 36, end: 48)
      TagNode(name: "font", start: 48, nameEnd: 53, bodyBegin: 61, bodyEnd: 80, end: 87, attribute: "Roboto") {
        TextNode(body: "Phantom Force Cards", start: 61, end: 80)
      }
      TextNode(body: "<br><br>", start: 87, end: 95)
      TagNode(name: "font", start: 95, nameEnd: 100, bodyBegin: 108, bodyEnd: 133, end: 140, attribute: "Roboto") {
        TextNode(body: "Phantom Forces Pokemon EX", start: 108, end: 133)
      }
    }
  }

  @Test
  void standalone_lastTagWithText() {
    assertParse("[b]Have a nice day![/b] [emoji]", [[0, 3], [19, 4], [24, 7]], []) {
      TagNode(name: "b", start: 0, nameEnd: 2, bodyBegin: 3, bodyEnd: 19, end: 23) {
        TextNode(body: "Have a nice day!", start: 3, end: 19)
      }
      TextNode(body: " ", start: 23, end: 24)
      TagNode(name: "emoji", start: 24, nameEnd: 30, bodyBegin: 31, bodyEnd: 31, end: 31)
    }
  }

  @Test
  void standalone_lastTagWithTextTrailingText() {
    assertParse("[b]Have a nice day![/b] [emoji] ", [[0, 3], [19, 4], [24, 7]], []) {
      TagNode(name: "b", start: 0, nameEnd: 2, bodyBegin: 3, bodyEnd: 19, end: 23) {
        TextNode(body: "Have a nice day!", start: 3, end: 19)
      }
      TextNode(body: " ", start: 23, end: 24)
      TagNode(name: "emoji", start: 24, nameEnd: 30, bodyBegin: 31, bodyEnd: 31, end: 31)
      TextNode(body: " ", start: 31, end: 32)
    }
  }

  @Test
  void standalone_embedded() {
    assertParse("[quote][emoji][/quote]", [[0, 7], [7, 7], [14, 8]], []) {
      TagNode(name: "quote", start: 0, nameEnd: 6, bodyBegin: 7, bodyEnd: 14, end: 22) {
        TagNode(name: "emoji", start: 7, nameEnd: 13, bodyBegin: 14, bodyEnd: 14, end: 14)
      }
    }
  }

  @Test
  void noBody_simpleAttribute() {
    assertParse("[color=#000000][/color]", [[0, 15], [15, 8]], [[7, 7]]) {
      TagNode(name: "color", start: 0, nameEnd: 6, bodyBegin: 15, bodyEnd: 15, end: 23, attribute: "#000000")
    }
  }

  @Test
  void standalone_embeddedWithText() {
    assertParse("[quote] [emoji] [/quote]", [[0, 7], [8, 7], [16, 8]], []) {
      TagNode(name: "quote", start: 0, nameEnd: 6, bodyBegin: 7, bodyEnd: 16, end: 24) {
        TextNode(body: " ", start: 7, end: 8)
        TagNode(name: "emoji", start: 8, nameEnd: 14, bodyBegin: 15, bodyEnd: 15, end: 15)
        TextNode(body: " ", start: 15, end: 16)
      }
    }
  }

  @Test
  void standalone_embeddedWithAttribute() {
    assertParse("[quote] [emoji=sad] [/quote]", [[0, 7], [8, 11], [20, 8]], [[15, 3]]) {
      TagNode(name: "quote", start: 0, nameEnd: 6, bodyBegin: 7, bodyEnd: 20, end: 28) {
        TextNode(body: " ", start: 7, end: 8)
        TagNode(name: "emoji", start: 8, nameEnd: 14, attributesBegin: 15, bodyBegin: 19, bodyEnd: 19, end: 19,
                attribute: "sad")
        TextNode(body: " ", start: 19, end: 20)
      }
    }
  }

  @Test
  void standalone_embeddedWithAttribute_WithTrailingText() {
    assertParse("[quote] [emoji=sad] [/quote] ", [[0, 7], [8, 11], [20, 8]], [[15, 3]]) {
      TagNode(name: "quote", start: 0, nameEnd: 6, bodyBegin: 7, bodyEnd: 20, end: 28) {
        TextNode(body: " ", start: 7, end: 8)
        TagNode(name: "emoji", start: 8, nameEnd: 14, attributesBegin: 15, bodyBegin: 19, bodyEnd: 19, end: 19,
                attribute: "sad")
        TextNode(body: " ", start: 19, end: 20)
      }
      TextNode(body: " ", start: 28, end: 29)
    }
  }

  @Test
  void offsets() {
    assertParse("z [b]abc defg [/b]hijk [ul][*]lmn opqr[*][/ul]",
                [[2, 3], [14, 4], [23, 4], [27, 3], [38, 3], [41, 5]], []) {
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
        [[8, 43], [88, 7]], [[20, 15], [45, 4]]) {
      TextNode(body: "Example ", start: 0, end: 8)
      TagNode(name: "code", start: 8, nameEnd: 13, attributesBegin: 14, bodyBegin: 51, bodyEnd: 88, end: 95,
              attributes: ["type": "see the java oo", "syntax": "java"]) {
        TextNode(body: " System.out.println(\"Hello World!\"); ", start: 51, end: 88)
      }
      TextNode(body: " ", start: 95, end: 96)
    }
  }
}
