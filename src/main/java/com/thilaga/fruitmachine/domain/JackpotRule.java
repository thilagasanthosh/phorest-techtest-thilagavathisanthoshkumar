package com.thilaga.fruitmachine.domain;

import java.util.Optional;

/** All slots the same colour (BR-04). */
public final class JackpotRule implements PrizeRule {

  @Override
  public Optional<PrizeType> evaluate(SpinProfile profile, MachineConfig config) {
    return profile.distinctColours() == 1 ? Optional.of(PrizeType.JACKPOT) : Optional.empty();
  }
}
