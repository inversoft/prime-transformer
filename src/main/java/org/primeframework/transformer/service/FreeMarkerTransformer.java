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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.primeframework.transformer.domain.Document;
import org.primeframework.transformer.domain.Node;
import org.primeframework.transformer.domain.Pair;
import org.primeframework.transformer.domain.TagNode;
import org.primeframework.transformer.domain.TextNode;
import org.primeframework.transformer.domain.TransformerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import freemarker.template.Template;

/**
 * FreeMarker transformer implementation.
 */
public class FreeMarkerTransformer implements Transformer {
  private final static Logger logger = LoggerFactory.getLogger(FreeMarkerTransformer.class);

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
    StringBuilder sb = new StringBuilder();
    List<Pair<Integer, Integer>> offsets = new ArrayList<>();
    for (Node node : document.children) {
      offsets.addAll(transformNode(sb, node));
    }
    return new TransformedResult(sb.toString().trim(), offsets);
  }

  private List<Pair<Integer, Integer>> transformNode(StringBuilder sb, Node node) throws TransformerException {
    int offset = node.getNodeStart();
    List<Pair<Integer, Integer>> offsets = new ArrayList<>();
    if (node instanceof TagNode) {
      TagNode tag = (TagNode) node;
      if (!tag.transform) {
        sb.append(tag.getRawString());
        return offsets;
      }
      StringBuilder childSB = new StringBuilder();
      for (Node child : tag.children) {
        offsets.addAll(transformNode(childSB, child));
      }
      Map<String, Object> data = new HashMap<>(3);
      data.put("body", childSB.toString());
      data.put("attributes", tag.attributes);
      data.put("attribute", tag.attribute);

      Template template = templates.get(tag.getName());
      if (template == null) {
        if (strict) {
          throw new TransformerException("No template found for tag [" + tag.getName() + "]");
        }
        sb.append(tag.getRawString());
      } else {
        try {
          // Get the transformed string
          Writer out = new StringWriter();
          template.process(data, out);
          String transformedNode = out.toString();
          sb.append(transformedNode);

          // compute the offsets
          if (tag.hasClosingTag) {
            // case2) the tag does contain a body
            //      X = the index in the input string of the ${body}
            //      Y = the index in the out string of the childSB
            int x = tag.bodyBegin - tag.tagBegin + offset;
            int y = transformedNode.indexOf(childSB.toString()); // if failed:  fuck
            if (y == -1) {
              logger.warn("Offsets are incorrect, couldn't find '" + childSB.toString() + "' in '" + sb.toString() + "'");
            }
            offsets.add(new Pair<>(x, y - x + offset));  // for the portion after the opening tag
          }
          offsets.add(new Pair<>(tag.tagEnd,
                                 transformedNode.length()
                                     - (tag.tagEnd - tag.tagBegin)
                                     - offsets.stream().mapToInt(p -> p.second).sum()
          ));
        } catch (Exception e) {
          throw new TransformerException("FreeMarker processing failed for template " + template.getName() + " \n\t Data model: " + data.get("body"), e);
        }
      }
    } else { // TextNode
      sb.append(((TextNode) node).getBody());
    }
    return offsets;
  }
}
