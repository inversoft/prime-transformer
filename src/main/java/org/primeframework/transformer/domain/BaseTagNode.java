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

/**
 * @author Daniel DeGroff
 */
public abstract class BaseTagNode extends BaseNode {

  /**
   * Add a node as a child to this node.
   *
   * @param node the node to be added to this nodes children.
   */
  public abstract void addChild(Node node);

  /**
   * @return Return a {@link List} of {@link TagNode} objects.
   */
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
   * Return a {@link List} of {@link Node} objects.
   *
   * @return Return the child nodes. An empty list indicates this node has no children.
   */
  public abstract List<Node> getChildren();
}
