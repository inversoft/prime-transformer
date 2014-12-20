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

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.primeframework.transformer.domain.Document;
import org.primeframework.transformer.domain.DocumentSource;
import org.primeframework.transformer.domain.TagNode;
import org.primeframework.transformer.service.Transformer.TransformedResult;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;

/**
 * Testing the offsets returned from the {@link FreeMarkerTransformer}
 *
 * @author Seth Musselman
 */
public class FreemarkerTransformerOffsetTest {

  private static final Map<String, Template> templates = new HashMap<>();

  @BeforeClass
  public static void beforeSuite() throws IOException {
    Configuration conf = new Configuration();
    templates.put("a", new Template("a", new StringReader("<aaaaaa>${body}</aaaaaa>"), conf));
    templates.put("b", new Template("b", new StringReader("<bbbbbb>${body}</bbbbbb>"), conf));
    templates.put("c", new Template("c", new StringReader("<cccccc>${body}</cccccc>"), conf));
    templates.put("d", new Template("d", new StringReader("[#ftl/][#macro attrMacro attrs][#if attrs??][#list attrs?keys as attr]${attr}=\"${attrs[attr]}\"[/#list][/#if][/#macro]<dddddd [@attrMacro attributes/]>${body}</dddddd>"), conf));
    templates.put("*", new Template("*", new StringReader("<p>smile</p>"), conf));
    templates.put("list", new Template("list", new StringReader("<ul>${body}</ul>"), conf));

  }

  @Test
  public void testTransformedResult() throws Exception {
    Parser parser = new BBCodeParser();
    Transformer transformer = new FreeMarkerTransformer(templates);
    Document doc = parser.buildDocument(new DocumentSource("[b] bbb [/b]"));

    TransformedResult expected = new TransformedResult("<bbbbbb> bbb </bbbbbb>");
    expected.addOffset(3, 5);
    expected.addOffset(12, 5);

    TransformedResult result = transformer.transform(doc);
    assertEquals(expected, result);
  }

  @Test
  public void testTransformedResultPrefixAndSuffix() throws Exception {
    Parser parser = new BBCodeParser();
    Transformer transformer = new FreeMarkerTransformer(templates);
    Document doc = parser.buildDocument(new DocumentSource("abc[b] bbb [/b]123"));

    TransformedResult expected = new TransformedResult("abc<bbbbbb> bbb </bbbbbb>123");
    expected.addOffset(6, 5);
    expected.addOffset(15, 5);

    TransformedResult result = transformer.transform(doc);
    assertEquals(result, expected);
  }

  @Test
  public void testSimpleOffsets() throws Exception {
    Parser parser = new BBCodeParser();
    Transformer transformer = new FreeMarkerTransformer(templates);
    //                                                         3    9       16  20
    Document doc = parser.buildDocument(new DocumentSource("1[a]2[b]3[/b]4[/a]5"));

    TransformedResult expected = new TransformedResult("1<aaaaaa>2<bbbbbb>3</bbbbbb>4</aaaaaa>5");
    expected.addOffset(4, 5);
    expected.addOffset(8, 5);
    expected.addOffset(13, 5);
    expected.addOffset(18, 5);

    TransformedResult result = transformer.transform(doc);
    assertEquals(result, expected);
  }

  @Test
  public void testTransformedResultWithEmbedding() throws Exception {
    Parser parser = new BBCodeParser();
    Transformer transformer = new FreeMarkerTransformer(templates);
    //                                                         3    9       16  20
    Document doc = parser.buildDocument(new DocumentSource("[a]123[c]xyz[/c][/a]"));

    TransformedResult expected = new TransformedResult("<aaaaaa>123<cccccc>xyz</cccccc></aaaaaa>");
    expected.addOffset(3, 5);
    expected.addOffset(9, 5);
    expected.addOffset(16, 5);
    expected.addOffset(20, 5);

    TransformedResult result = transformer.transform(doc);
    assertEquals(result, expected);
  }

  @Test
  public void testTransformedResultWithEmbeddingAndAdjacentTags() throws Exception {
    Parser parser = new BBCodeParser();
    Transformer transformer = new FreeMarkerTransformer(templates);
    //                                                            6     13   17    23     30  34
    Document doc = parser.buildDocument(new DocumentSource("123[b]abc[/b] [a]123[c]xyz[/c][/a] 456"));

    TransformedResult expected = new TransformedResult("123<bbbbbb>abc</bbbbbb> <aaaaaa>123<cccccc>xyz</cccccc></aaaaaa> 456");
    expected.addOffset(6, 5);
    expected.addOffset(13, 5);
    expected.addOffset(17, 5);
    expected.addOffset(23, 5);
    expected.addOffset(30, 5);
    expected.addOffset(34, 5);

    TransformedResult result = transformer.transform(doc);
    assertEquals(result, expected);
  }

  @Test
  public void testTransformedResultWithManyEmbeddingsAndAdjacentTags() throws Exception {
    Parser parser = new BBCodeParser();
    Transformer transformer = new FreeMarkerTransformer(templates);
    //                                                            6     12 15     22  26  30  34    40     47  51
    Document doc = parser.buildDocument(new DocumentSource("123[b]abc[a][c]wow[/c][/a][/b] [a]123[c]xyz[/c][/a] 456"));

    TransformedResult expected = new TransformedResult("123<bbbbbb>abc<aaaaaa><cccccc>wow</cccccc></aaaaaa></bbbbbb> <aaaaaa>123<cccccc>xyz</cccccc></aaaaaa> 456");
    expected.addOffset(6, 5);
    expected.addOffset(12, 5);
    expected.addOffset(15, 5);
    expected.addOffset(22, 5);
    expected.addOffset(26, 5);
    expected.addOffset(30, 5);
    expected.addOffset(34, 5);
    expected.addOffset(40, 5);
    expected.addOffset(47, 5);
    expected.addOffset(51, 5);

    TransformedResult result = transformer.transform(doc);
    assertEquals(result, expected);
  }

