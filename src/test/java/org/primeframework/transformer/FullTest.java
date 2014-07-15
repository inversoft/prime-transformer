package org.primeframework.transformer;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import org.primeframework.transformer.domain.Document;
import org.primeframework.transformer.domain.DocumentSource;
import org.primeframework.transformer.domain.TagNode;
import org.primeframework.transformer.service.BBCodeParser;
import org.primeframework.transformer.service.FreeMarkerTransformer;
import org.primeframework.transformer.service.Parser;
import org.primeframework.transformer.service.Transformer;
import org.testng.annotations.BeforeSuite;
import org.testng.annotations.Test;

import freemarker.template.Configuration;
import freemarker.template.Template;
import static org.testng.Assert.assertEquals;

/**
 * @author Seth Musselman
 */
@Test(groups = "unit")
public class FullTest {

  private static final Map<String, Template> templates = new HashMap<>();

  @BeforeSuite
  public void beforeSuite() throws IOException {
    templates.put("b", new Template("b", new StringReader("<strong>${body}</strong>"), new Configuration()));
  }

  @Test
  public void testTheTransformFlag() {
    Parser parser = new BBCodeParser();
    Transformer transformer = new FreeMarkerTransformer(templates);
    Document doc = parser.buildDocument(new DocumentSource("abc[b]bbb[/b]123"));

    ((TagNode) doc.children.stream().filter(node -> node instanceof TagNode).findFirst().get()).transform = false;
    assertEquals(transformer.transform(doc), "abc[b]bbb[/b]123");
  }
}
