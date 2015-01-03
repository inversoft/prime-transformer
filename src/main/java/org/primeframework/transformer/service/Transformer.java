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

import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;

import org.primeframework.transformer.domain.Document;
import org.primeframework.transformer.domain.Node;
import org.primeframework.transformer.domain.Pair;
import org.primeframework.transformer.domain.TagNode;
import org.primeframework.transformer.domain.TextNode;
import org.primeframework.transformer.domain.TransformerException;

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
   * @return The transformer result.
   * @throws TransformerException
   */
  String transform(Document document, Predicate<TagNode> transformPredicate, TransformFunction transformFunction,
                   NodeConsumer nodeConsumer)
      throws TransformerException;

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

    /**
     * A node consumer implementation that consumers all TagNode instances and calculates the offsets based on the
     * result of the transformer.
     *
     * @author Brian Pontarelli
     */
    public static class OffsetNodeConsumer implements NodeConsumer {
      private final Offsets offsets;

      public OffsetNodeConsumer(Offsets offsets) {
        this.offsets = offsets;
      }

      @Override
      public void accept(Node node, String result, String newBody) {
        if (!(node instanceof TagNode)) {
          return;
        }

        TagNode tagNode = (TagNode) node;

        // If the tag has no body, we add an offset for the difference between the original length and the new length.
        // This will ensure that the indexes are valid.
        int bodyOffset = newBody.isEmpty() ? -1 : result.indexOf(newBody);
        if (/*!tagNode.hasBody ||*/ bodyOffset == -1) {
          int lengthDelta = result.length() - tagNode.length();
          offsets.add(tagNode.tagEnd, lengthDelta);
          return;
        }

        // Calculate the change between the original body begin (relative to the tag start) and the new body begin.
        // i.e.   [b]foo[/b]   becomes    <strong>foo</strong>
        //        ^  ^                            ^
        //        0  3                            8
        //
        // originalBodyOffset = 3 - 0 = 3
        // bodyOffsetDelta = 8 - 3 = 5
        int originalBodyOffset = tagNode.bodyBegin - tagNode.tagBegin;
        int bodyOffsetDelta = bodyOffset - originalBodyOffset;
        offsets.add(tagNode.bodyBegin, bodyOffsetDelta);

        // Calculate the change between original length and the new length. This gives us the total change. If we subtract
        // the bodyOffsetDelta, it should give us the shift in the end tag as long as the body contents don't change.
        // i.e.   [b]foo[/b]   becomes    <strong>foo</strong>
        //              ^   ^                     ^  ^        ^
        //              6   10                    8  11       20
        //
        // originalEndTagLength = 10 - 6 + 1 = 4
        // newEndTagLength = 20 - 3 - 8 = 9
        // endTagDelta = 9 - 4 = 5
        int originalEndTagLength = tagNode.tagEnd - tagNode.bodyEnd;
        int newEndTagLength = result.length() - newBody.length() - bodyOffset;
        int endTagDelta = newEndTagLength - originalEndTagLength;
        offsets.add(tagNode.tagEnd, endTagDelta);
      }
    }
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
     * @return The transformed String.
     */
    String transform(TextNode node, String original);

    /**
     * An implementation of the TransformFunction that escapes HTML but also manages the Offsets created by the
     * escapes.
     *
     * @author Brian Pontarelli
     */
    public static class OffsetHTMLEscapeTransformFunction implements TransformFunction {
      private final Offsets offsets;

      public OffsetHTMLEscapeTransformFunction(Offsets offsets) {
        this.offsets = offsets;
      }

      @Override
      public String transform(TextNode node, String original) {
        StringBuilder build = new StringBuilder();
        char[] ca = original.toCharArray();
        for (int i = 0; i < ca.length; i++) {
          switch (ca[i]) {
            case '&':
              build.append("&amp;");
              offsets.add(node.tagBegin + i, 4);
              break;
            case '<':
              build.append("&lt;");
              offsets.add(node.tagBegin + i, 3);
              break;
            case '>':
              build.append("&gt;");
              offsets.add(node.tagBegin + i, 3);
              break;
            case '"':
              build.append("&quot;");
              offsets.add(node.tagBegin + i, 5);
              break;
            case '\n':
            case '\r':
              if (i + i < ca.length && (ca[i] == '\n' && ca[i + 1] == '\r') || (ca[i] == '\r' && ca[i + 1] == '\n')) {
                offsets.add(node.tagBegin + i, 3);
                i++;
              } else {
                offsets.add(node.tagBegin + i, 4);
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

  // e.g. at offset 1, shift right 3; at offset 4, shift right 6, etc...

  /**
   * Defines a set of offsets within a String. Each offset is at a position and has an amount. These are stored as Pairs
   * where the first value is the position and the second value is the amount.
   * <p>
   * i.e. at position 15 in the string, shift 3
   * <p>
   * Note that the amount might be negative.
   *
   * @author Brian Pontarelli
   */
  class Offsets {
    private final Set<Pair<Integer, Integer>> offsets = new TreeSet<>();

    /**
     * Adds the offset.
     *
     * @param position The position of the offset from the start of the string.
     * @param amount   The amount of the offset.
     */
    public void add(int position, int amount) {
      offsets.add(new Pair<>(position, amount));
    }

    /**
     * Calculates the new index using the offsets before the given original index.
     *
     * @param originalIndex The original index to translate to a new index.
     * @return The new index.
     */
    public int computeOffsetFromIndex(int originalIndex) {
      int totalOffset = 0;
      for (Pair<Integer, Integer> offset : offsets) {
        if (originalIndex >= offset.first) {
          totalOffset += offset.second;
        }
      }
      return totalOffset;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }

      Offsets offsets1 = (Offsets) o;
      return offsets.equals(offsets1.offsets);
    }

    @Override
    public int hashCode() {
      return offsets.hashCode();
    }

    @Override
    public String toString() {
      return offsets.toString();
    }

    public int total() {
      return offsets.stream().mapToInt((offset) -> offset.second).sum();
    }
  }
}
