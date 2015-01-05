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
package org.primeframework.transformer.service;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.primeframework.transformer.domain.Document;
import org.primeframework.transformer.domain.TagNode;
import org.primeframework.transformer.service.Transformer.NodeConsumer.OffsetNodeConsumer;
import org.primeframework.transformer.service.Transformer.Offsets;
import org.primeframework.transformer.service.Transformer.TransformFunction.OffsetHTMLEscapeTransformFunction;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

/**
 * Tests the FreeMarker transformer.
 *
 * @author Seth Musselman and Brian Pontarelli
 */
public class FreemarkerTransformerTest {
  private static final Map<String, Template> templates = new HashMap<>();

  @BeforeClass
  public static void beforeSuite() throws IOException {
    Configuration conf = new Configuration();
    templates.put("a", new Template("a", new StringReader("<aaaaaa>${body}</aaaaaa>"), conf));
    templates.put("b", new Template("b", new StringReader("<bbbbbb>${body}</bbbbbb>"), conf));
    templates.put("c", new Template("c", new StringReader("<cccccc>${body}</cccccc>"), conf));
    templates.put("d", new Template("d", new StringReader("[#ftl/][#macro attrMacro attrs][#if attrs??][#list attrs?keys as attr]${attr}=\"${attrs[attr]}\"[/#list][/#if][/#macro]<dddddd [@attrMacro attributes/]>${body}</dddddd>"), conf));
    templates.put("*", new Template("*", new StringReader("<li>${body}</li>"), conf));
    templates.put("nobody", new Template("nobody", new StringReader("<p>no body here</p>"), conf));
    templates.put("list", new Template("list", new StringReader("<ul>${body}</ul>"), conf));
    templates.put("change", new Template("change", new StringReader("<change>${body?replace('', '|')}</change>"), conf));
    templates.put("wrap", new Template("wrap", new StringReader("<wrap>left${body}right</wrap>"), conf));
    templates.put("bad", new Template("bad", new StringReader("<wrap>left${body.missing_method()}right</wrap>"), conf));
  }

  @DataProvider
  public static Object[][] strictness() {
    return new Object[][]{{true}, {false}};
  }

  @Test
  public void computedOffset() {
    Offsets offsets = new Offsets();
    offsets.add(0, 1);
    offsets.add(1, 2);
    offsets.add(1, 3);
    offsets.add(10, 5);
    offsets.add(12, 50);
    assertEquals(offsets.computeOffsetFromIndex(0), 1);
    assertEquals(offsets.computeOffsetFromIndex(1), 6);
    assertEquals(offsets.computeOffsetFromIndex(2), 6);
    assertEquals(offsets.computeOffsetFromIndex(10), 11);
    assertEquals(offsets.computeOffsetFromIndex(11), 11);
    assertEquals(offsets.computeOffsetFromIndex(12), 61);
    assertEquals(offsets.computeOffsetFromIndex(13), 61);
    assertEquals(offsets.computeOffsetFromIndex(14), 61);
  }

  @Test
  public void errorBadTemplate() throws Exception {
    Document doc = parseDocument("[bad testattr=33]xyz[/bad]");

    try {
      assertTransform(true, doc, (node) -> true, null, null);
    } catch (TransformerException e) {
      assertTrue(e.getMessage().startsWith("FreeMarker processing failed for template"));
    }
  }

  @Test
  public void errorStrictMissingTag() throws Exception {
    Document doc = parseDocument("[missing testattr=33]xyz[/missing]");

    try {
      assertTransform(true, doc, (node) -> true, null, null);
    } catch (TransformerException e) {
      assertEquals(e.getMessage(), "No template found for tag [missing]");
    }
  }

  @Test(dataProvider = "strictness")
  public void escapeHTMLWithOffsets(boolean strict) throws Exception {
    Document doc = parseDocument("A<>B&C<>D [b] f(x) = x < y > z &\r\n f(y) = \"Yo!\"\n [/b] A<>B&C<>D");

    String expected = "A&lt;&gt;B&amp;C&lt;&gt;D <bbbbbb> f(x) = x &lt; y &gt; z &amp;<br/> f(y) = &quot;Yo!&quot;<br/> </bbbbbb> A&lt;&gt;B&amp;C&lt;&gt;D";
    Offsets expectedOffsets = new Offsets();
    // HTML
    expectedOffsets.add(1, 3); // <
    expectedOffsets.add(2, 3); // >
    expectedOffsets.add(4, 4); // &
    expectedOffsets.add(6, 3); // <
    expectedOffsets.add(7, 3); // >
    expectedOffsets.add(23, 3); // <
    expectedOffsets.add(27, 3); // >
    expectedOffsets.add(31, 4); // &
    expectedOffsets.add(32, 3); // \r\n
    expectedOffsets.add(42, 5); // "
    expectedOffsets.add(46, 5); // "
    expectedOffsets.add(47, 4); // \n
    expectedOffsets.add(55, 3); // <
    expectedOffsets.add(56, 3); // >
    expectedOffsets.add(58, 4); // &
    expectedOffsets.add(60, 3); // <
    expectedOffsets.add(61, 3); // >

    // Tags
    expectedOffsets.add(13, 5);
    expectedOffsets.add(53, 5);

    assertTransform(strict, doc, (node) -> true, expected, expectedOffsets);
  }

