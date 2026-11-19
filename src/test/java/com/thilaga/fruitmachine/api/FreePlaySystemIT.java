package com.thilaga.fruitmachine.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** SC-07 to SC-09 over real HTTP under policy NONE. */
class FreePlaySystemIT extends SystemTestBase {

  @Test
  void sc07_shortfallBecomesOneFreePlay() throws Exception {
    String id = createMachine(machine(300, "NONE"));
    spins.enqueue(SMALL);

    var r = json(play(id, PAID));

    assertEquals(400, r.get("paidOut").asInt());
    assertEquals(500, r.get("nominalPayout").asInt());
    assertEquals(1, r.get("freePlaysCredited").asInt());
    assertEquals(1, r.get("freePlayBalance").asInt());
    assertEquals(0, r.get("floatAfter").asInt());
  }

  @Test
  void sc08_shortfallRoundedUpToWholeFreePlays() throws Exception {
    String id = createMachine(machine(250, "NONE"));
    spins.enqueue(SMALL);

    var r = json(play(id, PAID));

    assertEquals(350, r.get("paidOut").asInt());
    assertEquals(2, r.get("freePlaysCredited").asInt());
    assertEquals(2, r.get("freePlayBalance").asInt());
    assertEquals(0, r.get("floatAfter").asInt());
  }

  @Test
  void sc09_freePlayUsesCreditAddsNoStakeAndCanEarnMore() throws Exception {
    String id = createMachine(machine(300, "NONE"));
    spins.enqueue(SMALL, SMALL);
    play(id, PAID); // SC-07: leaves 1 credit, float 0

    var r = json(play(id, FREE));

    assertEquals(0, r.get("paidOut").asInt());
    assertEquals(5, r.get("freePlaysCredited").asInt());
    assertEquals(5, r.get("freePlayBalance").asInt());
    assertEquals(true, r.get("paidWithFreePlay").asBoolean());
    assertEquals(0, r.get("floatAfter").asInt());
  }

  @Test
  void sc09_freePlayWithZeroBalanceRefused() throws Exception {
    String id = createMachine(machine(0, "NONE"));

    var r = play(id, FREE);

    assertEquals(409, r.statusCode(), r.body());
    assertEquals("no-free-plays", json(r).get("type").asText());
    assertEquals(0, spins.calls());
  }
}
