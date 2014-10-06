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

package org.primeframework.transformer;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.primeframework.transformer.domain.Document;
import org.primeframework.transformer.domain.DocumentSource;
import org.primeframework.transformer.domain.TagNode;
import org.primeframework.transformer.service.BBCodeParser;
import org.primeframework.transformer.service.FreeMarkerTransformer;
import org.primeframework.transformer.service.Parser;
import org.primeframework.transformer.service.Transformer;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;

import static org.testng.AssertJUnit.assertEquals;

/**
 * @author Seth Musselman
 */
public class FullTest {

  private static final Map<String, Template> templates = new HashMap<>();

  @BeforeClass
  public static void beforeSuite() throws IOException {
    templates.put("b", new Template("b", new StringReader("<strong>${body}</strong>"), new Configuration()));
  }

  @Test
  public void testTheTransformFlag() throws Exception {
    Parser parser = new BBCodeParser();
    Transformer transformer = new FreeMarkerTransformer(templates);
    Document doc = parser.buildDocument(new DocumentSource("abc[b]bbb[/b]123"));

    ((TagNode) doc.children.stream().filter(node -> node instanceof TagNode).findFirst().get()).transform = false;
    assertEquals(transformer.transform(doc).result, "abc[b]bbb[/b]123");
  }
}
