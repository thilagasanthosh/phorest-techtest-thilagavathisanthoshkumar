package com.thilaga.fruitmachine.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class NoReservePolicyTest {

  private final NoReservePolicy policy = new NoReservePolicy();

  @Test
  void alwaysAcceptsEvenAtZeroFloat() {
    assertTrue(policy.canPlay(0, 100));
    assertEquals(MachineStatus.ACTIVE, policy.status(0, 100));
  }
}
