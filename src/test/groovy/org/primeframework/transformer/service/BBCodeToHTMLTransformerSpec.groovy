package org.primeframework.transformer.service

import org.primeframework.transformer.domain.DocumentSource
import org.primeframework.transformer.domain.ParserException
import org.primeframework.transformer.domain.TransformerException
import org.primeframework.transformer.domain.TransformerRuntimeException
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

    def "No FreeMarker template for tag with strict mode disabled"() {

        when: "A BBCodeParser is setup and a template has not been provided for a tag"
            def document = bbCodeParser.buildDocument(new DocumentSource("[unknown]Testing[/unknown]"))

        and: "transform is bbCodeTransformer"
            def html = bbCodeToFreeMarkerTransformer.init().transform(document);

        then: "the output should should equal the input"
            html == "[unknown]Testing[/unknown]"
    }

    def "No FreeMarker template for tag with strict mode enabled"() {

        when: "A BBCodeParser is setup and a template has not been provided for a tag"
            def document = bbCodeParser.buildDocument(new DocumentSource("[unknown]Testing[/unknown]"))

        and: "transform is bbCodeTransformer"
            def html = bbCodeToFreeMarkerTransformer.setStrict(true) transform(document);

        then: "an exception should be thrown"
            thrown TransformerException

        and: "html should be null"
            html == null
    }

    def "Call transform without calling init"() {

        when: "A BBCodeParser is setup with valid BBCode"
            def document = bbCodeParser.buildDocument(new DocumentSource("[b]Testing[/b]"))

        and: "transform is bbCodeTransformer without calling init()"
            def html = bbCodeToFreeMarkerTransformer.transform(document);

        then: "an exception should be thrown"
            thrown TransformerRuntimeException

        and: "html should be null"
            html == null
    }

    def "No FreeMarker template for tag with strict mode enabled in the constructor"() {

        when: "A BBCodeParser is setup and a template has not been provided for a tag"
            def document = bbCodeParser.buildDocument(new DocumentSource("[unknown]Testing[/unknown]"))

        and: "transform is bbCodeTransformer"
            def html = new BBCodeToHTMLTransformer(true).init().transform(document);

        then: "an exception should be thrown"
            thrown TransformerException

        and: "html should be null"
            html == null
    }

    def "No closing BBCode tag"() {

        when: "A BBCode string with a missing closing tag is parsed"
            def document = bbCodeParser.buildDocument(new DocumentSource("[b]Testing"));

        then: "a parse exception is thrown"
            thrown ParserException

        and: "document is null"
            document == null

    }

    //@Unroll("BBCode to HTML : #html")
    def "BBCode to HTML - simple"() {

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
            "<table><tr><th>Header 1</th></tr><tr><td>Row1 Column1</td></tr></table>"                                               | "[table][tr][th]Header 1[/th][/tr][tr][td]Row1 Column1[/td][/tr][/table]"
            "<ol><li>item 1</li></ol>"                                                                                              | "[ol][li]item 1[/li][/ol]"
            "<span style=\"text-decoration: line-through\">Strike</span>"                                                           | "[s]Strike[/s]"
            "<u>Underline</u>"                                                                                                      | "[u]Underline[/u]"
            "<a href=\"http://foo.com\">http://foo.com</a>"                                                                         | "[url=http://foo.com]http://foo.com[/url]"
            "<a href=\"http://foo.com\">foo.com</a>"                                                                                | "[url=http://foo.com]foo.com[/url]"
            "Testing []"                                                                                                            | "Testing []"
            "<a href=\"mailto:barney@rubble.com\">barney</a>"                                                                       | "[email=barney@rubble.com]barney[/email]"
            "<a href=\"mailto:barney@rubble.com\">barney@rubble.com</a>"                                                            | "[email=barney@rubble.com]barney@rubble.com[/email]"
            "Text <sub>subscript</sub> Other text"                                                                                  | "Text [sub]subscript[/sub] Other text"
            "Text <sup>superscript</sup> Other text"                                                                                | "Text [sup]superscript[/sup] Other text"
            "Testing <div>[b] Testing [/b] [url]http://www.google.com[/url]</div> Text"                                             | "Testing [noparse][b] Testing [/b] [url]http://www.google.com[/url][/noparse] Text"
            "Test color is <span style=\"color: red\">red</span>."                                                                  | "Test color is [color=red]red[/color]."
            "Test color is <span style=\"color: #FFF\">white</span>."                                                               | "Test color is [color=\"#FFF\"]white[/color]."
            "Test color is <span style=\"color: black\">black</span>."                                                              | "Test color is [color=\"black\"]black[/color]."
            "<div align=\"left\">Left</div>"                                                                                        | "[left]Left[/left]"
            "<div align=\"center\">Center</div>"                                                                                    | "[center]Center[/center]"
            "<div align=\"right\">Right</div>"                                                                                      | "[right]Right[/right]"
            "<span style=\"font-family: monospace\">mono</span>"                                                                    | "[font=monospace]mono[/font]"

    }

    //@Unroll("BBCode to HTML : #fileName")
    def "BBCode to HTML - complex"() {

        expect: "when HTML transformer is called with bbcode properly formatted HTML is returned"

            def bbcode = this.getClass().getResourceAsStream("/org/primeframework/transformer/bbcode/" + fileName).getText();
            def html = this.getClass().getResourceAsStream("/org/primeframework/transformer/html/" + fileName).getText();

            def document = bbCodeParser.buildDocument(new DocumentSource(bbcode))
            bbCodeToFreeMarkerTransformer.transform(document).replaceAll("<br>", "").replaceAll("\\s+", "") == html.replaceAll("\\s+", "")

        where:
            fileName | _
            "other"  | _
            "code"   | _
            "image"  | _
            "size"   | _
            "quote"  | _


    }

}
