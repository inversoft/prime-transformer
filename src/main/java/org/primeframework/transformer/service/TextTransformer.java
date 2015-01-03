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

import org.primeframework.transformer.domain.Document;
import org.primeframework.transformer.domain.TagNode;
import org.primeframework.transformer.domain.TransformerException;

/**
 * Text only Transformer implementation. The document source is stripped of all tags and only the text remains.
 * <p>
 * Example:
 * <pre>
 *     Document document = new Document("[b] Hello World![/b]")
 *     new TextTransformer().transform(document, null, null, null) == " Hello World! "
 * </pre>
 *
 * @author Daniel DeGroff
 */
public class TextTransformer implements Transformer {
  @Override
  public String transform(Document document, Predicate<TagNode> transformPredicate, TransformFunction transformFunction,
                          NodeConsumer nodeConsumer) throws TransformerException {
    // Build the plain text version of the document
    StringBuilder sb = new StringBuilder();
    document.getChildTextNodes().stream().forEach(n -> sb.append(n.getBody()));
    return sb.toString();
  }
}
