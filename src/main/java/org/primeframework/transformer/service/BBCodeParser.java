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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * BBCode Parser Implementation.
 *
 * @author Daniel DeGroff
 */
public class BBCodeParser extends AbstractParser {

  private static final Logger LOGGER = LoggerFactory.getLogger(BBCodeParser.class);

  private static final Pattern ATTRIBUTES_PATTERN = Pattern.compile("(\\w+)=\"*((?<=\")[^\"]+(?=\")|([^\\s]+))\"*");

  private static final Set<String> NO_CLOSING_TAG = new HashSet<>(Arrays.asList("*"));

  /**
   * The body of an 'escape' tag is not parsed.
   */
  private static final Set<String> ESCAPE_TAGS = new HashSet<>(Arrays.asList("code", "noparse"));

  @Override
  public Document buildDocument(DocumentSource documentSource) throws ParserException {

    Document document = new Document(documentSource);
    Deque<TagNode> nodes = new ArrayDeque<>();

    try {
      lrParser(document, nodes);
    } catch (ParserException e) {
      throw e;
    } catch (Exception e) {
      LOGGER.error("Failed to parse document source. The document returned will only contain a single text node.\n\t" + documentSource, e);
      document.children.add(new TextNode(document, 0, documentSource.source.length));
    }

    checkForUnclosedTags(nodes);
    return document;
  }

  private void checkForUnclosedTags(Deque<TagNode> nodes) throws ParserException {
    // If the nodes stack is not empty, a tag was not closed properly
    if (!nodes.isEmpty()) {
      TagNode node = nodes.peek();
      // Currently only the [*] does not require a closing tag
      if (NO_CLOSING_TAG.contains(node.getName())) {
        throw new ParserException("Missing enclosing tag for [" + node.getName() + "] at index " + node.tagBegin + ". This tag does not require a " +
           "closing tag itself but must be contained within another tag. \n\t For example, the [*] tag must be contained within a [list] or [ol] tag.");
      } else {
        throw new ParserException("Malformed markup. Missing closing tag for [" + node.getName() + "].");
      }
    }
  }

  private void lrParser(Document document, Deque<TagNode> nodes) throws ParserException {
    int sourceIndex = 0;
    int sourceLength = document.documentSource.source.length;
    boolean transform = true;

    while (sourceIndex < sourceLength) {
      int tagBegin = indexOfOpeningTagOpenCharacter(document, sourceIndex, sourceLength);
      if (tagBegin == -1) {
        if (sourceIndex < sourceLength) {
          TextNode text = new TextNode(document, sourceIndex, sourceLength);
          document.children.add(text);
        }
        break;
      } else if (tagBegin > sourceIndex) {
        if (!transform) {
          tagBegin = getTagBeginOfCurrentNode(document, nodes, sourceLength, tagBegin);
        }
        addNodeToDocument(document, nodes, new TextNode(document, sourceIndex, tagBegin));
      }

      int tagEnd = indexOfOpeningTagCloseCharacter(document, tagBegin, sourceLength, nodes) + 1;
      String openingTag = document.getString(tagBegin + 1, tagEnd - 1);

      StringTokenizer tokenizer = new StringTokenizer(openingTag, " =");
      String tagName = tokenizer.nextToken();

      if (tagName.indexOf('/') == 0) {

        // Found a closing tag
        if (nodes.peek().getName().equalsIgnoreCase(tagName.substring(1))) {
          String thisTagName = nodes.peek().getName();

          // Goodwill... poppin' tags... yeah!
          TagNode popped = nodes.pop();
          popped.bodyEnd = tagBegin;
          popped.tagEnd = tagEnd;

          addNodeToDocument(document, nodes, popped);

          // End of an escaped tag, re-enable the transform flag
          if (ESCAPE_TAGS.contains(thisTagName)) {
            transform = true;
          }
        } else {
          // If closing tag not required for this tag, handle
          if (NO_CLOSING_TAG.contains(nodes.peek().getName())) {
            dequeChildNodesAddParentToDocument(document, nodes, tagBegin, tagEnd);
          } else {
            // Expected a closing tag, one was not found.
            throw new ParserException("Malformed BBCode. A closing tag was expected but not found for tag [" + nodes.peek().getName()
               + "] starting at index " + nodes.peek().tagBegin + ".");
          }
        }

      } else {

        // Build a tag fragment and push it on the stack
        TagNode tag = new TagNode(document, tagBegin);
        tag.bodyBegin = tagEnd;
        tag.nameEnd = tagBegin + 1 + tagName.length();
        tag.transform = transform;
        tag.hasClosingTag = !NO_CLOSING_TAG.contains(tagName.toLowerCase());
        // Set the initial value for tagEnd, final value will be set later if it has a closing tag.
        tag.tagEnd = tag.bodyBegin;

        // Collect attributes
        if (tokenizer.hasMoreTokens()) {
          tag.attributesBegin = tag.nameEnd;

          // Re-initialize the tokenizer for simple attributes
          tokenizer = new StringTokenizer(openingTag.substring(tagName.length()), "=");
          if (tokenizer.countTokens() == 1) {
            // simple attribute
            tag.attribute = removeQuotes(tokenizer.nextToken());
            addAttributeOffset(document, tagBegin + tagName.length() + 2, tag.attribute.length());

          } else {
            // complex attributes
            String attributes = openingTag.substring(tagName.length()).trim();
            Matcher matcher = ATTRIBUTES_PATTERN.matcher(attributes);
            int index = tagBegin + tagName.length() + 1;
            while (matcher.find()) {
              String key = matcher.group(1);
              String value = matcher.group(2);
              tag.attributes.put(key, value);
              int attributeStart = matcher.start(2);
              while (Character.isWhitespace(document.documentSource.source[attributeStart])) {
                attributeStart++;
              }
              addAttributeOffset(document, index + attributeStart, value.length());
            }
          }
        }
        nodes.push(tag);

        // When an escape tag is found, sub-sequent nodes will be set to transform false until the tag is closed.
        if (ESCAPE_TAGS.contains(tagName)) {
          transform = false;
        }
      }
      sourceIndex = tagEnd;
    }
  }

