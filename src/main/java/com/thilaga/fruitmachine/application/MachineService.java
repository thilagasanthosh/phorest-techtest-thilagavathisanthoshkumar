package com.thilaga.fruitmachine.application;

import com.thilaga.fruitmachine.domain.Machine;
import com.thilaga.fruitmachine.domain.MachineConfig;
import com.thilaga.fruitmachine.domain.MachineNotFoundException;
import com.thilaga.fruitmachine.domain.MachineRepository;
import com.thilaga.fruitmachine.domain.MachineState;
import com.thilaga.fruitmachine.domain.PlayResult;
import com.thilaga.fruitmachine.domain.SpinGenerator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Use cases for fruit machines: each public method is one transaction (CLAUDE.md). */
@Service
public class MachineService {

  private static final Logger log = LoggerFactory.getLogger(MachineService.class);

  private final MachineRepository repository;
  private final SpinGenerator spinGenerator;
  private final Counter plays;
  private final Counter payoutPence;
  private final Counter freePlaysCredited;

  public MachineService(
      MachineRepository repository, SpinGenerator spinGenerator, MeterRegistry meters) {
    this.repository = repository;
    this.spinGenerator = spinGenerator;
    // shortcut: counters increment before commit; a commit failure after return leaves them ahead
    // of the database. Money is checked in the database (AC-33), upgrade if counts must be exact.
    this.plays = meters.counter("fruitmachine.plays");
    this.payoutPence = meters.counter("fruitmachine.payout.pence");
    this.freePlaysCredited = meters.counter("fruitmachine.freeplays.credited");
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
    plays.increment();
    payoutPence.increment(result.paidPence());
    freePlaysCredited.increment(result.freePlaysEarned());
    log.info(
        "play machine={} play={} prize={} paid={} freePlays={}",
        id,
        result.playId(),
        result.prize(),
        result.paidPence(),
        result.freePlaysEarned());
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
