package com.thilaga.fruitmachine.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class MachineConfigTest {

  @Test
  void defaultsApply_whenOnlyCostAndFloatGiven() {
    MachineConfig config = MachineConfig.withDefaults(100, 1_000);

    assertEquals(4, config.slots());
    assertEquals(List.of("black", "white", "green", "yellow"), config.colours());
    assertEquals(2, config.k());
    assertEquals(FloatPolicyType.MIN_RESERVE, config.floatPolicyType());
  }

  @Test
  void rejectsKZero() {
    InvalidMachineConfigException ex =
        assertThrows(
            InvalidMachineConfigException.class,
            () ->
                MachineConfig.of(
                    4, MachineConfig.DEFAULT_COLOURS, 0, 100, 1_000, FloatPolicyType.MIN_RESERVE));

    assertTrue(ex.errors().stream().anyMatch(m -> m.contains("k must be between")));
  }

  @Test
  void rejectsKGreaterThanSlots() {
    InvalidMachineConfigException ex =
        assertThrows(
            InvalidMachineConfigException.class,
            () ->
                MachineConfig.of(
                    4, MachineConfig.DEFAULT_COLOURS, 5, 100, 1_000, FloatPolicyType.MIN_RESERVE));

    assertTrue(ex.errors().stream().anyMatch(m -> m.contains("k must be between")));
  }

  @Test
  void rejectsDuplicateColours() {
    InvalidMachineConfigException ex =
        assertThrows(
            InvalidMachineConfigException.class,
            () ->
                MachineConfig.of(
                    4,
                    List.of("black", "black", "green", "yellow"),
                    2,
                    100,
                    1_000,
                    FloatPolicyType.MIN_RESERVE));

    assertTrue(ex.errors().stream().anyMatch(m -> m.contains("duplicates")));
  }

  @Test
  void rejectsNonPositivePlayCost() {
    InvalidMachineConfigException ex =
        assertThrows(
            InvalidMachineConfigException.class,
            () ->
                MachineConfig.of(
                    4, MachineConfig.DEFAULT_COLOURS, 2, 0, 1_000, FloatPolicyType.MIN_RESERVE));

    assertTrue(ex.errors().stream().anyMatch(m -> m.contains("playCostPence")));
  }

  @Test
  void rejectsNegativeFloat() {
    InvalidMachineConfigException ex =
        assertThrows(
            InvalidMachineConfigException.class,
            () ->
                MachineConfig.of(
                    4, MachineConfig.DEFAULT_COLOURS, 2, 100, -1, FloatPolicyType.MIN_RESERVE));

    assertTrue(
        ex.errors().stream().anyMatch(m -> m.contains("startingFloatPence must not be negative")));
  }

  @Test
  void rejectsStartingFloatBelowReserve() {
    assertThrows(
        InvalidMachineConfigException.class,
        () ->
            MachineConfig.of(
                4, MachineConfig.DEFAULT_COLOURS, 2, 100, 499, FloatPolicyType.MIN_RESERVE));
  }

  @Test
  void acceptsStartingFloatAtExactlyTheReserve() {
    MachineConfig config =
        MachineConfig.of(
            4, MachineConfig.DEFAULT_COLOURS, 2, 100, 500, FloatPolicyType.MIN_RESERVE);

    assertEquals(500, config.startingFloatPence());
  }

  @Test
  void acceptsLargeExplicitConfig() {
    List<String> colours =
        IntStream.range(0, 300).mapToObj(i -> "C" + i).collect(Collectors.toList());

    MachineConfig config = MachineConfig.of(10, colours, 3, 50, 1_000, FloatPolicyType.MIN_RESERVE);

    assertEquals(10, config.slots());
    assertEquals(300, config.colours().size());
    assertEquals(3, config.k());
    assertEquals(50, config.playCostPence());
    assertEquals(1_000, config.startingFloatPence());
  }
}
