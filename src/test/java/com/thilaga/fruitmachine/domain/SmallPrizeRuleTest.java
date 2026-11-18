package com.thilaga.fruitmachine.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SmallPrizeRuleTest {

  private final SmallPrizeRule rule = new SmallPrizeRule();
  private final MachineConfig config =
      MachineConfig.of(4, List.of("a", "b", "c", "d"), 2, 100, 0, FloatPolicyType.NONE);

  @Test
  void matchesWhenRunAtLeastK() {
    Optional<PrizeType> result = rule.evaluate(new SpinProfile(2, 3), config);

    assertEquals(Optional.of(PrizeType.SMALL_PRIZE), result);
  }

  @Test
  void doesNotMatchWhenRunBelowK() {
    Optional<PrizeType> result = rule.evaluate(new SpinProfile(1, 4), config);

    assertTrue(result.isEmpty());
  }
}
