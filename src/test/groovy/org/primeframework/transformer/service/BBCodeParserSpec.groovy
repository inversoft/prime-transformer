/*
 * Copyright (c) 2014, Inversoft Inc., All Rights Reserved
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

import org.primeframework.transformer.domain.Pair
import org.primeframework.transformer.domain.TagAttributes
import org.primeframework.transformer.domain.TagNode
import org.primeframework.transformer.domain.TextNode
import spock.lang.Shared
import spock.lang.Specification

import java.util.function.Predicate

public class BBCodeParserSpec extends Specification {

  /**
   * Shame I can't use lambdas until Groovy v3. :-(
   */
  @Shared
  def transformPredicate = new Predicate<TagNode>() {
    @Override
    public boolean test(TagNode tag) {
      return true;
    }
  }

  @Shared
  def attributes = [ '*' : new TagAttributes(true, false),
                     code : new TagAttributes(false, true),
                     noparse : new TagAttributes(false, true)]

  // TODO can be removed - keeping here now until I complete work on the parser

  def "unexpected unclosed tag"() {

    when: "A document is constructed"
      def source = "[b]Text[/i]"
      /*            ^      ^      */
      /*            0,3    7,4    */
      def document = new BBCodeParser().buildDocument(source, attributes)

    then: "no exceptions thrown"

      notThrown Exception

    and: "result should not be null"
      document != null

    and:
      document.children.size() == 1
      document.children.get(0) instanceof TextNode
      def text = (TextNode) document.children.get(0)
      text.getBody() == "[b]Text[/i]"

    and: "offsets to tags should be empty"
      document.offsets.empty
      document.attributeOffsets.empty
  }

  def "basic noparse tag"() {

    when: "A document is constructed"
      def source = "[noparse][b]Bold[/b][/noparse]"
      def document = new BBCodeParser().buildDocument(source, attributes)

    then:
      document.children.size() == 1
      document.children.get(0) instanceof TagNode
      def tag = (TagNode) document.children.get(0)
      tag.children.size() == 1
      tag.children.get(0) instanceof TextNode
      def text = (TextNode) tag.children.get(0)
      text.getBody() == "[b]Bold[/b]"

    and:
      document.offsets == [new Pair<>(0, 9), new Pair<>(20, 10)] as TreeSet
      document.attributeOffsets.empty
  }

  def "noparse tag with bad body"() {

    when: "A document is constructed"
      def source = "[noparse]abc[def[/noparse]"
      def document = new BBCodeParser().buildDocument(source, attributes)

    then:
      document.children.size() == 1
      document.children.get(0) instanceof TagNode
      def tag = (TagNode) document.children.get(0)
      tag.children.size() == 1
      tag.children.get(0) instanceof TextNode
      def text = (TextNode) tag.children.get(0)
      text.getBody() == "abc[def"

    and:
      document.offsets == [new Pair<>(0, 9), new Pair<>(16, 10)] as TreeSet
      document.attributeOffsets.empty
  }

  def "foo[bar] = \"Hello World\";"() {

    when: "A document is constructed"
      def source = "foo[bar] = \"Hello World\";"
      def document = new BBCodeParser().buildDocument(source, attributes)

    then:
      document.children.size() == 1
      document.children.get(0) instanceof TextNode

      def text1 = (TextNode) document.children.get(0)
      text1.getBody() == "foo[bar] = \"Hello World\";"

    and:
      document.offsets.empty
      document.attributeOffsets.empty
  }

  def "noparse tag with embedded noparse"() {

    when: "A document is constructed"
      def source = "[noparse]Example: [noparse] String test = null; [/noparse][/noparse]"
      def document = new BBCodeParser().buildDocument(source, attributes)

    then:
      document.children.size() == 1
      document.children.get(0) instanceof TagNode
      def tag = (TagNode) document.children.get(0)
      tag.children.size() == 1
      tag.children.get(0) instanceof TextNode
      def text = (TextNode) tag.children.get(0)
      text.getBody() == "Example: [noparse] String test = null; [/noparse]"

    and:
      document.offsets == [new Pair<>(0, 9), new Pair<>(58, 10)] as TreeSet
      document.attributeOffsets.empty
  }

  // TODO - END can be removed - keeping here now until I complete work on the parser

  def "Build a new document using BBCode - 1 Tag"() {

    when: "A document is constructed"
      def source = "[b]Text[/b]"
      /*            ^      ^      */
      /*            0,3    7,4    */
      def document = new BBCodeParser().buildDocument(source, attributes)

    then: "no exceptions thrown"
      notThrown Exception

    and: "result should not be null"
      document != null

    and:
      document.children.size() == 1
      ((TagNode) document.children.get(0)).children.size() == 1

    and: "offsets to tags should be correct"
      document.offsets == [new Pair<>(0, 3), new Pair<>(7, 4)] as TreeSet
  }

  def "Build a new document using BBCode - Two Tags"() {

    when: "A document is constructed"
      def source = "[b]Bold[/b][i]Italic[/i]"
      /*            ^      ^      */
      /*            0,3    7,4    */
      def document = new BBCodeParser().buildDocument(source, attributes)

    then: "no exceptions thrown"
      notThrown Exception

    and: "result should not be null"
      document != null

    and:
      document.children.size() == 2
      ((TagNode) document.children.get(0)).children.size() == 1
      ((TagNode) document.children.get(1)).children.size() == 1

    and: "offsets to tags should be correct"
      document.offsets == [new Pair<>(0, 3), new Pair<>(7, 4), new Pair<>(11, 3), new Pair<>(20, 4)] as TreeSet
  }

  def "Build a new document using BBCode - 1 Tag with nested Tag"() {

    when: "A document is constructed"
      def source = "[b][i]Italic[/i][/b]"
      /*               ^        ^   ^      */
      /*            0,3         12,4       */
      /*               3,3          16,4   */
      def document = new BBCodeParser().buildDocument(source, attributes)

    then: "no exceptions thrown"
      notThrown Exception

    and: "result should not be null"
      document != null

    and: "only a single child node should exist on the document"
      document.children.size() == 1

    and: "the first child should be a bold tag with a single child with correct name and offsets"
      def bold = (TagNode) document.children.get(0)

      bold.begin == 0
      bold.nameEnd == 2
      bold.bodyBegin == 3
      bold.bodyEnd == 16
      bold.end == 20

      bold.getName() == "b"
      bold.children.size() == 1

    and: "the bold child node should be italic with a single text node, correct offsets and name"
      def italic = (TagNode) bold.children.get(0)

      italic.begin == 3
      italic.nameEnd == 5
      italic.bodyBegin == 6
      italic.bodyEnd == 12
      italic.end == 16

      italic.getName() == "i"
      italic.children.size() == 1

      italic.children.get(0) instanceof TextNode
      def text = (TextNode) italic.children.get(0)
      text.getBody() == "Italic"

    and: "offsets to tags should be correct"
      document.offsets == [new Pair<>(0, 3), new Pair<>(3, 3), new Pair<>(12, 4), new Pair<>(16, 4)] as TreeSet
  }

  def "Build a new document using BBCode with uppercase tags"() {

    when: "A document is constructed"
      def source = "[B]Text[/B]"
      def document = new BBCodeParser().buildDocument(source, attributes)
      def result = new BBCodeToHTMLTransformer().transform(document, transformPredicate, null, null)

    then: "basic offsets are correct"
      def tag = document.childTagNodes.get(0)
      tag.begin == 0
      tag.nameEnd == 2
      tag.bodyBegin == 3
      tag.bodyEnd == 7
      tag.end == 11

    and: "the document is transformed properly"
      result == "<strong>Text</strong>"
  }

  def "Build a new document using BBCode with two tags and mixed case"() {

    when: "A document is constructed"
      def source = "[b]Hello[/B] [I]World[/i]"
      def document = new BBCodeParser().buildDocument(source, attributes)

    then: "number of nodes is correct"
      document.children.size() == 3

    then: "basic offsets are correct"
      def bold = document.childTagNodes.get(0)
      bold.begin == 0
      bold.nameEnd == 2
      bold.bodyBegin == 3
      bold.bodyEnd == 8
      bold.end == 12

      def italic = document.childTagNodes.get(1)
      italic.begin == 13
      italic.nameEnd == 15
      italic.bodyBegin == 16
      italic.bodyEnd == 21
      italic.end == 25

    and: "the document is transformed properly"
      def result = new BBCodeToHTMLTransformer().transform(document, transformPredicate, null, null)
      result == "<strong>Hello</strong> <em>World</em>"
  }

  def "Build a new document using BBCode in a char array"() {

    when: "A document is constructed"
      def source = ['[', 'b', ']', 'T', 'e', 'x', 't', '[', '/', 'b', ']'] as char[]
      def document = new BBCodeParser().buildDocument(source, attributes)

    then: "no exceptions thrown"
      notThrown Exception

    and: "result should not be null"
      document != null
  }

  def "Parse BBCode with simple attributes verify offsets"() {

    when: "A document is constructed"
      def source = "[url=\"http://www.google.com\"]http://www.google.com[/url]"
      /*            ^     ^                                             ^       */
      /* offsets    0,29                                                50,6    */
      /* attributes       6,12                                                  */
      def document = new BBCodeParser().buildDocument(source, attributes)

    then: "offsets to tags should be correct"
      document.offsets == [new Pair<>(0, 29), new Pair<>(50, 6)] as TreeSet

    and: "attribute and the offsets should be correct"
      document.attributeOffsets == [new Pair<>(6, 21)] as TreeSet
      def url = (TagNode) document.children.get(0)
      url.attribute == "http://www.google.com"
  }


  def "Parse BBCode with a complex attribute for a tag that only uses a simple attribute verify offsets"() {

    when: "A document is constructed"
      def source = "[font foo=\"abc\"] bar [/font]"
      /*            ^           ^           ^                                         */
      /* offsets    0,16                   21,7                                       */
      /* attributes            11,3                                                   */
      def document = new BBCodeParser().buildDocument(source, attributes)

    then: "offsets to tags should be correct"
      document.offsets == [new Pair<>(0, 16), new Pair<>(21, 7)] as TreeSet

    and: "attribute offsets should be correct"
      document.attributeOffsets == [new Pair<>(11, 3)] as TreeSet

    and: "and so should the attribute key value pair"
      def font = (TagNode) document.children.get(0)
      font.attributes.containsKey("foo")
      font.attributes.get("foo") == "abc"
  }

  def "Parse BBCode with complex attributes verify offsets"() {

    when: "A document is constructed"
      def source = "Hello [font size=\"10\" family=\"a fucker yo\"] World! [/font]"
      /*                  ^            ^            ^                      ^        */
      /* offsets          6,37                                             51,7     */
      /* attributes                    18,2         30,11                           */
      def document = new BBCodeParser().buildDocument(source, attributes)

    then: "offsets to tags should be correct"
      document.offsets == [new Pair<>(6, 37), new Pair<>(51, 7)] as TreeSet

    and: "attribute offsets should be correct"
      document.attributeOffsets == [new Pair<>(18, 2), new Pair<>(30, 11)] as TreeSet
  }

  def "Parse BBCode with a single tag that has no closing tag and no parent enclosing tag"() {

    when: "A document is constructed"
      def document = new BBCodeParser().buildDocument("[*] tester", attributes)

    then: "no exception should be thrown"
      notThrown Exception

    and: "the document should contain a single child tagNode"
      document.children.size() == 1
      document.children.get(0) instanceof TagNode
      def tag = (TagNode) document.children.get(0)
      tag.getName() == "*"
      !tag.hasClosingTag()
      tag.children.size() == 1

    and: "the tagNode should have a single child textNode"
      tag.children.get(0) instanceof TextNode
      def text = (TextNode) tag.children.get(0)
      text.getBody() == " tester"
  }

  def "Parse BBCode that does not require a closing tag in a parent tag that is not a list"() {

    when: "A document is constructed"
      def source = "[b][*]test1 [*]test2[/b]"
      /*          ^  ^        ^       ^                                             */
      /* offsets  0,3 3,3     12,3    20,4                                          */
      /* attributes                                                                 */
      def document = new BBCodeParser().buildDocument(source, attributes)

    then:
      document.offsets == [new Pair<>(0, 3), new Pair<>(3, 3), new Pair<>(12, 3), new Pair<>(20, 4)] as TreeSet

  }

  def "Parse BBCode that does not require a closing tag with no body"() {

    when: "A document is constructed"
      def source = "[list][*][/list]"
      /*            ^     ^  ^                                                      */
      /* offsets    0,6  6,3  9,7                                                   */
      /* attributes                                                                 */
      def document = new BBCodeParser().buildDocument(source, attributes)

    then:
      document.offsets == [new Pair<>(0, 6), new Pair<>(6, 3), new Pair<>(9, 7)] as TreeSet

  }

  def "Parse BBCode that does not require a closing tag with no body and text before bullet tag"() {

    when: "A document is constructed"
      def source = "[list]test[*][/list]"
      /*            ^         ^  ^                                                  */
      /* offsets    0,6       10,3  13,7                                            */
      /* attributes                                                                 */
      def document = new BBCodeParser().buildDocument(source, attributes)

    then:
      document.offsets == [new Pair<>(0, 6), new Pair<>(10, 3), new Pair<>(13, 7)] as TreeSet

  }

  def "Parse BBCode that has embedded tags of the same type"() {

    when: "A document is constructed"
      def source = "[quote][quote][quote]Hot pocket in a hot pocket[/quote][/quote][/quote]"
      /*            ^      ^      ^                                ^       ^       ^      */
      /* offsets    0,7    7,7  14,7                               47,8   55,8     63,8   */
      /* attributes                                                                       */
      def document = new BBCodeParser().buildDocument(source, attributes)

    then:
      document.offsets == [new Pair<>(0, 7), new Pair<>(7, 7), new Pair<>(14, 7), new Pair<>(47, 8), new Pair<>(55,
                                                                                                                8), new Pair<>(
          63, 8)] as TreeSet

  }

  def "Simple attribute"() {

    when: "A document is constructed"
      def source = "[color=blue]blah[/color]"
      /*            ^      ^        ^                                                     */
      /* offsets    0,12            16,8                                                  */
      /* attributes        7,4                                                              */
      def document = new BBCodeParser().buildDocument(source, attributes)

    then:
      document.offsets == [new Pair<>(0, 12), new Pair<>(16, 8)] as TreeSet

    and: "attribute offsets should be correct"
      document.attributeOffsets == [new Pair<>(7, 4)] as TreeSet
  }

  def "Complex attributes with an extra space"() {

    when: "A document is constructed"
      def source = "[font  size=5]blah[/font]"
      /*            ^      ^          ^                                                     */
      /* offsets    0,14            18,7                                                    */
      /* attributes             12,1                                                        */
      def document = new BBCodeParser().buildDocument(source, attributes)

    then:
      document.offsets == [new Pair<>(0, 14), new Pair<>(18, 7)] as TreeSet

    and: "attribute offsets should be correct"
      document.attributeOffsets == [new Pair<>(12, 1)] as TreeSet
  }

  def "Complex attributes with an extra space and a single quote"() {

    when: "A document is constructed"
      def source = "[font  size=\'5\']blah[/font]"
      /*            ^           ^         ^                                                 */
      /* offsets    0,16                  18,7                                              */
      /* attributes             13,1                                                        */
      def document = new BBCodeParser().buildDocument(source, attributes)

    then:
      document.offsets == [new Pair<>(0, 16), new Pair<>(20, 7)] as TreeSet

    and: "attribute offsets should be correct"
      document.attributeOffsets == [new Pair<>(13, 1)] as TreeSet
  }

}
