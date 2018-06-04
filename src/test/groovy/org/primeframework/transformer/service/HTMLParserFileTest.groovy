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
package org.primeframework.transformer.service

import com.fasterxml.jackson.core.SerializableString
import com.fasterxml.jackson.core.io.CharacterEscapes
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import org.primeframework.transformer.jackson.ProxyModule
import org.testng.annotations.BeforeTest
import org.testng.annotations.DataProvider
import org.testng.annotations.Test

import static java.util.Collections.emptyMap

/**
 * @author Tyler Scott
 */
class HTMLParserFileTest {

  private ObjectMapper objectMapper;

  @BeforeTest
  void beforeTest() {
    objectMapper = new ObjectMapper().configure(SerializationFeature.INDENT_OUTPUT, true)
                                     .registerModule(new ProxyModule())

    objectMapper.getFactory().setCharacterEscapes(new CharacterEscapes() {
      @Override
      int[] getEscapeCodesForAscii() {
        return standardAsciiEscapesForJSON()
      }

      @Override
      SerializableString getEscapeSequence(int ch) {
        return null
      }
    })
  }

  @DataProvider
  Object[][] inOutTests() {
//    List<String> files = this.getClass().getResourceAsStream("/org/primeframework/transformer/html/source").getText().
//        split('\r?\n')

    // Savant does something to the class loader (maybe?) that causes directory resources to return an empty string instead
    // of the files separated by new lines. Falling back to static filenames.

    List<String> files = ["custom.html", "github.com.html", "svg.html"]

    Object[][] results = new Object[files.size()][2]

    for (int i = 0; i < files.size(); ++i) {
      results[i][0] = this.getClass().
          getResourceAsStream("/org/primeframework/transformer/html/source/" + files.get(i)).
          getText()
      results[i][1] = this.getClass().getResourceAsStream(
          "/org/primeframework/transformer/html/json/" + files.get(i).replaceAll("\\.html\$", ".json"))
    }

    return results
  }

  @Test(dataProvider = "inOutTests")
  void parseRealSites(String inData, InputStream expectedOutData) {
    def parser = new HTMLParser()
    def doc = parser.buildDocument(inData, emptyMap())

    JsonNode actual = objectMapper.readTree(objectMapper.writeValueAsString(doc))
    JsonNode expected = objectMapper.readTree(expectedOutData)

    if (actual != expected) {
      String actualString = objectMapper.writeValueAsString(actual)
      String expectedString = objectMapper.writeValueAsString(expected)

      throw new AssertionError(
          "The output doesn't match the expected JSON output. expected [" + expectedString + "] but found [" + actualString + "]")
    }
  }
}
