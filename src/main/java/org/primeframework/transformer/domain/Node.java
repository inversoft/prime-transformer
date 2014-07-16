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

public interface Node {

  /**
   * Return the raw string representing the node in the document source. This will include the full tag and body.
   * <p>Example return value:
   * <pre> [url]http://foo.com[/url]</pre></p>
   *
   * @return
   */
  String getRawString();

  /**
   * @return the index of the first character in this node
   */
  int getNodeStart();
}
