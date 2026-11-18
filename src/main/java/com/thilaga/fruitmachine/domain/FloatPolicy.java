package com.thilaga.fruitmachine.domain;

/** Whether a machine's float is healthy enough to play (BR-07). Applies to any play (D10). */
public interface FloatPolicy {

  boolean canPlay(long floatPence, long playCostPence);

  MachineStatus status(long floatPence, long playCostPence);
}
