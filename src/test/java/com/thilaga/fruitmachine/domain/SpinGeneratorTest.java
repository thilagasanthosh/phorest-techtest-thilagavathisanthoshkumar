package com.thilaga.fruitmachine.domain;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SpinGeneratorTest {

  @Test
  void fixedGenerator_returnsExactlyWhatWasConfigured() {
    SpinGenerator generator = new FixedSpinGenerator(1, 2, 3, 0);

    assertArrayEquals(new int[] {1, 2, 3, 0}, generator.generate(4, 4));
    // Ignores the requested slot/colour counts - it always returns the forced spin.
    assertArrayEquals(new int[] {1, 2, 3, 0}, generator.generate(10, 2));
  }

  @Test
  void secureRandomGenerator_isFairOverLargeSample() {
    int slots = 4;
    int colours = 4;
    int samples = 100_000;
    double tolerance = 0.03; // AC-34: roughly equal frequency, not exact.

    int[][] counts = new int[slots][colours];
    SpinGenerator generator = new SecureRandomSpinGenerator();
    for (int i = 0; i < samples; i++) {
      int[] spin = generator.generate(slots, colours);
      assertTrue(spin.length == slots, "one colour per slot");
      for (int slot = 0; slot < slots; slot++) {
        counts[slot][spin[slot]]++;
      }
    }

    double expected = 1.0 / colours;
    for (int slot = 0; slot < slots; slot++) {
      for (int colour = 0; colour < colours; colour++) {
        double frequency = (double) counts[slot][colour] / samples;
        assertTrue(
            Math.abs(frequency - expected) < tolerance,
            "slot " + slot + " colour " + colour + " frequency " + frequency);
      }
    }
  }
}
