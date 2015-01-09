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

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.primeframework.transformer.domain.BaseNode;
import org.primeframework.transformer.domain.BaseTagNode;
import org.primeframework.transformer.domain.Document;
import org.primeframework.transformer.domain.Node;
import org.primeframework.transformer.domain.Pair;
import org.primeframework.transformer.domain.TagAttributes;
import org.primeframework.transformer.domain.TagNode;
import org.primeframework.transformer.domain.TextNode;

/**
 * BBCode Parser Implementation.
 *
 * @author Daniel DeGroff
 */
public class BBCodeParser implements Parser {

  @Override
  public Document buildDocument(String source, Map<String, TagAttributes> tagAttributes) {
    return buildDocument(source.toCharArray(), tagAttributes);
  }

  @Override
  public Document buildDocument(char[] source, Map<String, TagAttributes> tagAttributes) {
    Document document = new Document(source);
    parse(document, tagAttributes);
    return document;
  }

  /**
   * Add the provided node to node on the top of the stack if it isn't closed out yet, otherwise add it directly to the
   * document as a top level node.
   *
   * @param document   the document to add the node to
   * @param attributes the tag attribute map
   * @param node       the node to add
   * @param nodes      the stack of nodes being used for temporary storage
   */
  private void addNode(Document document, Map<String, TagAttributes> attributes, Node node, Deque<TagNode> nodes) {

    if (nodes.isEmpty()) {
      document.addChild(node);
    } else {
      TagNode current = nodes.peek();
      current.addChild(node);
      // Adjust parent indexes, they must be at least large enough to contain the child
      current.bodyEnd = ((BaseNode) node).end;
      if (doesNotRequireClosingTag(current, attributes)) {
        current.end = current.bodyEnd;
      }
    }

    // Add offsets for tag nodes
    if (node instanceof TagNode) {
      TagNode tag = (TagNode) node;
      document.offsets.add(new Pair<>(tag.begin, tag.bodyBegin - tag.begin));
      if (tag.hasClosingTag()) {
        document.offsets.add(new Pair<>(tag.bodyEnd, tag.end - tag.bodyEnd));
      }
    }
  }

  /**
   * Add the provided node to the parent node. Since this node has no closing tag, set bodyEnd and end as necessary.
   *
   * @param document   the document to add the node to
   * @param attributes the tag attribute map
   * @param nodes      the stack of nodes being used for temporary storage
   * @param node       the node to add
   */
  private void addNodeWithNoClosingTag(Document document, Map<String, TagAttributes> attributes, Deque<TagNode> nodes,
                                       TagNode node) {
    if (!node.children.isEmpty()) {
      node.bodyEnd = ((BaseNode) node.children.get(node.children.size() - 1)).end;
      node.end = node.bodyEnd;
    }
    addNode(document, attributes, node, nodes);
  }

  /**
   * Add a simple attribute value to the {@link TagNode} and add the offsets to the {@link Document}.
   *
   * @param document            the document where the node will be added.
   * @param attributeValueBegin the index where the value begins
   * @param index               the current index of the parser state
   * @param nodes               the stack of nodes being used for temporary storage
   */
  private void addSimpleAttribute(Document document, int attributeValueBegin, int index, Deque<TagNode> nodes) {
    TagNode current = nodes.peek();
    String name = document.getString(attributeValueBegin, index);
    // Ignore trailing space on the attribute value. e.g. [foo size=5    ] bar[/foo]
    int length = name.length();
    name = name.trim();

    // Keep the trimmed value and account for the shortened value in the offset
    document.attributeOffsets.add(new Pair<>(attributeValueBegin, index - attributeValueBegin - (length - name.length())));
    current.attribute = name;
  }

  /**
   * Return the closing tag name if it can be determined.
   *
   * @param document the document where the node will be added.
   * @param index    the current index of the parser state
   * @param tag      the tag to find the closing tag name
   *
   * @return the closing tag name, or null if it cannot be determined.
   */
  private String closingName(Document document, int index, TagNode tag) {
    if (tag.bodyEnd != -1 && index > tag.bodyEnd + 2) {
      return document.getString(tag.bodyEnd + 2, index - 1);
    }
    return null;
  }

  /**
   * Return true if the provided {@link TagNode} has an attribute indicating a closing tag is not required.
   *
   * @param tagNode    the tag to validate
   * @param attributes the tag attribute map
   *
   * @return true if this tag does not require a closing tag.
   */
  private boolean doesNotRequireClosingTag(TagNode tagNode, Map<String, TagAttributes> attributes) {
    String name = lc(tagNode.getName());
    return attributes.containsKey(name) && attributes.get(name).doesNotRequireClosingTag;
  }

