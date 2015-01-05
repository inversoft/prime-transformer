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

package org.primeframework.transformer.domain;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * The <code>Document</code> is the top level {@link Node} in the document model built to represent the document
 * source.
 */
public class Document extends BaseTagNode {
  /**
   * Set of attribute offsets, first value is the beginning of the attribute, the second value is the length.
   * <p>
   * <pre>
   * Example:
   *    a [font size="10" family="verdana"] testing [/font]
   *                  ^           ^
   *                  14,2        26,7
   * </pre>
   */
  public final Set<Pair<Integer, Integer>> attributeOffsets = new TreeSet<>();

  /**
   * Child nodes, may contain both {@link TagNode} or {@link TextNode} types.
   */
  public final List<Node> children = new ArrayList<>();

  /**
   * Set of offsets, first value is the beginning of the tag, the second value is the length. With these values the
   * original string can be reconstructed.
   * <p>
   * <pre>
   * Example:
   *    a [b] testing [/b]
   *      ^           ^
   *      2,3        14,4
   * </pre>
   */
  public final Set<Pair<Integer, Integer>> offsets = new TreeSet<>();

  /**
   * Unstructured source string.
   */
  public final char[] source;

  public Document(String source) {
    this(source.toCharArray());
  }

  public Document(char[] source) {
    this.source = source;
    this.begin = 0;
    this.end = this.source.length;
    this.document = this;
  }

  @Override
  public boolean addChild(Node node) {
    return children.add(node);
  }

  @Override
  public List<Node> getChildren() {
    return children;
  }

  public String getString(int start, int end) {
    return new String(source, start, end - start);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Document document = (Document) o;

    if (!children.equals(document.children)) {
      return false;
    }
    if (!Arrays.equals(source, document.source)) {
      return false;
    }
    if (!offsets.equals(document.offsets)) {
      return false;
    }
    if (!attributeOffsets.equals(document.attributeOffsets)) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = children.hashCode();
    result = 31 * result + Arrays.hashCode(source);
    result = 31 * result + offsets.hashCode();
    result = 31 * result + attributeOffsets.hashCode();
    return result;
  }

  @Override
  public String toString() {
    return "Document{" +
        "children=[" +
        String.join(", ", children.stream().map(Object::toString).collect(Collectors.toList())) +
        "]" +
        ", offsets=[" +
        String.join(", ", offsets.stream().map(o -> o.first + ":" + o.second).collect(Collectors.toList())) +
        "]" +
        ", attributeOffsets=[" +
        String.join(", ", attributeOffsets.stream().map(o -> o.first + ":" + o.second).collect(Collectors.toList())) +
        "]" +
        "}";
  }
}
