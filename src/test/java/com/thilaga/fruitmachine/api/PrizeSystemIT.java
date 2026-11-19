package com.thilaga.fruitmachine.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** SC-03 to SC-06 over real HTTP with forced spins. */
class PrizeSystemIT extends SystemTestBase {

  @Test
  void sc03_noPrizeKeepsStakeInFloat() throws Exception {
    String id = createMachine(machine(1000, "MIN_RESERVE"));
    spins.enqueue(NO_WIN);

    var r = json(play(id, PAID));

    assertEquals("NONE", r.get("prize").asText());
    assertEquals(0, r.get("paidOut").asInt());
    assertEquals(1100, r.get("floatAfter").asInt());
    assertEquals("ACTIVE", r.get("status").asText());
  }

  @Test
  void sc04_jackpotPaysWholeFloatAndNeedsRefill() throws Exception {
    String id = createMachine(machine(1000, "MIN_RESERVE"));
    spins.enqueue(JACKPOT);

    var r = json(play(id, PAID));

    assertEquals("JACKPOT", r.get("prize").asText());
    assertEquals(1100, r.get("paidOut").asInt());
    assertEquals(0, r.get("floatAfter").asInt());
    assertEquals("NEEDS_REFILL", r.get("status").asText());
    var state = json(get(BASE + "/" + id));
    assertEquals(0, state.get("floatPence").asInt());
    assertEquals("NEEDS_REFILL", state.get("status").asText());
  }

  @Test
  void sc05_fullHousePaysHalfOfOddFloatRoundedDown() throws Exception {
    String id = createMachine(machine(1001, "MIN_RESERVE"));
    spins.enqueue(FULL_HOUSE);

    var r = json(play(id, PAID));

    assertEquals("FULL_HOUSE", r.get("prize").asText());
    assertEquals(550, r.get("paidOut").asInt());
    assertEquals(551, r.get("floatAfter").asInt());
    assertEquals("ACTIVE", r.get("status").asText());
  }

  @Test
  void sc06_smallPrizePaysFiveTimesCost() throws Exception {
    String id = createMachine(machine(1000, "MIN_RESERVE"));
    spins.enqueue(SMALL);

    var r = json(play(id, PAID));

    assertEquals("SMALL_PRIZE", r.get("prize").asText());
    assertEquals(500, r.get("nominalPayout").asInt());
    assertEquals(500, r.get("paidOut").asInt());
    assertEquals(600, r.get("floatAfter").asInt());
  }
}
