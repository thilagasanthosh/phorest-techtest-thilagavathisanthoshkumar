package com.thilaga.fruitmachine.domain;

/** Which float policy a machine was configured with. */
public enum FloatPolicyType {
  MIN_RESERVE,
  NONE;

  private static final FloatPolicy MIN_RESERVE_POLICY = new MinimumReservePolicy();
  private static final FloatPolicy NONE_POLICY = new NoReservePolicy();

  /** Adding a third policy is one new {@link FloatPolicy} class plus one branch here. */
  FloatPolicy toPolicy() {
    return switch (this) {
      case MIN_RESERVE -> MIN_RESERVE_POLICY;
      case NONE -> NONE_POLICY;
    };
  }
}
