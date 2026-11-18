package com.thilaga.fruitmachine.domain;

/** Thrown when a free play is requested but the machine's free-play balance is 0 (AC-26). */
public class NoFreePlayAvailableException extends RuntimeException {

  public NoFreePlayAvailableException() {
    super("no free play available");
  }
}
