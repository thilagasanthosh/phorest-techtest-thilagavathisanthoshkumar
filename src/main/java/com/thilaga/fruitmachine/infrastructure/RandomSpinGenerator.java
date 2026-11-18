package com.thilaga.fruitmachine.infrastructure;

import com.thilaga.fruitmachine.domain.SpinGenerator;
import java.security.SecureRandom;

/** Production {@link SpinGenerator}: every colour is equally likely in every slot (BR-03). */
public final class RandomSpinGenerator implements SpinGenerator {

  private final SecureRandom random = new SecureRandom();

  @Override
  public int[] generate(int slots, int colours) {
    int[] spin = new int[slots];
    for (int i = 0; i < slots; i++) {
      spin[i] = random.nextInt(colours);
    }
    return spin;
  }
}
