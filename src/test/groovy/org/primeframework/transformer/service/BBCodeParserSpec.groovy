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
import org.primeframework.transformer.domain.DocumentSource
import org.primeframework.transformer.domain.Pair
import org.primeframework.transformer.domain.ParserException
import spock.lang.Specification

public class BBCodeParserSpec extends Specification {

  def "Build a new document using BBCode"() {

    when: "A document is constructed"
      def source = "[b]Text[/b]"
      /*            ^      ^      */
      /*            0,3    7,4    */
      def document = new BBCodeParser().buildDocument(new DocumentSource(source))

    then: "no exceptions thrown"
      notThrown Exception

    and: "result should not be null"
      document != null

    and: "offsets to tags should be correct"
      document.offsets == [new Pair<>(0, 3), new Pair<>(7, 4)] as TreeSet
  }

  def "Build a new document using BBCode with uppercase tags"() {

    when: "A document is constructed"
      def source = "[B]Text[/B]"
      def transFormResult = new BBCodeToHTMLTransformer().init().transform(new BBCodeParser().buildDocument(new DocumentSource(source)))

    then: "the document is transformed properly"
      transFormResult.result == "<strong>Text</strong>"
  }

  def "Build a new document using BBCode with mixed case tags"() {

    when: "A document is constructed"
      def source = "[b]Text[/B] [I]world[/i]"
      def transFormResult = new BBCodeToHTMLTransformer().init().transform(new BBCodeParser().buildDocument(new DocumentSource(source)))

    then: "the document is transformed properly"
      transFormResult.result == "<strong>Text</strong> <em>world</em>"
  }

  def "Build a new document using BBCode in a char array"() {

    when: "A document is constructed using the builder with all parameters"
      def source = ['[', 'b', ']', 'T', 'e', 'x', 't', '[', '/', 'b', ']'] as char[]
      def document = new BBCodeParser().buildDocument(new DocumentSource(source))

    then: "no exceptions thrown"
      notThrown Exception

    and: "result should not be null"
      document != null
  }

  def "Parse BBCode with simple attributes verify offsets"() {

    when: "A document is constructed using the builder with all parameters"
      def source = "[url=\"http://www.google.com\"]http://www.google.com[/url]"
      /*            ^     ^                                             ^       */
      /* offsets    0,29                                                50,6    */
      /* attributes       6,12                                                  */
      def document = new BBCodeParser().buildDocument(new DocumentSource(source))

    then: "offsets to tags should be correct"
      document.offsets == [new Pair<>(0, 29), new Pair<>(50, 6)] as TreeSet

    and: "attribute offsets should be correct"
      document.attributeOffsets == [new Pair<>(6, 21)] as TreeSet
  }


  def "Parse BBCode with a complex attribute for a tag that only uses a simple attribute verify offsets"() {

    when: "A document is constructed using the builder with all parameters"
      def source = "[font foo=\"abc\"] bar [/font]"
      /*            ^           ^           ^                                         */
      /* offsets    0,16                   21,7                                       */
      /* attributes            11,3                                                   */
      def document = new BBCodeParser().buildDocument(new DocumentSource(source))

    then: "offsets to tags should be correct"
      document.offsets == [new Pair<>(0, 16), new Pair<>(21, 7)] as TreeSet

    and: "attribute offsets should be correct"
      document.attributeOffsets == [new Pair<>(11, 3)] as TreeSet
  }

  def "Parse BBCode with complex attributes verify offsets"() {

    when: "A document is constructed using the builder with all parameters"
      def source = "Hello [font size=\"10\" family=\"a fucker yo\"] World! [/font]"
      /*                  ^            ^            ^                      ^        */
      /* offsets          6,37                                             51,7     */
      /* attributes                    18,2         30,11                           */
      def document = new BBCodeParser().buildDocument(new DocumentSource(source))

    then: "offsets to tags should be correct"
      document.offsets == [new Pair<>(6, 37), new Pair<>(51, 7)] as TreeSet

    and: "attribute offsets should be correct"
      document.attributeOffsets == [new Pair<>(18, 2), new Pair<>(30, 11)] as TreeSet
  }

  def "Parse BBCode with a single tag that has no closing tag and no parent enclosing tag"() {

    when: "A document is constructed using the builder with all parameters"
      new BBCodeParser().buildDocument(new DocumentSource("[*] tester"))

    then:
      thrown ParserException
  }

