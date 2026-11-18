package com.thilaga.fruitmachine.application;

import com.thilaga.fruitmachine.domain.Machine;
import com.thilaga.fruitmachine.domain.MachineConfig;
import com.thilaga.fruitmachine.domain.MachineNotFoundException;
import com.thilaga.fruitmachine.domain.MachineRepository;
import com.thilaga.fruitmachine.domain.MachineState;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use cases for fruit machines: each public method is one transaction (CLAUDE.md). */
@Service
public class MachineService {

  private final MachineRepository repository;

  public MachineService(MachineRepository repository) {
    this.repository = repository;
  }

  /** UC-01: creates a machine with zero free plays and zero totals. */
  @Transactional
  public UUID configure(MachineConfig config) {
    UUID id = UUID.randomUUID();
    repository.insert(id, new Machine(config));
    return id;
  }

  /** UC-06: reads a machine without locking it. */
  @Transactional(readOnly = true)
  public MachineState read(UUID id) {
    return repository.find(id).orElseThrow(() -> new MachineNotFoundException(id));
  }
}
