/*
 * Copyright (c) 2015, Inversoft Inc., All Rights Reserved
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
package org.primeframework.transformer.domain;

import org.primeframework.transformer.domain.Pair;

import java.util.Set;
import java.util.TreeSet;

/**
 * Defines a set of offsets within a String. Each offset is at a position and has an amount. These are stored as Pairs
 * where the first value is the position and the second value is the amount.
 * <p>
 * i.e. at position 15 in the string, shift 3
 * <p>
 * Note that the amount might be negative.
 *
 * @author Brian Pontarelli
 */
public class Offsets {
  private final Set<Pair<Integer, Integer>> offsets = new TreeSet<>();

  /**
   * Adds the offset.
   *
   * @param position The position of the offset from the start of the string.
   * @param amount   The amount of the offset.
   */
  public void add(int position, int amount) {
    offsets.add(new Pair<>(position, amount));
  }

  /**
   * Calculates the new index using the offsets before the given original index.
   *
   * @param originalIndex The original index to translate to a new index.
   * @return The new index.
   */
  public int computeOffsetFromIndex(int originalIndex) {
    int totalOffset = 0;
    for (Pair<Integer, Integer> offset : offsets) {
      if (originalIndex >= offset.first) {
        totalOffset += offset.second;
      }
    }
    return totalOffset;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    Offsets offsets1 = (Offsets) o;
    return offsets.equals(offsets1.offsets);
  }

  @Override
  public int hashCode() {
    return offsets.hashCode();
  }

  @Override
  public String toString() {
    return offsets.toString();
  }

  public int total() {
    return offsets.stream().mapToInt((offset) -> offset.second).sum();
  }
}