  def "Parse BBCode that does not require a closing tag in a parent tag that is not a list"() {

    when: "A document is constructed using the builder with all parameters"
    def source = "[b][*]test1 [*]test2[/b]"
      /*          ^  ^        ^       ^                                             */
      /* offsets  0,3 3,3     12,3    20,4                                          */
      /* attributes                                                                 */
      def document = new BBCodeParser().buildDocument(new DocumentSource(source))

    then:
      document.offsets == [new Pair<>(0, 3), new Pair<>(3, 3), new Pair<>(12,3), new Pair<>(20,4)] as TreeSet

  }

  def "Parse BBCode that does not require a closing tag with no body"() {

    when: "A document is constructed using the builder with all parameters"
      def source = "[list][*][/list]"
      /*            ^     ^  ^                                                      */
      /* offsets    0,6  6,3  9,7                                                   */
      /* attributes                                                                 */
      def document = new BBCodeParser().buildDocument(new DocumentSource(source))

    then:
      document.offsets == [new Pair<>(0, 6), new Pair<>(6, 3), new Pair<>(9,7)] as TreeSet

  }

  def "Parse BBCode that does not require a closing tag with no body and text before bullet tag"() {

    when: "A document is constructed using the builder with all parameters"
      def source = "[list]test[*][/list]"
      /*            ^         ^  ^                                                  */
      /* offsets    0,6       10,3  13,7                                            */
      /* attributes                                                                 */
      def document = new BBCodeParser().buildDocument(new DocumentSource(source))

    then:
      document.offsets == [new Pair<>(0, 6), new Pair<>(10, 3), new Pair<>(13,7)] as TreeSet

  }

  def "Parse BBCode that has embedded tags of the same type"() {

    when: "A document is constructed using the builder with all parameters"
      def source = "[quote][quote][quote]Hot pocket in a hot pocket[/quote][/quote][/quote]"
      /*            ^      ^      ^                                ^       ^       ^      */
      /* offsets    0,7    7,7  14,7                               47,8   55,8     63,8   */
      /* attributes                                                                       */
      def document = new BBCodeParser().buildDocument(new DocumentSource(source))

    then:
      document.offsets == [new Pair<>(0, 7), new Pair<>(7, 7), new Pair<>(14,7), new Pair<>(47,8), new Pair<>(55,8), new Pair<>(63,8)] as TreeSet

  }

  def "Simple attribute"() {

    when: "A document is constructed using the builder with all parameters"
      def source = "[color=blue]blah[/color]"
      /*            ^      ^        ^                                                     */
      /* offsets    0,12            16,8                                                  */
      /* attributes        7,4                                                              */
      def document = new BBCodeParser().buildDocument(new DocumentSource(source))

    then:
      document.offsets == [new Pair<>(0, 12), new Pair<>(16,8)] as TreeSet

    and: "attribute offsets should be correct"
      document.attributeOffsets == [new Pair<>(7, 4)] as TreeSet

  }

  def "Complex attributes with an extra space"() {

    when: "A document is constructed using the builder with all parameters"
      def source = "[font  size=5]blah[/font]"
      /*            ^      ^          ^                                                     */
      /* offsets    0,14            18,7                                                    */
      /* attributes             12,1                                                        */
      def document = new BBCodeParser().buildDocument(new DocumentSource(source))

    then:
      document.offsets == [new Pair<>(0, 14), new Pair<>(18,7)] as TreeSet

    and: "attribute offsets should be correct"
      document.attributeOffsets == [new Pair<>(12, 1)] as TreeSet

  }

  def "Complex attributes with an extra space and a single quote"() {

    when: "A document is constructed using the builder with all parameters"
      def source = "[font  size=\'5\']blah[/font]"
      /*            ^           ^         ^                                                 */
      /* offsets    0,16                  18,7                                              */
      /* attributes             13,1                                                        */
      def document = new BBCodeParser().buildDocument(new DocumentSource(source))

    then:
      document.offsets == [new Pair<>(0, 16), new Pair<>(20,7)] as TreeSet

    and: "attribute offsets should be correct"
      document.attributeOffsets == [new Pair<>(13, 3)] as TreeSet

  }

  def "Complex attributes with lots of extra space"() {

    when: "A document is constructed using the builder with all parameters"
      def source = "[font      size=5]blah[/font]"
      /*            ^          ^          ^                                                 */
      /* offsets    0,18                  22,7                                              */
      /* attributes            16,1                                                         */
      def document = new BBCodeParser().buildDocument(new DocumentSource(source))

    then:
      document.offsets == [new Pair<>(0, 18), new Pair<>(22,7)] as TreeSet

    and: "attribute offsets should be correct"
      document.attributeOffsets == [new Pair<>(16, 1)] as TreeSet

  }
}
