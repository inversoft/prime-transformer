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
import org.primeframework.transformer.domain.Pair;
import org.primeframework.transformer.domain.TransformerException;

import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Transformer interface.
 *
 * @author Daniel DeGroff
 */
public interface Transformer {

  /**
   * Return true if strict mode is enabled. When strict mode is enabled a {@link org.primeframework.transformer.domain.TransformerException}
   * will be thrown if a tag is not able to be transformed. If set to false, or disabled, when a tag is encountered that
   * is not able to be transformed, the entire string including the tag will be returned.
   *
   * @return
   */
  boolean isStrict();

  /**
   * Sets the strict mode of the transformer, passing a true value will enable strict mode.
   *
   * @param strict
   */
  Transformer setStrict(boolean strict);

  /**
   * Transform the document. The returned string will be defined by the implementation.
   *
   * @param document
   * @return
   * @throws TransformerException
   */
  TransformedResult transform(Document document) throws TransformerException;

  /**
   * Transform result object that provides the resulting transformed string and index and offset values that can be used
   * to understand how the the original string could be reconstructed.
   */
  public static class TransformedResult {

    // e.g. at offset 1, shift right 3; at offset 4, shift right 6, etc...
    private Set<Pair<Integer, Integer>> offsets = new TreeSet<>();

    public String result;

    public TransformedResult(String result) {
      this.result = result;
    }

    public TransformedResult(String result, List<Pair<Integer, Integer>> offsets) {
      this.offsets.addAll(offsets);
      this.result = result;
    }

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
      Pair<Integer, Integer> pair = new Pair<>(oldIndex, incrementBy);
      if (offsets.contains(pair)) {
        offsets.remove(pair);
        pair = new Pair<>(oldIndex, incrementBy * 2);
      }
      offsets.add(pair);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;

      TransformedResult that = (TransformedResult) o;

      if (!offsets.equals(that.offsets)) return false;
      if (result != null ? !result.equals(that.result) : that.result != null) return false;

      return true;
    }

    @Override
    public int hashCode() {
      int result1 = offsets.hashCode();
      result1 = 31 * result1 + (result != null ? result.hashCode() : 0);
      return result1;
    }

    @Override
    public String toString() {
      return "TransformedResult{" +
         "offsets=[" +
         String.join(", ", offsets.stream().map(o -> o.first + ":" + o.second).collect(Collectors.toList())) +
         "], result='" + result + "\'" +
         "}";
    }
  }
}
