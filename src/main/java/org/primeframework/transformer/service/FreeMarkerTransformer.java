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

import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

import freemarker.template.Template;
import org.primeframework.transformer.domain.Document;
import org.primeframework.transformer.domain.Node;
import org.primeframework.transformer.domain.TagNode;
import org.primeframework.transformer.domain.TextNode;

/**
 * FreeMarker transformer implementation.
 *
 * @author Daniel DeGroff
 */
public class FreeMarkerTransformer implements Transformer {
  private final Map<String, Template> templates = new HashMap<>();

  private final boolean strict;

  /**
   * Constructor takes the FreeMarker templates.
   *
   * @param templates The FreeMarker templates used to do the transformation.
   */
  public FreeMarkerTransformer(Map<String, Template> templates) {
    this(templates, false);
  }

  /**
   * Constructor takes the FreeMarker templates and strict mode.
   *
   * @param templates The FreeMarker templates used to do the transformation.
   * @param strict    Determines if the transformer is strict and throws exceptions if a FreeMarker template isn't found
   *                  for a specific tag.
   */
  public FreeMarkerTransformer(Map<String, Template> templates, boolean strict) {
    this.templates.putAll(templates);
    this.strict = strict;
  }

  @Override
  public String transform(Document document, Predicate<TagNode> transformPredicate, TransformFunction transformFunction,
                          NodeConsumer nodeConsumer)
      throws TransformerException {
    Objects.requireNonNull(transformPredicate, "A transform predicate is required");
    return recurse(document, transformPredicate, transformFunction, nodeConsumer);
  }

  private String executeTemplate(Template template, TagNode tagNode, String body) throws TransformerException {
    Map<String, Object> data = new HashMap<>();
    data.put("body", body);
    data.put("attributes", tagNode.attributes);
    data.put("attribute", tagNode.attribute);

    try {
      Writer out = new StringWriter();
      template.process(data, out);
      return out.toString();
    } catch (Exception e) {
      throw new TransformerException("FreeMarker processing failed for template [" + template.getName() + "]\n\t Data model [" + data + "]", e);
    }
  }

  private String recurse(Node node, Predicate<TagNode> transformPredicate, TransformFunction transformFunction,
                         NodeConsumer nodeConsumer)
      throws TransformerException {
    StringBuilder build = new StringBuilder();
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
        build.append(recurse(child, transformPredicate, transformFunction, nodeConsumer));
      }
    } else if (node instanceof TagNode) {
      TagNode tagNode = (TagNode) node;
      String tagName = tagNode.getName().toLowerCase();
      Template template = templates.get(tagName);
      if (template != null && transformPredicate.test(tagNode)) {
        // Transform the children first
        StringBuilder childBuild = new StringBuilder();
        for (Node child : tagNode.children) {
          childBuild.append(recurse(child, transformPredicate, transformFunction, nodeConsumer));
        }

        String body = childBuild.toString();
        String result = executeTemplate(template, tagNode, body);

        if (nodeConsumer != null) {
          nodeConsumer.accept(tagNode, result, body);
        }

        build.append(result);
      } else if (strict && template == null) {
        throw new TransformerException("No template found for tag [" + tagNode.getName() + "]");
      } else {
        build.append(tagNode.getRawString());
      }
    } else {
      throw new TransformerException("Invalid node class [" + node.getClass() + "]");
    }

    return build.toString();
  }
}
