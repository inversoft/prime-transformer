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
  public Document buildDocument(String source, Map<String, TagAttributes> tagAttributes) throws ParserException {
    return buildDocument(source.toCharArray(), tagAttributes);
  }

  @Override
  public Document buildDocument(char[] source, Map<String, TagAttributes> tagAttributes) throws ParserException {

    Document document = new Document(source);
    Deque<TagNode> nodes = new ArrayDeque<>();

    try {
      parse(document, nodes, tagAttributes);
    } catch (Exception e) {
      throw new ParserException("Failed to parse document source.\n\t" + new String(document.source), e);
    }
    return document;
  }

  /**
   * Finite State Machine parser implementation.
   *
   * @param document      The document to add nodes to.
   * @param nodes         The node queue for state management.
   * @param tagAttributes A map of tag attributes keyed by tag name.
   * @throws ParserException If parsing fails for any reason.
   */
  private void parse(Document document, Deque<TagNode> nodes, Map<String, TagAttributes> tagAttributes)
      throws ParserException {

    final Map<String, TagAttributes> attributes = new HashMap<>();
    if (tagAttributes != null) {
      attributes.putAll(tagAttributes);
    }
    // TODO Validate attributes

    int index = 0;

    String attributeName = null;
    int attributeNameBegin = 0;
    int attributeValueBegin = 0;

    Deque<String> preFormatted = new ArrayDeque<>();

    State state = State.initial;
    TagNode current;
    TextNode textNode = null;
    char[] source = document.source;

    while (index <= source.length) {
      if (index == source.length) {
        state = State.complete;
      }

      switch (state) {

        case initial:
          state = state.nextState(source[index]);
          index++;
          break;

        case tagBegin:
          state = state.nextState(source[index]);
          handleExpectedUnclosedTag(document, attributes, nodes, index);
          if (state == State.closingTagBegin) {
            if (nodes.isEmpty()) {
              state = State.text;
              index--;
            } else {
              nodes.peek().bodyEnd = index - 1;
            }
          } else if (state == State.tagName) {
            nodes.push(new TagNode(document, index - 1));
          } else if (state == State.text) {
            // Bad BBCode or not really a begin, i.e. 'char [] array = new char[1];'
            index--; // back up the pointer and continue in text state.
//            errorObserver.handleError(ErrorState.BAD_TAG, currentNode, index);
//            fixIt();
          }
          index++;
          break;

        case tagName:
          state = state.nextState(source[index]);
          if (state == State.tagBegin) {
            handleUnExpectedOpenTag(document, nodes, index);
          } else if (state != State.tagName) {
            nodes.peek().nameEnd = index;
          }
          index++;
          break;

        case openingTagEnd:
          state = state.nextState(source[index]);
          current = nodes.peek();
          current.end = index; // when tag is closed this will be updated
          current.bodyBegin = index;
          checkForPreFormattedTag(current, preFormatted, attributes);
          index++;
          break;

        case closingTagBegin:
          state = state.nextState(source[index]);
          index++;
          break;

        case closingTagName:
          state = state.nextState(source[index]);
          index++;
          if (state == State.closingTagEnd) {
            handlePreFormattedClosingTag(document, nodes, preFormatted);
            handleCompletedTagNode(document, index, nodes);
          }
          break;

        case closingTagEnd:
          state = state.nextState(source[index]);
          if (state == State.text) {
            if (textNode == null) {
              textNode = new TextNode(document, index, index + 1);
            }
          }
          index++;
          break;

        case complexAttribute:
          state = state.nextState(source[index]);
          if (state == State.complexAttributeName) {
            attributeNameBegin = index;

            TagNode tagNode = nodes.peek();
            if (tagNode.attributesBegin == -1) {
              nodes.peek().attributesBegin = index;
            }
          }
          index++;
          break;

        case complexAttributeName:
          state = state.nextState(source[index]);
          if (state == State.complexAttributeValue) {
            attributeName = document.getString(attributeNameBegin, index);
          }
          index++;
          break;

        case complexAttributeValue:
          state = state.nextState(source[index]);
          if (state == State.unQuotedAttributeValue) {
            attributeValueBegin = index;
          } else if (state == State.singleQuotedAttributeValue || state == State.doubleQuotedAttributeValue) {
            attributeValueBegin = index + 1;
          }
          index++;
          break;

        case doubleQuotedAttributeValue:
          state = state.nextState(source[index]);
          if (state != State.doubleQuotedAttributeValue) {
            nodes.peek().attributes.put(attributeName, document.getString(attributeValueBegin, index));
            document.attributeOffsets.add(new Pair<>(attributeValueBegin, index - attributeValueBegin));
          }
          index++;
          break;

        case singleQuotedAttributeValue:
          state = state.nextState(source[index]);
          if (state != State.singleQuotedAttributeValue) {
            nodes.peek().attributes.put(attributeName, document.getString(attributeValueBegin, index));
            document.attributeOffsets.add(new Pair<>(attributeValueBegin, index - attributeValueBegin));
          }
          index++;
          break;

        case unQuotedAttributeValue:
          state = state.nextState(source[index]);
          if (state != State.unQuotedAttributeValue) {
            nodes.peek().attributes.put(attributeName, document.getString(attributeValueBegin, index));
            document.attributeOffsets.add(new Pair<>(attributeValueBegin, index - attributeValueBegin));
          }
          index++;
          break;

        case simpleAttribute:
          state = state.nextState(source[index]);
          if (state == State.simpleAttributeBody) {
            nodes.peek().attributesBegin = index;
          }
          index++;
          break;

        case simpleAttributeBody:
          state = state.nextState(source[index]);
          if (state != State.simpleAttributeBody) {
            current = nodes.peek();
            document.attributeOffsets.add(new Pair<>(current.attributesBegin, index - current.attributesBegin));
            current.attribute = document.getString(current.attributesBegin, index);
          }
          index++;
          break;

        case simpleAttributeEnd:
          state = state.nextState(source[index]);
          index++;
          break;

        case text:
          state = state.nextState(source[index]);
          if (textNode == null) {
            textNode = new TextNode(document, index - 1, index);
          }
          if (state != State.text) {
            handleCompletedTextNode(document, index, textNode, nodes);
            textNode = null;
          }
          index++;
          break;

        case complete:
          handleDanglingNodes(document, index, nodes, textNode, attributes);
          index++;
          break;

        default:
          throw new IllegalStateException("Illegal parser state : " + state);
      }
    }
  }

  private void removeRelatedOffsets(Document document, TagNode current) {
    // clear out correlating offsets
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
   * Add the provided node to node on the top of the stack if it isn't closed out yet, otherwise add it directly to the
   * document as a top level node.
   *
   * @param document The document to add the node to.
   * @param nodes    The node queue.
   * @param node     The node to add.
   */
  private void addNode(Document document, Deque<TagNode> nodes, Node node) {

    if (nodes.isEmpty()) {
      document.addChild(node);
    } else {
      nodes.peek().addChild(node);
    }

    if (node instanceof TagNode) {
      TagNode tag = (TagNode) node;
      document.offsets.add(new Pair<>(tag.begin, tag.bodyBegin - tag.begin));
      if (tag.hasClosingTag()) {
        document.offsets.add(new Pair<>(tag.bodyEnd, tag.end - tag.bodyEnd));
      }
    }
  }

  private void checkForPreFormattedTag(TagNode current, Deque<String> preFormatted,
                                       Map<String, TagAttributes> attributes) {
    String name = current.getName().toLowerCase();
    if (attributes.containsKey(name)) {
      if (attributes.get(name).preFormattedBody) {
        preFormatted.push(name);
      }
    }
  }

  private void handleCompletedTagNode(Document document, int index, Deque<TagNode> nodes) {
    if (!nodes.isEmpty()) {
      TagNode current = nodes.peek();
      String closingTagName = document.getString(current.bodyEnd + 2, index - 1);
      if (current.getName().equalsIgnoreCase(closingTagName)) {
        TagNode tagNode = nodes.pop();
        tagNode.end = index;
        addNode(document, nodes, tagNode);
      } else {
        handleUnExpectedUnclosedTag(document, index, nodes);
      }
    }
  }

  private void handleCompletedTextNode(Document document, int index, TextNode textNode, Deque<TagNode> nodes) {
    if (textNode != null) {
      textNode.end = index;
      addNode(document, nodes, textNode);
    }
  }

  private void handleDanglingNodes(Document document, int index, Deque<TagNode> nodes, TextNode textNode,
                                   Map<String, TagAttributes> attributes) {
    if (textNode != null) {
      handleCompletedTextNode(document, index, textNode, nodes);
    }
    handleExpectedUnclosedTag(document, attributes, nodes, index);
    handleUnExpectedUnclosedTag(document, index, nodes);
    handleCompletedTagNode(document, index, nodes);
  }

  private void handleExpectedUnclosedTag(Document document, Map<String, TagAttributes> attributes, Deque<TagNode> nodes,
                                         int index) {
    if (!nodes.isEmpty()) {
      // check for tags that don't require a closing tag
      String name = nodes.peek().getName();
      if (name != null) {
        name = name.toLowerCase();
        if (attributes.containsKey(name) && !attributes.get(name).requiresClosingTag) {
          TagNode tagNode = nodes.pop();
          tagNode.bodyEnd = index - 1;
          tagNode.end = index - 1;
          addNode(document, nodes, tagNode);
        }
      }
    }

  }

  private void handlePreFormattedClosingTag(Document document, Deque<TagNode> nodes, Deque<String> preFormatted) {
    if (!preFormatted.isEmpty()) {
      TagNode current = nodes.peek();
      String tagName = preFormatted.peek();
      if (tagName.equalsIgnoreCase(current.getName())) {
        // Clear children and add a single text node
        current.children.clear();
        current.addChild(new TextNode(document, current.bodyBegin, current.bodyEnd));
        // remove completed pre-formatted tag and clear correlating offsets from the document
        preFormatted.pop();
        removeRelatedOffsets(document, current);
      }
    }
  }

  private void handleUnExpectedOpenTag(Document document, Deque<TagNode> nodes, int index) {
    TagNode tagNode = nodes.pop();
    TextNode textNode = tagNode.toTextNode();
    textNode.end = index;
    addNode(document, nodes, textNode);
  }

  private void handleUnExpectedUnclosedTag(Document document, int index, Deque<TagNode> nodes) {
    while (!nodes.isEmpty()) {
      TagNode tagNode = nodes.pop();
      TextNode textNode = tagNode.toTextNode();
      // If tagNode has children, find the last end
      if (tagNode.children.isEmpty()) {
        textNode.end = index;
      } else {
        Node last = tagNode.children.get(tagNode.children.size() - 1);
//        int end = ((BaseNode) last).end;
//        if (end > textNode.end) {
//          textNode.end = end;
//        }
        textNode.end = index;
      }
      addNode(document, nodes, textNode);
    }
  }

  /**
   * Finite States of this parser. Each defined state has one or more state transitions based upon the character at the
   * current index.
   * <pre>
   *
   *          ---------------< tagBody <----------------
   *          |                                         |
   *          ------------------- < ---------------------
   *          |                                         ^
   *           \                                        |
   *   initial --> begin --> tagName --> openingTagEnd ---> complete
   *             \                                       /
   *              \ -------------> text --------------->/
   *               \        ^                          |
   *                 \      |                          |
   *                  \- < ---------- < ---------------
   *
   * </pre>
   */
  private enum State {

    /**
     * Initial state of the parser.
     */
    initial {
      @Override
      public State nextState(char c) {
        if (c == '[') {
          return tagBegin;
        } else {
          return text;
        }
      }
    },

    /**
     * An opening tag character was found: '<code>[</code>' <br> <p>This state does not imply an opening or a closing
     * tag, the subsequent character will identify if this is an opening or closing tag.</p>
     */
    tagBegin {
      @Override
      public State nextState(char c) {
        if (c == '/') {
          return closingTagBegin;
        } else if (c == ']') {
          // No tag name exists, i.e. [], treat as text.
          return text;
        } else {
          return tagName;
        }
      }
    },

    tagName {
      @Override
      public State nextState(char c) {
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
      public State nextState(char c) {

        if (c == ']') {
          return openingTagEnd;
//        } else if (c == '\'') {
//          return singleQuotedAttributeValue;
//        } else if (c == '\"') {
//          return doubleQuotedAttributeValue;
        } else if (c == ' ') {
          // ignore additional white space here
          return complexAttribute;
        } else {
          return complexAttributeName;
        }
      }
    },

    complexAttributeName {
      @Override
      public State nextState(char c) {
        if (c == '=') {
          return complexAttributeValue;
        } else {
          return complexAttributeName;
        }
      }
    },

    /**
     * An attribute value when not quoted is not allowed to contain spaces.
     */
    complexAttributeValue {
      @Override
      public State nextState(char c) {
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
      public State nextState(char c) {
        if (c == '"') {
          return complexAttribute;
        } else {
          return doubleQuotedAttributeValue;
        }
      }
    },

    singleQuotedAttributeValue {
      @Override
      public State nextState(char c) {
        if (c == '\'') {
          return complexAttribute;
        } else {
          return singleQuotedAttributeValue;
        }
      }
    },

    unQuotedAttributeValue {
      @Override
      public State nextState(char c) {
        if ( c == ' ') {
          return complexAttribute;
        } else if (c == ']') {
          return openingTagEnd;
        } else {
          return unQuotedAttributeValue;
        }
      }
    },

    // TODO I could pull out some more states here, currently opening with a single quote and ending with a double
    // TODO won't be detected.  e.g.  [foo bar='test"]abc[/foo]
    simpleAttribute {
      @Override
      public State nextState(char c) {
        if (c == '"' || c == '\'') {
          return simpleAttribute;
        } else if (c == ']') {
          return openingTagEnd;
        } else {
          return simpleAttributeBody;
        }
      }
    },

    simpleAttributeBody {
      @Override
      public State nextState(char c) {
        if (c == '"' || c == '\'') {
          return simpleAttributeEnd;
        } else if (c == ']') {
          return openingTagEnd;
        } else {
          return simpleAttributeBody;
        }
      }
    },

    simpleAttributeEnd {
      @Override
      public State nextState(char c) {
        if (c == ']') {
          return openingTagEnd;
        } else {
          // TODO Error - unexpected
          return text;
        }
      }
    },

    openingTagEnd {
      @Override
      public State nextState(char c) {
        if (c == '[') {
          return tagBegin;
        } else {
          return text;
        }
      }
    },

    closingTagBegin {
      @Override
      public State nextState(char c) {
        if (c == ']') {
          // TODO Error condition - no name of closing tag
          return closingTagEnd;
        } else {
          return closingTagName;
        }
      }
    },

    closingTagName {
      @Override
      public State nextState(char c) {
        if (c == ']') {
          return closingTagEnd;
        } else {
          return closingTagName;
        }
      }
    },

    closingTagEnd {
      @Override
      public State nextState(char c) {
        if (c == '[') {
          return tagBegin;
        } else {
          return text;
        }
      }
    },

    tagBody {
      @Override
      public State nextState(char c) {
        if (c == '[') {
          return tagBegin;
        } else {
          return tagBody;
        }
      }
    },

    text {
      @Override
      public State nextState(char c) {
        if (c == '[') {
          return tagBegin;
        } else {
          return text;
        }
      }
    },

    complete {
      @Override
      public State nextState(char c) {
        return complete;
      }
    };

    public abstract State nextState(char c);
  }
}