  /**
   * Null safe case insensitive equals. Nulls will not evaluate to equal, if both values are null, false will be
   * returned.
   *
   * @param s1 first string
   * @param s2 second string
   *
   * @return true if these two strings are equal, if both are <code>null</code> false is returned.
   */
  private boolean eq(String s1, String s2) {
    if (s1 == null && s2 == null) {
      // Intentionally evaluating to false when both are null.
      return false;
    }

    if (s1 == null) {
      return false;
    }

    return s1.equalsIgnoreCase(s2);
  }

  /**
   * Handle completion of a {@link TagNode}. Set the end index of this tag, and then add the node to the {@link
   * Document} or its parent node.
   *
   * @param document   the document where the node will be added.
   * @param attributes the tag attribute map
   * @param index      the current index of the parser state
   * @param nodes      the stack of nodes being used for temporary storage
   */
  private void handleCompletedTagNode(Document document, Map<String, TagAttributes> attributes, int index,
                                      Deque<TagNode> nodes) {

    if (nodes.isEmpty()) {
      return;
    }

    // clear off any expected unclosed tags first
    handleExpectedUnclosedTag(document, attributes, nodes, index);

    TagNode current = nodes.peek();
    String closingTagName = closingName(document, index, current);
    if (eq(current.getName(), closingTagName)) {
      current.end = index;
      if (current.hasClosingTag()) {
        // Set the end of this tag, and add to the document or its parent node.
        TagNode tagNode = nodes.pop();
        tagNode.end = index;
        addNode(document, attributes, tagNode, nodes);
      }
    }
  }

  /**
   * Handle completion of a {@link TextNode}. Set the end index of this tag, and then add the node to the {@link
   * Document} or its parent node.
   *
   * @param document  the document where the node will be added.
   * @param index     the current index of the parser state
   * @param textNodes the stack of text nodes being used for temporary storage
   * @param nodes     the stack of nodes being used for temporary storage
   */
  private void handleCompletedTextNode(Document document, Map<String, TagAttributes> attributes, int index,
                                       Deque<TextNode> textNodes, Deque<TagNode> nodes) {
    if (!textNodes.isEmpty()) {
      textNodes.peek().end = index;
      addNode(document, attributes, textNodes.pop(), nodes);
    }
  }

  /**
   * Handle document cleanup, intended to be called once all other processing is complete.
   *
   * @param document     the document where the node will be added.
   * @param attributes   the tag attribute map
   * @param index        the current index of the parser state
   * @param textNodes    the stack of text nodes being used for temporary storage
   * @param nodes        the stack of nodes being used for temporary storage
   * @param preFormatted the stack of pre-formatted tag names found during processing
   */
  private void handleDocumentCleanup(Document document, Map<String, TagAttributes> attributes, int index,
                                     Deque<TextNode> textNodes, Deque<TagNode> nodes, Deque<String> preFormatted) {
    // Order of these cleanup steps is intentional
    handleCompletedTextNode(document, attributes, index, textNodes, nodes);
    handleExpectedUnclosedTag(document, attributes, nodes, index);
    handleUnExpectedUnclosedTag(document, attributes, index, nodes, preFormatted);
    handleCompletedTagNode(document, attributes, index, nodes);
    joinAdjacentTextNodes(document);
  }

  /**
   * Handle an expected unclosed {@link TagNode}. An expected unclosed tag can only be identified when the tag has a
   * corresponding tag attribute indicating one is not required. <p>For example:</p> The bullet tags [*] do not require
   * a closing tag.
   * <pre> [list][*]item 1[*]item 2[/list]</pre>
   *
   * @param document   the document where the node will be added.
   * @param attributes the tag attribute map
   * @param nodes      the stack of nodes being used for temporary storage
   * @param index      the current index of the parser state
   */
  private void handleExpectedUnclosedTag(Document document, Map<String, TagAttributes> attributes, Deque<TagNode> nodes,
                                         int index) {
    if (nodes.isEmpty()) {
      return;
    }

    TagNode current = nodes.peek();
    String closingTagName = closingName(document, index, current);
    // no closing tag name was found, if tag has a closing tag, bail
    if (closingTagName == null && current.hasClosingTag()) {
      return;
    } else if (eq(current.getName(), closingTagName)) {
      // found a matching closing tag name, bail
      return;
    }

    // Add tags not requiring a closing tag to the stack, and then pull them off and add
    Deque<TagNode> stack = new ArrayDeque<>();
    while (!nodes.isEmpty() && doesNotRequireClosingTag(nodes.peek(), attributes)) {
      stack.push(nodes.pop());
    }
    if (!stack.isEmpty()) {
      if (!nodes.isEmpty()) {
        // if the parent node is still on the stack, set the bodyEnd
        nodes.peek().bodyEnd = stack.getLast().end;
      }
      // pull off each tag and add it to the parent node
      while (!stack.isEmpty()) {
        addNodeWithNoClosingTag(document, attributes, nodes, stack.pop());
      }
    }
  }

