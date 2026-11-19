package com.thilaga.fruitmachine.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** SC-10 and SC-11 over real HTTP: refusal under MIN_RESERVE, operator refill. */
class RefusalSystemIT extends SystemTestBase {

  @Test
  void sc10_playBelowReserveRefusedAndChangesNothing() throws Exception {
    // Full house from float 898: stake makes 998, pays 499, leaves 499 (< 500 reserve).
    String id = createMachine(machine(898, "MIN_RESERVE"));
    spins.enqueue(FULL_HOUSE);
    assertEquals(499, json(play(id, PAID)).get("floatAfter").asInt());

    var r = play(id, PAID);

    assertEquals(409, r.statusCode(), r.body());
    assertEquals("machine-needs-refill", json(r).get("type").asText());
    assertEquals(499, json(r).get("floatPence").asInt());
    assertEquals(500, json(r).get("reservePence").asInt());
    assertEquals("NEEDS_REFILL", json(r).get("machineStatus").asText());
    assertEquals(1, spins.calls(), "a refused play must not spin");
    assertEquals(1, count("play"));
    assertEquals(499, json(get(BASE + "/" + id)).get("floatPence").asInt());
  }

  @Test
  void sc10_playAtExactlyReserveAccepted() throws Exception {
    String id = createMachine(machine(500, "MIN_RESERVE"));
    spins.enqueue(NO_WIN);

    var r = json(play(id, PAID));

    assertEquals(600, r.get("floatAfter").asInt());
  }

  @Test
  void sc11_operatorRefillRestoresActiveMachine() throws Exception {
    String id = createMachine(machine(1000, "MIN_RESERVE"));
    spins.enqueue(JACKPOT);
    play(id, PAID);

    var refilled = post(BASE + "/" + id + "/refills", "{\"amountPence\":5000}");

    assertEquals(200, refilled.statusCode(), refilled.body());
    assertEquals(5000, json(refilled).get("floatPence").asInt());
    assertEquals("ACTIVE", json(refilled).get("status").asText());
    var state = json(get(BASE + "/" + id));
    assertEquals(5000, state.get("floatPence").asInt());
    assertEquals("ACTIVE", state.get("status").asText());
    assertEquals(1, count("refill"));
  }
}
