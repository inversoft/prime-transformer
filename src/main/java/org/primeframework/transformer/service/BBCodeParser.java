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
import org.primeframework.transformer.domain.Node;
import org.primeframework.transformer.domain.Pair;
import org.primeframework.transformer.domain.ParserException;
import org.primeframework.transformer.domain.TagNode;
import org.primeframework.transformer.domain.TextNode;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * BBCode Parser Implementation.
 */
public class BBCodeParser extends AbstractParser {

  private static final String OPENING_TAG = "[%s]";

  private static final String CLOSING_TAG = "[/%s]";

  private static final Set<String> NO_CLOSING_TAG = new HashSet<>(Arrays.asList("*"));
  /**
   * The body of an 'escape' tag is not parsed.
   */
  private static final Set<String> ESCAPE_TAGS = new HashSet<>(Arrays.asList("code", "noparse"));

  /**
   * Find the next node between the start and end index and add to the provided node list.
   *
   * @param startIndex
   * @param endIndex
   * @param children
   * @return the working source index
   * @throws ParserException
   */
  private int addNextNode(Document document, int startIndex, int endIndex, List<Node> children) throws ParserException {

    int tagBegin = indexOfOpeningTagOpenCharacter(document, startIndex, endIndex);
    // if there is no next opening tag, set it to the source length
    if (tagBegin == -1) {
      tagBegin = endIndex;
    }

    if (tagBegin > startIndex) {
      // Text node
      TextNode textNode = new TextNode(document, startIndex, tagBegin);
      children.add(textNode);
      startIndex = tagBegin;
    } else {
      // Tag node
      int openingTagEndIndex = indexOfOpeningTagCloseCharacter(document, tagBegin, endIndex);

      // Parse attributes
      String attribute = null;
      Map<String, String> attributes = new LinkedHashMap<>(3);
      int attributesBegin = indexOfCharacter(document, tagBegin, openingTagEndIndex, ' ');
      int equalsIndex = indexOfCharacter(document, tagBegin, openingTagEndIndex, '=');
      if (attributesBegin != -1 && (equalsIndex == -1 || equalsIndex > attributesBegin)) {
        // Handle complex attributes
        int attributeIndex = attributesBegin;
        while (attributeIndex < openingTagEndIndex) {
          equalsIndex = indexOfCharacter(document, attributeIndex, openingTagEndIndex, '=');
          int nextEqualsIndex = indexOfCharacter(document, equalsIndex + 1, openingTagEndIndex, '=');
          int endKeyValueIndex = nextEqualsIndex;
          if (endKeyValueIndex == -1) {
            endKeyValueIndex = openingTagEndIndex;
          } else {
            while (document.documentSource.source[endKeyValueIndex] != ' ') {
              endKeyValueIndex--;
            }
          }
          String key = document.getString(attributeIndex, equalsIndex);
          String value = removeQuotes(document.getString(equalsIndex + 1, endKeyValueIndex));
          attributes.put(key, value);

          addAttributeOffset(document, equalsIndex, value.length());
          attributeIndex = endKeyValueIndex;
        }
      } else {
        // Handle simple attribute
        attributesBegin = indexOfCharacter(document, tagBegin, openingTagEndIndex, '=');
        if (attributesBegin != -1) {
          attribute = removeQuotes(document.getString(attributesBegin + 1, openingTagEndIndex));
          addAttributeOffset(document, attributesBegin, attribute.length());
        }
      }

      int tagNameEndIndex = attributesBegin != -1 ? attributesBegin : openingTagEndIndex;
      int bodyBegin = openingTagEndIndex + 1;
      String tagName = document.getString(tagBegin + 1, tagNameEndIndex);
      String closeTag = String.format(CLOSING_TAG, tagName);
      int bodyEnd = indexOfString(document, bodyBegin, endIndex, closeTag);

      int tagEnd;
      boolean closingTag = true;
      // If no closing tag is found
      if (bodyEnd == -1) {
        if (!NO_CLOSING_TAG.contains(tagName)) {
          throw new ParserException("Malformed markup. No closing tag was found for " + closeTag +
             ". Open tag started at index " + (openingTagEndIndex + 1 - closeTag.length()) + " and ended at " +
             openingTagEndIndex + 1 + ".\n\t" + document.documentSource);
        }
        /*
         *  When no closing tag is not required, the next opening tag will indicate the end of this body.
         *  If no additional opening tags are found, the endIndex will identify the end of this body.
         */
        String openTag = String.format(OPENING_TAG, tagName);
        int nextTagIndex = indexOfString(document, openingTagEndIndex, endIndex, openTag);
        if (nextTagIndex == -1) {
          bodyEnd = endIndex;
        } else {
          bodyEnd = nextTagIndex;
        }
        tagEnd = bodyEnd;
        closingTag = false;
      } else {
        tagEnd = bodyEnd + closeTag.length();
      }

      // Build tag node
      TagNode tag = new TagNode(document, tagBegin, attributesBegin, bodyBegin, bodyEnd, tagEnd, attribute, attributes, closingTag);
      addOpenAndClosingTagOffset(document, tag);

      // A tag such as [code] may not have embedded tags, only a text body.
      if (ESCAPE_TAGS.contains(tagName)) {
        TextNode textNode = new TextNode(document, bodyBegin, bodyEnd);
        tag.children.add(textNode);
      } else {
        // Add sub-nodes to this tag.
        int nestedStartIndex = bodyBegin;
        while (nestedStartIndex < bodyEnd) {
          nestedStartIndex = addNextNode(document, nestedStartIndex, bodyEnd, tag.children);
        }
      }
      children.add(tag);
      startIndex = NO_CLOSING_TAG.contains(tagName) ? bodyEnd : bodyEnd + closeTag.length();
    }
    return startIndex;
  }

  @Override
  public Document buildDocument(DocumentSource documentSource) throws ParserException {
    Document document = new Document(documentSource);
    int sourceIndex = 0;
    int sourceLength = documentSource.source.length;
    // Traverse the source and build nodes
    while (sourceIndex < sourceLength) {
      sourceIndex = addNextNode(document, sourceIndex, sourceLength, document.children);
    }
    return document;
  }

  @Override
  protected char getTagCloseChar() {
    return ']';
  }

  @Override
  protected char getTagOpenChar() {
    return '[';
  }

  /**
   * Add an offset to the attribute offsets stored in the document.
   *
   * @param document
   * @param attributeStartIndex
   * @param attributeLength
   */
  private void addAttributeOffset(Document document, int attributeStartIndex, int attributeLength) {
    int adjusted = startsWithQuote(document.documentSource.source, attributeStartIndex + 1) ? 1 : 0;
    document.attributeOffsets.add(new Pair<>(attributeStartIndex + 1 + adjusted, attributeLength));
  }

  /**
   * Add an offset to the tag offsets stored in the document.
   *
   * @param document
   * @param tag
   */
  private void addOpenAndClosingTagOffset(Document document, TagNode tag) {
    document.offsets.add(new Pair<>(tag.tagBegin, tag.bodyBegin - tag.tagBegin));
    if (tag.tagEnd != tag.bodyEnd) {
      document.offsets.add(new Pair<>(tag.bodyEnd, tag.tagEnd - tag.bodyEnd));
    }
  }

}
