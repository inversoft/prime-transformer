/*
 * Copyright (c) 2015-2018, Inversoft Inc., All Rights Reserved
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
package org.primeframework.transformer.service;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import org.primeframework.transformer.domain.Document;
import org.primeframework.transformer.domain.TagAttributes;
import org.primeframework.transformer.domain.TagNode;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;

/**
 * Tests the TextTransformer.
 *
 * @author Brian Pontarelli
 */
public class TextTransformerTest {

  private static Map<String, TagAttributes> attributes = new HashMap<>();

  @Test
  public void bbCodeToText() {
    assertTransform("[list] [*] foo [*] bar [/list]", "  foo  bar ", (node) -> true);
    assertTransform("[b]bold[/b]No format.[b]bold[/b]", "boldNo format.bold", (node) -> true);
    assertTransform("[b] bold [/b] No format. [b] bold [/b]", " bold  No format.  bold ", (node) -> true);
    assertTransform("[b]bold[i]italic[u]underline[/u]italic[/i]bold[/b]", "bolditalicunderlineitalicbold", (node) -> true);
    assertTransform("z [b]abc defg [/b]hijk [ul][*]lmn opqr[*][/ul]", "z abc defg hijk lmn opqr", (node) -> true);
    assertTransform("Example [code type=\"see the java oo\" syntax=\"java\"] System.out.println(\"Hello World!\"); [/code] ", "Example  System.out.println(\"Hello World!\");  ", (node) -> true);
    assertTransform("[foo bar=\"blah blah\"]Some ordinary text.[/foo] [font=\"verdana\"]Hello[/font]", "[foo bar=\"blah blah\"]Some ordinary text.[/foo] [font=\"verdana\"]Hello[/font]", (node) -> false);
    assertTransform("[list] [*] foo [*] bar [/list] [b]bold[/b]", "[list] [*] foo [*] bar [/list] bold", (node) -> !node.getName().equals("list"));
    assertTransform("\\[b]Hello World\\[/b]", "[b]Hello World[/b]", (node) -> true);
  }

  @Test
  public void htmlToText() {
    assertHTMLTransformToText("<ul> <li> foo </li> <li> bar </li> </ul>", "  foo   bar  ", (node) -> true);
    assertHTMLTransformToText("<strong>bold</strong>No format.<strong>bold</strong>", "boldNo format.bold", (node) -> true);
    assertHTMLTransformToText("<strong> bold </strong> No format. <strong> bold </strong>", " bold  No format.  bold ", (node) -> true);
    assertHTMLTransformToText("<strong>bold<em>italic<underline>underline</underline>italic</em>bold</strong>", "bolditalicunderlineitalicbold", (node) -> true);
    assertHTMLTransformToText("z <strong>abc defg </strong>hijk <ul><li>lmn opqr</li></ul>", "z abc defg hijk lmn opqr", (node) -> true);
    assertHTMLTransformToText("Example <pre class=\"code\" type=\"see the java oo\" syntax=\"java\"> System.out.println(\"Hello World!\"); </pre> ", "Example  System.out.println(\"Hello World!\");  ", (node) -> true);
  }

  private void assertHTMLTransformToText(String str, String expected, Predicate<TagNode> transformPredicate) {
    HTMLParser parser = new HTMLParser();
    Document doc = parser.buildDocument(str, attributes);
    Transformer transformer = new TextTransformer();
    String actual = transformer.transform(doc, transformPredicate, null, null);
    assertEquals(actual, expected);
  }

  private void assertTransform(String str, String expected, Predicate<TagNode> transformPredicate) {
    BBCodeParser parser = new BBCodeParser();
    Document doc = parser.buildDocument(str, attributes);
    Transformer transformer = new TextTransformer();
    String actual = transformer.transform(doc, transformPredicate, null, null);
    assertEquals(actual, expected);
  }

  static {
    attributes.put("*", new TagAttributes(true, false, false, true));
    attributes.put("code", new TagAttributes(false, true, false, true));
    attributes.put("noparse", new TagAttributes(false, true, false, true));
  }
}
