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
import org.primeframework.transformer.domain.TagNode
import spock.lang.Specification

/**
 * @author Daniel DeGroff
 */
class TextTransformerSpec extends Specification {

  def "BBCode to Text - simple"() {

    expect: "when Text transformer is called with bbcode only text is returned"
      def document = new BBCodeParser().buildDocument(new DocumentSource(bbCode))
      new TextTransformer().transform(document).result == text;

    where:
      text                            | bbCode
      "boldNo format.bold"            | "[b]bold[/b]No format.[b]bold[/b]"
      " bold  No format.  bold "      | "[b] bold [/b] No format. [b] bold [/b]"
      "www.google.com"                | "[url=\"http://www.google.com\"]www.google.com[/url]"
      "bolditalicunderlineitalicbold" | "[b]bold[i]italic[u]underline[/u]italic[/i]bold[/b]"
  }

  def "BBCode to Text and verify offsets"() {

    when: "bbcode is transformed"
      def document = new BBCodeParser().buildDocument(new DocumentSource("z [b]abc defg [/b]hijk [ul][*]lmn opqr[*][/ul]"))
      /*                                                                    ^           ^        ^   ^          ^  ^       */
      /*                                                                    2          14       23  27         38 41       */
      def transFormResult = new TextTransformer().transform(document);

    then: "the document is transformed properly"
      transFormResult.result == "z abc defg hijk lmn opqr"

    and: "offsets are correct"
      def expectedOffsets = [new Pair<>(2, 3), new Pair<>(14, 4), new Pair<>(23, 4), new Pair<>(27, 3), new Pair<>(38, 3), new Pair<>(41, 5)] as TreeSet
      document.offsets == expectedOffsets;

  }

  def "BBCode to Text and verify offsets and attribute offsets with complex attributes"() {

    when: "bbcode is transformed"
      def document = new BBCodeParser().buildDocument(new DocumentSource("Example [code type=\"see the java oo\" syntax=\"java\"] System.out.println(\"Hello World!\"); [/code] "))
      /*                                                                          ^            ^                           ^                                            ^            */
      /*  offsets                                                                 8,43                                                                                  88,7         */
      /*  attributeOffset                                                                      20,16                       46,4                                                      */
      def transFormResult = new TextTransformer().transform(document);

    then: "the document is transformed properly"
      transFormResult.result == "Example  System.out.println(\"Hello World!\");  "

    and: "offsets are correct"
      document.offsets == [new Pair<>(8, 43), new Pair<>(88, 7)] as TreeSet

    and: "attribute offsets are correct"
      document.attributeOffsets == [new Pair<>(20, 15), new Pair<>(45, 4)] as TreeSet
  }

  def "BBCode to Text with a tag node that is set to transform false"() {

    when: "bbcode is parsed"
      def document = new BBCodeParser().buildDocument(new DocumentSource("[foo bar=\"blah blah\"]Some ordinary text.[/foo] [font=\"verdana\"]Hello[/font]"))

    and: "you set one of the tag nodes not to transform"
      ((TagNode) document.children.get(0)).transform = false;

    and: "transform is called"
      def transformedResult = new TextTransformer().transform(document)

    then: "the tag node that has been set to transform false should be plain text"
      transformedResult.result == "[foo bar=\"blah blah\"]Some ordinary text.[/foo] Hello"
  }

}
