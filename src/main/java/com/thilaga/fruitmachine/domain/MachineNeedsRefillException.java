package com.thilaga.fruitmachine.domain;

/** Thrown when the float policy refuses a play - paid or free (BR-07, D10, AC-16, AC-27). */
public class MachineNeedsRefillException extends RuntimeException {

  public MachineNeedsRefillException() {
    super("machine needs refill");
  }
}
