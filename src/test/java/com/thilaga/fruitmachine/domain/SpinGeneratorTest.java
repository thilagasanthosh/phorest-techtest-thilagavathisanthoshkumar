package com.thilaga.fruitmachine.domain;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

class SpinGeneratorTest {

  @Test
  void fixedGenerator_returnsExactlyWhatWasConfigured() {
    SpinGenerator generator = new FixedSpinGenerator(1, 2, 3, 0);

    assertArrayEquals(new int[] {1, 2, 3, 0}, generator.generate(4, 4));
    // Ignores the requested slot/colour counts - it always returns the forced spin.
    assertArrayEquals(new int[] {1, 2, 3, 0}, generator.generate(10, 2));
  }
}
