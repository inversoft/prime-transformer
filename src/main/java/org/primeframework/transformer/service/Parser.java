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

import java.util.Map;

import org.primeframework.transformer.domain.Document;
import org.primeframework.transformer.domain.TagAttributes;

/**
 * Parser interface.
 * <p>
 * Parser implementations are only responsible for building the abstract syntax tree (AST). The
 * returned <code>Document</code> holds the original source and an AST that maps the source to index values identifying
 * the source markup tags and content.
 *
 * @author Daniel DeGroff
 */
public interface Parser {

  /**
   * Return a constructed <code>Document</code> representation of the document source.
   * <p>
   * No transformation is done as a
   * part of building the document.
   * <p>
   * The returned document may then be passed into any transformer
   * implementation.
   *
   * @param source        The source string that contains the BBCode.
   * @param tagAttributes The set of attributes for the tags being parsed. The key is the tag name (lowercase) and the
   *                      value is the attributes. Caller must ensure that the attributes are valid by calling the
   *                      {@link TagAttributes#validate()} method before calling.
   *
   * @return The Document that contains the BBCode.
   */
  Document buildDocument(String source, Map<String, TagAttributes> tagAttributes);

  /**
   * Return a constructed <code>Document</code> representation of the document source.
   * <p>
   * No transformation is done as a
   * part of building the document.
   * <p>
   * The returned document may then be passed into any transformer
   * implementation.
   *
   * @param source        The source string that contains the BBCode.
   * @param tagAttributes The set of attributes for the tags being parsed. The key is the tag name (lowercase) and the
   *                      value is the attributes. Caller must ensure that the attributes are valid by calling the
   *                      {@link TagAttributes#validate()} method before calling.
   *
   * @return The Document that contains the BBCode.
   */
  Document buildDocument(char[] source, Map<String, TagAttributes> tagAttributes);
}
