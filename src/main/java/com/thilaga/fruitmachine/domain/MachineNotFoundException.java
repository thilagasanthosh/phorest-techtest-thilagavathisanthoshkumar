package com.thilaga.fruitmachine.domain;

import java.util.UUID;

/** No machine has this id (AC-29). */
public class MachineNotFoundException extends RuntimeException {

  public MachineNotFoundException(UUID id) {
    super("machine not found: " + id);
  }
}
