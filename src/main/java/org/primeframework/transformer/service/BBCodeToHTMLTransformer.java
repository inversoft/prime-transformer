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

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.primeframework.transformer.domain.Document;
import org.primeframework.transformer.domain.TransformerException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * BBCode to HTML Transformer.
 * <p>
 * This implementation utilizes the provided FreeMarker templates and does not require the caller to provide their own.
 * </p>
 * <p>This a also means the caller has less control over the output. For more control over the generated HTML, provide your
 * own FreeMarker templates and use the {@link FreeMarkerTransformer} class or by using the provided factory.
 * </p>
 */
public class BBCodeToHTMLTransformer implements Transformer {

    private static final Map<String, String> DEFAULT_TEMPLATES = new HashMap<>();
    private FreeMarkerTransformer transformer;

    static {
        DEFAULT_TEMPLATES.put("b", "bold.ftl");
        DEFAULT_TEMPLATES.put("i", "italic.ftl");
        DEFAULT_TEMPLATES.put("u", "underline.ftl");
        DEFAULT_TEMPLATES.put("s", "strikethrough.ftl");
        DEFAULT_TEMPLATES.put("*", "item.ftl");
        DEFAULT_TEMPLATES.put("li", "item.ftl");
        DEFAULT_TEMPLATES.put("list", "list.ftl");
        DEFAULT_TEMPLATES.put("ul", "list.ftl");
        DEFAULT_TEMPLATES.put("ol", "ol.ftl");
        DEFAULT_TEMPLATES.put("url", "url.ftl");
        DEFAULT_TEMPLATES.put("table", "table.ftl");
        DEFAULT_TEMPLATES.put("tr", "tr.ftl");
        DEFAULT_TEMPLATES.put("td", "td.ftl");
        DEFAULT_TEMPLATES.put("code", "code.ftl");
        DEFAULT_TEMPLATES.put("quote", "quote.ftl");
        DEFAULT_TEMPLATES.put("email", "email.ftl");
        DEFAULT_TEMPLATES.put("img", "image.ftl");
        DEFAULT_TEMPLATES.put("size", "size.ftl");
        DEFAULT_TEMPLATES.put("sub", "sub.ftl");
        DEFAULT_TEMPLATES.put("sup", "sup.ftl");
        DEFAULT_TEMPLATES.put("noparse", "noparse.ftl");
        DEFAULT_TEMPLATES.put("color", "color.ftl");
        DEFAULT_TEMPLATES.put("left", "left.ftl");
        DEFAULT_TEMPLATES.put("center", "center.ftl");
        DEFAULT_TEMPLATES.put("right", "right.ftl");
    }

    public BBCodeToHTMLTransformer() {
        Map<String, Template> templates = new HashMap<>();

        Configuration configuration = new Configuration();
        configuration.setClassForTemplateLoading(this.getClass(), "/org/primeframework/transformer/templates/bbCode");

        for (String template : DEFAULT_TEMPLATES.keySet()) {
            try {
                templates.put(template, configuration.getTemplate(DEFAULT_TEMPLATES.get(template)));
            } catch (IOException e) {
                throw new TransformerException("Failed to load FreeMarker template " + template + " from classpath.", e);
            }
        }
        transformer = new FreeMarkerTransformer(templates);
    }

    @Override
    public String transform(Document document) {
        // transform and then replace line return and tabs
        return transformer.transform(document).replaceAll("\n", "<br>").replaceAll("\t", "&nbsp;&nbsp;");
    }
}
