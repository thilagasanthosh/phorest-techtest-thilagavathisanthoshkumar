package com.thilaga.fruitmachine.infrastructure;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.thilaga.fruitmachine.domain.SpinGenerator;
import org.junit.jupiter.api.Test;

class RandomSpinGeneratorTest {

  @Test
  void randomGenerator_isFairOverLargeSample() {
    int slots = 4;
    int colours = 4;
    int samples = 100_000;
    double tolerance = 0.03; // AC-34: roughly equal frequency, not exact.

    int[][] counts = new int[slots][colours];
    SpinGenerator generator = new RandomSpinGenerator();
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
