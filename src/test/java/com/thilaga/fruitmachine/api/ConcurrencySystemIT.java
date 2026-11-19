package com.thilaga.fruitmachine.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/** SC-13 and AC-32 / AC-33 over real HTTP: simultaneous plays on one machine. */
class ConcurrencySystemIT extends SystemTestBase {

  @Test
  void sc13_twoSimultaneousPlaysOneAfterTheOther() throws Exception {
    String id = createMachine(machine(500, "MIN_RESERVE"));
    spins.enqueue(SMALL, NO_WIN); // first to lock gets SMALL

    List<HttpResponse<String>> results = runTogether(2, () -> play(id, PAID));

    List<Integer> statuses = results.stream().map(HttpResponse::statusCode).sorted().toList();
    assertEquals(List.of(200, 409), statuses);
    HttpResponse<String> winner =
        results.stream().filter(r -> r.statusCode() == 200).findFirst().orElseThrow();
    assertEquals("SMALL_PRIZE", json(winner).get("prize").asText());
    HttpResponse<String> refused =
        results.stream().filter(r -> r.statusCode() == 409).findFirst().orElseThrow();
    assertEquals("machine-needs-refill", json(refused).get("type").asText());
    var state = json(get(BASE + "/" + id));
    assertEquals(100, state.get("floatPence").asInt());
    assertEquals("NEEDS_REFILL", state.get("status").asText());
    assertEquals(1, count("play"));
  }

  @Test
  void ac33_200ParallelPlaysKeepMoneyConsistent() throws Exception {
    String id = createMachine(machine(500, "MIN_RESERVE"));
    spins.enqueue(Stream.generate(() -> NO_WIN).limit(200).toArray(int[][]::new));

    List<HttpResponse<String>> results = runTogether(200, () -> play(id, PAID));

    long ok = results.stream().filter(r -> r.statusCode() == 200).count();
    long busy = results.stream().filter(r -> r.statusCode() == 503).count();
    assertEquals(200, ok + busy, "every play is either accepted or reported busy");
    // Each accepted no-win play adds one stake of 100p; busy plays add nothing.
    var state = json(get(BASE + "/" + id));
    assertEquals(500 + 100 * ok, state.get("floatPence").asLong());
    assertEquals(ok, count("play"));
  }

  /** Starts all tasks at once behind a latch so they truly overlap. */
  private static List<HttpResponse<String>> runTogether(int n, Callable<HttpResponse<String>> task)
      throws Exception {
    ExecutorService pool = Executors.newFixedThreadPool(n);
    CountDownLatch go = new CountDownLatch(1);
    try {
      List<Future<HttpResponse<String>>> futures = new ArrayList<>();
      for (int i = 0; i < n; i++) {
        futures.add(
            pool.submit(
                () -> {
                  go.await();
                  return task.call();
                }));
      }
      go.countDown();
      List<HttpResponse<String>> out = new ArrayList<>();
      for (Future<HttpResponse<String>> f : futures) {
        out.add(f.get());
      }
      return out;
    } finally {
      pool.shutdownNow();
    }
  }
}
