package org.primeframework.transformer.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Daniel DeGroff
 */
public abstract class BaseTagNode extends BaseNode {

  public abstract List<Node> getChildren();

  public List<TextNode> getChildTextNodes() {
    List<TextNode> textNodes = new ArrayList<>(getChildren().size());
    getChildren().stream().forEach(n -> {
      if (n instanceof TextNode) {
        textNodes.add((TextNode) n);
      } else {
        TagNode tag = (TagNode) n;
        textNodes.addAll(tag.getChildTextNodes());
      }
    });
    return textNodes;
  }

  public List<TagNode> getChildTagNodes() {
    List<TagNode> tagNodes = new ArrayList<>(getChildren().size());
    if (this instanceof TagNode) {
      tagNodes.add((TagNode) this);
    }
    getChildren().stream().forEach(n -> {
      if (n instanceof TagNode) {
        tagNodes.addAll(((TagNode) n).getChildTagNodes());
      }
    });
    return tagNodes;
  }

}
