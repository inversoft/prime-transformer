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

package org.primeframework.transformer.domain;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The <code>TagNode</code> holds index values to the document source to outline the start and end of the tag and the
 * body of the tag.
 * <p>
 * The following is an example in BBCode, but the same applies to other tags.
 * <pre>
 *     [url=http://foo.com] http://foo.com [/url]
 *     ^   ^                               ^    ^
 *     1   2               3 <--  body --> 4    5
 *
 *  Between index 1 and 2 is the opening tag.
 *  Between index 2 and 3 is the tag body.
 *  Between index 3 and 4 is the closing tag.
 *
 *  1. tagBegin
 *  2. attributeBegin
 *  3. bodyBegin
 *  4. bodyEnd
 *  5. tagEnd
 * </pre>
 * The body of the tag is contained in the children collection. A child may be either another <code>TagNode</code> or a
 * <code>TextNode</code>. The <code>TagNode</code> itself does not have a body, the body will be contained in a child
 * <code>TextNode</code>.
 */
public class TagNode extends BaseTagNode {

  /**
   * Support for a simple attribute. Example: [tag=foo]bar[/tag]
   */
  public String attribute;
  /**
   * Support for complex attributes. Example: [tag width="100" height="200" title="foo"]bar[/tag]
   */
  public Map<String, String> attributes = new LinkedHashMap<>();
  public List<Node> children = new ArrayList<>();
  public int attributesBegin;
  public int bodyBegin;
  public int bodyEnd;
  public int nameEnd;

  public boolean transform;

  /**
   * These two boolean states indicate what was found when the BBCode was parsed, not necessarily what is required for a specific TagNode.<br>
   * <p>
   * Expected Variations of these two boolean states:
   * <p>
   * <ol>
   * <li>Has closing tag and a body: <pre>[b]foo[/b]</pre></li>
   * <li>Has closing tag and no body: <pre>[gameCard][/gameCard]</pre></li>
   * <li>Has no closing tag and a body: <pre>[*]foo</pre></li>
   * <li>Has no closing tag and no body: <pre>[:)]</pre> (emoticons)</li>
   * </ol>
   */

  public boolean hasClosingTag;
  public boolean hasBody;

  public TagNode(Document document, int tagBegin) {
    this.document = document;
    this.tagBegin = tagBegin;
    this.transform = true;
  }

  public TagNode(Document document, int tagBegin, int attributesBegin, int bodyBegin, int bodyEnd, int tagEnd, String attribute, Map<String, String> attributes) {
    this.document = document;
    this.tagBegin = tagBegin;
    this.attributesBegin = attributesBegin;
    this.bodyBegin = bodyBegin;
    this.bodyEnd = bodyEnd;
    this.tagEnd = tagEnd;
    this.attribute = attribute;
    this.attributes = attributes;
    this.transform = true;
  }

  @Override
  public boolean addChild(Node node) {
    return children.add(node);
  }

  @Override
  public List<Node> getChildren() {
    return children;
  }

  public String getName() {
    return document.getString(tagBegin + 1, nameEnd);
  }

  public TextNode toTextNode() {
    return new TextNode(document, tagBegin, tagEnd);
  }

  @Override
  public String toString() {
    return "TagNode{" +
        "name=" + getName() +
        ", attribute='" + attribute + "'" +
        ", tagBegin=" + tagBegin +
        ", tagEnd=" + tagEnd +
        ", bodyBegin=" + bodyBegin +
        ", bodyEnd=" + bodyEnd +
        ", nameEnd=" + nameEnd +
        ", attributesBegin=" + attributesBegin +
        ", attributes=" + attributes +
        ", children=" + children +
        ", transform=" + transform +
        ", hasClosingTag=" + hasClosingTag +
        '}';
  }
}
