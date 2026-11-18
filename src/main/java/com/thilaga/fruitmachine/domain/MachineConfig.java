package com.thilaga.fruitmachine.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Immutable machine settings. Colour ids used by {@link SpinGenerator}, {@link SpinProfile} and the
 * {@link PrizeRule}s are positions into {@link #colours()}; nothing in the domain maps an id back
 * to a name, that happens at the persistence and API boundaries.
 */
public record MachineConfig(
    int slots,
    List<String> colours,
    int k,
    long playCostPence,
    long startingFloatPence,
    FloatPolicyType floatPolicyType) {

  public static final List<String> DEFAULT_COLOURS = List.of("black", "white", "green", "yellow");
  public static final int DEFAULT_SLOTS = 4;
  public static final int DEFAULT_K = 2;
  public static final FloatPolicyType DEFAULT_POLICY = FloatPolicyType.MIN_RESERVE;

  public MachineConfig {
    List<String> errors = new ArrayList<>();

    if (slots < 1) {
      errors.add("slots must be at least 1");
    }
    if (colours == null || colours.isEmpty()) {
      errors.add("colours must not be empty");
    } else if (colours.stream().anyMatch(Objects::isNull)) {
      errors.add("colours must not contain null");
    } else if (Set.copyOf(colours).size() != colours.size()) {
      errors.add("colours must not contain duplicates");
    }
    if (k < 1 || k > slots) {
      errors.add("k must be between 1 and slots");
    }
    if (playCostPence <= 0) {
      errors.add("playCostPence must be greater than 0");
    }
    if (startingFloatPence < 0) {
      errors.add("startingFloatPence must not be negative");
    }
    if (floatPolicyType == null) {
      errors.add("floatPolicyType must not be null");
    } else if (playCostPence > 0 && startingFloatPence >= 0) {
      String policyError =
          floatPolicyType.toPolicy().validateStartingFloat(startingFloatPence, playCostPence);
      if (policyError != null) {
        errors.add(policyError);
      }
    }

    if (!errors.isEmpty()) {
      throw new InvalidMachineConfigException(errors);
    }

    colours = List.copyOf(colours);
  }

  public static MachineConfig of(
      int slots,
      List<String> colours,
      int k,
      long playCostPence,
      long startingFloatPence,
      FloatPolicyType floatPolicyType) {
    return new MachineConfig(slots, colours, k, playCostPence, startingFloatPence, floatPolicyType);
  }

  public static MachineConfig withDefaults(long playCostPence, long startingFloatPence) {
    return new MachineConfig(
        DEFAULT_SLOTS,
        DEFAULT_COLOURS,
        DEFAULT_K,
        playCostPence,
        startingFloatPence,
        DEFAULT_POLICY);
  }
}
