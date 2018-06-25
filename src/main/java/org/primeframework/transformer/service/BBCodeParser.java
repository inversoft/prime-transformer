/*
 * Copyright (c) 2015-2018, Inversoft Inc., All Rights Reserved
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
import java.util.Map;

import org.primeframework.transformer.domain.Document;
import org.primeframework.transformer.domain.Pair;
import org.primeframework.transformer.domain.TagAttributes;
import org.primeframework.transformer.domain.TagNode;
import org.primeframework.transformer.domain.TextNode;

/**
 * BBCode Parser Implementation.
 *
 * @author Daniel DeGroff
 */
public class BBCodeParser extends AbstractParser {

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
   * Finite State Machine parser implementation.
   *
   * @param document      The document to add nodes to.
   * @param tagAttributes A map of tag attributes keyed by tag name.
   */
  @SuppressWarnings("Duplicates")
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
            nodes.push(new TagNode(document, nodes.peek(), index - 1));
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
            textNode = new TextNode(document, nodes.peek(), index, index + 1);
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
            textNode = new TextNode(document, nodes.peek(), index - 1, index);
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
  @SuppressWarnings("Duplicates")
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