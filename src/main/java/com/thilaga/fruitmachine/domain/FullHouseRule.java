package com.thilaga.fruitmachine.domain;

import java.util.Optional;

/**
 * All slots a different colour (BR-04). Structurally impossible when there are more slots than
 * colours (D6) - {@code distinctColours} can never exceed the colour pool size, so it can never
 * reach {@code slots} either; no special-case code is needed.
 */
public final class FullHouseRule implements PrizeRule {

  @Override
  public Optional<PrizeType> evaluate(SpinProfile profile, MachineConfig config) {
    return profile.distinctColours() == config.slots()
        ? Optional.of(PrizeType.FULL_HOUSE)
        : Optional.empty();
  }
}
