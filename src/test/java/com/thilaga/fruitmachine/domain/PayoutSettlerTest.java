package com.thilaga.fruitmachine.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class PayoutSettlerTest {

  @Test
  void jackpotPaysTheEntireFloat() {
    // AC-07: float 1,000p, stake 100p -> float after stake 1,100p; jackpot pays it all.
    Settlement s = PayoutSettler.settle(1_000, 100, 100, PrizeType.JACKPOT);

    assertEquals(1_100, s.paidPence());
    assertEquals(0, s.freePlaysEarned());
    assertEquals(0, s.floatAfterPence());
  }

  @Test
  void fullHousePaysHalf() {
    // AC-08: float after stake 1,100p; full house pays 550p.
    Settlement s = PayoutSettler.settle(1_000, 100, 100, PrizeType.FULL_HOUSE);

    assertEquals(550, s.paidPence());
    assertEquals(0, s.freePlaysEarned());
    assertEquals(550, s.floatAfterPence());
  }

  @Test
  void smallPrizePaysFiveTimesPlayCost() {
    // AC-09: float after stake 1,100p; small prize pays 5 x 100p = 500p.
    Settlement s = PayoutSettler.settle(1_000, 100, 100, PrizeType.SMALL_PRIZE);

    assertEquals(500, s.paidPence());
    assertEquals(0, s.freePlaysEarned());
    assertEquals(600, s.floatAfterPence());
  }

  @Test
  void noWinPaysNothing() {
    // AC-10: float after stake 1,100p; nothing is paid.
    Settlement s = PayoutSettler.settle(1_000, 100, 100, PrizeType.NONE);

    assertEquals(0, s.paidPence());
    assertEquals(0, s.freePlaysEarned());
    assertEquals(1_100, s.floatAfterPence());
  }

  @Test
  void fullHouseRoundsDownOnAnOddFloat() {
    // AC-12: float 1,001p, stake 100p -> 1,101p; half rounded down is 550p; 551p stays.
    Settlement s = PayoutSettler.settle(1_001, 100, 100, PrizeType.FULL_HOUSE);

    assertEquals(550, s.paidPence());
    assertEquals(551, s.floatAfterPence());
  }

  @Test
  void shortfallBecomesAFreePlay() {
    // AC-22 (NONE policy): float 300p, stake 100p -> 400p; 500p due, 400p paid, 100p shortfall
    // -> 1 free play.
    Settlement s = PayoutSettler.settle(300, 100, 100, PrizeType.SMALL_PRIZE);

    assertEquals(400, s.paidPence());
    assertEquals(1, s.freePlaysEarned());
    assertEquals(0, s.floatAfterPence());
  }

  @Test
  void shortfallIsRoundedUp() {
    // AC-23: float 250p, stake 100p -> 350p; 500p due, 350p paid, 150p shortfall
    // -> ceil(150/100) = 2 free plays.
    Settlement s = PayoutSettler.settle(250, 100, 100, PrizeType.SMALL_PRIZE);

    assertEquals(350, s.paidPence());
    assertEquals(2, s.freePlaysEarned());
    assertEquals(0, s.floatAfterPence());
  }

  @Test
  void jackpotNeverCreatesFreePlaysEvenFromAFreePlay() {
    // AC-24: NONE machine, float 0p, a free play (stake 0) wins the jackpot.
    Settlement s = PayoutSettler.settle(0, 0, 100, PrizeType.JACKPOT);

    assertEquals(0, s.paidPence());
    assertEquals(0, s.freePlaysEarned());
    assertEquals(0, s.floatAfterPence());
  }
}
