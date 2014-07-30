package org.primeframework.transformer.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 * @author Daniel DeGroff
 */
public abstract class BaseTagNode extends BaseNode {


  public abstract List<Node> getChildren();

  public List<TextNode> getChildTextNodes() {
    List<TextNode> textNodes = new ArrayList<>(getChildren().size());
    getChildren().forEach(n -> {
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
    getChildren().forEach(n -> {
      if (n instanceof TagNode) {
        tagNodes.addAll(((TagNode) n).getChildTagNodes());
      }
    });
    return tagNodes;
  }

  /**
   * Walk the document nodes and apply the action to each node of the specified type..
   *
   * @param action
   */
  public <T> Stream<Node> walk(Class<T> consumerType, Consumer<? super T> action) {

    getChildren().forEach(n -> {
      if (consumerType.isAssignableFrom(n.getClass())) {
        T node = (T) n;
        action.accept((T) n);
        if (node instanceof TagNode) {
          ((TagNode) node).walk(consumerType, action);
        }
      }
    });
    return getChildren().stream();
  }

  /**
   * Walk the document nodes and apply the action to each node.
   *
   * @param action
   */
  public Stream<Node> walk(Consumer<? super Node> action) {
    return walk(Node.class, action);
  }

}
