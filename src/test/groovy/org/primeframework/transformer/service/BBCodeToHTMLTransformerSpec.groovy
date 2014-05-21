package org.primeframework.transformer.service

import org.primeframework.transformer.domain.DocumentSource
import org.primeframework.transformer.domain.ParserException
import spock.lang.Shared
import spock.lang.Specification

public class BBCodeToHTMLTransformerSpec extends Specification {

    @Shared
    def bbCodeParser;
    @Shared
    def bbCodeToFreeMarkerTransformer;

    def setupSpec() {
        bbCodeParser = ParserFactory.newBBCodeParser()
        bbCodeToFreeMarkerTransformer = TransformerFactory.newBBCodeToHTMLTransformer()
    }

    def "No FreeMarker template for tag"() {

        when: "A BBCodeParser is setup and a template has not been provided for a tag"
            def document = bbCodeParser.buildDocument(new DocumentSource("[unknown]Testing[/unknown]"))

        and: "transform is bbCodeTransformer"
            def html = bbCodeToFreeMarkerTransformer.transform(document);

        then: "the output should should equal the input"
            html == "[unknown]Testing[/unknown]"
    }

    def "No closing BBCode tag"() {

        when: "A BBCode string with a missing closing tag is parsed"
            def document = bbCodeParser.buildDocument(new DocumentSource("[b]Testing"));

        then: "a parse exception is thrown"
            thrown ParserException

        and: "document is null"
            document == null

    }

    //@Unroll("BBCode to HTML : #bbCode")
    def "BBCode to HTML"() {

        expect: "when HTML transformer is called with bbcode properly formatted HTML is returned"
            def document = bbCodeParser.buildDocument(new DocumentSource(bbCode))
            bbCodeToFreeMarkerTransformer.transform(document).replaceAll(" ", "").replaceAll("<br>", "").replaceAll("&nbsp;", "") == html.replaceAll(" ", "");

        where:
            html                                                                                                                    | bbCode
            "<strong>bold</strong> No format. <strong>bold</strong>"                                                                | "[b]bold[/b]No format.[b]bold[/b]"
            "<strong>bold <em>italic embedded</em> bold</strong>"                                                                   | "[b]bold[i]italic embedded[/i]bold[/b]"
            "<a href=\"http://foo.com\">http://foo.com</a>"                                                                         | "[url]http://foo.com[/url]"
            "<ul><li>item1</li><li>item2</li></ul>"                                                                                 | "[list][*]item1[*]item2[/list]"
            "<ul><li>item1</li><li>item2</li></ul>"                                                                                 | "[list][li]item1[/li][li]item2[/li][/list]"
            "<ul><li>1</li><li>2</li></ul>"                                                                                         | "[list][*]1[*]2[/list]"
            "<ul><li><strong><em>1</em></strong></li><li><strong><em>2</em></strong></li></ul>"                                     | "[list][*][b][i]1[/i][/b][*][b][i]2[/i][/b][/list]"
            "<table><tr><td>Row1 Column1</td><td>Row1 Column2</td></tr><tr><td>Row2 Column1</td><td>Row2 Column2</td></tr></table>" | "[table][tr][td]Row1 Column1[/td][td]Row1 Column2[/td][/tr][tr][td]Row2 Column1[/td][td]Row2 Column2[/td][/tr][/table]"
            "<ol><li>item 1</li></ol>"                                                                                              | "[ol][li]item 1[/li][/ol]"
            "<pre>String test = \"test\";</pre>"                                                                                    | "[code]String test = \"test\";[/code]"
            "<div class=\"quote\">Quote</div>"                                                                                      | "[quote]Quote[/quote]"
            "<span style=\"text-decoration: line-through\">Strike</span>"                                                           | "[s]Strike[/s]"
            "<span style=\"text-decoration: underline\">Underline</span>"                                                           | "[u]Underline[/u]"
            "<a href=\"http://foo.com\">http://foo.com</a>"                                                                         | "[url=http://foo.com]http://foo.com[/url]"
            "<a href=\"http://foo.com\">foo.com</a>"                                                                                | "[url=http://foo.com]foo.com[/url]"
            "<pre>public void main(String[]) {System.out.println(\"Hello World!\");}</pre>"                                         | "[code]public void main(String[]) {\n\tSystem.out.println(\"Hello World!\");\n}[/code]"
            "<pre> [b] bold text [/b] </pre>"                                                                                       | "[code][b]bold text[/b][/code]"
            "Testing []"                                                                                                            | "Testing []"
            "<a href=\"mailto:barney@rubble.com\">barney</a>"                                                                       | "[email=barney@rubble.com]barney[/email]"
            "<a href=\"mailto:barney@rubble.com\">barney@rubble.com</a>"                                                            | "[email=barney@rubble.com]barney@rubble.com[/email]"


    }
}