  /**
   * Handle the end of a the open tag. Set necessary indexes and check for pre-formatted tags.
   *
   * @param tagNode      the current node on the stack
   * @param attributes   the tag attribute map
   * @param index        the current index of the parser state
   * @param preFormatted the stack of pre-formatted tag names found during processing
   */
  private void handleOpenTagCompleted(TagNode tagNode, Map<String, TagAttributes> attributes,
                                      int index, Deque<String> preFormatted) {
    // if this node already hasn't been handled
    tagNode.bodyBegin = index;
    tagNode.bodyEnd = index; // if a body is found, this will be adjusted
    tagNode.end = index; // adjusted when tag is closed
    // Keep track of pre-formatted tags
    if (hasPreFormattedBody(tagNode, attributes)) {
      preFormatted.push(lc(tagNode.getName()));
    }
  }

  /**
   * Handle the closing a pre-formatted tag. When a pre-formatted tag end is found, all children of this tag will be
   * converted to a single text node. <p> For example:
   * <pre>
   *     [code][b] System.out.println("Hello World"); [/b][/code]
   *   </pre>
   * The [code] tag will contain one child, a [b] node which itself will have a text node that is the body. This call
   * will collapse the [b] tag node into a single text node this this body
   * <pre>
   *   [b] System.out.println("Hello World"); [/b]
   *   </pre>
   * </p>
   *
   * @param document     the document where the node will be added.
   * @param nodes        the stack of nodes being used for temporary storage
   * @param preFormatted the stack of pre-formatted tag names found during processing
   */
  private void handlePreFormattedClosingTag(Document document, Deque<TagNode> nodes, Deque<String> preFormatted) {

    if (preFormatted.isEmpty()) {
      return;
    }

    // Remove all child nodes, and add a single text node
    TagNode current = nodes.peek();
    if (eq(preFormatted.peek(), current.getName())) {
      current.children.clear();
      current.addChild(new TextNode(document, current.bodyBegin, current.bodyEnd));
      preFormatted.pop();
      removeRelatedOffsets(document, current);
    }
  }

  /**
   * Handle an unexpected tag that is not closed properly.
   *
   * @param document     the document where the node will be added.
   * @param attributes   the tag attribute map
   * @param index        the current index of the parser state
   * @param nodes        the stack of nodes being used for temporary storage
   * @param preFormatted the stack of pre-formatted tag names found during processing
   */
  private void handleUnExpectedUnclosedTag(Document document, Map<String, TagAttributes> attributes, int index,
                                           Deque<TagNode> nodes, Deque<String> preFormatted) {
    while (!nodes.isEmpty()) {

      // if this is the last node on the stack, bail if we find a closing tag, or a pre-formatted tag
      if (nodes.size() == 1) {
        TagNode current = nodes.peek();
        String closingName = closingName(document, index, current);
        boolean closingTagFound = eq(closingName, current.getName());
        // not in a pre-formatted tag, if closing tag name is found, bail
        if (preFormatted.isEmpty() && closingTagFound) {
          return;
        } else if (eq(current.getName(), preFormatted.peek()) && closingTagFound) {
          // if a closing tag is found for the pre-formatted tag, bail
          return;
        }
      }

      TagNode tagNode = nodes.pop();
      TextNode textNode = tagNode.toTextNode();
      removeRelatedOffsets(document, tagNode);
      // If tagNode has children, find the last end
      if (tagNode.children.isEmpty()) {
        textNode.end = index;
      } else {
        if (nodes.isEmpty()) {
          textNode.end = index;
        } else {
          textNode.end = ((BaseNode) tagNode.children.get(tagNode.children.size() - 1)).end;
        }
      }
      addNode(document, attributes, textNode, nodes);
    }
  }

