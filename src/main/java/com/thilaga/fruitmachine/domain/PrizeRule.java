package com.thilaga.fruitmachine.domain;

import java.util.Optional;

/**
 * One prize's matching rule. A new rule is one new class implementing this interface plus one
 * registration line in {@link PrizeEvaluator} - existing rules and their tests stay unchanged
 * (NFR-11).
 */
public interface PrizeRule {

  Optional<PrizeType> evaluate(SpinProfile profile, MachineConfig config);
}
