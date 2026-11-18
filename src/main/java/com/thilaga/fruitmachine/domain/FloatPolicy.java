package com.thilaga.fruitmachine.domain;

/** Whether a machine's float is healthy enough to play (BR-07). Applies to any play (D10). */
public interface FloatPolicy {

  boolean canPlay(long floatPence, long playCostPence);

  MachineStatus status(long floatPence, long playCostPence);

  /** Float the policy holds back before a play (D10), or 0 if it holds none. */
  default long reservePence(long playCostPence) {
    return 0;
  }

  /** Config-time invariant this policy imposes on a starting float, or null if none. */
  default String validateStartingFloat(long startingFloatPence, long playCostPence) {
    return null;
  }
}
