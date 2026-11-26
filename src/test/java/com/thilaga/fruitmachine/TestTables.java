package com.thilaga.fruitmachine;

import org.springframework.jdbc.core.JdbcTemplate;

/** Shared {@code @BeforeEach} helper: clears child tables then machine, in FK order. */
public final class TestTables {

  private TestTables() {}

  public static void clearAll(JdbcTemplate jdbc) {
    jdbc.update("DELETE FROM play_idempotency");
    jdbc.update("DELETE FROM refill");
    jdbc.update("DELETE FROM play");
    jdbc.update("DELETE FROM machine");
  }
}
