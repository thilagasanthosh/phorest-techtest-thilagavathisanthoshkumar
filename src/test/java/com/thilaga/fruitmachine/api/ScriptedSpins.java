package com.thilaga.fruitmachine.api;

import com.thilaga.fruitmachine.domain.SpinGenerator;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;

/** Test-only spin source: returns queued spins in order and counts calls (NFR-09). */
final class ScriptedSpins implements SpinGenerator {

  private final Deque<int[]> queue = new ArrayDeque<>();
  private int calls;

  synchronized void enqueue(int[]... spins) {
    queue.addAll(Arrays.asList(spins));
  }

  synchronized void reset() {
    queue.clear();
    calls = 0;
  }

  int calls() {
    return calls;
  }

  @Override
  public synchronized int[] generate(int slots, int colours) {
    calls++;
    int[] next = queue.poll();
    if (next == null) {
      throw new IllegalStateException("no scripted spin queued");
    }
    return next;
  }
}
