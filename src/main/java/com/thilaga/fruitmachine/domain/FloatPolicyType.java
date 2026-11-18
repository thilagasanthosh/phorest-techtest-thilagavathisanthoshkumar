package com.thilaga.fruitmachine.domain;

/** Which float policy a machine was configured with. */
public enum FloatPolicyType {
  MIN_RESERVE,
  NONE;

  /** Adding a third policy is one new {@link FloatPolicy} class plus one branch here. */
  FloatPolicy toPolicy() {
    return switch (this) {
      case MIN_RESERVE -> new MinimumReservePolicy();
      case NONE -> new NoReservePolicy();
    };
  }
}
