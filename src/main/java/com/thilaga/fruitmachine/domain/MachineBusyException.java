package com.thilaga.fruitmachine.domain;

/**
 * The machine is locked by another play or refill (lock timeout or deadlock). UC-12: retry later.
 */
public class MachineBusyException extends RuntimeException {

  public MachineBusyException(Throwable cause) {
    super("machine is busy, retry later", cause);
  }
}
