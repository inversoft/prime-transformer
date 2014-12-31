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
import org.primeframework.transformer.domain.Document;
import org.primeframework.transformer.domain.FreeMarkerTemplateDefinition;
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
  private static final Map<String, FreeMarkerTemplateDefinition> DEFAULT_TEMPLATES = new HashMap<>();

  private boolean ready;

  private boolean strict;

  private FreeMarkerTransformer transformer;

  static {
    DEFAULT_TEMPLATES.put("b", new FreeMarkerTemplateDefinition("bold.ftl"));
    DEFAULT_TEMPLATES.put("i", new FreeMarkerTemplateDefinition("italic.ftl"));
    DEFAULT_TEMPLATES.put("u", new FreeMarkerTemplateDefinition("underline.ftl"));
    DEFAULT_TEMPLATES.put("s", new FreeMarkerTemplateDefinition("strikethrough.ftl"));
    DEFAULT_TEMPLATES.put("*", new FreeMarkerTemplateDefinition("item.ftl", false, false, false));
    DEFAULT_TEMPLATES.put("li",new FreeMarkerTemplateDefinition( "item.ftl"));
    DEFAULT_TEMPLATES.put("list", new FreeMarkerTemplateDefinition("list.ftl"));
    DEFAULT_TEMPLATES.put("ul", new FreeMarkerTemplateDefinition("list.ftl"));
    DEFAULT_TEMPLATES.put("ol", new FreeMarkerTemplateDefinition("ol.ftl"));
    DEFAULT_TEMPLATES.put("url", new FreeMarkerTemplateDefinition("url.ftl"));
    DEFAULT_TEMPLATES.put("table", new FreeMarkerTemplateDefinition("table.ftl"));
    DEFAULT_TEMPLATES.put("tr", new FreeMarkerTemplateDefinition("tr.ftl"));
    DEFAULT_TEMPLATES.put("td", new FreeMarkerTemplateDefinition("td.ftl"));
    DEFAULT_TEMPLATES.put("code", new FreeMarkerTemplateDefinition("code.ftl", true, true, true));
    DEFAULT_TEMPLATES.put("quote", new FreeMarkerTemplateDefinition("quote.ftl"));
    DEFAULT_TEMPLATES.put("email", new FreeMarkerTemplateDefinition("email.ftl"));
    DEFAULT_TEMPLATES.put("img", new FreeMarkerTemplateDefinition("image.ftl"));
    DEFAULT_TEMPLATES.put("size", new FreeMarkerTemplateDefinition("size.ftl"));
    DEFAULT_TEMPLATES.put("sub", new FreeMarkerTemplateDefinition("sub.ftl"));
    DEFAULT_TEMPLATES.put("sup", new FreeMarkerTemplateDefinition("sup.ftl"));
    DEFAULT_TEMPLATES.put("noparse", new FreeMarkerTemplateDefinition("noparse.ftl", true, true, true));
    DEFAULT_TEMPLATES.put("color", new FreeMarkerTemplateDefinition("color.ftl"));
    DEFAULT_TEMPLATES.put("left", new FreeMarkerTemplateDefinition("left.ftl"));
    DEFAULT_TEMPLATES.put("center", new FreeMarkerTemplateDefinition("center.ftl"));
    DEFAULT_TEMPLATES.put("right", new FreeMarkerTemplateDefinition("right.ftl"));
    DEFAULT_TEMPLATES.put("th", new FreeMarkerTemplateDefinition("th.ftl"));
    DEFAULT_TEMPLATES.put("font", new FreeMarkerTemplateDefinition("font.ftl"));
  }

  public BBCodeToHTMLTransformer() {
  }

  public BBCodeToHTMLTransformer(boolean strict) {
    this();
    this.strict = strict;
  }

  public BBCodeToHTMLTransformer init() {
    Map<String, FreeMarkerTemplateDefinition> templates = new HashMap<>();

    Configuration configuration = new Configuration();
    configuration.setTagSyntax(Configuration.SQUARE_BRACKET_TAG_SYNTAX);
    configuration.setClassForTemplateLoading(this.getClass(), "/org/primeframework/transformer/templates/bbCode");

    for (String templateName : DEFAULT_TEMPLATES.keySet()) {
      try {
        FreeMarkerTemplateDefinition templateDefinition = DEFAULT_TEMPLATES.get(templateName);
        templateDefinition.template = configuration.getTemplate(templateDefinition.fileName);
        templates.put(templateName, templateDefinition);
      } catch (IOException e) {
        throw new TransformerRuntimeException("Failed to load FreeMarker template " + templateName + " from classpath.", e);
      }
    }
    transformer = new FreeMarkerTransformer(templates);
    ready = true;
    return this;
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
  public String transform(Document document, Predicate<TagNode> transformPredicate, TransformFunction transformFunction) throws TransformerException {
    if (!ready) {
      throw new TransformerException("Transformer has not yet been initialized. Run init() prior to transform().");
    }
    transformer.setStrict(strict);
    return transformer.transform(document, transformPredicate, null);
  }
}
