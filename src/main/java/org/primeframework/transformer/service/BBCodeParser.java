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
package org.primeframework.transformer.service;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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
    // Ignore trailing space. e.g. [foo size=5    ] bar[/foo]
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
   * Walk the document and collapse adjacent {@link TextNode} tags. When malformed BBCode is encountered they may be
   * converted to a {@link TextNode}, which may result in having adjacent text nodes in the document. Before return the
   * document these should be joined.
   *
   * @param node the node for which the child text nodes will be collapsed
   */
  private void handleAdjacentTextNodes(BaseTagNode node) {
    Iterator<Node> nodes = node.getChildren().iterator();
    Deque<TextNode> textNodes = new ArrayDeque<>(2);
    while (nodes.hasNext()) {
      Node n = nodes.next();
      if (n instanceof TextNode) {
        TextNode current = (TextNode) n;
        // Push the first text node onto the stack
        if (textNodes.isEmpty()) {
          textNodes.push(current);
        } else {
          // if adjacent, adjust the end index and then remove this node
          TextNode first = textNodes.peek();
          if (first.end == current.begin) {
            first.end = current.end;
            nodes.remove();
          } else {
            // no match, set the current at the top of the stack for comparison
            textNodes.push(current);
          }
        }
      } else {
        // if we hit a TagNode, start over and recurse on this node.
        textNodes.clear();
        handleAdjacentTextNodes((TagNode) n);
      }
    }
  }

  private boolean handleClosingTagName(Document document, Map<String, TagAttributes> attributes, int index,
                                    Deque<TagNode> nodes, boolean parsingEnabled) {
    String closingName = closingName(document, index, nodes.peek());
    if (eq(closingName, nodes.peek().getName())) {
      nodes.peek().end = index;
      if (parsingEnabled) {
        handleCompletedTagNode(document, attributes, index, nodes);
      } else {
        handlePreFormattedClosingTag(document, attributes, nodes);
        return true; // Re-enable parsing because we just closed the no-parse tag
      }
    } else if (parsingEnabled) {
      handleExpectedUnclosedTags(document, attributes, nodes);
      handleCompletedTagNode(document, attributes, index, nodes);
    }

    return parsingEnabled;
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

    TagNode current = nodes.peek();
    String closingTagName = closingName(document, index, current);
    // if no closing tag is required, or this is the correct closing tag for this node
    if (doesNotRequireClosingTag(current, attributes) || eq(current.getName(), closingTagName)) {
      TagNode tagNode = nodes.pop();
      tagNode.end = index;
      addNode(document, attributes, tagNode, nodes);
    } else {
      handleUnexpectedState(document, attributes, index, nodes);
    }
  }

  /**
   * Handle document cleanup, intended to be called once all other processing is complete.
   *
   * @param document            the document where the node will be added.
   * @param attributes          the tag attribute map
   * @param index               the current index of the parser state
   * @param nodes               the stack of nodes being used for temporary storage
   * @param textNode            the text node being used for temporary storage
   */
  private void handleDocumentCleanup(Document document, Map<String, TagAttributes> attributes, int index,
                                     Deque<TagNode> nodes, TextNode textNode) {
    // Complete unclosed text node
    if (textNode != null) {
      textNode.end = index;
      addNode(document, attributes, textNode, nodes);
    }

    // Complete an open tag
    if (!nodes.isEmpty() && nodes.peek().bodyBegin == -1) {
      handleOpenTagCompleted(index, nodes);
    }

    // Complete a standalone tag
    if (!nodes.isEmpty() && isStandalone(nodes.peek(), attributes)) {
      TagNode tagNode = nodes.pop();
      tagNode.end = index;
      addNode(document, attributes, tagNode, nodes);
    }

    handleUnclosedPreFormattedTag(document, attributes, index, nodes);
    if (!nodes.isEmpty()) {
      handleUnexpectedState(document, attributes, index, nodes);
    }

    // last tag end should be equal to the index, handle remaining text
    if (!document.children.isEmpty()) {
      BaseNode last = (BaseNode) document.children.get(document.children.size() - 1);
      if (last.end < index) {
        addNode(document, attributes, new TextNode(document, last.end, index), nodes);
      }
    }

    handleAdjacentTextNodes(document);
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
   */
  private void handleExpectedUnclosedTags(Document document, Map<String, TagAttributes> attributes,
                                          Deque<TagNode> nodes) {
    int count = nodes.size();
    // only make # of passes equal to that of the nodes
    while (count > 0) {

      // Add tags not requiring a closing tag to the stack, and then pull them off and add
      Deque<TagNode> stack = new ArrayDeque<>();
      while (!nodes.isEmpty() && doesNotRequireClosingTag(nodes.peek(), attributes)) {
        stack.push(nodes.pop());
        count--;
      }

      // no nodes found that do not require a closing tag
      if (stack.isEmpty()) {
        return;
      }

      // if the parent node is still on the stack, set the bodyEnd
      if (!nodes.isEmpty()) {
        nodes.peek().bodyEnd = stack.getLast().end;
      }

      // pull off each tag and add it to the parent node
      while (!stack.isEmpty()) {
        addNodeWithNoClosingTag(document, attributes, nodes, stack.pop());
      }
      count--;
    }
  }

  /**
   * Handle the end of a the open tag. Set necessary indexes.
   *
   * @param index the current index of the parser state
   * @param nodes the stack of nodes being used for temporary storage
   */
  private void handleOpenTagCompleted(int index, Deque<TagNode> nodes) {
    TagNode current = nodes.peek();
    current.bodyBegin = index;
    current.bodyEnd = index; // adjusted when body end is found
    current.end = index; // adjusted when tag is closed
  }

  /**
   * Handle the closing a pre-formatted tag. When a pre-formatted tag end is found, the body of this tag will be
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
   * @param document   the document where the node will be added.
   * @param attributes the tag attribute map
   * @param nodes      the stack of nodes being used for temporary storage
   */
  private void handlePreFormattedClosingTag(Document document, Map<String, TagAttributes> attributes,
                                            Deque<TagNode> nodes) {
    TagNode tagNode = nodes.pop();
    // Add a single text node for the entire body
    tagNode.addChild(new TextNode(document, tagNode.bodyBegin, tagNode.bodyEnd));
    addNode(document, attributes, tagNode, nodes);
  }

  /**
   * Remove offsets between the beginning and ending index provided.
   *
   * @param offsets the set of offsets to remove from
   * @param begin the starting index
   * @param end the ending index
   */
  private void handleRemovingOffsets(Set<Pair<Integer, Integer>> offsets, int begin, int end) {
    Iterator<Pair<Integer, Integer>> iter = offsets.iterator();
    while (iter.hasNext()) {
      Pair<Integer, Integer> offset = iter.next();
      if (offset.first >= begin && offset.first < end) {
        iter.remove();
      }
    }
  }

  /**
   * Handle an unclosed pre-formatted tag.
   *
   * @param document   the document where the node will be added.
   * @param attributes the tag attribute map
   * @param index      the current index of the parser state
   * @param nodes      the stack of nodes being used for temporary storage
   */
  private void handleUnclosedPreFormattedTag(Document document, Map<String, TagAttributes> attributes, int index,
                                             Deque<TagNode> nodes) {
    if (nodes.isEmpty()) {
      return;
    }

    if (hasPreFormattedBody(nodes.peek(), attributes)) {
      addNode(document, attributes, new TextNode(document, nodes.peek().bodyBegin, index), nodes);
    }
    if (doesNotRequireClosingTag(nodes.peek(), attributes)) {
      handleExpectedUnclosedTags(document, attributes, nodes);
    } else {
      String closingName = closingName(document, index, nodes.peek());
      if (!eq(nodes.peek().getName(), closingName)) {
        handleUnexpectedState(document, attributes, index, nodes);
      }
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
    handleRemovingOffsets(document.offsets, tagNode.begin, index);
    handleRemovingOffsets(document.attributeOffsets, tagNode.begin, index);
    TextNode textNode = tagNode.toTextNode();
    textNode.end = index;
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
   * Return true if the provided {@link TagNode} has an attribute indicating it is a standalone tag.
   *
   * @param tagNode    the tag to validate
   * @param attributes the tag attribute map
   *
   * @return true if this tag is a standalone.
   */
  private boolean isStandalone(TagNode tagNode, Map<String, TagAttributes> attributes) {
    String name = lc(tagNode.getName());
    return attributes.containsKey(name) && attributes.get(name).standalone;
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
    TextNode textNode = null;

    boolean parsingEnabled = true;

    Map<String, TagAttributes> attributes = new HashMap<>();
    if (tagAttributes != null) {
      attributes.putAll(tagAttributes);
    }

    String attributeName = null;
    int attributeNameBegin = 0;
    int attributeValueBegin = 0;

    State state = State.start;
    State previous;

    int index = 0;
    char[] source = document.source;

    while (index <= source.length) {
      previous = state;

      if (index == source.length) {
        state = State.complete;
      }

      switch (state) {

        case start:
        case escape:
        case closingTagBegin:
          state = state.next(source[index]);
          index++;
          break;

        case tagBegin:
          state = state.next(source[index]);
          // No tags to end, malformed, set state to text
          if (state == State.closingTagBegin && nodes.isEmpty()) {
            state = State.text;
          } else if (state == State.tagName && parsingEnabled) {
            nodes.push(new TagNode(document, index - 1));
          }
          if (!nodes.isEmpty()) {
            nodes.peek().bodyEnd = index - 1;
          }
          // Increment only if not in text state
          if (state != State.text) {
            index++;
          }
          break;

        case tagName:
          state = state.next(source[index]);
          if (parsingEnabled) {
            if (state == State.tagBegin) {
              handleUnexpectedState(document, attributes, index, nodes);
            } else if (state != State.tagName) {
              nodes.peek().nameEnd = index;
            }
          }
          index++;
          break;

        case openingTagEnd:
          // Since parsing is enabled (this is not a no-parse tag), we can update the bodyBegin, end, etc indexes. We can
          // also determine if we should disable the parsing based on the tagName
          if (parsingEnabled) {
            handleOpenTagCompleted(index, nodes);
            parsingEnabled = !hasPreFormattedBody(nodes.peek(), attributes);
            if (parsingEnabled && isStandalone(nodes.peek(), attributes)) {
              TagNode tagNode = nodes.pop();
              tagNode.end = index;
              addNode(document, attributes, tagNode, nodes);
            }
          }

          state = state.next(source[index]);
          index++;
          break;

        case closingTagName:
          state = state.next(source[index]);
          index++;
          if (state == State.closingTagEnd) {
            parsingEnabled = handleClosingTagName(document, attributes, index, nodes, parsingEnabled);
          }
          break;

        case closingTagEnd:
          state = state.next(source[index]);
          if (state == State.text && textNode == null && parsingEnabled) {
            textNode = new TextNode(document, index, index + 1);
          }
          index++;
          break;

        case simpleAttribute:
          state = state.next(source[index]);
          if (parsingEnabled) {
            if (state == State.simpleUnQuotedValue) {
              attributeValueBegin = index;
            } else if (state == State.simpleSingleQuotedValue || state == State.simpleDoubleQuotedValue) {
              attributeValueBegin = index + 1;
            }
          }
          index++;
          break;

        case simpleDoubleQuotedValue:
        case simpleSingleQuotedValue:
        case simpleUnQuotedValue:
          state = state.next(source[index]);
          if (parsingEnabled) {
            if (state != previous) {
              addSimpleAttribute(document, attributeValueBegin, index, nodes);
            }
          }
          index++;
          break;

        case complexAttribute:
          state = state.next(source[index]);
          if (parsingEnabled) {
            if (state == State.complexAttributeName) {
              attributeNameBegin = index;
            } else if (state == State.text && parsingEnabled) {
              handleUnexpectedState(document, attributes, index, nodes);
            }
          }
          index++;
          break;

        case complexAttributeName:
          state = state.next(source[index]);
          if (parsingEnabled) {
            if (state == State.complexAttributeValue) {
              attributeName = document.getString(attributeNameBegin, index);
            } else if (state == State.text) {
              handleUnexpectedState(document, attributes, index, nodes);
            }
          }
          index++;
          break;

        case complexAttributeValue:
          state = state.next(source[index]);
          if (parsingEnabled) {
            if (state == State.openingTagEnd) {
              nodes.peek().attributes.put(attributeName, "");  // No attribute value, store empty string
              document.attributeOffsets.add(new Pair<>(index, 0));
            } else if (state == State.complexUnQuotedValue) {
              attributeValueBegin = index;
            } else if (state == State.complexSingleQuotedValue || state == State.complexDoubleQuotedValue) {
              attributeValueBegin = index + 1;
            }
          }
          index++;
          break;

        case complexDoubleQuotedValue:
        case complexSingleQuotedValue:
        case complexUnQuotedValue:
          state = state.next(source[index]);
          if (parsingEnabled) {
            if (state != previous) {
              nodes.peek().attributes.put(attributeName, document.getString(attributeValueBegin, index));
              document.attributeOffsets.add(new Pair<>(attributeValueBegin, index - attributeValueBegin));
            }
          }
          index++;
          break;

        case text:
          state = state.next(source[index]);
          // start a text node
          if (textNode == null && parsingEnabled) {
            textNode = new TextNode(document, index - 1, index);
          }
          if (state != State.text && parsingEnabled) {
            textNode.end = index;
            addNode(document, attributes, textNode, nodes);
            textNode = null;
          }
          index++;
          break;

        case complete: // accepting state
          handleDocumentCleanup(document, attributes, index, nodes, textNode);
          index++;
          break;
      }
    }
  }

  /**
   * Finite States of this parser. Each defined state has one or more state transitions based upon the character at the
   * current index.
   */
  private enum State {

    start {
      @Override
      public State next(char c) {
        if (c == '[') {
          return tagBegin;
        } else if (c == '\\') {
          return escape;
        } else {
          return text;
        }
      }
    },

    escape {
      @Override
      public State next(char c) {
        return text;
      }
    },

    tagBegin {
      @Override
      public State next(char c) {
        if (c == '/') {
          return closingTagBegin;
        } else if (Character.isWhitespace(c) || c == '[' || c == ']') {
          // No tag name exists, i.e. [], treat as text.
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

    simpleAttribute {
      @Override
      public State next(char c) {
        if (c == ']') {
          return openingTagEnd;
        } else if (c == '\'') {
          return simpleSingleQuotedValue;
        } else if (c == '"') {
          return simpleDoubleQuotedValue;
        } else {
          return simpleUnQuotedValue;
        }
      }
    },

    simpleSingleQuotedValue {
      @Override
      public State next(char c) {
        if (c == '\'') {
          return simpleAttribute;
        } else {
          return simpleSingleQuotedValue;
        }
      }
    },

    simpleDoubleQuotedValue {
      @Override
      public State next(char c) {
        if (c == '"') {
          return simpleAttribute;
        } else {
          return simpleDoubleQuotedValue;
        }
      }
    },

    simpleUnQuotedValue {
      @Override
      public State next(char c) {
        if (c == ']') {
          return openingTagEnd;
        } else {
          return simpleUnQuotedValue;
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
          return complexSingleQuotedValue;
        } else if (c == '\"') {
          return complexDoubleQuotedValue;
        } else {
          return complexUnQuotedValue;
        }
      }
    },

    complexDoubleQuotedValue {
      @Override
      public State next(char c) {
        if (c == '"') {
          return complexAttribute;
        } else {
          return complexDoubleQuotedValue;
        }
      }
    },

    complexSingleQuotedValue {
      @Override
      public State next(char c) {
        if (c == '\'') {
          return complexAttribute;
        } else {
          return complexSingleQuotedValue;
        }
      }
    },

    complexUnQuotedValue {
      @Override
      public State next(char c) {
        if (c == ' ') {
          return complexAttribute;
        } else if (c == ']') {
          return openingTagEnd;
        } else {
          return complexUnQuotedValue;
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

    text {
      @Override
      public State next(char c) {
        if (c == '[') {
          return tagBegin;
        } else if (c == '\\') {
          return escape;
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