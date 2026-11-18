package com.thilaga.fruitmachine.infrastructure.persistence;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.thilaga.fruitmachine.TestcontainersConfiguration;
import com.thilaga.fruitmachine.domain.FloatPolicyType;
import com.thilaga.fruitmachine.domain.Machine;
import com.thilaga.fruitmachine.domain.MachineBusyException;
import com.thilaga.fruitmachine.domain.MachineConfig;
import com.thilaga.fruitmachine.domain.MachineRepository;
import com.thilaga.fruitmachine.domain.MachineState;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class JpaMachineRepositoryTest {

  private static final MachineConfig CONFIG =
      MachineConfig.of(
          4,
          List.of("black", "white", "green", "yellow"),
          2,
          100,
          1_000,
          FloatPolicyType.MIN_RESERVE);

  @Autowired private MachineRepository repository;
  @Autowired private TransactionTemplate tx;
  @Autowired private JdbcTemplate jdbc;

  private final ExecutorService threads = Executors.newFixedThreadPool(2);

  @BeforeEach
  void clearTables() {
    jdbc.update("DELETE FROM refill");
    jdbc.update("DELETE FROM play");
    jdbc.update("DELETE FROM machine");
  }

  @AfterEach
  void stopThreads() {
    threads.shutdownNow();
  }

  @Test
  void insertThenFindReturnsTheSameMachine() {
    UUID id = UUID.randomUUID();
    tx.executeWithoutResult(s -> repository.insert(id, new Machine(CONFIG, 1_000, 2)));

    MachineState state = tx.execute(s -> repository.find(id)).orElseThrow();

    assertEquals(id, state.id());
    assertEquals(CONFIG, state.machine().config());
    assertEquals(1_000, state.machine().floatPence());
    assertEquals(2, state.machine().freePlayBalance());
  }

  @Test
  void findForUpdateOfUnknownIdIsEmpty() {
    assertFalse(tx.execute(s -> repository.findForUpdate(UUID.randomUUID())).isPresent());
  }

  @Test
  void deadlockIsReportedAsBusyForExactlyOneOfTwoOppositeLockers() throws Exception {
    UUID a = UUID.randomUUID();
    UUID b = UUID.randomUUID();
    tx.executeWithoutResult(s -> repository.insert(a, new Machine(CONFIG, 1_000, 0)));
    tx.executeWithoutResult(s -> repository.insert(b, new Machine(CONFIG, 1_000, 0)));

    CountDownLatch aHeld = new CountDownLatch(1);
    CountDownLatch bHeld = new CountDownLatch(1);
    Future<Boolean> first = threads.submit(() -> lockInOrder(a, aHeld, b, bHeld));
    Future<Boolean> second = threads.submit(() -> lockInOrder(b, bHeld, a, aHeld));

    int busy =
        (first.get(20, TimeUnit.SECONDS) ? 1 : 0) + (second.get(20, TimeUnit.SECONDS) ? 1 : 0);
    assertEquals(
        1, busy, "MySQL picks one deadlock victim; it must surface as MachineBusyException");
  }

  @Test
  void lockTimeoutIsReportedAsBusyWithinTwoSeconds() throws Exception {
    UUID id = UUID.randomUUID();
    tx.executeWithoutResult(s -> repository.insert(id, new Machine(CONFIG, 1_000, 0)));
    CountDownLatch held = new CountDownLatch(1);
    CountDownLatch release = new CountDownLatch(1);
    Future<?> holder =
        threads.submit(
            () ->
                tx.executeWithoutResult(
                    s -> {
                      repository.findForUpdate(id);
                      held.countDown();
                      await(release);
                    }));
    assertTrue(held.await(10, TimeUnit.SECONDS));

    long start = System.nanoTime();
    try {
      assertThrows(
          MachineBusyException.class,
          () -> tx.executeWithoutResult(s -> repository.findForUpdate(id)));
      Duration waited = Duration.ofNanos(System.nanoTime() - start);
      assertTrue(waited.compareTo(Duration.ofSeconds(5)) < 0, "waited " + waited);
    } finally {
      release.countDown();
      holder.get(10, TimeUnit.SECONDS);
    }
  }

  /**
   * Locks {@code mine}, signals, waits for the other thread to lock its machine, then locks {@code
   * theirs}.
   */
  private boolean lockInOrder(
      UUID mine, CountDownLatch iHold, UUID theirs, CountDownLatch theyHold) {
    try {
      tx.executeWithoutResult(
          s -> {
            repository.findForUpdate(mine);
            iHold.countDown();
            await(theyHold);
            repository.findForUpdate(theirs);
          });
      return false;
    } catch (MachineBusyException e) {
      return true;
    }
  }

  private static void await(CountDownLatch latch) {
    try {
      latch.await(15, TimeUnit.SECONDS);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException(e);
    }
  }
}
