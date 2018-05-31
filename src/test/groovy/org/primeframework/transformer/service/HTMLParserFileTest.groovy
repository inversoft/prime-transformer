/*
 * Copyright (c) 2018, Inversoft Inc., All Rights Reserved
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

import groovy.json.JsonBuilder
import org.primeframework.transformer.domain.Document
import org.primeframework.transformer.domain.Node
import org.primeframework.transformer.domain.TagNode
import org.primeframework.transformer.domain.TextNode
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

import static java.util.Collections.emptyMap
import static org.testng.Assert.assertEquals

/**
 * @author Tyler Scott
 */
class HTMLParserFileTest {
  @DataProvider
  Object[][] inOutTests() {
    List<String> files = this.getClass().getResourceAsStream("/org/primeframework/transformer/inputHtml").getText().
        split('\r?\n')

    Object[][] results = new Object[files.size()][2]

    for (int i = 0; i < files.size(); ++i) {
      results[i][0] = this.getClass().getResourceAsStream("/org/primeframework/transformer/inputHtml/" + files.get(i)).
          getText()
      results[i][1] = this.getClass().getResourceAsStream(
          "/org/primeframework/transformer/intermediateParseResults/" + files.get(i).replaceAll("\\.html\$", ".json")).
          getText()
    }

    return results
  }

  @Test(dataProvider = "inOutTests")
  void parseRealSites(String inData, String expectedOutData) {
    def parser = new HTMLParser()
    def doc = parser.buildDocument(inData, emptyMap())

    assertEquals(documentToJSON(doc), expectedOutData)
  }

  private static String documentToJSON(Document document) {
    def builder = new JsonBuilder()

    builder.setContent(toJSON(document))

    return builder.toPrettyString()
  }

  private static Map<String, Object> toJSON(Node node) {
    if (node instanceof Document) {
      def tag = node as Document
      return [
          begin   : tag.begin,
          end     : tag.end,
          children: toJSON(tag.children)
      ]
    } else if (node instanceof TagNode) {
      def tag = node as TagNode
      return [
          begin     : tag.begin,
          end       : tag.end,
          attributes: tag.attributes,
          children  : toJSON(tag.children)
      ]
    } else { // Text node
      def text = node as TextNode
      return [
          begin: text.begin,
          end  : text.end,
          body : text.body
      ]
    }
  }

  private static List<Object> toJSON(List<Node> nodes) {
    return nodes.collect {
      toJSON(it)
    }
  }
}
