package org.primeframework.transformer.service

import org.primeframework.transformer.domain.DocumentSource
import org.primeframework.transformer.domain.Pair
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

}