  /**
   * Handle an unexpected state transition. The current current {@link TagNode} will be converted to a {@link
   * TextNode}.
   *
   * @param document   the document where the node will be added.
   * @param attributes the tag attribute map
   * @param index      the current index of the parser state
   * @param nodes      the stack of nodes being used for temporary storage
   */
  private void handleUnexpectedState(Document document, Map<String, TagAttributes> attributes, int index,
                                     Deque<TagNode> nodes) {
    TagNode tagNode = nodes.pop();
    TextNode textNode = tagNode.toTextNode();
    textNode.end = index;
    removeRelatedOffsets(document, tagNode);
    addNode(document, attributes, textNode, nodes);
  }

  /**
   * Return true if the provided {@link TagNode} has an attribute indicating the body is pre-formatted.
   *
   * @param tagNode    the tag to validate
   * @param attributes the tag attribute map
   *
   * @return true if this tag has a pre-formatted body
   */
  private boolean hasPreFormattedBody(TagNode tagNode, Map<String, TagAttributes> attributes) {
    String name = lc(tagNode.getName());
    return attributes.containsKey(name) && attributes.get(name).hasPreFormattedBody;
  }

  /**
   * Walk the document and collapse adjacent {@link TextNode} tags. When malformed BBCode is encountered they may be
   * converted to a {@link TextNode}, which may result in having adjacent text nodes in the document. Before return the
   * document these should be joined.
   *
   * @param node the node for which the child text nodes will be collapsed
   */
  private void joinAdjacentTextNodes(BaseTagNode node) {
    Iterator<Node> nodes = node.getChildren().iterator();
    Deque<TextNode> textNodes = new ArrayDeque<>(2);
    while (nodes.hasNext()) {
      Node n = nodes.next();
      // Push the first text node onto the stack
      if (n instanceof TextNode) {
        TextNode current = (TextNode) n;
        if (textNodes.isEmpty()) {
          textNodes.push(current);
        } else {
          // if adjacent, adjust the end index and then remove this node
          TextNode first = textNodes.peek();
          if (first.end == current.begin) {
            first.end = current.end;
            nodes.remove();
          }
        }
      } else {
        // if we hit a TagNode, start over and recurse on this node.
        textNodes.clear();
        joinAdjacentTextNodes((TagNode) n);
      }
    }
  }

  /**
   * Null safe lowercase.
   *
   * @param string string input to return as a lowercase.
   *
   * @return a lowercase version of the input string, or null if the input string is null.
   */
  private String lc(String string) {
    return string == null ? null : string.toLowerCase();
  }

