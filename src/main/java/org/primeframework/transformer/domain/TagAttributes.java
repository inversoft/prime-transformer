/*
 * Copyright (c) 2015, Inversoft Inc., All Rights Reserved
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
package org.primeframework.transformer.domain;

/**
 * Optional attributes specific to a single {@link TagNode} that may be passed to the {@Parser} interface that will
 * affect the behavior used to build the {@link Document}.
 *
 * @author Daniel DeGroff
 */
public class TagAttributes {

  /**
   * Indicates a {@link TagNode} does not require a closing tag. Setting this attribute <code>true</code> does not mean
   * the tag may
   * not be closed.
   * <p>
   * <p>For example, the [*] does not require a closing tag.</p>
   * The following then is properly formed BBCode.
   * <pre>
   *   [list][*]item 1[*]item 2[/list]
   * </pre>
   */
  public boolean doesNotRequireClosingTag;

  /**
   * Indicates a {@link TagNode} has a pre-formatted body and the contents of the body will be returned as a single
   * child {@link TextNode}.
   * <p>
   * For example, [code] tag is often configured to contain pre-formatted content.
   * <p>
   * </p>
   * In the following example, you can see that the body of the first [code] tag contains BBCode, but it will treated
   * as
   * text when setting this attribute <code>true</code>.
   * <pre>
   *   [code]BBCode example: [code] System.out.println("Hello World"); [/code][/code]
   * </pre>
   */
  public boolean hasPreFormattedBody;

  public TagAttributes(boolean doesNotRequireClosingTag, boolean hasPreFormattedBody) {
    this.doesNotRequireClosingTag = doesNotRequireClosingTag;
    this.hasPreFormattedBody = hasPreFormattedBody;
  }

  public boolean validate() {
    // these two attributes are mutually exclusive.
    return hasPreFormattedBody && !doesNotRequireClosingTag;
  }
}
