/*
 * Copyright (c) 2018, Inversoft Inc., All Rights Reserved
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
package org.primeframework.transformer.jackson;

import com.fasterxml.jackson.databind.module.SimpleModule;
import org.primeframework.transformer.domain.BaseNode;
import org.primeframework.transformer.domain.Document;
import org.primeframework.transformer.domain.TagNode;
import org.primeframework.transformer.domain.TextNode;
import org.primeframework.transformer.jackson.proxy.ProxyBaseNode;
import org.primeframework.transformer.jackson.proxy.ProxyDocument;
import org.primeframework.transformer.jackson.proxy.ProxyTagNode;
import org.primeframework.transformer.jackson.proxy.ProxyTextNode;

/**
 * @author Daniel DeGroff
 */
public class ProxyModule extends SimpleModule {
  @Override
  public void setupModule(SetupContext context) {
    super.setupModule(context);
    context.setMixInAnnotations(BaseNode.class, ProxyBaseNode.class);
    context.setMixInAnnotations(Document.class, ProxyDocument.class);
    context.setMixInAnnotations(TagNode.class, ProxyTagNode.class);
    context.setMixInAnnotations(TextNode.class, ProxyTextNode.class);
  }
}
