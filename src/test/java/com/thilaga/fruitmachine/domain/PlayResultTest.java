package com.thilaga.fruitmachine.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlayResultTest {

  private static final UUID ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

  @Test
  void equalsAndHashCodeCompareSpinContentNotReference() {
    PlayResult a =
        new PlayResult(
            ID,
            new int[] {0, 1, 2},
            PrizeType.NONE,
            0,
            0,
            0,
            0,
            1_000,
            false,
            MachineStatus.ACTIVE);
    PlayResult b =
        new PlayResult(
            ID,
            new int[] {0, 1, 2},
            PrizeType.NONE,
            0,
            0,
            0,
            0,
            1_000,
            false,
            MachineStatus.ACTIVE);

    assertEquals(a, b);
    assertEquals(a.hashCode(), b.hashCode());
    assertTrue(a.toString().contains("[0, 1, 2]"));
  }

  @Test
  void equalityIncludesNominalPayout() {
    PlayResult a =
        new PlayResult(
            ID,
            new int[] {0, 1, 2},
            PrizeType.NONE,
            500,
            0,
            0,
            0,
            1_000,
            false,
            MachineStatus.ACTIVE);
    PlayResult b =
        new PlayResult(
            ID,
            new int[] {0, 1, 2},
            PrizeType.NONE,
            0,
            0,
            0,
            0,
            1_000,
            false,
            MachineStatus.ACTIVE);

    assertNotEquals(a, b);
  }

  @Test
  void equalityIncludesPlayId() {
    PlayResult a =
        new PlayResult(
            ID, new int[] {0}, PrizeType.NONE, 0, 0, 0, 0, 0, false, MachineStatus.ACTIVE);
    PlayResult b =
        new PlayResult(
            UUID.randomUUID(),
            new int[] {0},
            PrizeType.NONE,
            0,
            0,
            0,
            0,
            0,
            false,
            MachineStatus.ACTIVE);

    assertNotEquals(a, b);
  }
}
