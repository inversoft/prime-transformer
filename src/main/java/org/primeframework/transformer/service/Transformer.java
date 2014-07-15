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

import java.util.List;

import org.primeframework.transformer.domain.Document;
import org.primeframework.transformer.domain.Pair;

public interface Transformer {

  String transform(Document document);

  /**
   * This is 'in work'.....
   */
  public static class TransformedResult {

    // e.g. at offset 1, shift right 3; at offset 4, shift right 6, etc...
    private List<Pair<Integer, Integer>> offsets;

    public String result;

    /**
     * @return the number of chars the source string must be moved at oldIndex. e.g. if the source string is A[B]C and
     * the target is A12345C. Here are sample calls to this method:
     * @{code computeOffsetFromIndex(0)} => 0
     * @{code computeOffsetFromIndex(1)} => 1
     * @{code computeOffsetFromIndex(2)} => 1
     * @{code computeOffsetFromIndex(3)} => 1
     * @{code computeOffsetFromIndex(4)} => 6
     */
    public int computeOffsetFromIndex(int oldIndex) {
      // sample implementation
      int totalOffset = 0;
      for (Pair<Integer, Integer> offset : offsets) {
        if (oldIndex >= offset.first) {
          totalOffset += offset.second;
        }
      }
      return totalOffset;
    }

    public void addOffset(int oldIndex, int incrementBy) {

    }
  }
}