  @Test(dataProvider = "strictness")
  public void nestedNodesAreNotTransformedIfParentFailsPredicate(boolean strict) throws Exception {
    Document doc = parseDocument("[list] [*] foo [*] bar [/list]");
    String expected = "[list] [*] foo [*] bar [/list]";
    Offsets expectedOffsets = new Offsets();

    assertTransform(strict, doc, (node) -> !node.getName().equals("list"), expected, expectedOffsets);
  }

  @Test(dataProvider = "strictness")
  public void prefixAndSuffix(boolean strict) throws Exception {
    Document doc = parseDocument("abc[b] bbb [/b]123");
    String expected = "abc<bbbbbb> bbb </bbbbbb>123";
    Offsets expectedOffsets = new Offsets();
    expectedOffsets.add(6, 5);
    expectedOffsets.add(15, 5);

    assertTransform(strict, doc, (node) -> true, expected, expectedOffsets);
  }

  @Test(dataProvider = "strictness")
  public void simpleOffsets(boolean strict) throws Exception {
    Document doc = parseDocument("1[a]2[b]3[/b]4[/a]5");

    String expected = "1<aaaaaa>2<bbbbbb>3</bbbbbb>4</aaaaaa>5";
    Offsets expectedOffsets = new Offsets();
    expectedOffsets.add(4, 5);
    expectedOffsets.add(8, 5);
    expectedOffsets.add(13, 5);
    expectedOffsets.add(18, 5);

    assertTransform(strict, doc, (node) -> true, expected, expectedOffsets);
  }

  @Test(dataProvider = "strictness")
  public void soloAttributes(boolean strict) throws Exception {
    Document doc = parseDocument("[d testattr=33]xyz[/d]");
    String expected = "<dddddd testattr=\"33\">xyz</dddddd>";
    Offsets expectedOffsets = new Offsets();
    expectedOffsets.add(15, 7);
    expectedOffsets.add(22, 5);

    assertTransform(strict, doc, (node) -> true, expected, expectedOffsets);
  }

  @Test(dataProvider = "strictness")
  public void templateChangesBody(boolean strict) throws Exception {
    Document doc = parseDocument("[change] foo [/change]");

    String expected = "<change>| |f|o|o| |</change>";
    Offsets expectedOffsets = new Offsets();
    expectedOffsets.add(22, 6);

    assertTransform(strict, doc, (node) -> true, expected, expectedOffsets);
  }

  @Test(dataProvider = "strictness")
  public void templateWrapsBody(boolean strict) throws Exception {
    Document doc = parseDocument("[wrap] foo [/wrap]");

    String expected = "<wrap>left foo right</wrap>";
    Offsets expectedOffsets = new Offsets();
    expectedOffsets.add(6, 4);
    expectedOffsets.add(18, 5);

    assertTransform(strict, doc, (node) -> true, expected, expectedOffsets);
  }

  @Test(dataProvider = "strictness")
  public void transformedResult(boolean strict) throws Exception {
    Document doc = parseDocument("[b] bbb [/b]");
    String expected = "<bbbbbb> bbb </bbbbbb>";
    Offsets expectedOffsets = new Offsets();
    expectedOffsets.add(3, 5);
    expectedOffsets.add(12, 5);

    assertTransform(strict, doc, (node) -> true, expected, expectedOffsets);
  }

  @Test(dataProvider = "strictness")
  public void withEmbedding(boolean strict) throws Exception {
    Document doc = parseDocument("[a]123[c]xyz[/c][/a]");

    String expected = "<aaaaaa>123<cccccc>xyz</cccccc></aaaaaa>";
    Offsets expectedOffsets = new Offsets();
    expectedOffsets.add(3, 5);
    expectedOffsets.add(9, 5);
    expectedOffsets.add(16, 5);
    expectedOffsets.add(20, 5);

    assertTransform(strict, doc, (node) -> true, expected, expectedOffsets);
  }

  @Test(dataProvider = "strictness")
  public void withEmbeddingAndAdjacentTags(boolean strict) throws Exception {
    Document doc = parseDocument("123[b]abc[/b] [a]123[c]xyz[/c][/a] 456");
    String expected = "123<bbbbbb>abc</bbbbbb> <aaaaaa>123<cccccc>xyz</cccccc></aaaaaa> 456";
    Offsets expectedOffsets = new Offsets();
    expectedOffsets.add(6, 5);
    expectedOffsets.add(13, 5);
    expectedOffsets.add(17, 5);
    expectedOffsets.add(23, 5);
    expectedOffsets.add(30, 5);
    expectedOffsets.add(34, 5);

    assertTransform(strict, doc, (node) -> true, expected, expectedOffsets);
  }

