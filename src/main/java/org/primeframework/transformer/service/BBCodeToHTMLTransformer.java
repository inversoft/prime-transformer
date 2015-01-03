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
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.primeframework.transformer.domain.Document;
import org.primeframework.transformer.domain.TagNode;
import org.primeframework.transformer.domain.TransformerException;
import org.primeframework.transformer.domain.TransformerRuntimeException;

/**
 * BBCode to HTML Transformer.
 * <p>
 * This implementation utilizes the provided FreeMarker templates and does not require the caller to provide their own.
 * <p>
 * This a also means the caller has less control over the output. For more control over the generated HTML, provide your
 * own FreeMarker templates and use the {@link FreeMarkerTransformer} class or by using the provided factory.
 *
 * @author Daniel DeGroff
 */
public class BBCodeToHTMLTransformer implements Transformer {
  private static final Map<String, Template> DEFAULT_TEMPLATES = new HashMap<>();

  private FreeMarkerTransformer transformer;

  static {
    Configuration configuration = new Configuration();
    configuration.setTagSyntax(Configuration.SQUARE_BRACKET_TAG_SYNTAX);
    configuration.setClassForTemplateLoading(BBCodeToHTMLTransformer.class, "/org/primeframework/transformer/templates/bbCode");

    try {
      DEFAULT_TEMPLATES.put("b", configuration.getTemplate("bold.ftl"));
      DEFAULT_TEMPLATES.put("i", configuration.getTemplate("italic.ftl"));
      DEFAULT_TEMPLATES.put("u", configuration.getTemplate("underline.ftl"));
      DEFAULT_TEMPLATES.put("s", configuration.getTemplate("strikethrough.ftl"));
      DEFAULT_TEMPLATES.put("*", configuration.getTemplate("item.ftl"));
      DEFAULT_TEMPLATES.put("li", configuration.getTemplate("item.ftl"));
      DEFAULT_TEMPLATES.put("list", configuration.getTemplate("list.ftl"));
      DEFAULT_TEMPLATES.put("ul", configuration.getTemplate("list.ftl"));
      DEFAULT_TEMPLATES.put("ol", configuration.getTemplate("ol.ftl"));
      DEFAULT_TEMPLATES.put("url", configuration.getTemplate("url.ftl"));
      DEFAULT_TEMPLATES.put("table", configuration.getTemplate("table.ftl"));
      DEFAULT_TEMPLATES.put("tr", configuration.getTemplate("tr.ftl"));
      DEFAULT_TEMPLATES.put("td", configuration.getTemplate("td.ftl"));
      DEFAULT_TEMPLATES.put("code", configuration.getTemplate("code.ftl"));
      DEFAULT_TEMPLATES.put("quote", configuration.getTemplate("quote.ftl"));
      DEFAULT_TEMPLATES.put("email", configuration.getTemplate("email.ftl"));
      DEFAULT_TEMPLATES.put("img", configuration.getTemplate("image.ftl"));
      DEFAULT_TEMPLATES.put("size", configuration.getTemplate("size.ftl"));
      DEFAULT_TEMPLATES.put("sub", configuration.getTemplate("sub.ftl"));
      DEFAULT_TEMPLATES.put("sup", configuration.getTemplate("sup.ftl"));
      DEFAULT_TEMPLATES.put("noparse", configuration.getTemplate("noparse.ftl"));
      DEFAULT_TEMPLATES.put("color", configuration.getTemplate("color.ftl"));
      DEFAULT_TEMPLATES.put("left", configuration.getTemplate("left.ftl"));
      DEFAULT_TEMPLATES.put("center", configuration.getTemplate("center.ftl"));
      DEFAULT_TEMPLATES.put("right", configuration.getTemplate("right.ftl"));
      DEFAULT_TEMPLATES.put("th", configuration.getTemplate("th.ftl"));
      DEFAULT_TEMPLATES.put("font", configuration.getTemplate("font.ftl"));
    } catch (IOException e) {
      throw new TransformerRuntimeException("Failed to load FreeMarker template from classpath.", e);
    }
  }

  public BBCodeToHTMLTransformer() {
    this(false);
  }

  public BBCodeToHTMLTransformer(boolean strict) {
    this.transformer = new FreeMarkerTransformer(DEFAULT_TEMPLATES, strict);
  }

  @Override
  public String transform(Document document, Predicate<TagNode> transformPredicate, TransformFunction transformFunction,
                          NodeConsumer nodeConsumer) throws TransformerException {
    return transformer.transform(document, transformPredicate, transformFunction, nodeConsumer);
  }
}
