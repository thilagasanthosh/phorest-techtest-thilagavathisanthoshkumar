package com.thilaga.fruitmachine.domain;

/**
 * Test double: always returns the colour ids it was built with, ignoring the requested slot and
 * colour counts. Lets a test force any spin result (NFR-09). Never wired into a production Spring
 * profile.
 */
public final class FixedSpinGenerator implements SpinGenerator {

  private final int[] spin;

  public FixedSpinGenerator(int... spin) {
    this.spin = spin.clone();
  }

  @Override
  public int[] generate(int slots, int colours) {
    return spin.clone();
  }
}
