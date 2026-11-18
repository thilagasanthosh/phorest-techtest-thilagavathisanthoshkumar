package com.thilaga.fruitmachine.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.Test;

class PrizeEvaluatorTest {

  @Test
  void onlyHighestPrizePays() {
    // AC-11: four identical colours is both a jackpot AND (incidentally) a run >= k=2.
    MachineConfig config =
        MachineConfig.of(4, List.of("a", "b", "c", "d"), 2, 100, 0, FloatPolicyType.NONE);
    SpinProfile profile = SpinProfile.of(new int[] {0, 0, 0, 0});

    assertEquals(PrizeType.JACKPOT, PrizeEvaluator.evaluate(profile, config));
  }

  @Test
  void noWinWhenNoRuleMatches() {
    // AC-10: black, white, black, white - no jackpot, no full house, no run >= k=2.
    MachineConfig config =
        MachineConfig.of(4, List.of("black", "white"), 2, 100, 0, FloatPolicyType.NONE);
    SpinProfile profile = SpinProfile.of(new int[] {0, 1, 0, 1});

    assertEquals(PrizeType.NONE, PrizeEvaluator.evaluate(profile, config));
  }

  @Test
  void oneSlotIsAlwaysJackpot() {
    // AC-14
    MachineConfig config = MachineConfig.of(1, List.of("a"), 1, 100, 0, FloatPolicyType.NONE);
    SpinProfile profile = SpinProfile.of(new int[] {0});

    assertEquals(PrizeType.JACKPOT, PrizeEvaluator.evaluate(profile, config));
  }
}
