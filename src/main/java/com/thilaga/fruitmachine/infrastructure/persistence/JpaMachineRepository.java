package com.thilaga.fruitmachine.infrastructure.persistence;

import com.thilaga.fruitmachine.domain.Machine;
import com.thilaga.fruitmachine.domain.MachineBusyException;
import com.thilaga.fruitmachine.domain.MachineNotFoundException;
import com.thilaga.fruitmachine.domain.MachineRepository;
import com.thilaga.fruitmachine.domain.MachineState;
import com.thilaga.fruitmachine.domain.MachineTotals;
import com.thilaga.fruitmachine.domain.PlayResult;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

/** JPA adapter for the domain port. Runs inside the caller's transaction (MachineService). */
@Repository
class JpaMachineRepository implements MachineRepository {

  private static final int ER_LOCK_WAIT_TIMEOUT = 1205;
  private static final int ER_LOCK_DEADLOCK = 1213;

  /**
   * NFR-04: 2 s lock wait. Hibernate drops jakarta.persistence.lock.timeout on MySQL, and MySQL 8.4
   * ignores SET_VAR for innodb_lock_wait_timeout, so the wait is set on the session around the
   * locking read and reset straight after, so the pooled connection keeps its default.
   */
  private static final String SET_LOCK_WAIT = "SET SESSION innodb_lock_wait_timeout = 2";

  private static final String RESET_LOCK_WAIT = "SET SESSION innodb_lock_wait_timeout = DEFAULT";
  private static final String LOCK_SQL = "SELECT * FROM machine WHERE id = ? FOR UPDATE";

  private final MachineJpaRepository machines;
  private final PlayIdempotencyJpaRepository idempotency;
  private final EntityManager entityManager;
  private final ObjectMapper json;

  JpaMachineRepository(
      MachineJpaRepository machines,
      PlayIdempotencyJpaRepository idempotency,
      EntityManager entityManager,
      ObjectMapper json) {
    this.machines = machines;
    this.idempotency = idempotency;
    this.entityManager = entityManager;
    this.json = json;
  }

  @Override
  public void insert(UUID id, Machine machine) {
    entityManager.persist(MachineMapper.toEntity(id, machine, new MachineTotals(0, 0, 0)));
  }

  @Override
  public Optional<MachineState> find(UUID id) {
    return machines.findById(id).map(MachineMapper::toState);
  }

  @Override
  public Optional<Machine> findForUpdate(UUID id) {
    try {
      entityManager.createNativeQuery(SET_LOCK_WAIT).executeUpdate();
      List<MachineEntity> rows;
      try {
        rows =
            entityManager
                // UUID binding relies on Hibernate's default UUID<->BINARY(16) mapping matching
                // the entity attribute's mapping (no explicit JDBC type set here); upgrade to an
                // explicit type if the two ever diverge.
                .createNativeQuery(LOCK_SQL, MachineEntity.class)
                .setParameter(1, id)
                .getResultList();
      } finally {
        resetLockWaitQuietly();
      }
      return rows.stream().findFirst().map(entity -> MachineMapper.toState(entity).machine());
    } catch (PersistenceException e) {
      if (isLockFailure(e)) {
        throw new MachineBusyException(e);
      }
      throw e;
    }
  }

  /**
   * Best-effort reset: after a deadlock, MySQL has already rolled back the connection, so this
   * statement can itself fail. It must never replace the real lock/deadlock exception from the try
   * block above with its own.
   */
  private void resetLockWaitQuietly() {
    try {
      entityManager.createNativeQuery(RESET_LOCK_WAIT).executeUpdate();
    } catch (PersistenceException ignored) {
      // The original exception from the locking read, if any, is what the caller must see.
    }
  }

  /** Lock wait timeout (1205) or deadlock victim (1213), per section 6.2. */
  private static boolean isLockFailure(Throwable e) {
    for (Throwable t = e; t != null; t = t.getCause()) {
      if (t instanceof SQLException sql
          && (sql.getErrorCode() == ER_LOCK_WAIT_TIMEOUT
              || sql.getErrorCode() == ER_LOCK_DEADLOCK)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public void save(UUID id, Machine machine) {
    MachineEntity entity = managed(id);
    MachineMapper.applyState(entity, machine);
  }

  @Override
  public void recordPlay(UUID id, PlayResult result, boolean usedFreePlay) {
    MachineEntity entity = managed(id);
    entity.totalPlays++;
    entity.totalPaidPence += result.paidPence();
    entityManager.persist(
        MachineMapper.toPlayEntity(result.playId(), id, entity.colours, result, usedFreePlay));
  }

  @Override
  public void recordRefill(UUID id, long amountPence, long floatAfterPence) {
    MachineEntity entity = managed(id);
    entity.totalRefilledPence += amountPence;
    entityManager.persist(
        MachineMapper.toRefillEntity(UUID.randomUUID(), id, amountPence, floatAfterPence));
  }

  @Override
  public Optional<PlayResult> findReplay(UUID id, String idempotencyKey) {
    return idempotency
        .findByMachineIdAndIdempotencyKey(id, idempotencyKey)
        .map(row -> readResult(row.result));
  }

  @Override
  public void recordReplay(UUID id, String idempotencyKey, PlayResult result) {
    entityManager.persist(
        new PlayIdempotencyEntity(UUID.randomUUID(), id, idempotencyKey, writeResult(result)));
  }

  private PlayResult readResult(String value) {
    try {
      return json.readValue(value, PlayResult.class);
    } catch (JacksonException e) {
      throw new IllegalStateException("stored idempotent play result is unreadable", e);
    }
  }

  private String writeResult(PlayResult result) {
    try {
      return json.writeValueAsString(result);
    } catch (JacksonException e) {
      throw new IllegalStateException("play result cannot be stored", e);
    }
  }

  /** The row loaded by findForUpdate in the same transaction; changes flush at commit. */
  private MachineEntity managed(UUID id) {
    return machines.findById(id).orElseThrow(() -> new MachineNotFoundException(id));
  }
}
