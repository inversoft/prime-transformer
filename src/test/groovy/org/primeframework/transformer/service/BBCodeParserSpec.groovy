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
      document.offsets == [new Pair<>(0,3), new Pair<>(7,4)] as TreeSet
  }

  def "Build a new document using BBCode in a char array"() {

    when: "A document is constructed using the builder with all parameters"
      def source = ['[', 'b', ']', 'T', 'e', 'x', 't', '[', '/', 'b', ']'] as char[]
      def document = ParserFactory.newBBCodeParser().buildDocument(new DocumentSource(source))

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

      def document = ParserFactory.newBBCodeParser().buildDocument(new DocumentSource(source))

    then: "offsets to tags should be correct"
      document.offsets == [new Pair<>(0,29), new Pair<>(50,6)] as TreeSet

    and: "attribute offsets should be correct"
      document.attributeOffsets == [new Pair<>(6,21)] as TreeSet
  }

  def "Parse BBCode with complex attributes verify offsets"() {

    when: "A document is constructed using the builder with all parameters"
      def source = "Hello [font size=\"10\" family=\"a fucker yo\"] World! [/font]"
      /*                  ^            ^            ^                      ^        */
      /* offsets          6,37                                             51,7     */
      /* attributes                    18,12        30,11                           */

      def document = ParserFactory.newBBCodeParser().buildDocument(new DocumentSource(source))

    then: "offsets to tags should be correct"
      document.offsets == [new Pair<>(6,37), new Pair<>(51,7)] as TreeSet

    and: "attribute offsets should be correct"
      document.attributeOffsets == [new Pair<>(18,2), new Pair<>(30,11)] as TreeSet
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
      document.offsets ==  [new Pair<>(8, 43), new Pair<>(88, 7)] as TreeSet

    and: "attribute offsets are correct"
      document.attributeOffsets == [new Pair<>(20,15), new Pair<>(45, 4)] as TreeSet

  }
}
