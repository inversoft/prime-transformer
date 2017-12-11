/*
 * Copyright (c) 2014-2017, Inversoft Inc., All Rights Reserved
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

public abstract class BaseNode implements Node {
  public Document document;

  /**
   * The index of the beginning of this node (inclusive):
   *
   * <pre>
   *   foo [b] bar [/b]
   *       ^
   * </pre>
   */
  public int begin;

  /**
   * The index of the end of this node (exclusive):
   *
   * <pre>
   *   foo [b] bar [/b]
   *                   ^
   * </pre>
   */
  public int end;

  @Override
  public String getRawString() {
    if (this instanceof Document) {
      throw new UnsupportedOperationException("This method may only be called on a TagNode or TextNode.");
    }
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

    BaseNode baseNode = (BaseNode) o;

    if (begin != baseNode.begin) {
      return false;
    }
    if (end != baseNode.end) {
      return false;
    }

    return true;
  }

  @Override
  public int hashCode() {
    int result = begin;
    result = 31 * result + end;
    return result;
  }

  public int length() {
    return end - begin;
  }
}
