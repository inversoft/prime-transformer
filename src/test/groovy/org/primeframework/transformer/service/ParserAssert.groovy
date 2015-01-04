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
package org.primeframework.transformer.service

import org.primeframework.transformer.domain.BaseTagNode
import org.primeframework.transformer.domain.Document
import org.primeframework.transformer.domain.TagNode
import org.primeframework.transformer.domain.TextNode
import org.testng.Assert

/**
 * Provides an assertion DSL for asserting the parser results.
 *
 * @author Brian Pontarelli
 */
class ParserAssert {
  /**
   * Asserts the results of a parse. This uses a DSL via the closure.
   *
   * @param str The String to parse.
   * @param closure The closure for the DSL.
   */
  public static void assertParse(String str, @DelegatesTo(NodeDelegate.class) Closure closure) {
    Document expected = new Document(str)
    closure.delegate = new NodeDelegate(expected)
    closure()

    Document actual = new BBCodeParser().buildDocument(str);
    Assert.assertEquals(actual, expected)
  }

  public static class NodeDelegate {
    private BaseTagNode node

    public NodeDelegate(BaseTagNode node) {
      this.node = node
    }

    def TextNode(String body, int start, int end) {
      def textNode = new TextNode(node.document, start, end)
      node.children.add(textNode)
      Assert.assertEquals(textNode.body, body)
    }

    def TagNode(String name, int start, int nameEnd, int attributeStart, int bodyBegin, int bodyEnd, int tagEnd,
                String attribute = null,
                Map<String, String> attributes = [:], @DelegatesTo(NodeDelegate.class) Closure closure) {
      TagNode child = new TagNode(node.document, start, nameEnd, attributeStart, bodyBegin, bodyEnd, tagEnd, attribute,
                                  attributes)
      node.children.add(child)

      Assert.assertEquals(child.name, name)

      if (closure != null) {
//        closure.delegate = new NodeDelegate(child)
        BaseTagNode old = node
        node = child
        closure.delegate = this
        closure()
        node = old
      }
    }
  }
}