  @Test
  public void testTransformedResultSoloAttributes() throws Exception {
    Parser parser = new BBCodeParser();
    Transformer transformer = new FreeMarkerTransformer(templates);
    //                                                                     15     22
    Document doc = parser.buildDocument(new DocumentSource("[d testattr=33]xyz[/d]"));
    TransformedResult expected = new TransformedResult("<dddddd testattr=\"33\">xyz</dddddd>");
    expected.addOffset(15, 7);
    expected.addOffset(22, 5);

    TransformedResult result = transformer.transform(doc);
    assertEquals(expected, result);
  }

  @Test
  public void testTransformedResultWithEmbeddingAndAdjacentTagsAndAttributes() throws Exception {
    Parser parser = new BBCodeParser();
    Transformer transformer = new FreeMarkerTransformer(templates);
    //                                                            6     13   17                35     42  46
    Document doc = parser.buildDocument(new DocumentSource("123[b]abc[/b] [a]123[d testattr=33]xyz[/d][/a] 456"));
    TransformedResult expected = new TransformedResult("123<bbbbbb>abc</bbbbbb> <aaaaaa>123<dddddd testattr=\"33\">xyz</dddddd></aaaaaa> 456");
    expected.addOffset(6, 5);
    expected.addOffset(13, 5);
    expected.addOffset(17, 5);
    expected.addOffset(35, 7);
    expected.addOffset(42, 5);
    expected.addOffset(46, 5);

    TransformedResult result = transformer.transform(doc);
    assertEquals(expected, result);
  }

  @Test
  public void testTransformedResultWithEmbeddingAndAdjacentTagsAndAttributesAndSingleBBCodeTag() throws Exception {
    Parser parser = new BBCodeParser();
    Transformer transformer = new FreeMarkerTransformer(templates);
    //                                                              9     15    22  26                44     51  55
    Document doc = parser.buildDocument(new DocumentSource("123[list]abc[*][/list] [a]123[d testattr=33]xyz[/d][/a] 456"));
    TransformedResult expected = new TransformedResult("123<ul>abc<p>smile</p></ul> <aaaaaa>123<dddddd testattr=\"33\">xyz</dddddd></aaaaaa> 456");
    expected.addOffset(9, -2);
    expected.addOffset(15, 0);
    expected.addOffset(22, 5);
    expected.addOffset(26, 5);
    expected.addOffset(44, 7);
    expected.addOffset(51, 5);
    expected.addOffset(55, 5);

    TransformedResult result = transformer.transform(doc);
    assertEquals(expected, result);
  }

  @Test
  public void testTransformedResultWithEmbeddingNoLeadingTextNode() throws Exception {
    Parser parser = new BBCodeParser();
    Transformer transformer = new FreeMarkerTransformer(templates);
    //                                                           6  9       17          29
    Document doc = parser.buildDocument(new DocumentSource("[list][*]item1[*]item2[/list]"));
    TransformedResult expected = new TransformedResult("<ul><p>smile</p><p>smile</p></ul>");
    expected.addOffset(6, -2);
    expected.addOffset(9, 0);
    expected.addOffset(17, 0);
    expected.addOffset(29, 4);

    TransformedResult result = transformer.transform(doc);
    assertEquals(expected, result);
  }


  @Test
  public void testTransformedResultWithEmbeddingAndNonTransformedAndAdjacentTagsAndAttributesAndSingleBBCodeTag() throws Exception {
    Parser parser = new BBCodeParser();
    Transformer transformer = new FreeMarkerTransformer(templates);
    //                                                            6     12 16   20                38     45  49
    Document doc = parser.buildDocument(new DocumentSource("123[b]abc[*][/b] [a]123[d testattr=33]xyz[/d][/a] 456"));
    ((TagNode) doc.children.get(1)).transform = false;

    TransformedResult expected = new TransformedResult("123[b]abc[*][/b] <aaaaaa>123<dddddd testattr=\"33\">xyz</dddddd></aaaaaa> 456");
    expected.addOffset(20, 5);
    expected.addOffset(38, 7);
    expected.addOffset(45, 5);
    expected.addOffset(49, 5);

    TransformedResult result = transformer.transform(doc);
    assertEquals(expected, result);
  }

  @Test
  public void testTransformedResultComputedOffset() {
    TransformedResult tr = new TransformedResult(null);
    tr.addOffset(0, 1);
    tr.addOffset(1, 2);
    tr.addOffset(1, 3);
    tr.addOffset(10, 5);
    tr.addOffset(12, 50);
    assertEquals(tr.computeOffsetFromIndex(0), 1);
    assertEquals(tr.computeOffsetFromIndex(1), 6);
    assertEquals(tr.computeOffsetFromIndex(2), 6);
    assertEquals(tr.computeOffsetFromIndex(10), 11);
    assertEquals(tr.computeOffsetFromIndex(11), 11);
    assertEquals(tr.computeOffsetFromIndex(12), 61);
    assertEquals(tr.computeOffsetFromIndex(13), 61);
    assertEquals(tr.computeOffsetFromIndex(14), 61);
  }

  @Test
  public void testTransformedResultDuplicatePairs() {
    TransformedResult tr = new TransformedResult(null);
    tr.addOffset(0, 3);
    tr.addOffset(0, 3);
    assertEquals(tr.computeOffsetFromIndex(1), 6);
  }
}
