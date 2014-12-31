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

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Predicate;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.primeframework.transformer.domain.Document;
import org.primeframework.transformer.domain.FreeMarkerTemplateDefinition;
import org.primeframework.transformer.domain.Node;
import org.primeframework.transformer.domain.TagNode;
import org.primeframework.transformer.domain.TextNode;
import org.primeframework.transformer.domain.TransformerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * FreeMarker transformer implementation.
 *
 * @author Daniel DeGroff
 */
public class FreeMarkerTransformer implements Transformer {
  private final static Logger LOGGER = LoggerFactory.getLogger(FreeMarkerTransformer.class);

  private final static String BODY_MARKER = "xxx" + UUID.randomUUID() + "xxx";

  /**
   * This makes an assumption that the FreeMarker templates contain HTML. For now this is a safe assumption.
   * <p>
   * We don't want to allow something like
   * <p>
   * <pre>
   *   [b]&lt;script&gt; window.location.replace("http://foo.bar"); &lt;/script&gt;[/b]
   * </pre>
   */
  private boolean escapeHtml = true;

  private boolean strict;

  private Map<String, FreeMarkerTemplateDefinition> templates = new HashMap<>();

  /**
   * Constructor takes the FreeMarker templates.
   *
   * @param templates The FreeMarker templates used to do the transformation.
   */
  public FreeMarkerTransformer(Map<String, FreeMarkerTemplateDefinition> templates) {
    this.templates.putAll(templates);
  }

  /**
   * Constructor takes the FreeMarker templates and strict mode.
   *
   * @param templates  The FreeMarker templates used to do the transformation.
   * @param strict     Determines if the transformer is strict and throws exceptions if a FreeMarker template isn't
   *                   found for a specific tag.
   * @param escapeHtml Determines if the HTML inside text nodes and tag bodies is escaped.
   */
  public FreeMarkerTransformer(Map<String, FreeMarkerTemplateDefinition> templates, boolean strict, boolean escapeHtml) {
    this.escapeHtml = escapeHtml;
    this.strict = strict;
    this.templates.putAll(templates);
  }

  /**
   * Constructor takes the FreeMarker templates and strict mode.
   *
   * @param templates The FreeMarker templates used to do the transformation.
   * @param strict    Determines if the transformer is strict and throws exceptions if a FreeMarker template isn't found
   *                  for a specific tag.
   */
  public FreeMarkerTransformer(Map<String, FreeMarkerTemplateDefinition> templates, boolean strict) {
    this.strict = strict;
    this.templates.putAll(templates);
  }

  @Override
  public boolean isStrict() {
    return strict;
  }

  @Override
  public Transformer setStrict(boolean strict) {
    this.strict = strict;
    return this;
  }

//  @Override
//  public String transform(Document document) throws TransformerException {
//    return transform(document, null);
//  }

  @Override
  public String transform(Document document, Predicate<TagNode> transformPredicate, TransformFunction transformFunction)
      throws TransformerException {
    Objects.requireNonNull(transformPredicate, "A transform predicate is required");
    return recurse(document, transformPredicate, transformFunction);
  }

  private String recurse(Node node, Predicate<TagNode> transformPredicate, TransformFunction transformFunction)
      throws TransformerException {
    StringBuilder build = new StringBuilder();
    if (node instanceof TextNode) {
      TextNode textNode = (TextNode) node;
      String text = textNode.getBody();
      if (transformFunction != null) {
        text = transformFunction.transform(textNode, text);
      }

      build.append(text);
    } else if (node instanceof Document) {
      Document document = (Document) node;
      for (Node child : document.children) {
        build.append(recurse(child, transformPredicate, transformFunction));
      }
    } else if (node instanceof TagNode) {
      StringBuilder childBuild = new StringBuilder();
      TagNode tagNode = (TagNode) node;
      for (Node child : tagNode.children) {
        childBuild.append(recurse(child, transformPredicate, transformFunction));
      }

      String tagName = tagNode.getName().toLowerCase();
      if (templates.containsKey(tagName) && transformPredicate.test(tagNode)) {
        Map<String, Object> data = new HashMap<>();
        data.put("body", childBuild.toString());
        data.put("attributes", tagNode.attributes);
        data.put("attribute", tagNode.attribute);

        Template template = templates.get(tagName).template;
        try {
          Writer out = new StringWriter();
          template.process(data, out);
          String result = out.toString();
          build.append(result);
        } catch (Exception e) {
          throw new TransformerException("FreeMarker processing failed for template " + template.getName() + "\n\t Data model: " + data, e);
        }
      } else {
        // If strict mode is enabled, throw an exception, else append the raw string from the node
        if (strict) {
          throw new TransformerException("No template found for tag [" + tagNode.getName() + "]");
        }

        build.append(tagNode.getRawString());
      }
    } else {
      throw new TransformerException("Invalid node class [" + node.getClass() + "]");
    }
    return build.toString();
  }

  private int getBodyOffset(FreeMarkerTemplateDefinition definition, TagNode tag) throws IOException, TemplateException {
    Map<String, Object> data = new HashMap<>(3);
    data.put("body", BODY_MARKER);
    data.put("attributes", tag.attributes);
    data.put("attribute", tag.attribute);
    Writer out = new StringWriter();
    definition.template.process(data, out);
    return out.toString().indexOf(BODY_MARKER);
  }
}
