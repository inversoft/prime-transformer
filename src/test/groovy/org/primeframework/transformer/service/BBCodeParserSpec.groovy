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
      /* attributes                    18,12        30,11                           */
      def document = new BBCodeParser().buildDocument(new DocumentSource(source))

    then: "offsets to tags should be correct"
      document.offsets == [new Pair<>(6, 37), new Pair<>(51, 7)] as TreeSet

    and: "attribute offsets should be correct"
      document.attributeOffsets == [new Pair<>(18, 2), new Pair<>(30, 11)] as TreeSet
  }
}
