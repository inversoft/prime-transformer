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

import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.primeframework.transformer.domain.Document;
import org.primeframework.transformer.domain.Node;
import org.primeframework.transformer.domain.Pair;
import org.primeframework.transformer.domain.TagNode;
import org.primeframework.transformer.domain.TextNode;
import org.primeframework.transformer.domain.TransformerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

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
   * <p>We don't want to allow something like
   * <pre>
   *   [b]&lt;script&gt; window.location.replace("http://foo.bar"); &lt;/script&gt;[/b]
   * </pre></p>
   */
  private boolean escapeHtml = true;

  private boolean strict;

  private Map<String, Template> templates = new HashMap<>();

  /**
   * Constructor takes the FreeMarker templates.
   *
   * @param templates
   */
  public FreeMarkerTransformer(Map<String, Template> templates) {
    this.templates.putAll(templates);
  }

  /**
   * Constructor takes the FreeMarker templates and strict mode.
   *
   * @param templates
   * @param escapeHtml
   * @param strict
   */
  public FreeMarkerTransformer(Map<String, Template> templates, boolean strict, boolean escapeHtml) {
    this.escapeHtml = escapeHtml;
    this.strict = strict;
    this.templates.putAll(templates);
  }

  /**
   * Constructor takes the FreeMarker templates and strict mode.
   *
   * @param templates
   * @param strict
   */
  public FreeMarkerTransformer(Map<String, Template> templates, boolean strict) {
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

  @Override
  public TransformedResult transform(Document document) throws TransformerException {
    return transform(document, null);
  }

  @Override
  public TransformedResult transform(Document document, Predicate<TagNode> transformPredicate) throws TransformerException {
    StringBuilder sb = new StringBuilder();
    List<Pair<Integer, Integer>> offsets = new ArrayList<>();
    for (Node node : document.children) {
      offsets.addAll(transformNode(sb, node, transformPredicate));
    }
    return new TransformedResult(sb.toString().trim(), offsets);
  }

  private List<Pair<Integer, Integer>> transformNode(StringBuilder sb, Node node) throws TransformerException {
    return transformNode(sb, node, null);
  }

  private List<Pair<Integer, Integer>> transformNode(StringBuilder sb, Node node, Predicate<TagNode> predicate) throws TransformerException {

    List<Pair<Integer, Integer>> offsets = new ArrayList<>();

    if (node instanceof TagNode) {
      TagNode tag = (TagNode) node;
      // If tag is marked for transformation, apply the predicate if provided
      boolean transform = tag.transform ? (predicate == null || predicate.test(tag)) : false;
      if (transform) {
        doTransform(sb, offsets, tag);
      } else {
        if (escapeHtml) {
          String raw = tag.getRawString();
          sb.append(escapeHtml(raw));
        } else {
          sb.append(tag.getRawString());
        }
      }
    } else { // TextNode
      TextNode tag = (TextNode) node;
      if (escapeHtml) {
        String body = tag.getBody();
        sb.append(escapeHtml(body));
      } else {
        sb.append(tag.getBody());
      }
    }

    return offsets;
  }

  private StringBuilder escapeHtml(String input) {

    StringBuilder escaped = new StringBuilder(input.length() * 2);
    for (int i = 0; i < input.length(); i++) {
      switch (input.charAt(i)) {
        case '&':
          escaped.append("&amp;");
          break;
        case '<':
          escaped.append("&lt;");
          break;
        case '>':
          escaped.append("&gt;");
          break;
        case '"':
          escaped.append("&quot;");
          break;
        default:
          escaped.append(input.charAt(i));
      }
    }
    return escaped;
  }

  private void doTransform(StringBuilder sb, List<Pair<Integer, Integer>> offsets, TagNode tag) throws TransformerException {

    int offset = tag.getNodeStart();
    StringBuilder childSB = new StringBuilder();
    for (Node child : tag.children) {
      offsets.addAll(transformNode(childSB, child));
    }

    Map<String, Object> data = new HashMap<>(3);
    data.put("body", childSB.toString());
    data.put("attributes", tag.attributes);
    data.put("attribute", tag.attribute);

    String lowerCaseTagName = tag.getName().toLowerCase();
    if (templates.containsKey(lowerCaseTagName)) {
      Template template = templates.get(lowerCaseTagName);
      try {
        int transformedLength = appendTransformedNodeToBuilder(data, sb, template);
        addTransformationOffsets(tag, offsets, offset, template, transformedLength);
      } catch (Exception e) {
        throw new TransformerException("FreeMarker processing failed for template " + template.getName() + " \n\t Data model: " + data.get("body"), e);
      }
    } else {
      // If strict mode is enabled, throw an exception, else append the raw string from the node
      if (strict) {
        throw new TransformerException("No template found for tag [" + tag.getName() + "]");
      }
      sb.append(tag.getRawString());
    }

  }

  /**
   * Append the transformed node to the {@link StringBuilder} and return the length of the transformed node.
   *
   * @param data
   * @param sb
   * @param template
   * @return the length of the transformed node.
   * @throws IOException
   * @throws TemplateException
   */
  private int appendTransformedNodeToBuilder(Map<String, Object> data, StringBuilder sb, Template template) throws IOException, TemplateException {
    int length = sb.length();
    Writer out = new StringWriter();
    template.process(data, out);
    sb.append(out.toString());
    return sb.length() - length;
  }

  private void addTransformationOffsets(TagNode tag, List<Pair<Integer, Integer>> offsets, int offset, Template template, int transformedNodeLength) throws IOException, TemplateException {
    // compute the offsets
    if (tag.hasClosingTag) {
      // case2) the tag does contain a body
      //      X = the index in the input string of the ${body}
      //      Y = the index in the out string of the childSB
      int x = tag.bodyBegin - tag.tagBegin + offset;
      int y = getBodyOffset(template, tag);
      if (y == -1) {
        LOGGER.warn("Offsets are incorrect. Body offset could not be found.");
      }
      offsets.add(new Pair<>(x, y - x + offset));  // for the portion after the opening tag
    }
    int x = tag.tagEnd;
    int cumulativeOffsets = offsets.stream().mapToInt(p -> p.second > 0 ? p.second : 0).sum();
    int y = tag.hasClosingTag ? transformedNodeLength - (tag.tagEnd - tag.tagBegin) - cumulativeOffsets : 0;
    offsets.add(new Pair<>(x, y));
  }

  private int getBodyOffset(Template template, TagNode tag) throws IOException, TemplateException {
    Map<String, Object> data = new HashMap<>(3);
    data.put("body", BODY_MARKER);
    data.put("attributes", tag.attributes);
    data.put("attribute", tag.attribute);
    Writer out = new StringWriter();
    template.process(data, out);
    return out.toString().indexOf(BODY_MARKER);
  }
}
