package com.thilaga.fruitmachine.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MinimumReservePolicyTest {

  private final MinimumReservePolicy policy = new MinimumReservePolicy();

  @Test
  void refusesBelowTheReserve() {
    // AC-16: play cost 100p, float 499p (reserve is 500p).
    assertFalse(policy.canPlay(499, 100));
    assertEquals(MachineStatus.NEEDS_REFILL, policy.status(499, 100));
  }

  @Test
  void acceptsAtExactlyTheReserve() {
    // AC-17: play cost 100p, float 500p.
    assertTrue(policy.canPlay(500, 100));
    assertEquals(MachineStatus.ACTIVE, policy.status(500, 100));
  }

  @Test
  void statusFollowsFloat_zeroThenRefilled() {
    // AC-18: float 0p after a jackpot, then refilled to 1,000p.
    assertEquals(MachineStatus.NEEDS_REFILL, policy.status(0, 100));
    assertEquals(MachineStatus.ACTIVE, policy.status(1_000, 100));
  }
}