  /**
   * Finite State Machine parser implementation.
   *
   * @param document      The document to add nodes to.
   * @param tagAttributes A map of tag attributes keyed by tag name.
   */
  private void parse(Document document, Map<String, TagAttributes> tagAttributes) {

    // temporary stack storage for processing
    Deque<TagNode> nodes = new ArrayDeque<>();
    Deque<TextNode> textNodes = new ArrayDeque<>(1);
    Deque<String> preFormatted = new ArrayDeque<>();

    Map<String, TagAttributes> attributes = new HashMap<>();
    if (tagAttributes != null) {
      attributes.putAll(tagAttributes);
    }

    int index = 0;

    String attributeName = null;
    int attributeNameBegin = 0;
    int attributeValueBegin = 0;

    State state = State.initial;
    State previous;

    char[] source = document.source;

    while (index <= source.length) {
      if (index == source.length) {
        state = State.complete;
      }
      previous = state;

      switch (state) {

        case initial:
        case closingTagBegin:
        case simpleAttributeEnd:
          state = state.next(source[index]);
          index++;
          break;

        case tagBegin:
          state = state.next(source[index]);
          // No tags to end, malformed, set state to text
          if (state == State.closingTagBegin && nodes.isEmpty()) {
            state = State.text;
          } else if (state == State.tagName) {
            nodes.push(new TagNode(document, index - 1));
          }
          // Increment only if not in text state
          if (state != State.text) {
            index++;
          }
          break;

        case tagName:
          state = state.next(source[index]);
          if (state == State.tagBegin) {
            handleUnexpectedState(document, attributes, index, nodes);
          } else if (state != State.tagName) {
            nodes.peek().nameEnd = index;
          }
          if (state == State.openingTagEnd) {
            handleOpenTagCompleted(nodes.peek(), attributes, index + 1, preFormatted);
          }
          index++;
          break;

        case openingTagEnd:
          state = state.next(source[index]);
          // coming from tagName this has already been taken care of
          if (previous != State.tagName) {
            handleOpenTagCompleted(nodes.peek(), attributes, index, preFormatted);
          }
          index++;
          break;

        case closingTagName:
          state = state.next(source[index]);
          index++;
          if (state == State.closingTagEnd) {
            handlePreFormattedClosingTag(document, nodes, preFormatted);
            handleCompletedTagNode(document, attributes, index, nodes);
          }
          break;

        case closingTagEnd:
          state = state.next(source[index]);
          if (state == State.text && textNodes.isEmpty()) {
            textNodes.push(new TextNode(document, index, index + 1));
          }
          index++;
          break;

        case simpleAttribute:
          state = state.next(source[index]);
          if (state == State.unQuotedSimpleAttributeValue) {
            attributeValueBegin = index;
          } else if (state == State.singleQuotedSimpleAttributeValue || state == State.doubleQuotedSimpleAttributeValue) {
            attributeValueBegin = index + 1;
          }
          index++;
          break;

        case doubleQuotedSimpleAttributeValue:
        case singleQuotedSimpleAttributeValue:
        case unQuotedSimpleAttributeValue:
          state = state.next(source[index]);
          if (state != previous) {
            addSimpleAttribute(document, attributeValueBegin, index, nodes);
          }
          index++;
          break;

        case complexAttribute:
          state = state.next(source[index]);
          if (state == State.complexAttributeName) {
            attributeNameBegin = index;
          } else if (state == State.text) {
            handleUnexpectedState(document, attributes, index, nodes);
          }
          index++;
          break;

        case complexAttributeName:
          state = state.next(source[index]);
          if (state == State.complexAttributeValue) {
            attributeName = document.getString(attributeNameBegin, index);
          } else if (state == State.text) {
            handleUnexpectedState(document, attributes, index, nodes);
          }
          index++;
          break;

        case complexAttributeValue:
          state = state.next(source[index]);
          if (state == State.openingTagEnd) {
            // No attribute value, store empty string
            nodes.peek().attributes.put(attributeName, "");
            document.attributeOffsets.add(new Pair<>(index, 0));
          } else if (state == State.unQuotedAttributeValue) {
            attributeValueBegin = index;
          } else if (state == State.singleQuotedAttributeValue || state == State.doubleQuotedAttributeValue) {
            attributeValueBegin = index + 1;
          }
          index++;
          break;

        case doubleQuotedAttributeValue:
        case singleQuotedAttributeValue:
        case unQuotedAttributeValue:
          state = state.next(source[index]);
          if (state != previous) {
            nodes.peek().attributes.put(attributeName, document.getString(attributeValueBegin, index));
            document.attributeOffsets.add(new Pair<>(attributeValueBegin, index - attributeValueBegin));
          }
          index++;
          break;

        case text:
          state = state.next(source[index]);
          if (textNodes.isEmpty()) {
            textNodes.push(new TextNode(document, index - 1, index));
          }
          textNodes.peek().end = index;
          if (state != State.text) {
            handleCompletedTextNode(document, attributes, index, textNodes, nodes);
          }
          index++;
          break;

        case complete:
          handleDocumentCleanup(document, attributes, index, textNodes, nodes, preFormatted);
          index++;
          break;
      }
    }
  }

  private void removeRelatedOffsets(Document document, TagNode current) {
    Iterator<Pair<Integer, Integer>> offsets = document.offsets.iterator();
    while (offsets.hasNext()) {
      Pair<Integer, Integer> offset = offsets.next();
      if (offset.first >= current.bodyBegin && offset.first < current.bodyEnd) {
        offsets.remove();
      }
    }

    Iterator<Pair<Integer, Integer>> attributeOffsets = document.attributeOffsets.iterator();
    while (attributeOffsets.hasNext()) {
      Pair<Integer, Integer> offset = attributeOffsets.next();
      if (offset.first > current.bodyBegin && offset.first < current.bodyEnd) {
        attributeOffsets.remove();
      }
    }
  }

  /**
   * Finite States of this parser. Each defined state has one or more state transitions based upon the character at the
   * current index.
   */
  private enum State {

    initial {
      @Override
      public State next(char c) {
        if (c == '[') {
          return tagBegin;
        } else {
          return text;
        }
      }
    },


    tagBegin {
      @Override
      public State next(char c) {
        if (c == '/') {
          return closingTagBegin;
        } else if (c == ']') {
          // No tag name exists, i.e. [], treat as text.
          return text;
        } else if (c == ' ') {
          // No tag name exists, i.e. '[ ' - illegal.
          return text;
        } else {
          return tagName;
        }
      }
    },

