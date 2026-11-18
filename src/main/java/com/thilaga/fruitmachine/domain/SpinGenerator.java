package com.thilaga.fruitmachine.domain;

/** Produces one colour id per slot, for a machine with the given slot and colour counts. */
public interface SpinGenerator {

  /**
   * @param slots number of slots to fill
   * @param colours number of distinct colour ids available (0 .. colours - 1)
   * @return one colour id per slot, in slot order
   */
  int[] generate(int slots, int colours);
}
