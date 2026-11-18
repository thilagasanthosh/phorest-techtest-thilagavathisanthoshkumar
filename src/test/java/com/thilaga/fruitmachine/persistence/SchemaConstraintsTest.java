package com.thilaga.fruitmachine.persistence;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.thilaga.fruitmachine.TestcontainersConfiguration;
import java.nio.ByteBuffer;
import java.sql.SQLException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * NFR-07: the database itself rejects invalid machine, play and refill rows. Spring wraps MySQL
 * errors as UncategorizedSQLException, so each test checks the MySQL error code instead.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class SchemaConstraintsTest {

  private static final int ER_CHECK_CONSTRAINT_VIOLATED = 3819;
  private static final int ER_NO_REFERENCED_ROW = 1452;

  private static final String COLOURS = "[\"black\",\"white\",\"green\",\"yellow\"]";
  private static final String SLOTS = "[\"black\",\"black\",\"white\",\"green\"]";

  @Autowired private JdbcTemplate jdbc;

  @BeforeEach
  void clearTables() {
    jdbc.update("DELETE FROM refill");
    jdbc.update("DELETE FROM play");
    jdbc.update("DELETE FROM machine");
  }

  @Test
  void validMachinePlayAndRefillAreAccepted() {
    UUID machine = insertMachine(1_000, 2, 4, "MIN_RESERVE");

    assertDoesNotThrow(() -> insertPlay(machine, "NONE"));
    assertDoesNotThrow(() -> insertRefill(machine, 500, 1_500));
  }

  @Test
  void negativeFloatIsRejected() {
    assertRejected(ER_CHECK_CONSTRAINT_VIOLATED, () -> insertMachine(-1, 2, 4, "MIN_RESERVE"));
  }

  @Test
  void kBelowOneIsRejected() {
    assertRejected(ER_CHECK_CONSTRAINT_VIOLATED, () -> insertMachine(1_000, 0, 4, "MIN_RESERVE"));
  }

  @Test
  void kAboveSlotCountIsRejected() {
    assertRejected(ER_CHECK_CONSTRAINT_VIOLATED, () -> insertMachine(1_000, 5, 4, "MIN_RESERVE"));
  }

  @Test
  void unknownFloatPolicyIsRejected() {
    assertRejected(ER_CHECK_CONSTRAINT_VIOLATED, () -> insertMachine(1_000, 2, 4, "SOMETIMES"));
  }

  @Test
  void unknownPrizeIsRejected() {
    UUID machine = insertMachine(1_000, 2, 4, "MIN_RESERVE");

    assertRejected(ER_CHECK_CONSTRAINT_VIOLATED, () -> insertPlay(machine, "BIG_WIN"));
  }

  @Test
  void orphanPlayIsRejected() {
    assertRejected(ER_NO_REFERENCED_ROW, () -> insertPlay(UUID.randomUUID(), "NONE"));
  }

  @Test
  void orphanRefillIsRejected() {
    assertRejected(ER_NO_REFERENCED_ROW, () -> insertRefill(UUID.randomUUID(), 500, 500));
  }

  private static void assertRejected(int mysqlErrorCode, Executable insert) {
    DataAccessException e = assertThrows(DataAccessException.class, insert);
    Throwable cause = e.getMostSpecificCause();
    assertInstanceOf(SQLException.class, cause);
    assertEquals(mysqlErrorCode, ((SQLException) cause).getErrorCode());
  }

  private UUID insertMachine(long floatPence, int k, int slots, String floatPolicy) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO machine (id, slot_count, colours, k, play_cost_pence,"
            + " starting_float_pence, float_policy, float_pence, free_plays, total_plays,"
            + " total_paid_pence, total_refilled_pence, version)"
            + " VALUES (?, ?, ?, ?, 100, ?, ?, ?, 0, 0, 0, 0, 0)",
        bytes(id),
        slots,
        COLOURS,
        k,
        floatPence,
        floatPolicy,
        floatPence);
    return id;
  }

  private void insertPlay(UUID machineId, String prize) {
    jdbc.update(
        "INSERT INTO play (id, machine_id, slots, prize, used_free_play, nominal_pence,"
            + " paid_pence, free_plays_credited, float_after_pence) VALUES (?, ?, ?, ?, false,"
            + " 0, 0, 0, 1000)",
        bytes(UUID.randomUUID()),
        bytes(machineId),
        SLOTS,
        prize);
  }

  private void insertRefill(UUID machineId, long amountPence, long floatAfterPence) {
    jdbc.update(
        "INSERT INTO refill (id, machine_id, amount_pence, float_after_pence) VALUES (?, ?, ?, ?)",
        bytes(UUID.randomUUID()),
        bytes(machineId),
        amountPence,
        floatAfterPence);
  }

  private static byte[] bytes(UUID id) {
    return ByteBuffer.allocate(16)
        .putLong(id.getMostSignificantBits())
        .putLong(id.getLeastSignificantBits())
        .array();
  }
}
