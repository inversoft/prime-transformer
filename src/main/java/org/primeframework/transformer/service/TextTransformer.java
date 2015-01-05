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
package org.primeframework.transformer.service;

import java.util.function.Predicate;

import freemarker.template.Template;
import org.primeframework.transformer.domain.Document;
import org.primeframework.transformer.domain.Node;
import org.primeframework.transformer.domain.TagNode;
import org.primeframework.transformer.domain.TextNode;

/**
 * Text only Transformer implementation. The document source is stripped of all tags and only the text remains.
 * <p>
 * Example:
 * <pre>
 *     Document document = new Document("[b] Hello World![/b]")
 *     new TextTransformer().transform(document, null, null, null) == " Hello World!"
 * </pre>
 *
 * @author Daniel DeGroff
 */
public class TextTransformer implements Transformer {
  @Override
  public String transform(Document document, Predicate<TagNode> transformPredicate, TransformFunction transformFunction,
                          NodeConsumer nodeConsumer) throws TransformerException {
    // Build the plain text version of the document
    StringBuilder build = new StringBuilder();
    recurse(document, build, transformPredicate, transformFunction, nodeConsumer);
    return build.toString();
  }

  private void recurse(Node node, StringBuilder build, Predicate<TagNode> transformPredicate, TransformFunction transformFunction,
                       NodeConsumer nodeConsumer) throws TransformerException {
    if (node instanceof TextNode) {
      TextNode textNode = (TextNode) node;
      String text = textNode.getBody();
      if (transformFunction != null) {
        text = transformFunction.transform(textNode, text);
      }

      if (nodeConsumer != null) {
        nodeConsumer.accept(node, text, text);
      }

      build.append(text);
    } else if (node instanceof Document) {
      Document document = (Document) node;
      for (Node child : document.children) {
        recurse(child, build, transformPredicate, transformFunction, nodeConsumer);
      }
    } else if (node instanceof TagNode) {
      TagNode tagNode = (TagNode) node;
      if (transformPredicate.test(tagNode)) {
        // Transform the children first
        for (Node child : tagNode.children) {
          recurse(child, build, transformPredicate, transformFunction, nodeConsumer);
        }
      } else {
        build.append(tagNode.getRawString());
      }
    } else {
      throw new TransformerException("Invalid node class [" + node.getClass() + "]");
    }
  }
}
