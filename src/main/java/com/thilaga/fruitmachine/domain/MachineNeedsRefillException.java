package com.thilaga.fruitmachine.domain;

/**
 * Thrown when the float policy refuses a play - paid or free (BR-07, D10, AC-16, AC-27). Carries
 * the float and reserve so the refusal can state its reason (UC-10).
 */
public class MachineNeedsRefillException extends RuntimeException {

  private final long floatPence;
  private final long reservePence;

  public MachineNeedsRefillException(long floatPence, long reservePence) {
    super("machine needs refill");
    this.floatPence = floatPence;
    this.reservePence = reservePence;
  }

  public long floatPence() {
    return floatPence;
  }

  public long reservePence() {
    return reservePence;
  }
}
