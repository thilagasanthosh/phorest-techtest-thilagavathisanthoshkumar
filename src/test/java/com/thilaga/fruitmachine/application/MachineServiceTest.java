package com.thilaga.fruitmachine.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.thilaga.fruitmachine.TestcontainersConfiguration;
import com.thilaga.fruitmachine.domain.FloatPolicyType;
import com.thilaga.fruitmachine.domain.MachineConfig;
import com.thilaga.fruitmachine.domain.MachineNotFoundException;
import com.thilaga.fruitmachine.domain.MachineState;
import com.thilaga.fruitmachine.domain.MachineStatus;
import com.thilaga.fruitmachine.domain.MachineTotals;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Integration tests on real MySQL. Not @Transactional, so MachineService's own transactions run.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MachineServiceTest {

  private static final List<String> COLOURS = List.of("black", "white", "green", "yellow");

  @Autowired private MachineService service;
  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void clearTables() {
    jdbc.update("DELETE FROM refill");
    jdbc.update("DELETE FROM play");
    jdbc.update("DELETE FROM machine");
  }

  @Test
  void configureCreatesAnActiveMachineWithAFreshId() {
    // AC-01: 4 slots, colours black/white/green/yellow, k = 2, 100p play, float 1,000p, MIN_RESERVE
    MachineConfig config = MachineConfig.of(4, COLOURS, 2, 100, 1_000, FloatPolicyType.MIN_RESERVE);

    UUID first = service.configure(config);
    UUID second = service.configure(config);
    MachineState state = service.read(first);

    assertNotEquals(first, second);
    assertEquals(config, state.machine().config());
    assertEquals(1_000, state.machine().floatPence());
    assertEquals(500, state.machine().reservePence());
    assertEquals(MachineStatus.ACTIVE, state.machine().status());
  }

  @Test
  void configureStartsWithNoFreePlaysOrHistory() {
    // UC-01 postcondition, and AC-28 totals
    UUID id =
        service.configure(MachineConfig.of(4, COLOURS, 2, 100, 1_000, FloatPolicyType.MIN_RESERVE));

    MachineState state = service.read(id);

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
}
