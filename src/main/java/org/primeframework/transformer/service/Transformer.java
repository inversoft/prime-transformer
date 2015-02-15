/*
 * Copyright (c) 2015, Inversoft Inc., All Rights Reserved
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

import java.util.Map;
import java.util.function.Predicate;

import org.primeframework.transformer.domain.Document;
import org.primeframework.transformer.domain.Node;
import org.primeframework.transformer.domain.Offsets;
import org.primeframework.transformer.domain.TagAttributes;
import org.primeframework.transformer.domain.TagNode;
import org.primeframework.transformer.domain.TextNode;

/**
 * Transformer interface.
 *
 * @author Daniel DeGroff
 */
public interface Transformer {
  /**
   * Transform the document. The returned string will be defined by the implementation.
   *
   * @param document           The document to transform.
   * @param transformPredicate This predicate will be evaluated on each {@link TagNode}. If it evaluates to false, the
   *                           node will not be transformed.
   * @param transformFunction  A function that can be optionally provided to transform text nodes.
   * @param nodeConsumer       A consumer that accepts each node as they are traversed during the transformation.
   *
   * @return The transformer result.
   *
   * @throws TransformException If the transformation fails for any reason.
   */
  String transform(Document document, Predicate<TagNode> transformPredicate, TransformFunction transformFunction,
                   NodeConsumer nodeConsumer)
      throws TransformException;

  /**
   * Defines a consumer that accepts each node in the Document as they are transformed.
   *
   * @author Brian Pontarelli
   */
  public interface NodeConsumer {
    /**
     * Accepts the given node and its transformed result.
     *
     * @param node    The node.
     * @param result  The complete result of the transformation of the node.
     * @param newBody The new body of the node that is the result of nested transformations.
     */
    void accept(Node node, String result, String newBody);
  }

  /**
   * Defines a function that can transform text within a Document. This is useful for things like escaping HTML.
   *
   * @author Brian Pontarelli
   */
  public interface TransformFunction {
    /**
     * Transforms the given TextNode and original body String  of the TextNode into a different String.
     *
     * @param node     The node.
     * @param original The original body of the node.
     *
     * @return The transformed String.
     */
    String transform(TextNode node, String original);

    /**
     * An implementation of the TransformFunction that escapes HTML. Can handle offsets and ignoring newline
     * transformation.
     *
     * @author Daniel DeGroff
     */
    public static class HTMLTransformFunction implements TransformFunction {
      private final Map<String, TagAttributes> attributes;

      private final Offsets offsets;

      public HTMLTransformFunction() {
        this.offsets = null;
        this.attributes = null;
      }

      public HTMLTransformFunction(Offsets offsets, Map<String, TagAttributes> attributes) {
        this.offsets = offsets;
        this.attributes = attributes;
      }

      @Override
      public String transform(TextNode node, String original) {
        StringBuilder build = new StringBuilder();
        char[] ca = original.toCharArray();
        for (int i = 0; i < ca.length; i++) {
          switch (ca[i]) {
            case '&':
              build.append("&amp;");
              if (offsets != null) {
                offsets.add(node.begin + i, 4);
              }
              break;
            case '<':
              build.append("&lt;");
              if (offsets != null) {
                offsets.add(node.begin + i, 3);
              }
              break;
            case '>':
              build.append("&gt;");
              if (offsets != null) {
                offsets.add(node.begin + i, 3);
              }
              break;
            case '"':
              build.append("&quot;");
              if (offsets != null) {
                offsets.add(node.begin + i, 5);
              }
              break;
            case '\n':
            case '\r':
              String parentTagNode = node.parent != null ? node.parent.getName().toLowerCase() : null;
              if (parentTagNode != null && attributes != null && attributes.containsKey(parentTagNode) &&
                  !attributes.get(parentTagNode).transformNewLines) {
                build.append(ca[i]);
                break;
              }

              if (i + 1 < ca.length && ((ca[i] == '\n' && ca[i + 1] == '\r') || (ca[i] == '\r' && ca[i + 1] == '\n'))) {
                if (offsets != null) {
                  offsets.add(node.begin + i, 3);
                }

                i++;
              } else if (offsets != null) {
                offsets.add(node.begin + i, 4);
              }

              build.append("<br/>");
              break;
            default:
              build.append(ca[i]);
          }
        }

        return build.toString();
      }
    }
  }
}
