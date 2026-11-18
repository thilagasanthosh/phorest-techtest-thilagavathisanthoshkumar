package com.thilaga.fruitmachine.domain;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class MachineTest {

  private static MachineConfig minReserveConfig(long playCostPence, long startingFloatPence) {
    return MachineConfig.of(
        4,
        MachineConfig.DEFAULT_COLOURS,
        2,
        playCostPence,
        startingFloatPence,
        FloatPolicyType.MIN_RESERVE);
  }

  private static MachineConfig noneConfig(long playCostPence, long startingFloatPence) {
    return MachineConfig.of(
        4,
        MachineConfig.DEFAULT_COLOURS,
        2,
        playCostPence,
        startingFloatPence,
        FloatPolicyType.NONE);
  }

  @Test
  void paidPlayReturnsFullResultShape() {
    // AC-15
    Machine machine = new Machine(minReserveConfig(100, 1_000));
    int[] smallPrizeSpin = {0, 0, 1, 2}; // run of 2 >= k, distinct colours (3) != slots (4)

    PlayResult result = machine.play(false, new FixedSpinGenerator(smallPrizeSpin));

    assertArrayEquals(smallPrizeSpin, result.spin());
    assertEquals(PrizeType.SMALL_PRIZE, result.prize());
    assertEquals(500, result.paidPence());
    assertEquals(0, result.freePlaysEarned());
    assertEquals(0, result.freePlayBalanceAfter());
    assertEquals(600, result.floatAfterPence());
    assertEquals(MachineStatus.ACTIVE, result.status());
  }

  @Test
  void rejectsPlayBelowReserve_noStateChange() {
    // AC-16: play cost 100p, float 499p (below the 500p reserve).
    Machine machine = new Machine(minReserveConfig(100, 1_000), 499, 0);

    assertThrows(
        MachineNeedsRefillException.class,
        () -> machine.play(false, new SecureRandomSpinGenerator()));

    assertEquals(499, machine.floatPence());
    assertEquals(0, machine.freePlayBalance());
  }

  @Test
  void acceptsPlayAtExactlyTheReserve() {
    // AC-17: play cost 100p, float 500p; wins a small prize, paid in full, no free plays.
    Machine machine = new Machine(minReserveConfig(100, 1_000), 500, 0);
    int[] smallPrizeSpin = {0, 0, 1, 2};

    PlayResult result = machine.play(false, new FixedSpinGenerator(smallPrizeSpin));

    assertEquals(500, result.paidPence());
    assertEquals(0, result.freePlaysEarned());
  }

  @Test
  void statusFollowsFloat_afterJackpotThenRefill() {
    // AC-18: a machine that has just paid a jackpot, float 0p.
    Machine machine = new Machine(minReserveConfig(100, 1_000), 0, 0);

    assertEquals(MachineStatus.NEEDS_REFILL, machine.status());

    machine.refill(1_000);

    assertEquals(MachineStatus.ACTIVE, machine.status());
  }

  @Test
  void shortfallBecomesAFreePlay_noneMachine() {
    // AC-22: NONE machine, play cost 100p, float 300p, wins a small prize.
    Machine machine = new Machine(noneConfig(100, 300));
    int[] smallPrizeSpin = {0, 0, 1, 2};

    PlayResult result = machine.play(false, new FixedSpinGenerator(smallPrizeSpin));

    assertEquals(400, result.paidPence());
    assertEquals(1, result.freePlaysEarned());
    assertEquals(1, result.freePlayBalanceAfter());
    assertEquals(0, result.floatAfterPence());
  }

  @Test
  void shortfallIsRoundedUp_noneMachine() {
    // AC-23: NONE machine, play cost 100p, float 250p, wins a small prize.
    Machine machine = new Machine(noneConfig(100, 250));
    int[] smallPrizeSpin = {0, 0, 1, 2};

    PlayResult result = machine.play(false, new FixedSpinGenerator(smallPrizeSpin));

    assertEquals(2, result.freePlaysEarned());
  }

  @Test
  void jackpotNeverCreatesFreePlays_fromAFreePlay() {
    // AC-24: NONE machine, float 0p, 1 free play, the free play wins the jackpot.
    Machine machine = new Machine(noneConfig(100, 0), 0, 1);
    int[] jackpotSpin = {0, 0, 0, 0};

    PlayResult result = machine.play(true, new FixedSpinGenerator(jackpotSpin));

    assertEquals(PrizeType.JACKPOT, result.prize());
    assertEquals(0, result.paidPence());
    assertEquals(0, result.freePlaysEarned());
    assertEquals(0, result.freePlayBalanceAfter());
  }

  @Test
  void usingAFreePlayCredit() {
    // AC-25: NONE machine, play cost 100p, float 0p, 1 free play, wins a small prize.
    Machine machine = new Machine(noneConfig(100, 0), 0, 1);
    int[] smallPrizeSpin = {0, 0, 1, 2};

    PlayResult result = machine.play(true, new FixedSpinGenerator(smallPrizeSpin));

    assertEquals(0, result.paidPence()); // no stake added, nothing available to pay
    assertEquals(5, result.freePlaysEarned()); // ceil(500 / 100)
    assertEquals(5, result.freePlayBalanceAfter()); // 1 - 1 + 5
  }

  @Test
  void rejectsFreePlayWithoutCredit() {
    // AC-26
    Machine machine = new Machine(minReserveConfig(100, 1_000));

    assertThrows(
        NoFreePlayAvailableException.class,
        () -> machine.play(true, new SecureRandomSpinGenerator()));
  }

  @Test
  void heldFreePlaySurvivesARefusal() {
    // AC-27: MIN_RESERVE machine holding 1 free play, float below the reserve.
    Machine machine = new Machine(minReserveConfig(100, 1_000), 400, 1);

    assertThrows(
        MachineNeedsRefillException.class,
        () -> machine.play(true, new SecureRandomSpinGenerator()));
    assertEquals(1, machine.freePlayBalance());

    machine.refill(600); // float is now 1,000p, at the reserve.

    int[] noWinSpin = {0, 1, 0, 1};
    PlayResult result = machine.play(true, new FixedSpinGenerator(noWinSpin));

    assertEquals(PrizeType.NONE, result.prize());
    assertEquals(0, machine.freePlayBalance());
  }
}
