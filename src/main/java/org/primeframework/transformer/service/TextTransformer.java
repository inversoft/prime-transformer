package org.primeframework.transformer.service;

/**
 * @author Daniel DeGroff
 */

import org.primeframework.transformer.domain.Document;
import org.primeframework.transformer.domain.TransformerException;

/**
 * Text only Transformer implementation. The document source is stripped of all tags and only the text remains.
 * <p>
 * Example:
 * <pre>
 *     Document document = new Document(new DocumentSource("[b] Hello World![/b]"))
 *     new TextTransformer().transform(document).result == " Hello World! "
 * </pre>
 */
public class TextTransformer implements Transformer {

  @Override
  public boolean isStrict() {
    return false;
  }

  @Override
  public Transformer setStrict(boolean strict) {
    throw new UnsupportedOperationException(this.getClass().getSimpleName() + " does not support strict mode.");
  }

  @Override
  public TransformedResult transform(Document document) throws TransformerException {

    /* Build the plain text version of the document */
    StringBuilder sb = new StringBuilder();
    document.getChildTextNodes().stream().forEach(n -> sb.append(n.getBody()));

    return new TransformedResult(sb.toString());
  }

}
