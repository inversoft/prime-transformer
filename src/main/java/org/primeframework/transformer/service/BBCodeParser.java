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
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;

import org.primeframework.transformer.domain.BaseNode;
import org.primeframework.transformer.domain.Document;
import org.primeframework.transformer.domain.Node;
import org.primeframework.transformer.domain.Pair;
import org.primeframework.transformer.domain.TagNode;
import org.primeframework.transformer.domain.TextNode;

/**
 * BBCode Parser Implementation.
 *
 * @author Daniel DeGroff
 */
public class BBCodeParser implements Parser {
  /**
   * The body of an 'escape' tag is not parsed.
   */
  // TODO Allow this to be configured in the parser
  private static final Set<String> ESCAPE_TAGS = new HashSet<>(Arrays.asList("code", "noparse"));

  // TODO Allow this to be configured in the parser.
  private static final Set<String> NO_CLOSING_TAG = new HashSet<>(Arrays.asList("*"));

  @Override
  public Document buildDocument(String source) throws ParserException {
    return buildDocument(source.toCharArray());
  }

  @Override
  public Document buildDocument(char[] source) throws ParserException {

    Document document = new Document(source);
    Deque<TagNode> nodes = new ArrayDeque<>();

    try {
      parse(document, nodes);
    } catch (Exception e) {
      throw new ParserException("Failed to parse document source.\n\t" + new String(document.source), e);
    }
    return document;
  }

  /**
   * Finite State Machine parser implementation.
   *
   * @param document The document to add nodes to.
   * @param nodes    The node queue for state management.
   * @throws ParserException If parsing fails for any reason.
   */
  private void parse(Document document, Deque<TagNode> nodes) throws ParserException {

    // abc[ def
    // TextNode[abc] --> add to parent node
    // TagNode
    //      -> illegal state
    //       ? previous node text?
    //           --> Remove previous sibling from parent and make current node
    //           --> Add borked text to current node
    //       :
    //           --> Create new TextNode with borked chraracters and make current
    //           --> TextNode[[ def]
    //
    // char[] ca = new char[1024];
    //
    // foo[bar] = "Hello World";
    //
    // [noparse]abc[def[/noparse]


    int index = 0;

    String attributeName = null;
    int attributeNameBegin = 0;
    int attributeValueBegin = 0;

    State state = State.initial;
    TextNode textNode = null;
    char[] source = document.source;
    TagNode current;

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
          handleExpectedUnclosedTag(document, nodes, index);
          if (state == State.closingTagBegin) {
            // Expecting a tagNode, set bodyEnd
            TagNode tag = nodes.peek();
            tag.bodyEnd = index - 1;
            tag.hasBody = tag.bodyBegin != tag.bodyEnd;
          } else if (state == State.tagName) {
            nodes.push(new TagNode(document, index - 1));
          } else if (state == State.text) {
            // Bad BBCode or not really a tagBegin, i.e. 'char [] array = new char[1];'
            index--; // back up the pointer and continue in text state.
//            errorObserver.handleError(ErrorState.BAD_TAG, currentNode, index);
//            fixIt();
          }
          index++;
          break;

        case tagName:
          state = state.nextState(source[index]);
          if (state != State.tagName) {
            nodes.peek().nameEnd = index;
          }
          index++;
          break;

        case openingTagEnd:
          state = state.nextState(source[index]);
          // Set initial tagEnd to bodyBegin, if a closing tag exists this will be set again
          current = nodes.peek();
          current.tagEnd = index;
          current.bodyBegin = index;
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
            handleCompletedTagNode(document, nodes, index);
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
          if (state == State.singleQuotedAttributeValue || state == State.doubleQuotedAttributeValue) {
            attributeValueBegin = index + 1;
          } else if (state == State.complexAttributeValue) {
            attributeValueBegin = index;
          } else if (state != State.complexAttributeValue) {
            nodes.peek().attributes.put(attributeName, document.getString(attributeValueBegin, index));
            document.attributeOffsets.add(new Pair<>(attributeValueBegin, index - attributeValueBegin));
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
            handleCompletedTextNode(document, nodes, textNode, index);
            textNode = null;
          }
          index++;
          break;

        case complete:
          handleDanglingNodes(document, nodes, textNode, index);
          index++;
          break;

        default:
          throw new IllegalStateException("Illegal parser state : " + state);
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
      document.offsets.add(new Pair<>(tag.tagBegin, tag.bodyBegin - tag.tagBegin));
      if (tag.hasClosingTag) {
        document.offsets.add(new Pair<>(tag.bodyEnd, tag.tagEnd - tag.bodyEnd));
      }
    }
  }

  private void handleCompletedTagNode(Document document, Deque<TagNode> nodes, int index) {
    if (!nodes.isEmpty()) {
      TagNode tagNode = nodes.pop();
      tagNode.tagEnd = index;
      tagNode.hasClosingTag = true;
      addNode(document, nodes, tagNode);
    }
  }

  private void handleCompletedTextNode(Document document, Deque<TagNode> nodes, TextNode textNode, int index) {
    if (textNode != null) {
      textNode.tagEnd = index;
      addNode(document, nodes, textNode);
    }
  }

  private void handleDanglingNodes(Document document, Deque<TagNode> nodes, TextNode textNode, int index) {
    if (textNode != null) {
      handleCompletedTextNode(document, nodes, textNode, index);
    }
    handleExpectedUnclosedTag(document, nodes, index);
    handleUnExpectedUnclosedTag(document, nodes, index);
    handleCompletedTagNode(document, nodes, index);
  }

  private void handleExpectedUnclosedTag(Document document, Deque<TagNode> nodes, int index) {
    if (!nodes.isEmpty()) {
      // check for tags that don't require a closing tag
      if (NO_CLOSING_TAG.contains(nodes.peek().getName())) {
        TagNode tagNode = nodes.pop();
        tagNode.bodyEnd = index - 1;
        tagNode.tagEnd = index - 1;
        tagNode.hasClosingTag = false;
        addNode(document, nodes, tagNode);
      }
    }
  }

  private void handleUnExpectedUnclosedTag(Document document, Deque<TagNode> nodes, int index) {
    while (!nodes.isEmpty()) {
      TagNode tagNode = nodes.pop();
      TextNode textNode = tagNode.toTextNode();
      // If tagNode has children, find the last tagEnd
      if (tagNode.children.isEmpty()) {
        textNode.tagEnd = index;
      } else {
        Node last = tagNode.children.get(tagNode.children.size() - 1);
        textNode.tagEnd = ((BaseNode) last).tagEnd;
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
   *   initial --> tagBegin --> tagName --> openingTagEnd ---> complete
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
          return complexAttributeValue;
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