package com.thilaga.fruitmachine.domain;

/** Accepts every play regardless of float; shortfalls become free plays instead (BR-07). */
public final class NoReservePolicy implements FloatPolicy {

  @Override
  public boolean canPlay(long floatPence, long playCostPence) {
    return true;
  }

  @Override
  public MachineStatus status(long floatPence, long playCostPence) {
    return MachineStatus.ACTIVE;
  }
}
