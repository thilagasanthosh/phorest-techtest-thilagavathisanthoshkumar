package com.thilaga.fruitmachine.domain;

import java.util.BitSet;

/**
 * What a {@link PrizeRule} needs from a spin, computed in one pass so evaluation time depends on
 * the number of slots only, never on the number of colours (NFR-01).
 */
public record SpinProfile(int longestRun, int distinctColours) {

  public static SpinProfile of(int[] spin) {
    int longestRun = 0;
    int currentRun = 0;
    int previous = -1;
    BitSet seen = new BitSet();

    for (int colour : spin) {
      currentRun = (colour == previous) ? currentRun + 1 : 1;
      previous = colour;
      if (currentRun > longestRun) {
        longestRun = currentRun;
      }
      seen.set(colour);
    }

    return new SpinProfile(longestRun, seen.cardinality());
  }
}
