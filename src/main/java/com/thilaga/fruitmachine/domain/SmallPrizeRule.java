package com.thilaga.fruitmachine.domain;

import java.util.Optional;

/** A run of at least k adjacent slots of the same colour (BR-04). */
public final class SmallPrizeRule implements PrizeRule {

  @Override
  public Optional<PrizeType> evaluate(SpinProfile profile, MachineConfig config) {
    return profile.longestRun() >= config.k()
        ? Optional.of(PrizeType.SMALL_PRIZE)
        : Optional.empty();
  }
}