  @Test(dataProvider = "strictness")
  public void withEmbeddingAndAdjacentTagsAndAttributes(boolean strict) throws Exception {
    Document doc = parseDocument("123[b]abc[/b] [a]123[d testattr=33]xyz[/d][/a] 456");
    String expected = "123<bbbbbb>abc</bbbbbb> <aaaaaa>123<dddddd testattr=\"33\">xyz</dddddd></aaaaaa> 456";
    Offsets expectedOffsets = new Offsets();
    expectedOffsets.add(6, 5);
    expectedOffsets.add(13, 5);
    expectedOffsets.add(17, 5);
    expectedOffsets.add(35, 7);
    expectedOffsets.add(42, 5);
    expectedOffsets.add(46, 5);

    assertTransform(strict, doc, (node) -> true, expected, expectedOffsets);
  }

  @Test(dataProvider = "strictness")
  public void withEmbeddingAndAdjacentTagsAndAttributesAndSingleBBCodeTag(boolean strict) throws Exception {
    Document doc = parseDocument("123[list]abc[*][/list] [a]123[d testattr=33]xyz[/d][/a] 456");
    String expected = "123<ul>abc<li></li></ul> <aaaaaa>123<dddddd testattr=\"33\">xyz</dddddd></aaaaaa> 456";
    Offsets expectedOffsets = new Offsets();
    expectedOffsets.add(9, -2);
    expectedOffsets.add(15, 6);
    expectedOffsets.add(22, -2);
    expectedOffsets.add(26, 5);
    expectedOffsets.add(44, 7);
    expectedOffsets.add(51, 5);
    expectedOffsets.add(55, 5);

    assertTransform(strict, doc, (node) -> true, expected, expectedOffsets);
  }

  @Test(dataProvider = "strictness")
  public void withEmbeddingAndNonTransformedAndAdjacentTagsAndAttributesAndSingleBBCodeTag(boolean strict)
      throws Exception {
    Document doc = parseDocument("123[b]abc[*][/b] [a]123[d testattr=33]xyz[/d][/a] 456");
    String expected = "123[b]abc[*][/b] <aaaaaa>123<dddddd testattr=\"33\">xyz</dddddd></aaaaaa> 456";
    Offsets expectedOffsets = new Offsets();
    expectedOffsets.add(20, 5);
    expectedOffsets.add(38, 7);
    expectedOffsets.add(45, 5);
    expectedOffsets.add(49, 5);

    assertTransform(strict, doc, (node) -> !node.getName().equals("b"), expected, expectedOffsets);
  }

  @Test(dataProvider = "strictness")
  public void withEmbeddingNoLeadingTextNode(boolean strict) throws Exception {
    Document doc = parseDocument("[list][*]item1[*]item2[/list]");
    String expected = "<ul><li>item1</li><li>item2</li></ul>";
    Offsets expectedOffsets = new Offsets();
    expectedOffsets.add(6, -2);
    expectedOffsets.add(9, 1);
    expectedOffsets.add(14, 4);
    expectedOffsets.add(17, 1);
    expectedOffsets.add(22, 4);
    expectedOffsets.add(29, -2);

    assertTransform(strict, doc, (node) -> true, expected, expectedOffsets);
  }

  @Test(dataProvider = "strictness")
  public void withManyEmbeddingsAndAdjacentTags(boolean strict) throws Exception {
    Document doc = parseDocument("123[b]abc[a][c]wow[/c][/a][/b] [a]123[c]xyz[/c][/a] 456");
    String expected = "123<bbbbbb>abc<aaaaaa><cccccc>wow</cccccc></aaaaaa></bbbbbb> <aaaaaa>123<cccccc>xyz</cccccc></aaaaaa> 456";
    Offsets expectedOffsets = new Offsets();
    expectedOffsets.add(6, 5);
    expectedOffsets.add(12, 5);
    expectedOffsets.add(15, 5);
    expectedOffsets.add(22, 5);
    expectedOffsets.add(26, 5);
    expectedOffsets.add(30, 5);
    expectedOffsets.add(34, 5);
    expectedOffsets.add(40, 5);
    expectedOffsets.add(47, 5);
    expectedOffsets.add(51, 5);

    assertTransform(strict, doc, (node) -> true, expected, expectedOffsets);
  }

  private void assertTransform(boolean strict, Document doc, Predicate<TagNode> transformPredicate, String expected,
                               Offsets expectedOffsets) throws Exception {
    Offsets actualOffsets = new Offsets();
    Transformer transformer = new FreeMarkerTransformer(templates, strict);
    String actual = transformer.transform(doc, transformPredicate, new OffsetHTMLEscapeTransformFunction(actualOffsets),
        new OffsetNodeConsumer(actualOffsets));
    assertEquals(actual, expected);
    assertEquals(actualOffsets, expectedOffsets);
    assertEquals(doc.source.length + actualOffsets.total(), expected.length()); // Lengths with offsets should be equal
  }

  private Document parseDocument(String string) throws Exception {
    Parser parser = new BBCodeParser();
    return parser.buildDocument(string);
  }
}
