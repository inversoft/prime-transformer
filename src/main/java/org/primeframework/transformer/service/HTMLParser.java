/*
 * Copyright (c) 2018, Inversoft Inc., All Rights Reserved
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
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.primeframework.transformer.domain.Document;
import org.primeframework.transformer.domain.Pair;
import org.primeframework.transformer.domain.TagAttributes;
import org.primeframework.transformer.domain.TagNode;
import org.primeframework.transformer.domain.TextNode;

/**
 * HTML Parser Implementation.
 *
 * @author Tyler Scott
 */
public class HTMLParser extends AbstractParser {
  private static Map<String, TagAttributes> DEFAULT_TAG_ATTRIBUTES;

  @Override
  public Document buildDocument(String source, Map<String, TagAttributes> tagAttributes) {
    return buildDocument(source.toCharArray(), tagAttributes);
  }

  @Override
  public Document buildDocument(char[] source, Map<String, TagAttributes> tagAttributes) {
    Document document = new Document(source);

    // Copy the default attributes and overwrite keys with any values specified in tagAttributes
    Map<String, TagAttributes> defaultCopy = new HashMap<>(DEFAULT_TAG_ATTRIBUTES);
    defaultCopy.putAll(tagAttributes);

    parse(document, defaultCopy);
    return document;
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
        case bangDash:
        case inComment:
        case inCommentDash:
        case closingTagBegin:
          state = state.next(source[index]);
          index++;
          break;

        case bang:
          state = state.next(source[index]);
          textNode = new TextNode(document, nodes.peek(), index - 2, index);
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

        case inCommentDashDash:
        case openingTagSelfClose:
        case closingTagEnd:
          state = state.next(source[index]);
          if (state == State.text && textNode == null && parsingEnabled) {
            textNode = new TextNode(document, nodes.peek(), index, index + 1);
          }
          index++;
          break;

        case attribute:
          state = state.next(source[index]);
          if (parsingEnabled) {
            if (state == State.attributeName) {
              attributeNameBegin = index;
            } else if (state == State.tagBegin) {
              handleUnexpectedState(document, attributes, index, nodes);
            }
          }
          index++;
          break;

        case attributeName:
          state = state.next(source[index]);
          if (parsingEnabled) {
            if (state == State.attributeValue) {
              attributeName = document.getString(attributeNameBegin, index);
            } else if (state == State.tagBegin) {
              handleUnexpectedState(document, attributes, index, nodes);
            } else if (state == State.openingTagEnd || state == State.openingTagSelfClose) {
              // Boolean attribute
              attributeName = document.getString(attributeNameBegin, index);
              nodes.peek().attributes.put(attributeName, "true");
              document.attributeOffsets.add(new Pair<>(index, 0));
            }
          }
          index++;
          break;

        case attributeValue:
          state = state.next(source[index]);
          if (parsingEnabled) {
            if (state == State.openingTagEnd) {
              nodes.peek().attributes.put(attributeName, "");  // No attribute value, store empty string
              document.attributeOffsets.add(new Pair<>(index, 0));
            } else if (state == State.unquotedAttributeValue) {
              attributeValueBegin = index;
            } else if (state == State.singleQuotedAttributeValue || state == State.doubleQuotedAttributeValue) {
              attributeValueBegin = index + 1;
            } else if (state == State.tagBegin) {
              handleUnexpectedState(document, attributes, index, nodes);
            }
          }
          index++;
          break;

        case doubleQuotedAttributeValue:
        case singleQuotedAttributeValue:
        case unquotedAttributeValue:
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
        if (c == '<') {
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
        } else if (Character.isWhitespace(c) || c == '<' || c == '>') {
          // No tag name exists, i.e. <>, treat as text.
          return text;
        } else if (c == '!') {
          return bang;
        } else {
          return tagName;
        }
      }
    },

    tagName {
      @Override
      public State next(char c) {
        switch (c) {
          case '\t':
          case '\n':
          case '\r':
          case ' ':
            return attribute;
          case '>':
            return openingTagEnd;
          case '<':
            return tagBegin;
          default:
            return tagName;
        }
      }
    },

    attribute {
      @Override
      public State next(char c) {
        switch (c) {
          case '>':
            return openingTagEnd;
          case '/':
            return openingTagSelfClose;
          case '\t':
          case '\n':
          case '\r':
          case ' ':
            // Ignore whitespace
            return attribute;
          case '<':
            return tagBegin; // tag is not closed properly

          default:
            return attributeName;
        }
      }
    },

    attributeName {
      @Override
      public State next(char c) {
        switch (c) {
          case '=':
            return attributeValue;
          case '\t':
          case '\n':
          case '\r':
          case ' ':
            // Ignore whitespace
            return attributeName;
          case '<':
            return tagBegin; // tag not closed properly
          case '>':
            return openingTagEnd;
          case '/':
            return openingTagSelfClose;
          default:
            return attributeName;
        }
      }
    },

    attributeValue {
      @Override
      public State next(char c) {
        switch (c) {
          case '<':
            return tagBegin;
          case '>':
            return openingTagEnd;
          case '/':
            return openingTagSelfClose;
          case '\t':
          case '\n':
          case '\r':
          case ' ':
            return attribute;
          case '\'':
            return singleQuotedAttributeValue;
          case '\"':
            return doubleQuotedAttributeValue;
          default:
            return unquotedAttributeValue;
        }
      }
    },

    doubleQuotedAttributeValue {
      @Override
      public State next(char c) {
        if (c == '"') {
          return attribute;
        } else {
          return doubleQuotedAttributeValue;
        }
      }
    },

    singleQuotedAttributeValue {
      @Override
      public State next(char c) {
        if (c == '\'') {
          return attribute;
        } else {
          return singleQuotedAttributeValue;
        }
      }
    },

    // Source: https://mothereff.in/unquoted-attributes
    unquotedAttributeValue {
      @Override
      public State next(char c) {
        switch (c) {
          case '"':
          case '\'':
          case '=':
          case '<':
          case '`':
            // Disallowed characters in unquoted attribute (won't render properly anyways, so will likely become text or get erased by a browser)
            return text;
          case '\t':
          case '\n':
          case '\r':
          case ' ':
            // Any whitespace ends the attribute value
            return attribute;
          case '>':
            return openingTagEnd;
          default:
            return unquotedAttributeValue;
        }
      }
    },

    openingTagSelfClose {
      @Override
      public State next(char c) {
        if (c == '>') {
          return openingTagEnd;
        } else {
          return text;
        }
      }
    },

    openingTagEnd {
      @Override
      public State next(char c) {
        if (c == '<') {
          return tagBegin;
        } else {
          return text;
        }
      }
    },

    closingTagBegin {
      @Override
      public State next(char c) {
        if (c == '>') {
          return closingTagEnd; // no name of closing tag
        } else {
          return closingTagName;
        }
      }
    },

    closingTagName {
      @Override
      public State next(char c) {
        if (c == '>') {
          return closingTagEnd;
        } else {
          return closingTagName;
        }
      }
    },

    closingTagEnd {
      @Override
      public State next(char c) {
        if (c == '<') {
          return tagBegin;
        } else {
          return text;
        }
      }
    },

    text {
      @Override
      public State next(char c) {
        if (c == '<') {
          return tagBegin;
        } else {
          return text;
        }
      }
    },

    bang {
      @Override
      public State next(char c) {
        if (c == '-') {
          return bangDash;
        } else {
          return text;
        }
      }
    },

    bangDash {
      @Override
      public State next(char c) {
        if (c == '-') {
          return inComment;
        } else {
          return text;
        }
      }
    },

    inComment {
      @Override
      public State next(char c) {
        if (c == '-') {
          return inCommentDash;
        } else {
          return inComment;
        }
      }
    },

    inCommentDash {
      @Override
      public State next(char c) {
        if (c == '-') {
          return inCommentDashDash;
        } else {
          return inComment;
        }
      }
    },

    inCommentDashDash {
      @Override
      public State next(char c) {
        if (c == '>') {
          return text;
        } else {
          return inComment;
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

  static {
    /*
      HTML has several classes of elements.

      Void elements: (Elements with nothing inside)
      [area, base, br, col, embed, hr, img, input, link, meta, param, source, track, wbr]

      Template elements: (Define elements for a shadow dom)

      Raw text elements: (The text is literal text, effectively a nested text document)
      [script, style]

      Escapable raw text elements: (The text is displayed and special characters need to be html escaped, no other html elements can go inside)
      [textarea, title]

      Foreign Elements: (Non html but still xmlish)
      [MathML, svg]

      Normal elements: (Everything else)

      Source: http://w3c.github.io/html/syntax.html#void-elements
      Source: https://stackoverflow.com/questions/3741896/what-do-you-call-tags-that-need-no-ending-tag
   */

    Map<String, TagAttributes> tagAttributesHashMap = new HashMap<>();

    // Setup void tags
    TagAttributes voidTagAttributes = new TagAttributes(true, false, true, true);
    Stream.of("area", "base", "br", "col", "embed", "hr", "img", "input", "link", "meta", "param", "source", "track", "wbr")
          .forEach(tag -> tagAttributesHashMap.put(tag, voidTagAttributes));

    // Skipping templates, very free form but it still makes sense to try to parse them, they will fallback to text

    // Setup raw text elements
    TagAttributes rawTextElements = new TagAttributes(false, true, false, false);
    Stream.of("script", "style")
          .forEach(tag -> tagAttributesHashMap.put(tag, rawTextElements));

    // Skipping escapable raw text elements

    // Ignore svg stuff. It explodes the parser.
    tagAttributesHashMap.put("svg", new TagAttributes(false, true, false, true));

    // Everything else has a good default mode.

    DEFAULT_TAG_ATTRIBUTES = Collections.unmodifiableMap(tagAttributesHashMap);
  }
}
