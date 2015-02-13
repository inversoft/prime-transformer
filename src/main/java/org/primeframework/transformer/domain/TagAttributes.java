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

import org.primeframework.transformer.service.Parser;

/**
 * Optional attributes specific to a single {@link TagNode} that may be passed to the {@link Parser} interface that will
 * affect the behavior used to build the {@link Document}.
 *
 * @author Daniel DeGroff
 */
public class TagAttributes {

  /**
   * Indicates a {@link TagNode} does not require a closing tag. Setting this attribute <code>true</code> does not mean
   * the tag may not be closed.
   * <p>
   * For example, the [*] does not require a closing tag.
   * <p>
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
   * In the following example, you can see that the body of the first [code] tag contains BBCode, but it will treated as
   * text when setting this attribute <code>true</code>.
   * <pre>
   *   [code]BBCode example: [code] System.out.println("Hello World"); [/code][/code]
   * </pre>
   */
  public boolean hasPreFormattedBody;

  /**
   * Indicates a {@link TagNode} does not have a body and no closing tag.
   * <pre>
   *   [emoji]
   * </pre>
   */
  public boolean standalone;

  /**
   * Determines if newlines should be transformed during transformation.
   */
  public boolean transformNewLines;

  public TagAttributes(boolean doesNotRequireClosingTag, boolean hasPreFormattedBody, boolean standalone,
                       boolean transformNewLines) {
    this.doesNotRequireClosingTag = doesNotRequireClosingTag;
    this.hasPreFormattedBody = hasPreFormattedBody;
    this.standalone = standalone;
    this.transformNewLines = transformNewLines;
  }

  public boolean validate() {
    // If you have a pre-formatted body, you must have a closing tag and must not be standalone
    if (hasPreFormattedBody) {
      if (doesNotRequireClosingTag) {
        return false;
      } else if (standalone) {
        return false;
      }
    }

    // If you do not require a closing tag you must not be standalone
    if (doesNotRequireClosingTag) {
      return !standalone;
    }
    return true;
  }
}
