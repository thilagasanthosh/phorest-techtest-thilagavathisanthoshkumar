package com.thilaga.fruitmachine.domain;

import java.util.List;

/** Thrown when a machine configuration fails validation. Carries every faulty field found. */
public class InvalidMachineConfigException extends RuntimeException {

  private final List<String> errors;

  public InvalidMachineConfigException(List<String> errors) {
    super(String.join("; ", errors));
    this.errors = List.copyOf(errors);
  }

  public List<String> errors() {
    return errors;
  }
}
