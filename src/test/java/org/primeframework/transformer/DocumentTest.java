package org.primeframework.transformer;

import org.primeframework.transformer.domain.Document;
import org.primeframework.transformer.domain.DocumentSource;
import org.primeframework.transformer.domain.TagNode;
import org.primeframework.transformer.service.BBCodeParser;
import org.primeframework.transformer.service.BBCodeToHTMLTransformer;
import org.primeframework.transformer.service.Transformer;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;

/**
 * @author Daniel DeGroff
 */
@Test(groups = "unit")
public class DocumentTest {

  @Test
  public void testWalk() throws Exception {

    Document document = new BBCodeParser().buildDocument(new DocumentSource("Hello [size=\"14\"][b]World!![/b][/size] Yo."));
    document.walk(TagNode.class, node -> node.transform = false);

    // assert all tag nodes are set to not transform
    document.walk(TagNode.class, node -> assertFalse(node.transform));

    Transformer.TransformedResult transformedResult = new BBCodeToHTMLTransformer().init().transform(document);
    assertEquals(transformedResult.result, "Hello [size=\"14\"][b]World!![/b][/size] Yo.");

  }

  @Test
  public void testWalkWithNoType() throws Exception {

    Document document = new BBCodeParser().buildDocument(new DocumentSource("Hello [size=\"14\"][b]World!![/b][/size] Yo."));
    document.walk(node -> {
      if (node instanceof TagNode) {
        ((TagNode) node).transform = false;
      }
    });

    // assert all tag nodes are set to not transform
    document.walk(node -> {
      if (node instanceof TagNode) {
        assertFalse(((TagNode) node).transform);
      }
    });

    Transformer.TransformedResult transformedResult = new BBCodeToHTMLTransformer().init().transform(document);
    assertEquals(transformedResult.result, "Hello [size=\"14\"][b]World!![/b][/size] Yo.");
  }

}
