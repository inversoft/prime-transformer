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
import java.util.List;
import java.util.stream.Collectors;

/**
 * The <code>Document</code> is the top level {@link Node} in the document model built to represent the document
 * source.
 */
public class Document extends BaseTagNode {

  public List<Node> children = new ArrayList<>();

  public DocumentSource documentSource;

  public Document(DocumentSource documentSource) {
    this.documentSource = documentSource;
    this.tagBegin = 0;
    this.tagEnd = this.documentSource.source.length;
  }

  @Override
  public List<Node> getChildren() {
    return children;
  }

  @Override
  public int getNodeStart() {
    return 0;
  }

  public String getString(int start, int end) {
    return new String(documentSource.source, start, end - start);
  }

  @Override
  public String toString() {
    return "Document{" +
        "children=[" +
        String.join(", ", children.stream().map(Object::toString).collect(Collectors.toList())) +
        "]}";
  }
}
