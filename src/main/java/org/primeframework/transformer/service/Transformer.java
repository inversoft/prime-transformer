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

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.function.Predicate;
import java.util.stream.Collectors;

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
   * @return true if strict mode is enabled. When strict mode is enabled a {@link TransformerException} will be thrown
   * if a tag is not able to be transformed. If set to false, or disabled, when a tag is encountered that is not able to
   * be transformed, the original string including the tag will be returned.
   */
  boolean isStrict();

  /**
   * Sets the strict mode of the transformer, passing a true value will enable strict mode. See {@link #isStrict()} for
   * information about the strict setting.
   *
   * @param strict Whether or not the transformer is strict.
   */
  Transformer setStrict(boolean strict);

  /**
   * Transform the document. The returned string will be defined by the implementation.
   *
   * @param document The document to transform.
   * @return The transformer result.
   * @throws TransformerException If the document could not be transformed due to an error, such as a template execution
   *                              error or an invalid tag.
   */
//  String transform(Document document) throws TransformerException;

  /**
   * Transform the document. The returned string will be defined by the implementation.
   *
   * @param document           The document to transform.
   * @param transformPredicate This predicate will be evaluated on each {@link TagNode}. If it evaluates to false, the
   *                           node will not be transformed.
   * @param transformFunction  A function that can be optionally provided to transform text nodes.
   * @return The transformer result.
   * @throws TransformerException
   */
  String transform(Document document, Predicate<TagNode> transformPredicate, TransformFunction transformFunction)
      throws TransformerException;

  public interface NodeObserver {
    void observe(Node node, String result, int bodyOffset);
  }

  /**
   * Defines a function that can transform text within a Document. This is useful for things like escaping HTML.
   *
   * @author Brian Pontarelli
   */
  public interface TransformFunction {
    /**
     * An HTML escaping transform function.
     */
    TransformFunction HTML_ESCAPE = (node, original) -> original.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;");

    String transform(TextNode node, String original);

    /**
     * An implementation of the TransformFunction that escapes HTML but also manages the Offsets created by the escapes.
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
  }

  /**
   * Transform result object that provides the resulting transformed string and index and offset values that can be used
   * to understand how the the original string could be reconstructed.
   */
  public static class TransformedResult {

    public String result;

    // e.g. at offset 1, shift right 3; at offset 4, shift right 6, etc...
    private Set<Pair<Integer, Integer>> offsets = new TreeSet<>();

    public TransformedResult(String result) {
      this.result = result;
    }

    public TransformedResult(String result, List<Pair<Integer, Integer>> offsets) {
      this.offsets.addAll(offsets);
      this.result = result;
    }

    public void addOffset(int oldIndex, int incrementBy) {
      Pair<Integer, Integer> pair = new Pair<>(oldIndex, incrementBy);
      if (offsets.contains(pair)) {
        offsets.remove(pair);
        pair = new Pair<>(oldIndex, incrementBy * 2);
      }
      offsets.add(pair);
    }

    public int computeOffsetFromIndex(int oldIndex) {
      // sample implementation
      int totalOffset = 0;
      for (Pair<Integer, Integer> offset : offsets) {
        if (oldIndex >= offset.first) {
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

      TransformedResult that = (TransformedResult) o;

      if (!offsets.equals(that.offsets)) {
        return false;
      }
      if (result != null ? !result.equals(that.result) : that.result != null) {
        return false;
      }

      return true;
    }

    @Override
    public int hashCode() {
      int result1 = offsets.hashCode();
      result1 = 31 * result1 + (result != null ? result.hashCode() : 0);
      return result1;
    }

    @Override
    public String toString() {
      return "TransformedResult{" +
          "offsets=[" +
          String.join(", ", offsets.stream().map(o -> o.first + ":" + o.second).collect(Collectors.toList())) +
          "], result='" + result + "\'" +
          "}";
    }
  }
}