    tagName {
      @Override
      public State next(char c) {
        if (c == '=') {
          return simpleAttribute;
        } else if (c == ' ') {
          return complexAttribute;
        } else if (c == ']') {
          return openingTagEnd;
        } else if (c == '[') {
          return tagBegin;
        } else {
          return tagName;
        }
      }
    },

    complexAttribute {
      @Override
      public State next(char c) {
        if (c == ']') {
          return openingTagEnd;
        } else if (c == ' ') {
          // Ignore whitespace
          return complexAttribute;
        } else if (c == '[') {
          return text; // tag is not closed properly
        } else {
          return complexAttributeName;
        }
      }
    },

    complexAttributeName {
      @Override
      public State next(char c) {
        if (c == '=') {
          return complexAttributeValue;
        } else if (c == ' ') {
          return text; // no spaces allowed between name and equals
        } else if (c == ']') {
          return text; // missing name and value of attribute
        } else {
          return complexAttributeName;
        }
      }
    },

    complexAttributeValue {
      @Override
      public State next(char c) {
        if (c == ']') {
          return openingTagEnd;
        } else if (c == ' ') {
          return complexAttribute;
        } else if (c == '\'') {
          return singleQuotedAttributeValue;
        } else if (c == '\"') {
          return doubleQuotedAttributeValue;
        } else {
          return unQuotedAttributeValue;
        }
      }
    },

    doubleQuotedAttributeValue {
      @Override
      public State next(char c) {
        if (c == '"') {
          return complexAttribute;
        } else {
          return doubleQuotedAttributeValue;
        }
      }
    },

    singleQuotedAttributeValue {
      @Override
      public State next(char c) {
        if (c == '\'') {
          return complexAttribute;
        } else {
          return singleQuotedAttributeValue;
        }
      }
    },

    unQuotedAttributeValue {
      @Override
      public State next(char c) {
        if (c == ' ') {
          return complexAttribute;
        } else if (c == ']') {
          return openingTagEnd;
        } else {
          return unQuotedAttributeValue;
        }
      }
    },

    simpleAttribute {
      @Override
      public State next(char c) {
        if (c == ']') {
          return openingTagEnd;
        } else if (c == '\'') {
          return singleQuotedSimpleAttributeValue;
        } else if (c == '"') {
          return doubleQuotedSimpleAttributeValue;
        } else {
          return unQuotedSimpleAttributeValue;
        }
      }
    },

    singleQuotedSimpleAttributeValue {
      @Override
      public State next(char c) {
        if (c == '\'') {
          return simpleAttribute;
        } else {
          return singleQuotedSimpleAttributeValue;
        }
      }
    },

    doubleQuotedSimpleAttributeValue {
      @Override
      public State next(char c) {
        if (c == '"') {
          return simpleAttribute;
        } else {
          return doubleQuotedSimpleAttributeValue;
        }
      }
    },

    unQuotedSimpleAttributeValue {
      @Override
      public State next(char c) {
        if (c == ']') {
          return openingTagEnd;
        } else {
          return unQuotedSimpleAttributeValue;
        }
      }
    },

    simpleAttributeEnd {
      @Override
      public State next(char c) {
        if (c == ']') {
          return openingTagEnd;
        } else {
          return text; // tag was not closed
        }
      }
    },

    openingTagEnd {
      @Override
      public State next(char c) {
        if (c == '[') {
          return tagBegin;
        } else {
          return text;
        }
      }
    },

    closingTagBegin {
      @Override
      public State next(char c) {
        if (c == ']') {
          return closingTagEnd; // no name of closing tag
        } else {
          return closingTagName;
        }
      }
    },

    closingTagName {
      @Override
      public State next(char c) {
        if (c == ']') {
          return closingTagEnd;
        } else {
          return closingTagName;
        }
      }
    },

    closingTagEnd {
      @Override
      public State next(char c) {
        if (c == '[') {
          return tagBegin;
        } else {
          return text;
        }
      }
    },

    tagBody {
      @Override
      public State next(char c) {
        if (c == '[') {
          return tagBegin;
        } else {
          return tagBody;
        }
      }
    },

    text {
      @Override
      public State next(char c) {
        if (c == '[') {
          return tagBegin;
        } else {
          return text;
        }
      }
    },

    complete {
      @Override
      public State next(char c) {
        return complete;
      }
    };

    /**
     * Transition the parser to the next state based upon the current character.
     *
     * @param c the current character on the input string.
     *
     * @return the next state of the parser.
     */
    public abstract State next(char c);
  }
}