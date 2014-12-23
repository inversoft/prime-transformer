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

import org.primeframework.transformer.domain.Document;
import org.primeframework.transformer.domain.TagNode;
import org.primeframework.transformer.domain.TransformerException;

import java.util.function.Predicate;

/**
 * Text only Transformer implementation. The document source is stripped of all tags and only the text remains.
 * <p>
 * Example:
 * <pre>
 *     Document document = new Document(new DocumentSource("[b] Hello World![/b]"))
 *     new TextTransformer().transform(document).result == " Hello World! "
 * </pre>
 *
 * @author Daniel DeGroff
 */
public class TextTransformer implements Transformer {

  @Override
  public boolean isStrict() {
    return false;
  }

  @Override
  public Transformer setStrict(boolean strict) {
    throw new UnsupportedOperationException(this.getClass().getSimpleName() + " does not support strict mode.");
  }

  @Override
  public TransformedResult transform(Document document) throws TransformerException {

    // Build the plain text version of the document
    StringBuilder sb = new StringBuilder();
    document.getChildTextNodes().stream().forEach(n -> sb.append(n.getBody()));

    return new TransformedResult(sb.toString());
  }

  @Override
  public TransformedResult transform(Document document, Predicate<TagNode> transformPredicate) throws TransformerException {
    throw new UnsupportedOperationException("The TextTransformer does not support taking a predicate on the transform() method.");
  }

}
