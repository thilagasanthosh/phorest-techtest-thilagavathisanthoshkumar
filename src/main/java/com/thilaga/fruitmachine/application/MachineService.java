package com.thilaga.fruitmachine.application;

import com.thilaga.fruitmachine.domain.Machine;
import com.thilaga.fruitmachine.domain.MachineConfig;
import com.thilaga.fruitmachine.domain.MachineNotFoundException;
import com.thilaga.fruitmachine.domain.MachineRepository;
import com.thilaga.fruitmachine.domain.MachineState;
import com.thilaga.fruitmachine.domain.PlayResult;
import com.thilaga.fruitmachine.domain.SpinGenerator;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use cases for fruit machines: each public method is one transaction (CLAUDE.md). */
@Service
public class MachineService {

  private final MachineRepository repository;
  private final SpinGenerator spinGenerator;

  public MachineService(MachineRepository repository, SpinGenerator spinGenerator) {
    this.repository = repository;
    this.spinGenerator = spinGenerator;
  }

  /** UC-01: creates a machine with zero free plays and zero totals. */
  @Transactional
  public UUID configure(MachineConfig config) {
    UUID id = UUID.randomUUID();
    repository.insert(id, new Machine(config));
    return id;
  }

  /**
   * One play in one transaction (NFR-05). A refusal throws before anything is written; any failure
   * after the float is saved rolls the float and history back together.
   */
  @Transactional
  public PlayResult play(UUID id, boolean useFreePlay) {
    Machine machine = findForUpdateOrThrow(id);
    PlayResult result = machine.play(UUID.randomUUID(), useFreePlay, spinGenerator);
    repository.save(id, machine);
    repository.recordPlay(id, result, useFreePlay);
    return result;
  }

  /**
   * UC-08: an operator refill, with its own history row (D11). Refill may stay below the reserve.
   */
  @Transactional
  public void refill(UUID id, long amountPence) {
    Machine machine = findForUpdateOrThrow(id);
    machine.refill(amountPence);
    repository.save(id, machine);
    repository.recordRefill(id, amountPence, machine.floatPence());
  }

  /** UC-06: reads a machine without locking it. */
  @Transactional(readOnly = true)
  public MachineState read(UUID id) {
    return repository.find(id).orElseThrow(() -> new MachineNotFoundException(id));
  }

  private Machine findForUpdateOrThrow(UUID id) {
    return repository.findForUpdate(id).orElseThrow(() -> new MachineNotFoundException(id));
  }
}
