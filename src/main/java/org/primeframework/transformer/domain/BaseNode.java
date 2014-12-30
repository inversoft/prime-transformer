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

public abstract class BaseNode implements Node {

  public Document document;

  /**
   * Document source index of the beginning of this node.
   * Example:
   * <pre>
   *   foo [b] bar [/b]
   *       ^
   *       4
   * </pre>
   */
  public int tagBegin;

  /**
   * Document source index of the end of this node.
   * Example:
   * <pre>
   *   foo [b] bar [/b]
   *                  ^
   *                  16
   * </pre>
   */
  public int tagEnd;

  @Override
  public String getRawString() {
    return document.getString(tagBegin, tagEnd);
  }

}
