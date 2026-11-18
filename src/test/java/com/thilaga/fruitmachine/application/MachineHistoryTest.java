package com.thilaga.fruitmachine.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

import com.thilaga.fruitmachine.FruitmachineApplication;
import com.thilaga.fruitmachine.TestTables;
import com.thilaga.fruitmachine.TestcontainersConfiguration;
import com.thilaga.fruitmachine.domain.FloatPolicyType;
import com.thilaga.fruitmachine.domain.MachineConfig;
import com.thilaga.fruitmachine.domain.MachineNeedsRefillException;
import com.thilaga.fruitmachine.domain.MachineState;
import com.thilaga.fruitmachine.domain.MachineTotals;
import com.thilaga.fruitmachine.domain.SpinGenerator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.testcontainers.mysql.MySQLContainer;

/** AC-30 history counts (forced spins), and restart survival on the same MySQL. */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MachineHistoryTest {

  private static final MachineConfig CONFIG =
      MachineConfig.of(
          4,
          List.of("black", "white", "green", "yellow"),
          2,
          100,
          1_000,
          FloatPolicyType.MIN_RESERVE);
  private static final int[] JACKPOT = {0, 0, 0, 0};
  private static final int[] NO_WIN = {0, 1, 0, 1};

  @Autowired private MachineService service;
  @Autowired private JdbcTemplate jdbc;
  @Autowired private MySQLContainer mysql;
  @MockitoBean private SpinGenerator spinGenerator;

  @BeforeEach
  void clearTables() {
    TestTables.clearAll(jdbc);
  }

  @Test
  void threePlaysOneRefusalAndOneRefillLeaveExactlyThreePlayAndOneRefillRows() {
    // AC-30: three successful plays, one refused play, one refill.
    UUID id = service.configure(CONFIG);
    when(spinGenerator.generate(anyInt(), anyInt())).thenReturn(NO_WIN);
    service.play(id, false);
    service.play(id, false);
    when(spinGenerator.generate(anyInt(), anyInt())).thenReturn(JACKPOT);
    service.play(id, false); // pays 1,300p and leaves float 0
    assertThrows(MachineNeedsRefillException.class, () -> service.play(id, false));
    service.refill(id, 1_000);

    assertEquals(3, count("play"));
    assertEquals(1, count("refill"));
    MachineState state = service.read(id);
    assertEquals(new MachineTotals(3, 1_300, 1_000), state.totals());
    // Money conserved: float + paid out = starting float + stakes + refills.
    assertEquals(
        1_000 + 3 * 100 + 1_000, state.machine().floatPence() + state.totals().totalPaidPence());
  }

  @Test
  void jackpotHistoryRowCarriesTheBr10Fields() {
    // BR-10: nominal, paid, float after and the prize are recorded for the play.
    UUID id = service.configure(CONFIG);
    when(spinGenerator.generate(anyInt(), anyInt())).thenReturn(JACKPOT);

    service.play(id, false);

    Map<String, Object> row =
        jdbc.queryForMap(
            "SELECT prize, nominal_pence, paid_pence, float_after_pence, used_free_play"
                + " FROM play");
    assertEquals("JACKPOT", row.get("prize"));
    assertEquals(1_100L, ((Number) row.get("nominal_pence")).longValue());
    assertEquals(1_100L, ((Number) row.get("paid_pence")).longValue());
    assertEquals(0L, ((Number) row.get("float_after_pence")).longValue());
    assertFalse((Boolean) row.get("used_free_play"));
  }

  @Test
  void machineStateSurvivesARestartOfTheApplication() {
    // NFR-06, NFR-08: a second application context on the same MySQL reads what the first wrote.
    // This is a second context, not a process restart; the process check belongs to Issue 6/7.
    UUID id;
    MachineState before;
    try (ConfigurableApplicationContext first = startApplication()) {
      MachineService firstService = first.getBean(MachineService.class);
      id = firstService.configure(CONFIG);
      firstService.play(id, false); // real random spin in this context
      firstService.refill(id, 1_000);
      before = firstService.read(id);
    }

    try (ConfigurableApplicationContext second = startApplication()) {
      MachineState after = second.getBean(MachineService.class).read(id);

      assertEquals(before.machine().config(), after.machine().config());
      assertEquals(before.machine().floatPence(), after.machine().floatPence());
      assertEquals(before.machine().freePlayBalance(), after.machine().freePlayBalance());
      assertEquals(before.totals(), after.totals());
    }
    assertEquals(1, count("play"));
    assertEquals(1, count("refill"));
  }

  private ConfigurableApplicationContext startApplication() {
    return new SpringApplicationBuilder(FruitmachineApplication.class)
        .web(WebApplicationType.NONE)
        .properties(
            "spring.datasource.url=" + mysql.getJdbcUrl(),
            "spring.datasource.username=" + mysql.getUsername(),
            "spring.datasource.password=" + mysql.getPassword())
        .run();
  }

  private long count(String table) {
    return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
  }
}
