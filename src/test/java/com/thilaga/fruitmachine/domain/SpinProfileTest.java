package com.thilaga.fruitmachine.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SpinProfileTest {

  @Test
  void runLengthBoundary_exactlyThreeWinsOnlyTwoDoesNot() {
    SpinProfile exactlyThree = SpinProfile.of(new int[] {0, 0, 0, 1});
    SpinProfile onlyTwo = SpinProfile.of(new int[] {0, 0, 1, 1});

    assertEquals(3, exactlyThree.longestRun());
    assertEquals(2, onlyTwo.longestRun());
  }

  @Test
  void fullHouseCountCannotExceedColourPool() {
    // 5 slots, only 4 distinct colour ids (0-3) available - a full house (5 distinct) is
    // structurally impossible; the most distinct colours achievable is 4 (AC-13, D6).
    SpinProfile profile = SpinProfile.of(new int[] {0, 1, 2, 3, 3});

    assertEquals(4, profile.distinctColours());
    assertNotEquals(5, profile.distinctColours());
  }

  @Test
  void oneSlotSpin_isTriviallyAllSameColour() {
    SpinProfile profile = SpinProfile.of(new int[] {0});

    assertEquals(1, profile.longestRun());
    assertEquals(1, profile.distinctColours());
  }

  @Test
  void largeSpinEvaluatesUnder100ms() {
    int[] spin = new int[1_000_000];
    for (int i = 0; i < spin.length; i++) {
      spin[i] = i % 1000;
    }

    long start = System.nanoTime();
    SpinProfile.of(spin);
    long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

    assertTrue(elapsedMillis < 100, "took " + elapsedMillis + "ms");
  }
}
