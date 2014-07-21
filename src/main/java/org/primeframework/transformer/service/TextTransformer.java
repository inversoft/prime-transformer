package org.primeframework.transformer.service;

/**
 * @author Daniel DeGroff
 */

import org.primeframework.transformer.domain.Document;
import org.primeframework.transformer.domain.Pair;
import org.primeframework.transformer.domain.TagNode;
import org.primeframework.transformer.domain.TransformerException;

import java.util.ArrayList;
import java.util.List;

/**
 * Text only Transformer implementation. The document source is stripped of all tags and only the text remains.
 *
 * Example:
 * <pre>
 *     Document document = new Document(new DocumentSource("[b] Hello World![/b]"))
 *     new TextTransformer().transform(document).result == " Hello World! "
 * </pre>
 *
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

    /* Node Start index and offset values */
    List<Pair<Integer, Integer>> forwardOffsets = new ArrayList<>();

    /* Add in all tag start with offsets indicating length of the tag */
    for (TagNode node : document.getChildTagNodes()) {
      forwardOffsets.add(new Pair<>(node.tagBegin, node.bodyBegin - node.tagBegin));
      // Not all nodes require an end tag, only add offset when tagEnd != bodyEnd
      if (node.tagEnd != node.bodyEnd) {
        forwardOffsets.add(new Pair<>(node.bodyEnd, node.tagEnd - node.bodyEnd));
      }
    }

    /* Build the plain text version of the document */
    StringBuilder sb = new StringBuilder();
    document.getChildTextNodes().stream().forEach(n -> {
      sb.append(n.getBody());
    });

    return TransformedResult.Builder(sb.toString()).forwardOffsets((forwardOffsets)).build();
  }

}
