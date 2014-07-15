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
import org.primeframework.transformer.domain.DocumentSource;

/**
 * Parser interface.
 * <p>Parser implementations are only responsible for building the abstract syntax tree (AST). The
 * returned <code>Document</code> holds the original source and an AST that maps the source to index values identifying
 * the source markup tags and content.
 */
public interface Parser {

    /**
     * Return a constructed <code>Document</code> representation of the document source.<p>No transformation
     * is done as a part of building the document.</p><p>The returned document may then be passed into any
     * transformer implementation.</p>
     *
     * @param documentSource
     * @return
     */
    Document buildDocument(DocumentSource documentSource);
}
