package com.thilaga.fruitmachine.domain;

/** Refuses any play while the float is below 5 x the play cost (D10). */
public final class MinimumReservePolicy implements FloatPolicy {

  /** D10: the float must be at least this many times the play cost before a play is allowed. */
  public static final int RESERVE_MULTIPLIER = 5;

  @Override
  public boolean canPlay(long floatPence, long playCostPence) {
    return floatPence >= RESERVE_MULTIPLIER * playCostPence;
  }

  @Override
  public MachineStatus status(long floatPence, long playCostPence) {
    return canPlay(floatPence, playCostPence) ? MachineStatus.ACTIVE : MachineStatus.NEEDS_REFILL;
  }
}
