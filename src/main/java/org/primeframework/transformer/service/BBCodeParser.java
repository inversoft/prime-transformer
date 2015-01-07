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
    Deque<TagNode> nodes = new ArrayDeque<>();
    parse(document, nodes, tagAttributes);
    return document;
  }

  /**
   * Finite State Machine parser implementation.
   *
   * @param document      The document to add nodes to.
   * @param nodes         The node queue for state management.
   * @param tagAttributes A map of tag attributes keyed by tag name.
   */
  private void parse(Document document, Deque<TagNode> nodes, Map<String, TagAttributes> tagAttributes) {

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
    Deque<TextNode> textNodes = new ArrayDeque<>(1);
    char[] source = document.source;

    while (index <= source.length) {
      if (index == source.length) {
        state = State.complete;
      }

      switch (state) {

        case initial:
          state = state.next(source[index]);
          index++;
          break;

        case tagBegin:
          state = state.next(source[index]);
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
          }
          index++;
          break;

        case tagName:
          state = state.next(source[index]);
          if (state == State.tagBegin) {
            handleUnExpectedOpenTag(document, nodes, index);
          } else if (state != State.tagName) {
            nodes.peek().nameEnd = index;
          }
          index++;
          break;

        case openingTagEnd:
          state = state.next(source[index]);
          current = nodes.peek();
          current.end = index; // when tag is closed this will be updated
          current.bodyBegin = index;
          current.bodyEnd = index; // if a body is found, this index will be adjusted.
          checkForPreFormattedTag(current, preFormatted, attributes);
          index++;
          break;

        case closingTagBegin:
          state = state.next(source[index]);
          index++;
          break;

        case closingTagName:
          state = state.next(source[index]);
          index++;
          if (state == State.closingTagEnd) {
            handlePreFormattedClosingTag(document, nodes, preFormatted);
            handleCompletedTagNode(document, index, nodes, attributes);
          }
          break;

        case closingTagEnd:
          state = state.next(source[index]);
          if (state == State.text) {
            if (textNodes.isEmpty()) {
              textNodes.push(new TextNode(document, index, index + 1));
            }
          }
          index++;
          break;

        case simpleAttribute:
          state = state.next(source[index]);
          if (state == State.unQuotedSimpleAttributeValue) {
            nodes.peek().attributesBegin = index;
          } else if (state == State.singleQuotedSimpleAttributeValue || state == State.doubleQuotedSimpleAttributeValue) {
            nodes.peek().attributesBegin = index + 1;
          }
          index++;
          break;

        case unQuotedSimpleAttributeValue:
          state = state.next(source[index]);
          if (state != State.unQuotedSimpleAttributeValue) {
            addSimpleAttribute(document, index, nodes);
          }
          index++;
          break;

        case singleQuotedSimpleAttributeValue:
          state = state.next(source[index]);
          if (state != State.singleQuotedSimpleAttributeValue) {
            addSimpleAttribute(document, index, nodes);
          }
          index++;
          break;

        case doubleQuotedSimpleAttributeValue:
          state = state.next(source[index]);
          if (state != State.doubleQuotedSimpleAttributeValue) {
            addSimpleAttribute(document, index, nodes);
          }
          index++;
          break;

        case complexAttribute:
          state = state.next(source[index]);
          if (state == State.complexAttributeName) {
            attributeNameBegin = index;

            TagNode tagNode = nodes.peek();
            if (tagNode.attributesBegin == -1) {
              nodes.peek().attributesBegin = index;
            }
          } else if (state == State.text) {
            handleUnExpectedOpenTag(document, nodes, index);
          }
          index++;
          break;

        case complexAttributeName:
          state = state.next(source[index]);
          if (state == State.complexAttributeValue) {
            attributeName = document.getString(attributeNameBegin, index);
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
          state = state.next(source[index]);
          if (state != State.doubleQuotedAttributeValue) {
            nodes.peek().attributes.put(attributeName, document.getString(attributeValueBegin, index));
            document.attributeOffsets.add(new Pair<>(attributeValueBegin, index - attributeValueBegin));
          }
          index++;
          break;

        case singleQuotedAttributeValue:
          state = state.next(source[index]);
          if (state != State.singleQuotedAttributeValue) {
            nodes.peek().attributes.put(attributeName, document.getString(attributeValueBegin, index));
            document.attributeOffsets.add(new Pair<>(attributeValueBegin, index - attributeValueBegin));
          }
          index++;
          break;

        case unQuotedAttributeValue:
          state = state.next(source[index]);
          if (state != State.unQuotedAttributeValue) {
            nodes.peek().attributes.put(attributeName, document.getString(attributeValueBegin, index));
            document.attributeOffsets.add(new Pair<>(attributeValueBegin, index - attributeValueBegin));
          }
          index++;
          break;

        case simpleAttributeBody:
          state = state.next(source[index]);
          if (state != State.simpleAttributeBody) {
            addSimpleAttribute(document, index, nodes);
          }
          index++;
          break;

        case simpleAttributeEnd:
          state = state.next(source[index]);
          index++;
          break;

        case text:
          state = state.next(source[index]);
          if (textNodes.isEmpty()) {
            textNodes.push(new TextNode(document, index - 1, index));
          }
          if (state != State.text) {
            handleCompletedTextNode(document, index, textNodes, nodes);
          }
          index++;
          break;

        case complete:
          handleDocumentCleanup(document, attributes, index, textNodes, nodes);
          index++;
          break;

        default:
          throw new IllegalStateException("Illegal parser state : " + state);
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
   * Add the provided node to node on the top of the stack if it isn't closed out yet, otherwise add it directly to the
   * document as a top level node.
   *
   * @param document The document to add the node to.
   * @param nodes    The node queue.
   * @param node     The node to add.
   */
  private static void addNode(Document document, Deque<TagNode> nodes, Node node) {

    if (nodes.isEmpty()) {
      document.addChild(node);
    } else {
      nodes.peek().addChild(node);
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

  private void addSimpleAttribute(Document document, int index, Deque<TagNode> nodes) {
    TagNode current = nodes.peek();
    String name = document.getString(current.attributesBegin, index);
    // Ignore trailing space on the attribute value. e.g. [foo size=5    ] bar[/foo]
    int length = name.length();
    name = name.trim();

    // Keep the trimmed value and account for the shortened value in the offset
    document.attributeOffsets.add(new Pair<>(current.attributesBegin, index - current.attributesBegin - (length - name.length())));
    current.attribute = name;
  }

  /**
   * If the current {@link TagNode} is identified as having a pre-formatted body, stash the name.
   *
   * @param node The node to identify as having a pre-formatted body
   * @param preFormatted A stack of pre-formatted tag names.
   * @param attributes The {@link TagAttributes} map provided by the caller.
   */
  private void checkForPreFormattedTag(TagNode node, Deque<String> preFormatted,
                                       Map<String, TagAttributes> attributes) {
    String name = lc(node.getName());
    if (attributes.containsKey(name) && attributes.get(name).hasPreFormattedBody) {
      preFormatted.push(name);
    }
  }

  /**
   * Null safe lowercase.
   * @param string
   * @return
   */
  private String lc(String string) {
    return string == null ? string : string.toLowerCase();
  }

  private boolean eq(String s1, String s2) {
    if (s1 == null && s2 == null) {
      return true;
    }

    if (s1 == null) {
      return false;
    }

    return s1.equals(s2);
  }

  private boolean neq(String s1, String s2) {
    return !eq(s1, s2);
  }

  /**
   * Walk the document and collapse adjacent {@link TextNode} tags. When malformed BBCode is encountered they may be converted to a {@link TextNode}, which may result in having adjacent text nodes in the document.
   *
   * @param node
   */
  private void collapseAdjacentTextNodes(BaseTagNode node) {
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
        collapseAdjacentTextNodes((TagNode) n);
      }
    }
  }

  /**
   * Handle completion of a {@link TagNode}. Set the end index of this tag, and then add the node to the {@link
   * Document} or its parent node.
   *
   * @param document
   * @param index
   * @param nodes
   * @param attributes
   */
  private void handleCompletedTagNode(Document document, int index, Deque<TagNode> nodes,
                                      Map<String, TagAttributes> attributes) {

    if (!nodes.isEmpty()) {
      // Start by handling expected unclosed tags.
      // i.e. If we hit this end tag [/list], there may be [*] nodes on the stack, they need to be handled first.
      handleExpectedUnclosedTag(document, attributes, nodes, index);
      TagNode current = nodes.peek();
      String closingTagName = document.getString(current.bodyEnd + 2, index - 1);
      if (current.getName().equalsIgnoreCase(closingTagName)) {
        // Set the end of this tag, and add to the document or its parent node.
        TagNode tagNode = nodes.pop();
        tagNode.end = index;
        addNode(document, nodes, tagNode);
      } else {
        // If this closing tag isn't what we expect, handle it.
        handleUnExpectedUnclosedTag(document, index, nodes);
      }
    }
  }

  /**
   * Handle completion of a {@link TextNode}. Set the end index of this tag, and then add the node to the {@link
   * Document} or its parent node.
   *
   * @param document
   * @param index
   * @param textNodes
   * @param nodes
   */
  private void handleCompletedTextNode(Document document, int index, Deque<TextNode> textNodes, Deque<TagNode> nodes) {
    if (!textNodes.isEmpty()) {
      textNodes.peek().end = index;
      addNode(document, nodes, textNodes.pop());
    }
  }

  private void handleDocumentCleanup(Document document, Map<String, TagAttributes> attributes, int index,
                                     Deque<TextNode> textNodes, Deque<TagNode> nodes) {
    // Order of these cleanup steps is intentional
    handleCompletedTextNode(document, index, textNodes, nodes);
    handleExpectedUnclosedTag(document, attributes, nodes, index);
    handleUnExpectedUnclosedTag(document, index, nodes);
    handleCompletedTagNode(document, index, nodes, attributes);
    collapseAdjacentTextNodes(document);
  }

  private void handleExpectedUnclosedTag(Document document, Map<String, TagAttributes> attributes, Deque<TagNode> nodes,
                                         int index) {

    if (!nodes.isEmpty()) {
      TagNode current = nodes.peek();
      boolean handleNoClosingTag = !current.hasClosingTag();
      // If current node has a closing tag, if the closing tag doesn't match, continue
      if (!handleNoClosingTag) {
        if (current.bodyEnd != -1) {
          String closingTagName = document.getString(current.bodyEnd + 2, index - 1);
          handleNoClosingTag = neq(lc(current.getName()), closingTagName);
        }
      }

      if (handleNoClosingTag) {
        String name = lc(nodes.peek().getName());
        Deque<TagNode> stack = new ArrayDeque<>(2);

        if (name != null) {
          while (!nodes.isEmpty() && attributes.containsKey(name) && !attributes.get(name).doesNotRequireClosingTag) {
            stack.push(nodes.pop());
            if (nodes.isEmpty()) {
              break;
            }
            name = lc(nodes.peek().getName());
          }
        }

        if (!stack.isEmpty()) {

          if (nodes.isEmpty()) {

            while(!stack.isEmpty()) {
              TagNode tag = stack.pop();
              if (!tag.children.isEmpty()) {
                tag.bodyEnd = ((BaseNode) tag.children.get(tag.children.size() - 1)).end;
                tag.end = tag.bodyEnd;
              }

//              if (stack.isEmpty()) {
//                tag.end = index;
//              } else {
//                tag.bodyEnd = stack.peek().begin;
//                tag.end = tag.bodyEnd;
//              }
              addNode(document, nodes, tag);
            }

          } else {
            nodes.peek().bodyEnd = stack.getLast().bodyEnd;
            while (!stack.isEmpty()) {
              TagNode kid = stack.pop();
              if (!kid.children.isEmpty()) {
                kid.end = ((BaseNode) kid.children.get(kid.children.size() - 1)).end;
                kid.bodyEnd = kid.end;
              }
              addNode(document, nodes, kid);
            }
          }

        }

//        if (!stack.isEmpty() && !nodes.isEmpty()) {
//          nodes.peek().bodyEnd = stack.getLast().bodyEnd;
//          while (!stack.isEmpty()) {
//            TagNode kid = stack.pop();
//            if (!kid.children.isEmpty()) {
//              kid.end = ((BaseNode) kid.children.get(kid.children.size() - 1)).end;
//              kid.bodyEnd = kid.end;
//            }
//            addNode(document, nodes, kid);
//          }
//        }
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

  private static void handleUnExpectedOpenTag(Document document, Deque<TagNode> nodes, int index) {
    TagNode tagNode = nodes.pop();
    TextNode textNode = tagNode.toTextNode();
    textNode.end = index;
    addNode(document, nodes, textNode);
  }

  private static void handleUnExpectedOpenTag(Document document, ParserState parser) {
    handleUnExpectedOpenTag(document, parser.nodes, parser.index);
  }

  private void handleUnExpectedUnclosedTag(Document document, int index, Deque<TagNode> nodes) {
    while (!nodes.isEmpty()) {
      TagNode tagNode = nodes.pop();
      TextNode textNode = tagNode.toTextNode();
      // If tagNode has children, find the last end
      if (tagNode.children.isEmpty()) {
        textNode.end = index;
      } else {
        // TODO - add some edge cases for this path, I thought I needed this logic earlier...
//        Node last = tagNode.children.get(tagNode.children.size() - 1);
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
   */
  private enum State {

    /**
     * Initial state of the parser.
     */
    initial {
      @Override
      public State next(char c) {
        if (c == '[') {
          return tagBegin;
        } else {
          return text;
        }
      }

      @Override
      public void process(Document document, ParserState parser) {
        parser.state = parser.state.next(parser.source[parser.index]);
        parser.index++;
      }
    },

    /**
     * An opening tag character was found: '<code>[</code>' <br> <p>This state does not imply an opening or a closing
     * tag, the subsequent character will identify if this is an opening or closing tag.</p>
     */
    tagBegin {
      @Override
      public State next(char c) {
        if (c == '/') {
          return closingTagBegin;
        } else if (c == ']') {
          // No tag name exists, i.e. [], treat as text.
          return text;
        } else {
          return tagName;
        }
      }

      @Override
      public void process(Document document, ParserState parserState) {

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

      @Override
      public void process(Document document, ParserState parser) {

        parser.state = parser.state.next(parser.source[parser.index]);
        if (parser.state == State.tagBegin) {
          handleUnExpectedOpenTag(document, parser);
        } else if (parser.state != State.tagName) {
          parser.nodes.peek().nameEnd = parser.index;
        }
        parser.index++;

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
          // tag is not closed properly.
          return text;
        } else {
          return complexAttributeName;
        }
      }

      @Override
      public void process(Document document, ParserState parserState) {

      }
    },

    complexAttributeName {
      @Override
      public State next(char c) {
        if (c == '=') {
          return complexAttributeValue;
        } else {
          return complexAttributeName;
        }
      }

      @Override
      public void process(Document document, ParserState parserState) {

      }
    },

    /**
     * An attribute value when not quoted is not allowed to contain spaces.
     */
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

      @Override
      public void process(Document document, ParserState parserState) {


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

      @Override
      public void process(Document document, ParserState parserState) {

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

      @Override
      public void process(Document document, ParserState parserState) {

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

      @Override
      public void process(Document document, ParserState parserState) {

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

      @Override
      public void process(Document document, ParserState parserState) {

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

      @Override
      public void process(Document document, ParserState parserState) {

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

      @Override
      public void process(Document document, ParserState parserState) {

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

      @Override
      public void process(Document document, ParserState parserState) {

      }
    },


    simpleAttributeBody {
      @Override
      public State next(char c) {
        if (c == '"' || c == '\'') {
          return simpleAttributeEnd;
        } else if (c == ']') {
          return openingTagEnd;
        } else {
          return simpleAttributeBody;
        }
      }

      @Override
      public void process(Document document, ParserState parserState) {

      }
    },

    simpleAttributeEnd {
      @Override
      public State next(char c) {
        if (c == ']') {
          return openingTagEnd;
        } else {
          // TODO Error - unexpected
          return text;
        }
      }

      @Override
      public void process(Document document, ParserState parserState) {

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

      @Override
      public void process(Document document, ParserState parserState) {

      }
    },

    closingTagBegin {
      @Override
      public State next(char c) {
        if (c == ']') {
          // TODO Error condition - no name of closing tag
          return closingTagEnd;
        } else {
          return closingTagName;
        }
      }

      @Override
      public void process(Document document, ParserState parserState) {

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

      @Override
      public void process(Document document, ParserState parserState) {

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

      @Override
      public void process(Document document, ParserState parserState) {

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

      @Override
      public void process(Document document, ParserState parserState) {

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

      @Override
      public void process(Document document, ParserState parserState) {

      }
    },

    complete {
      @Override
      public State next(char c) {
        return complete;
      }

      @Override
      public void process(Document document, ParserState parserState) {

      }
    };

    public abstract State next(char c);

    public abstract void process(Document document, ParserState parserState);
  }

//  private static Map<State, StateProcessor> processors = new HashMap<>(State.values().length);
//
//  static {
//
//    /**
//     * Initial State Processing.
//     */
//    processors.put(State.initial, (document, index, nodes, state, source) -> {
//      state = state.next(source[index]);
//      return index++;
//    });
//
//    /**
//     * State.tagBegin
//     */
//    processors.put(State.tagBegin, (document, index, nodes, state, source) -> {
//      state = state.next(source[index]);
//      if (state == State.closingTagBegin) {
//        if (nodes.isEmpty()) {
//          state = State.text;
//          index--;
//        } else {
//          nodes.peek().bodyEnd = index - 1;
//        }
//      } else if (state == State.tagName) {
//        nodes.push(new TagNode(document, index - 1));
//      } else if (state == State.text) {
//        // Bad BBCode or not really a begin, i.e. 'char [] array = new char[1];'
//        index--; // back up the pointer and continue in text state.
////            errorObserver.handleError(ErrorState.BAD_TAG, currentNode, index);
//      }
//      index++;
//
//      return index++;
//    });
//
//  }

//  private static StateTransitionMap stateMap = new StateTransitionMap();
//  static {
//    stateMap
//        // Initial State
//        .addState(State.initial,
//            new StateMapping('[', State.tagBegin),
//            new StateMapping(null, State.text))
//            // Tag Begin
//        .addState(State.tagBegin,
//            new StateMapping('/', State.closingTagBegin),
//            new StateMapping(']', State.text),
//            new StateMapping(null, State.tagName))
//            // Tag Name
//        .addState(State.tagName)
//        .addState(State.openingTagEnd)
//        .addState(State.closingTagBegin)
//        .addState(State.closingTagName)
//        .addState(State.closingTagEnd)
//        .addState(State.simpleAttribute)
//        .addState(State.unQuotedSimpleAttributeValue)
//        .addState(State.singleQuotedSimpleAttributeValue)
//        .addState(State.doubleQuotedSimpleAttributeValue)
//        .addState(State.complexAttribute)
//        .addState(State.complexAttributeName)
//        .addState(State.complexAttributeValue)
//        .addState(State.doubleQuotedAttributeValue)
//        .addState(State.singleQuotedAttributeValue)
//        .addState(State.unQuotedAttributeValue)
//        .addState(State.singleQuotedAttributeValue)
//        .addState(State.simpleAttributeBody)
//        .addState(State.simpleAttributeEnd)
//        .addState(State.text)
//        .addState(State.complete)
//    ;
//  }
//
//  static class StateMapping {
//
//    public Character c;
//    public State state;
//
//    public StateMapping(Character c, State state) {
//      this.c = c;
//      this.state = state;
//    }
//  }
//
//  private static class StateTransitionMap {
//
//    public Map<State, Map<Character, State>> stateTransitions = new HashMap<>();
//
//    public StateTransitionMap addState(State state, StateMapping... mappings) {
//      Map<Character, State> stateMappings = new HashMap<>();
//      for (StateMapping mapping : mappings) {
//        stateMappings.put(mapping.c, mapping.state);
//      }
//      stateTransitions.put(state, stateMappings);
//      return this;
//    }
//
//    public StateMapping map(Character c, State state) {
//      return new StateMapping(c, state);
//    }
//  }

  private class ParserState {
    public int index;
    public Deque<TagNode> nodes;
    public char[] source;
    public State state = State.initial;
  }

}