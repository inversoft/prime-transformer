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
import org.primeframework.transformer.domain.Pair
import org.primeframework.transformer.domain.TagAttributes
import org.primeframework.transformer.domain.TagNode
import org.primeframework.transformer.domain.TextNode
import org.testng.Assert

/**
 * Provides an assertion DSL for asserting the parser results.
 *
 * @author Brian Pontarelli
 */
class ParserAssert {

  static defaultAttributes = ['*'    : new TagAttributes(true, false, false),
                              code   : new TagAttributes(false, true, false),
                              noparse: new TagAttributes(false, true, false),
                              emoji  : new TagAttributes(false, false, true)]
  /**
   * Asserts the results of a parse. This uses a DSL via the closure. Uses the default tag attributes.
   *
   * @param str The String to parse.
   * @param offsets
   * @param attributeOffsets
   * @param closure The closure for the DSL.
   */
  public static void assertParse(String str, List offsets = null, List attributeOffsets = null,
                                 @DelegatesTo(NodeDelegate.class) Closure closure) {
    assertParse(defaultAttributes, str, offsets, attributeOffsets, closure);
  }

  /**
   * Asserts the results of a parse. This uses a DSL via the closure.
   *
   * @param attributes the tag attributes used by the parser.
   * @param str The String to parse.
   * @param offsets
   * @param attributeOffsets
   * @param closure The closure for the DSL.
   */
  public static void assertParse(Map<String, TagAttributes> attributes, String str, List offsets = null,
                                               List attributeOffsets = null,
                                               @DelegatesTo(NodeDelegate.class) Closure closure) {

    Document expected = new Document(str)
    closure.delegate = new NodeDelegate(expected)
    closure()

    // Make sure we've build the Tag Attributes correctly
    //attributes.each { attribute ->
    //  Assert.assertTrue(attribute.getValue().validate(), "TagAttribute [" + attribute.getKey() + "] failed validation.")
    //}

    Document actual = new BBCodeParser().buildDocument(str, attributes);
    if (offsets != null) {
      offsets.each { pair ->
        expected.offsets.add(new Pair<>(pair[0], pair[1]))
      }
    } else {
      expected.offsets.addAll(actual.offsets)
    }

    if (attributeOffsets != null) {
      attributeOffsets.each { pair ->
        expected.attributeOffsets.add(new Pair<>(pair[0], pair[1]))
      }
    } else {
      expected.attributeOffsets.addAll(actual.attributeOffsets)
    }

    Assert.assertEquals(actual, expected)
  }

  public static class NodeDelegate {
    private BaseTagNode node

    public NodeDelegate(BaseTagNode node) {
      this.node = node
    }

    def TextNode(Map attributes) {
      Objects.requireNonNull(attributes['body'], 'The [body] attribute is required')
      Objects.requireNonNull(attributes['start'], 'The [start] attribute is required')
      Objects.requireNonNull(attributes['end'], 'The [end] attribute is required')

      def textNode = new TextNode(node.document, attributes['start'], attributes['end'])
      node.children.add(textNode)
      Assert.assertEquals(textNode.body, attributes['body'])
    }

    def TagNode(Map attributes, @DelegatesTo(NodeDelegate.class) Closure closure = null) {
      Objects.requireNonNull(attributes['name'], 'The [name] attribute is required')
      Objects.requireNonNull(attributes['start'], 'The [start] attribute is required')
      Objects.requireNonNull(attributes['nameEnd'], 'The [nameEnd] attribute is required')
      Objects.requireNonNull(attributes['bodyBegin'], 'The [bodyBegin] attribute is required')
      Objects.requireNonNull(attributes['end'], 'The [end] attribute is required')

      TagNode child = new TagNode(node.document,
                                  attributes['start'],
                                  attributes['nameEnd'],
                                  attributes['bodyBegin'],
                                  attributes['bodyEnd'] != null ? attributes['bodyEnd'] : attributes['bodyBegin'],
                                  attributes['end'],
                                  attributes['attribute'],
                                  attributes['attributes'])
      node.children.add(child)

      Assert.assertEquals(child.name, attributes['name'])

      if (closure != null) {
        BaseTagNode old = node
        node = child
        closure.delegate = this
        closure()
        node = old
      }
    }
  }
}
