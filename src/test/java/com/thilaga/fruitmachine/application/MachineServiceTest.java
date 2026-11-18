package com.thilaga.fruitmachine.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.thilaga.fruitmachine.TestTables;
import com.thilaga.fruitmachine.TestcontainersConfiguration;
import com.thilaga.fruitmachine.domain.FloatPolicyType;
import com.thilaga.fruitmachine.domain.Machine;
import com.thilaga.fruitmachine.domain.MachineConfig;
import com.thilaga.fruitmachine.domain.MachineNeedsRefillException;
import com.thilaga.fruitmachine.domain.MachineNotFoundException;
import com.thilaga.fruitmachine.domain.MachineRepository;
import com.thilaga.fruitmachine.domain.MachineState;
import com.thilaga.fruitmachine.domain.MachineStatus;
import com.thilaga.fruitmachine.domain.MachineTotals;
import com.thilaga.fruitmachine.domain.NoFreePlayAvailableException;
import com.thilaga.fruitmachine.domain.PlayResult;
import com.thilaga.fruitmachine.domain.PrizeType;
import com.thilaga.fruitmachine.domain.SpinGenerator;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Integration tests on real MySQL. Not @Transactional, so MachineService's own transactions run.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MachineServiceTest {

  private static final List<String> COLOURS = List.of("black", "white", "green", "yellow");
  private static final MachineConfig CONFIG =
      MachineConfig.of(4, COLOURS, 2, 100, 1_000, FloatPolicyType.MIN_RESERVE);

  // Forced spins, test-only (never in src/main).
  private static final int[] JACKPOT = {0, 0, 0, 0};
  private static final int[] SMALL_PRIZE = {0, 0, 1, 2};
  private static final int[] NO_WIN = {0, 1, 0, 1};

  @Autowired private MachineService service;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private TransactionTemplate tx;
  @MockitoSpyBean private MachineRepository repository;
  @MockitoBean private SpinGenerator spinGenerator;

  @BeforeEach
  void clearTables() {
    TestTables.clearAll(jdbc);
  }

  @Test
  void configureCreatesAnActiveMachineWithAFreshId() {
    // AC-01: 4 slots, colours black/white/green/yellow, k = 2, 100p play, float 1,000p, MIN_RESERVE
    UUID first = service.configure(CONFIG);
    UUID second = service.configure(CONFIG);
    MachineState state = service.read(first);

    assertNotEquals(first, second);
    assertEquals(CONFIG, state.machine().config());
    assertEquals(1_000, state.machine().floatPence());
    assertEquals(500, state.machine().reservePence());
    assertEquals(MachineStatus.ACTIVE, state.machine().status());
  }

  @Test
  void configureStartsWithNoFreePlaysOrHistory() {
    // UC-01 postcondition, and AC-28 totals
    MachineState state = service.read(service.configure(CONFIG));

    assertEquals(0, state.machine().freePlayBalance());
    assertEquals(new MachineTotals(0, 0, 0), state.totals());
  }

  @Test
  void noneMachineMayStartBelowTheReserve() {
    // UC-01 A2: policy NONE accepts a starting float below the reserve; status is ACTIVE.
    UUID id = service.configure(MachineConfig.of(4, COLOURS, 2, 100, 300, FloatPolicyType.NONE));

    assertEquals(MachineStatus.ACTIVE, service.read(id).machine().status());
  }

  @Test
  void readOfAnUnknownMachineIsNotFound() {
    // AC-29
    assertThrows(MachineNotFoundException.class, () -> service.read(UUID.randomUUID()));
  }

  @Test
  void paidJackpotPaysTheWholeFloatAndCreatesNoFreePlays() {
    // BR-05, BR-06: stake 100p joins float 1,000p; the jackpot pays all 1,100p.
    UUID id = service.configure(CONFIG);
    spin(JACKPOT);

    PlayResult result = service.play(id, false);

    assertEquals(PrizeType.JACKPOT, result.prize());
    assertEquals(1_100, result.paidPence());
    assertEquals(0, result.floatAfterPence());
    assertEquals(0, floatOf(id));
    assertEquals(0, service.read(id).machine().freePlayBalance());
    assertEquals(1, countPlays("JACKPOT"));
  }

  @Test
  void paidNoWinKeepsTheStakeInTheFloat() {
    // UC-02 A1: nothing paid, stake stays in the float, one history row.
    UUID id = service.configure(CONFIG);
    spin(NO_WIN);

    PlayResult result = service.play(id, false);

    assertEquals(PrizeType.NONE, result.prize());
    assertEquals(0, result.paidPence());
    assertEquals(0, result.nominalPence());
    assertEquals(1_100, floatOf(id));
    assertEquals(1, countPlays("NONE"));
  }

  @Test
  void refusedPlayChangesNothing() {
    // AC-30 (refused play): float 0 after a jackpot; the next play is refused, no history added.
    UUID id = service.configure(CONFIG);
    spin(JACKPOT);
    service.play(id, false);

    assertThrows(MachineNeedsRefillException.class, () -> service.play(id, false));

    assertEquals(0, floatOf(id));
    assertEquals(1, countPlays());
  }

  @Test
  void freePlayUsesACreditAndAddsNoStake() {
    // D7: one credit used, no stake added; the small prize is paid from the float.
    UUID id = seed(1_000, 1);
    spin(SMALL_PRIZE);

    PlayResult result = service.play(id, true);

    assertEquals(500, result.paidPence());
    assertEquals(0, result.freePlayBalanceAfter());
    assertEquals(500, result.floatAfterPence());
    assertTrue(jdbc.queryForObject("SELECT used_free_play FROM play", Boolean.class));
  }

  @Test
  void freePlayWithNoBalanceIsRefusedAndChangesNothing() {
    // AC-26, persisted: no credit, nothing changes.
    UUID id = service.configure(CONFIG);

    assertThrows(NoFreePlayAvailableException.class, () -> service.play(id, true));

    assertEquals(1_000, floatOf(id));
    assertEquals(0, countPlays());
  }

  @Test
  void playOfAnUnknownMachineIsNotFoundAndChangesNothing() {
    // AC-29
    assertThrows(MachineNotFoundException.class, () -> service.play(UUID.randomUUID(), false));

    assertEquals(0, countPlays());
  }

  @Test
  void playOnOneMachineLeavesAnotherUnchanged() {
    // AC-31
    UUID a = service.configure(CONFIG);
    UUID b = service.configure(CONFIG);
    spin(JACKPOT);

    service.play(a, false);

    MachineState other = service.read(b);
    assertEquals(0, floatOf(a));
    assertEquals(1_000, other.machine().floatPence());
    assertEquals(0, other.machine().freePlayBalance());
    assertEquals(new MachineTotals(0, 0, 0), other.totals());
  }

  @Test
  void failureAfterTheFloatIsSavedRollsBackTheWholePlay() {
    // NFR-05: recordPlay fails after save has run; float and history must be unchanged.
    UUID id = service.configure(CONFIG);
    spin(SMALL_PRIZE);
    doThrow(new IllegalStateException("boom"))
        .when(repository)
        .recordPlay(any(), any(), anyBoolean());

    // The repository proxy translates the injected exception, so only the type of runtime is
    // checked.
    assertThrows(RuntimeException.class, () -> service.play(id, false));

    assertEquals(1_000, floatOf(id));
    assertEquals(0, service.read(id).machine().freePlayBalance());
    assertEquals(0, countPlays());
    assertEquals(new MachineTotals(0, 0, 0), service.read(id).totals());
  }

  @Test
  void refillAddsToTheFloatAndIsRecorded() {
    // AC-19: float 0 after a jackpot; refill 5,000p gives float 5,000p, one refill row, ACTIVE.
    UUID id = service.configure(CONFIG);
    spin(JACKPOT);
    service.play(id, false);

    service.refill(id, 5_000);

    MachineState state = service.read(id);
    assertEquals(5_000, state.machine().floatPence());
    assertEquals(MachineStatus.ACTIVE, state.machine().status());
    assertEquals(1, countRefills());
    assertEquals(5_000, state.totals().totalRefilledPence());
  }

  @Test
  void refillToExactlyTheReserveAllowsPlayingAgain() {
    // BR-08: refused at float 0, refilled to 500p (the reserve), accepted again.
    UUID id = service.configure(CONFIG);
    spin(JACKPOT);
    service.play(id, false);
    spin(NO_WIN);
    assertThrows(MachineNeedsRefillException.class, () -> service.play(id, false));

    service.refill(id, 500);

    assertEquals(MachineStatus.ACTIVE, service.read(id).machine().status());
    assertEquals(PrizeType.NONE, service.play(id, false).prize());
  }

  @Test
  void refillBelowTheReserveIsAcceptedButStillNeedsRefill() {
    // UC-08 A1: the refill is kept; status stays NEEDS_REFILL and play is still refused.
    UUID id = service.configure(CONFIG);
    spin(JACKPOT);
    service.play(id, false);

    service.refill(id, 100);

    assertEquals(100, floatOf(id));
    assertEquals(MachineStatus.NEEDS_REFILL, service.read(id).machine().status());
    spin(NO_WIN);
    assertThrows(MachineNeedsRefillException.class, () -> service.play(id, false));
  }

  @Test
  void refillOfZeroOrLessIsRejectedAndRecordsNothing() {
    // UC-08 step 2
    UUID id = service.configure(CONFIG);

    assertThrows(IllegalArgumentException.class, () -> service.refill(id, 0));
    assertThrows(IllegalArgumentException.class, () -> service.refill(id, -5));

    assertEquals(1_000, floatOf(id));
    assertEquals(0, countRefills());
  }

  @Test
  void refillOfAnUnknownMachineIsNotFound() {
    // AC-29
    assertThrows(MachineNotFoundException.class, () -> service.refill(UUID.randomUUID(), 500));

    assertEquals(0, countRefills());
  }

  private void spin(int[] forced) {
    when(spinGenerator.generate(anyInt(), anyInt())).thenReturn(forced);
  }

  private long floatOf(UUID id) {
    return service.read(id).machine().floatPence();
  }

  private long countPlays() {
    return jdbc.queryForObject("SELECT COUNT(*) FROM play", Long.class);
  }

  private long countRefills() {
    return jdbc.queryForObject("SELECT COUNT(*) FROM refill", Long.class);
  }

  private long countPlays(String prize) {
    return jdbc.queryForObject("SELECT COUNT(*) FROM play WHERE prize = ?", Long.class, prize);
  }

  /** Seeds a machine with free plays; configure always starts at zero (UC-01). */
  private UUID seed(long floatPence, int freePlays) {
    UUID id = UUID.randomUUID();
    tx.executeWithoutResult(s -> repository.insert(id, new Machine(CONFIG, floatPence, freePlays)));
    return id;
  }
}
