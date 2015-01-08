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

import org.primeframework.transformer.domain.Document
import org.primeframework.transformer.domain.TagAttributes
import org.primeframework.transformer.domain.TagNode
import org.primeframework.transformer.domain.TextNode
import spock.lang.Shared
import spock.lang.Specification
import spock.lang.Unroll

import java.util.function.Predicate

public class BBCodeToHTMLTransformerSpec extends Specification {

  @Shared
  def bbCodeParser;
  @Shared
  def bbCodeToFreeMarkerTransformer;

  @Shared
  def attributes = [ '*' : new TagAttributes(true, false),
                     code : new TagAttributes(false, true),
                     noparse : new TagAttributes(false, true)]

  /**
   * No lambdas until Groovy v3.
   */
  @Shared
  def transformPredicate = new Predicate<TagNode>() {
    @Override
    public boolean test(TagNode tag) {
      return true;
    }
  }

  def setupSpec() {
    bbCodeParser = new BBCodeParser()
    bbCodeToFreeMarkerTransformer = new BBCodeToHTMLTransformer()
  }

  // Covered in the Java test
  def "No FreeMarker template for tag with strict mode disabled"() {

    when: "A BBCodeParser is setup and a template has not been provided for a tag"
      def document = bbCodeParser.buildDocument("[unknown]Testing[/unknown]", attributes)

    and: "transform is bbCodeTransformer"
      def html = bbCodeToFreeMarkerTransformer.transform(document, transformPredicate, null, null)

    then: "the output should should equal the input"
      html == "[unknown]Testing[/unknown]"
  }

  // Covered in the Java test
  def "No FreeMarker template for tag with strict mode enabled in the constructor"() {

    when: "A BBCodeParser is setup and a template has not been provided for a tag"
      Document document = bbCodeParser.buildDocument("[unknown]Testing[/unknown]", attributes)

    and: "transform is bbCodeTransformer"
      def html = new BBCodeToHTMLTransformer(true).transform(document, transformPredicate, null, null)

    then: "an exception should be thrown"
      thrown TransformException

    and: "html should be null"
      html == null
  }

  // This is a parser tests not a transformer test
  def "No closing BBCode tag"() {

    when: "A BBCode string with a missing closing tag is parsed"
      def document = bbCodeParser.buildDocument("[b]Testing", attributes);

    then: "an exception is not thrown"
      notThrown Exception

    and: "document is not null"
      document != null

    and: "a single text node should exist in the document"
      document.children.size() == 1
      document.children.get(0) instanceof TextNode
      def text = (TextNode) document.children.get(0)
      text.begin == 0
      text.end == 10
      text.getBody() == "[b]Testing"

  }

  // This test seems solid. Though it isn't using the transform function for HTML replacement, so it is a bit light.
  //@Unroll("BBCode to HTML : #html")
  def "BBCode to HTML - simple"() {

    expect: "when HTML transformer is called with bbCode properly formatted HTML is returned"
      def document = bbCodeParser.buildDocument(bbCode, attributes)
      Transformer.Offsets offsets = new Transformer.Offsets();
      def transformFunction = new Transformer.TransformFunction.OffsetHTMLEscapeTransformFunction(offsets)
      bbCodeToFreeMarkerTransformer.transform(document, transformPredicate, transformFunction, null).replaceAll(" ", "").replaceAll("<br>", "").replaceAll("&nbsp;", "") == html.replaceAll(" ", "");

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
      "<strong>bold</strong> No format. <strong>bold</strong> <strong>bold</strong>"                                          | "[B]bold[/B]No format.[b]bold[/B] [B]bold[/b]"
      "<em>italic</em> No format. <em>italic</em> <em>italic</em>"                                                            | "[I]italic[/I]No format.[i]italic[/I] [I]italic[/i]"
      "the <em>XY </em>Trainer"                                                                                               | "the [I]XY [/I]Trainer"
      "<span style=\"font-family: times new roman\">Matthew(not 69) (175) </span>"                                            | "[font=times new roman]Matthew(not 69) (175) [/font]"
      "<u>&lt;script&gt; var inject=true;&lt;/script&gt;</u>"                                                                 | "[u]<script> var inject=true;</script>[/u]"
      "<div>Example: [noparse]foo[/noparse]</div>"                                                                            | "[noparse]Example: [noparse]foo[/noparse][/noparse]"

  }

  // Not sure what this test is doing. It doesn't look like it asserts anything
  @Unroll("BBCode to HTML : #fileName")
  def "BBCode to HTML - complex"() {

    expect: "when HTML transformer is called with bbcode properly formatted HTML is returned"

      def bbCode = this.getClass().getResourceAsStream("/org/primeframework/transformer/bbcode/" + fileName).getText();
      def html = this.getClass().getResourceAsStream("/org/primeframework/transformer/html/" + fileName).getText();

      Transformer.Offsets offsets = new Transformer.Offsets();
      def transformFunction = new Transformer.TransformFunction.OffsetHTMLEscapeTransformFunction(offsets)
      def document = bbCodeParser.buildDocument(bbCode, attributes)
      // assert the transformed content equals the expected HTML
      bbCodeToFreeMarkerTransformer.transform(document, transformPredicate, transformFunction, null).replaceAll("<br/>", "").replaceAll("\\s+", "") == html.replaceAll("\\s+", "")

    where:
      fileName   | _
      "other"    | _
//      "code"     | _
      "image"    | _
      "size"     | _
      "quote"    | _
      "customer" | _
  }

}