  /**
   * Called when transform is set to false. Returns the beginning index of the tag which is currently on the nodes stack.
   *
   * @param document
   * @param nodes
   * @param sourceLength
   * @param tagBegin
   * @return
   * @throws ParserException
   */
  private int getTagBeginOfCurrentNode(Document document, Deque<TagNode> nodes, int sourceLength, int tagBegin) throws ParserException {
    // When transform is false, find closing tag and treat the body as text.
    int tempTagBegin = tagBegin;
    int tempTagEnd = indexOfOpeningTagCloseCharacter(document, tempTagBegin, sourceLength, nodes) + 1;
    String tempTagName = document.getString(tempTagBegin + 1, tempTagEnd - 1);
    while (!nodes.peek().getName().equals(tempTagName.substring(1))) {
      tempTagBegin = indexOfOpeningTagOpenCharacter(document, tempTagEnd, sourceLength);
      tempTagEnd = indexOfOpeningTagCloseCharacter(document, tempTagBegin, sourceLength, nodes) + 1;
      tempTagName = document.getString(tempTagBegin + 1, tempTagEnd - 1);
    }
    return tempTagBegin;
  }

  /**
   * Pop all of the nodes from the stack that don't require a closing tag, and we then expect to find a parent tag.
   * <p>For example, <pre>[list][*]Item 1[*]Item 2[/list]</pre></p>
   *
   * @param document
   * @param nodes
   * @param tagBegin
   * @param tagEnd
   */
  private void dequeChildNodesAddParentToDocument(Document document, Deque<TagNode> nodes, int tagBegin, int tagEnd) {

    Deque<TagNode> children = new ArrayDeque<>();
    while (NO_CLOSING_TAG.contains(nodes.peek().getName())) {
      children.push(nodes.pop());
    }
    // Add this tag to the parent
    TagNode parent = nodes.pop();
    while (!children.isEmpty()) {
      TagNode child = children.pop();
      parent.children.add(child);
      addOpenAndClosingTagOffset(document, child);
    }
    parent.bodyEnd = tagBegin;
    parent.tagEnd = tagEnd;
    addNodeToDocument(document, nodes, parent);
  }

  /**
   * Add the provided node to node on the top of the stack if it isn't closed out yet, otherwise add it directly to the document as a top level node.
   *
   * @param document
   * @param nodes
   * @param node
   */
  private void addNodeToDocument(Document document, Deque<TagNode> nodes, Node node) {

    if (nodes.isEmpty()) {
      document.children.add(node);
    } else {
      nodes.peek().children.add(node);
    }

    if (node instanceof TagNode) {
      addOpenAndClosingTagOffset(document, (TagNode) node);
    }
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
    int adjusted = startsWithQuote(document.documentSource.source, attributeStartIndex) ? 1 : 0;
    document.attributeOffsets.add(new Pair<>(attributeStartIndex + adjusted, attributeLength));
  }

  /**
   * Add an offset to the tag offsets stored in the document.
   *
   * @param document
   * @param tag
   */
  private void addOpenAndClosingTagOffset(Document document, TagNode tag) {
    document.offsets.add(new Pair<>(tag.tagBegin, tag.bodyBegin - tag.tagBegin));
    if (tag.hasClosingTag) {
      document.offsets.add(new Pair<>(tag.bodyEnd, tag.tagEnd - tag.bodyEnd));
    }
  }

}