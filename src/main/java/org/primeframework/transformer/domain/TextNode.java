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

/**
 * The <code>TextNode</code> holds index values to the document source to outline the start and end of the plain text
 * body.
 * <p>
 * The following is an example in BBCode, but the same applies to other tags.
 * <pre>
 *     [url] http://foo.com [/url]
 *     ^   ^                ^    ^
 *         1 <--- body ---> 2
 *
 *  Between index 1 and 2 is the tag body.
 *
 *  1. bodyBegin
 *  2. bodyEnd
 * </pre>
 */
public class TextNode extends BaseNode {
  public TagNode parent;

  public TextNode(Document document, TagNode parent, int begin, int end) {
    this.document = document;
    this.parent = parent;
    this.begin = begin;
    this.end = end;
  }

  public String getBody() {
    return document.getString(begin, end);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    if (!super.equals(o)) {
      return false;
    }

    TextNode textNode = (TextNode) o;

    if (parent != null ? textNode.parent == null || parent.begin != textNode.parent.begin || parent.end != textNode.parent.end : textNode.parent != null) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = super.hashCode();
    result = 31 * result + (parent != null ? parent.begin : 0);
    result = 31 * result + (parent != null ? parent.end : 0);
    return result;
  }

  @Override
  public String toString() {
    return "TextNode{" +
        "body=" + getBody() +
        "}";
  }
}
