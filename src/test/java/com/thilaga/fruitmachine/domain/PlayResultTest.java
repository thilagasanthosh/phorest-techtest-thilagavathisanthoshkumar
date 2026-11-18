package com.thilaga.fruitmachine.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PlayResultTest {

  @Test
  void equalsAndHashCodeCompareSpinContentNotReference() {
    PlayResult a =
        new PlayResult(
            new int[] {0, 1, 2}, PrizeType.NONE, 0, 0, 0, 0, 1_000, MachineStatus.ACTIVE);
    PlayResult b =
        new PlayResult(
            new int[] {0, 1, 2}, PrizeType.NONE, 0, 0, 0, 0, 1_000, MachineStatus.ACTIVE);

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertTrue(a.toString().contains("[0, 1, 2]"));
  }

  @Test
  void equalityIncludesNominalPayout() {
    PlayResult a =
        new PlayResult(
            new int[] {0, 1, 2}, PrizeType.NONE, 500, 0, 0, 0, 1_000, MachineStatus.ACTIVE);
    PlayResult b =
        new PlayResult(
            new int[] {0, 1, 2}, PrizeType.NONE, 0, 0, 0, 0, 1_000, MachineStatus.ACTIVE);

    assertNotEquals(a, b);
  }
}
