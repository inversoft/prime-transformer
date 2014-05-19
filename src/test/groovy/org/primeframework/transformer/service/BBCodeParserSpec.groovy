package org.primeframework.transformer.service

import org.primeframework.transformer.domain.DocumentSource
import spock.lang.Specification

public class BBCodeParserSpec extends Specification {

    def "Build a new document using BBCode"() {

        when: "A document is constructed using the builder with all parameters"
            def document = ParserFactory.newBBCodeParser().buildDocument(new DocumentSource("[b]Text[/b]"));

        then: "no exceptions thrown"
            notThrown Exception

        and: "result should not be null"
            document != null

    }

}
