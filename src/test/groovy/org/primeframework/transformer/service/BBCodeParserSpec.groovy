package org.primeframework.transformer.service

import org.primeframework.transformer.domain.DocumentSource
import spock.lang.Specification

public class BBCodeParserSpec extends Specification {

  def "Build a new document using BBCode"() {

    when: "A document is constructed using the builder with all parameters"
    def source = "[b]Text[/b]"
    def document = ParserFactory.newBBCodeParser().buildDocument(new DocumentSource(source))

    then: "no exceptions thrown"
    notThrown Exception

    and: "result should not be null"
    document != null
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
}
