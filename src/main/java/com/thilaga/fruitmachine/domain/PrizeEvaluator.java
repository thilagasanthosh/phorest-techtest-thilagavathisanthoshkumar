package com.thilaga.fruitmachine.domain;

import java.util.List;
import java.util.Optional;

/** First match wins: jackpot, then full house, then small prize (D1). */
public final class PrizeEvaluator {

  private static final List<PrizeRule> RULES =
      List.of(new JackpotRule(), new FullHouseRule(), new SmallPrizeRule());

  private PrizeEvaluator() {}

  public static PrizeType evaluate(SpinProfile profile, MachineConfig config) {
    for (PrizeRule rule : RULES) {
      Optional<PrizeType> match = rule.evaluate(profile, config);
      if (match.isPresent()) {
        return match.get();
      }
    }
    return PrizeType.NONE;
  }
}
