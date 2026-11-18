package com.thilaga.fruitmachine.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class JackpotRuleTest {

  private final JackpotRule rule = new JackpotRule();
  private final MachineConfig config =
      MachineConfig.of(4, List.of("a", "b", "c", "d"), 2, 100, 0, FloatPolicyType.NONE);

  @Test
  void matchesWhenOneDistinctColour() {
    Optional<PrizeType> result = rule.evaluate(new SpinProfile(4, 1), config);

    assertEquals(Optional.of(PrizeType.JACKPOT), result);
  }

  @Test
  void doesNotMatchWhenMoreThanOneDistinctColour() {
    Optional<PrizeType> result = rule.evaluate(new SpinProfile(2, 2), config);

    assertTrue(result.isEmpty());
  }
}
