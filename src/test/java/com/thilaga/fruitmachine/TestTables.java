package com.thilaga.fruitmachine;

import org.springframework.jdbc.core.JdbcTemplate;

/** Shared {@code @BeforeEach} helper: clears refill, play and machine, in FK order. */
public final class TestTables {

  private TestTables() {}

  public static void clearAll(JdbcTemplate jdbc) {
    jdbc.update("DELETE FROM refill");
    jdbc.update("DELETE FROM play");
    jdbc.update("DELETE FROM machine");
  }
}
