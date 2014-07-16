package org.primeframework.transformer.service;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.primeframework.transformer.domain.Document;
import org.primeframework.transformer.domain.DocumentSource;
import org.primeframework.transformer.domain.TransformerException;
import org.primeframework.transformer.service.Transformer.TransformedResult;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import freemarker.template.Configuration;
import freemarker.template.Template;
import static org.testng.Assert.assertEquals;

/**
 * Testing the offsets returned from the {@link FreeMarkerTransformer}
 *
 * @author Seth Musselman
 */
@Test(groups = "unit")
public class FreemarkerTransformerOffsetTest {

  private static final Map<String, Template> templates = new HashMap<>();

  @BeforeSuite
  public void beforeSuite() throws IOException {
    Configuration conf = new Configuration();
    templates.put("a", new Template("a", new StringReader("<aaaaaa>${body}</aaaaaa>"), conf));
    templates.put("b", new Template("b", new StringReader("<bbbbbb>${body}</bbbbbb>"), conf));
    templates.put("c", new Template("c", new StringReader("<cccccc>${body}</cccccc>"), conf));
    templates.put("d", new Template("d", new StringReader("[#ftl/][#macro attrMacro attrs][#if attrs??][#list attrs?keys as attr]${attr}=\"${attrs[attr]}\"[/#list][/#if][/#macro]<dddddd [@attrMacro attributes/]>${body}</dddddd>"), conf));
    templates.put("*", new Template("*", new StringReader("<p>smile</p>"), conf));
  }

  @Test
  public void testTransformedResult() throws TransformerException {
    Parser parser = new BBCodeParser();
    Transformer transformer = new FreeMarkerTransformer(templates);
    Document doc = parser.buildDocument(new DocumentSource("[b] bbb [/b]"));

    TransformedResult expected = new TransformedResult("<bbbbbb> bbb </bbbbbb>");
    expected.addOffset(3, 5);
    expected.addOffset(12, 5);

    TransformedResult result = transformer.transform(doc);
    assertEquals(result, expected);
  }

  @Test
  public void testTransformedResultPrefixAndSuffix() throws TransformerException {
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
  public void testSimpleOffsets() throws TransformerException {
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
  public void testTransformedResultWithEmbedding() throws TransformerException {
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
  public void testTransformedResultWithEmbeddingAndAdjacentTags() throws TransformerException {
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
  public void testTransformedResultWithManyEmbeddingsAndAdjacentTags() throws TransformerException {
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
  public void testTransformedResultSoloAttributes() throws TransformerException {
    Parser parser = new BBCodeParser();
    Transformer transformer = new FreeMarkerTransformer(templates);
    //                                                                     15     22
    Document doc = parser.buildDocument(new DocumentSource("[d testattr=33]xyz[/d]"));
    TransformedResult expected = new TransformedResult("<dddddd  testattr=\"33\">xyz</dddddd>");
    expected.addOffset(15, 8);
    expected.addOffset(22, 5);

    TransformedResult result = transformer.transform(doc);
    assertEquals(result, expected);
  }

  @Test
  public void testTransformedResultWithEmbeddingAndAdjacentTagsAndAttributes() throws TransformerException {
    Parser parser = new BBCodeParser();
    Transformer transformer = new FreeMarkerTransformer(templates);
    //                                                            6     13   17                35     42  46
    Document doc = parser.buildDocument(new DocumentSource("123[b]abc[/b] [a]123[d testattr=33]xyz[/d][/a] 456"));
    TransformedResult expected = new TransformedResult("123<bbbbbb>abc</bbbbbb> <aaaaaa>123<dddddd  testattr=\"33\">xyz</dddddd></aaaaaa> 456");
    expected.addOffset(6, 5);
    expected.addOffset(13, 5);
    expected.addOffset(17, 5);
    expected.addOffset(35, 8);
    expected.addOffset(42, 5);
    expected.addOffset(46, 5);

    TransformedResult result = transformer.transform(doc);
    assertEquals(result, expected);
  }

  @Test
  public void testTransformedResultWithEmbeddingAndAdjacentTagsAndAttributesAndSingleBBCodeTag() throws TransformerException {
    Parser parser = new BBCodeParser();
    Transformer transformer = new FreeMarkerTransformer(templates);
    //                                                            6     12 16   20                38     45  49
    Document doc = parser.buildDocument(new DocumentSource("123[b]abc[*][/b] [a]123[d testattr=33]xyz[/d][/a] 456"));
    TransformedResult expected = new TransformedResult("123<bbbbbb>abc<p>smile</p></bbbbbb> <aaaaaa>123<dddddd  testattr=\"33\">xyz</dddddd></aaaaaa> 456");
    expected.addOffset(6, 5);
    expected.addOffset(12, 9);
    expected.addOffset(16, 5);
    expected.addOffset(20, 5);
    expected.addOffset(38, 8);
    expected.addOffset(45, 5);
    expected.addOffset(49, 5);

    TransformedResult result = transformer.transform(doc);
    assertEquals(result, expected);
  }
}
