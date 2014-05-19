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

import org.primeframework.transformer.domain.*;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * BBCode Parser Implementation.
 */
public class BBCodeParser extends AbstractParser {

    private static final char OPEN_CHAR = '[';
    private static final char CLOSE_CHAR = ']';
    private static final String OPENING_TAG = "[%s]";

    private static final String CLOSING_TAG = "[/%s]";

    private static final Set<String> NO_CLOSING_TAG = new HashSet<>(Arrays.asList("*"));

    @Override
    public Document buildDocument(DocumentSource documentSource) {
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
        return CLOSE_CHAR;
    }

    @Override
    protected char getTagOpenChar() {
        return OPEN_CHAR;
    }

    /**
     * Find the next node between the start and end index and add to the provided node list.
     *
     * @param startIndex
     * @param endIndex
     * @param children
     * @return the working source index
     */
    private int addNextNode(Document document, int startIndex, int endIndex, List<Node> children) {

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

            String attribute = null;
            int attributeBegin = indexOfCharacter(document, tagBegin, openingTagEndIndex, '=');
            if (attributeBegin == -1) {
                attributeBegin = openingTagEndIndex;
            } else {
                attribute = document.getString(attributeBegin + 1, openingTagEndIndex);
            }

            String tagName = document.getString(tagBegin + 1, attributeBegin);
            String openTag = String.format(OPENING_TAG, tagName);
            String closeTag = String.format(CLOSING_TAG, tagName);

            int bodyBegin = openingTagEndIndex + 1;
            int bodyEnd = indexOfString(document, bodyBegin, endIndex, closeTag);

            int tagEnd;
            // If no closing tag is found
            if (bodyEnd == -1) {
                if (!NO_CLOSING_TAG.contains(tagName)) {
                    throw new ParserException("Malformed markup. No closing tag was found for " + closeTag + ". "
                            + "Open tag started at index " + (openingTagEndIndex + 1 - closeTag.length()) + " and ended at " + openingTagEndIndex + 1 + ".\n\t" + document.documentSource);

                }
                // When no closing tag is not required:
                //  - 1. Find the next opening tag, this will be the end of this body
                //  - 2. Else, use the end of this range (endIndex) as the end of this body
                int nextTagIndex = indexOfString(document, openingTagEndIndex, endIndex, openTag);
                if (nextTagIndex == -1) {
                    bodyEnd = endIndex;
                } else {
                    bodyEnd = nextTagIndex;
                }
                tagEnd = bodyEnd;
            } else {
                tagEnd = bodyEnd + closeTag.length();
            }

            // Build tag node
            TagNode tag = new TagNode(document, tagBegin, attributeBegin, bodyBegin, bodyEnd, tagEnd);
            tag.attribute = attribute;

            // Check for additional nested nodes
            int nestedStartIndex = bodyBegin;
            while (nestedStartIndex < bodyEnd) {
                nestedStartIndex = addNextNode(document, nestedStartIndex, bodyEnd, tag.children);
            }
            children.add(tag);
            startIndex = NO_CLOSING_TAG.contains(tagName) ? bodyEnd : bodyEnd + closeTag.length();
        }
        return startIndex;
    }

}
