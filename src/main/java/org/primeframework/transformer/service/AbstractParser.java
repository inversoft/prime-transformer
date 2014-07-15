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
import org.primeframework.transformer.domain.ParserException;

/**
 * Base class for Parser implementations.
 */
public abstract class AbstractParser implements Parser {

  /**
   * Return the character used to identify the end of a tag.
   * <pre>Example: ']' </pre>
   *
   * @return
   */
  protected abstract char getTagCloseChar();

  /**
   * Return the character used to identify the start of a new tag.
   * <pre>Example: '[' </pre>
   *
   * @return
   */
  protected abstract char getTagOpenChar();

  /**
   * Return the index of the provided character between the start and end index of the document source.
   *
   * @param document
   * @param startIndex
   * @param endIndex
   * @param character
   * @return
   */
  protected int indexOfCharacter(Document document, int startIndex, int endIndex, char character) {
    char[] source = document.documentSource.source;
    for (int i = startIndex; i < endIndex; i++) {
      if (source[i] == character) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Return the index of the end tag character. The opening tag must be closed, if the tag is found not to be closed a
   * {@link ParserException} will be thrown.
   *
   * @param document
   * @param startIndex
   * @param endIndex
   * @return
   */
  protected int indexOfOpeningTagCloseCharacter(Document document, int startIndex, int endIndex) {
    int tagEndIndex = indexOfCharacter(document, startIndex, endIndex, getTagCloseChar());
    if (tagEndIndex == -1) {
      throw new ParserException("Malformed markup. Open tag was not closed");
    }
    return tagEndIndex;
  }

  /**
   * Return the index of the start of the next tag between the start and end index values. A valid opening tag must not
   * be followed immediately by a closing tag.
   *
   * @param document
   * @param startIndex
   * @param endIndex
   * @return -1 is returned if the opening tag character is not found within the provided range.
   */
  protected int indexOfOpeningTagOpenCharacter(Document document, int startIndex, int endIndex) {
    int openTag = indexOfCharacter(document, startIndex, endIndex, getTagOpenChar());
    if (document.documentSource.source[openTag + 1] == getTagCloseChar()) {
      openTag = indexOfCharacter(document, openTag + 2, endIndex, getTagOpenChar());
    }
    return openTag;
  }

  /**
   * Return the index of the provided string between the start and end index values.
   *
   * @param document
   * @param startIndex
   * @param endIndex
   * @param string
   * @return -1 is returned if the string is not found within the provided range.
   */
  protected int indexOfString(Document document, int startIndex, int endIndex, String string) {

    char[] source = document.documentSource.source;
    for (int i = startIndex; i < endIndex; i++) {
      for (int j = 0; j < string.length(); j++) {
        if (source[i + j] != string.charAt(j)) {
          break;
        }
        if (j == string.length() - 1) {
          return i;
        }
      }

    }
    return -1;
  }

  /**
   * Remove leading and trailing single or double quotes.
   * <p>Examples: <pre> "htt://.foo.com"     --> http://foo.com </pre>
   * <pre> 'htt://.foo.com'     --> http://foo.com </pre>
   * <pre> "testing " string"   --> testing " string </pre></p>
   *
   * @param string
   * @return
   */
  protected String removeQuotes(String string) {
    return string.replaceAll("^\"|\"$", "").replaceAll("^'|'$", "");
  }
}
