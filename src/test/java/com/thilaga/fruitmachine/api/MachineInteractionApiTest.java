package com.thilaga.fruitmachine.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.thilaga.fruitmachine.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** API-02 play, plus the refused-play and refill flow: BR-02, BR-07, AC-07 to AC-36 (API part). */
@SpringBootTest
@Import({TestcontainersConfiguration.class, ScriptedSpinsConfig.class})
class MachineInteractionApiTest extends AbstractApiTest {

  private static final String PLAY = "/plays";
  private static final String PAID = "{\"useFreePlay\":false}";
  private static final String FREE = "{\"useFreePlay\":true}";

  @Test
  void paidPlayReturnsEveryFieldOfTheResult() throws Exception { // AC-15
    String id = createMachine(FULL_MIN_RESERVE);
    spins.enqueue(SMALL_PRIZE);

    postJson(BASE + "/" + id + PLAY, PAID)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.playId").isNotEmpty())
        .andExpect(jsonPath("$.slots[0]").value("black"))
        .andExpect(jsonPath("$.slots[2]").value("white"))
        .andExpect(jsonPath("$.prize").value("SMALL_PRIZE"))
        .andExpect(jsonPath("$.nominalPayout").value(500))
        .andExpect(jsonPath("$.paidOut").value(500))
        .andExpect(jsonPath("$.freePlaysCredited").value(0))
        .andExpect(jsonPath("$.freePlayBalance").value(0))
        .andExpect(jsonPath("$.floatAfter").value(600))
        .andExpect(jsonPath("$.paidWithFreePlay").value(false))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
    assertEquals(1, count("play"));
  }

  @Test
  void playAtExactlyTheReserveIsAccepted() throws Exception { // AC-17
    String id = createMachine(FULL_MIN_RESERVE.replace("1000", "500"));
    spins.enqueue(SMALL_PRIZE);

    postJson(BASE + "/" + id + PLAY, PAID)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paidOut").value(500))
        .andExpect(jsonPath("$.freePlaysCredited").value(0))
        .andExpect(jsonPath("$.floatAfter").value(100));
  }

  @Test
  void jackpotDrainsMachineAndRefusedPlayChangesNothing() throws Exception { // AC-07, AC-16, AC-18
    String id = createMachine(ONE_SLOT_JACKPOT);
    spins.enqueue(new int[] {0});

    postJson(BASE + "/" + id + PLAY, PAID)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.prize").value("JACKPOT"))
        .andExpect(jsonPath("$.paidOut").value(1100))
        .andExpect(jsonPath("$.floatAfter").value(0))
        .andExpect(jsonPath("$.status").value("NEEDS_REFILL"));

    postJson(BASE + "/" + id + PLAY, PAID)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.type").value("machine-needs-refill"))
        .andExpect(jsonPath("$.floatPence").value(0))
        .andExpect(jsonPath("$.reservePence").value(500))
        .andExpect(jsonPath("$.machineStatus").value("NEEDS_REFILL"));

    assertEquals(1, spins.calls(), "a refused play must not spin");
    assertEquals(1, count("play"));
    getJson(BASE + "/" + id)
        .andExpect(jsonPath("$.floatPence").value(0))
        .andExpect(jsonPath("$.totalPlays").value(1))
        .andExpect(jsonPath("$.status").value("NEEDS_REFILL"));
  }

  @Test
  void refilledMachineAcceptsPlaysAgain() throws Exception { // AC-18
    String id = createMachine(ONE_SLOT_JACKPOT);
    spins.enqueue(new int[] {0}, new int[] {0});
    postJson(BASE + "/" + id + PLAY, PAID).andExpect(status().isOk());

    postJson(BASE + "/" + id + "/refills", "{\"amountPence\":1000}")
        .andExpect(jsonPath("$.status").value("ACTIVE"));
    getJson(BASE + "/" + id)
        .andExpect(jsonPath("$.floatPence").value(1000))
        .andExpect(jsonPath("$.status").value("ACTIVE"));

    postJson(BASE + "/" + id + PLAY, PAID).andExpect(status().isOk());
  }

  @Test
  void configurePlayDrainRefusedRefillPlayInOneFlow() throws Exception { // AC-18, AC-19
    String id = createMachine(ONE_SLOT_JACKPOT);
    spins.enqueue(new int[] {0}, new int[] {0});

    postJson(BASE + "/" + id + PLAY, PAID)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.prize").value("JACKPOT"))
        .andExpect(jsonPath("$.floatAfter").value(0))
        .andExpect(jsonPath("$.status").value("NEEDS_REFILL"));

    postJson(BASE + "/" + id + PLAY, PAID)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.type").value("machine-needs-refill"));
    assertEquals(1, spins.calls(), "a refused play must not spin");

    postJson(BASE + "/" + id + "/refills", "{\"amountPence\":5000}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.floatPence").value(5000))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
    assertEquals(1, count("refill"));

    // One-slot machine: every spin is a jackpot, so the play pays the 5,000p float plus the 100p
    // stake.
    postJson(BASE + "/" + id + PLAY, PAID)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paidOut").value(5100));
    assertEquals(2, count("play"));
  }

  @Test
  void freePlayCreditIsEarnedAndSpentOnNoneMachine() throws Exception { // AC-22, AC-25
    String id = createMachine(NONE_EMPTY);
    spins.enqueue(SMALL_PRIZE, SMALL_PRIZE);

    // Paid play on an empty NONE machine: stake 100p, prize 500p due, 100p paid, shortfall 400p ->
    // 4 credits.
    postJson(BASE + "/" + id + PLAY, PAID)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paidOut").value(100))
        .andExpect(jsonPath("$.freePlaysCredited").value(4))
        .andExpect(jsonPath("$.freePlayBalance").value(4))
        .andExpect(jsonPath("$.floatAfter").value(0));

    // Free play: no stake, 500p due on an empty float -> 5 more credits; balance 4 - 1 + 5 = 8.
    postJson(BASE + "/" + id + PLAY, FREE)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.paidWithFreePlay").value(true))
        .andExpect(jsonPath("$.paidOut").value(0))
        .andExpect(jsonPath("$.freePlaysCredited").value(5))
        .andExpect(jsonPath("$.freePlayBalance").value(8));
  }

  @Test
  void freePlayWithoutCreditIsRefusedWithoutSpinning() throws Exception { // AC-26
    String id = createMachine(FULL_MIN_RESERVE);

    postJson(BASE + "/" + id + PLAY, FREE)
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.type").value("no-free-plays"));
    assertEquals(0, spins.calls());
  }

  @Test
  void malformedPlayRequestsAreRefusedAndChangeNothing() throws Exception { // AC-36
    String id = createMachine(FULL_MIN_RESERVE);

    for (String body : new String[] {"{\"useFreePlay\":\"yes\"}", "{", "{}"}) {
      postJson(BASE + "/" + id + PLAY, body)
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.type").value("validation-error"))
          .andExpect(jsonPath("$.errors.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }
    assertEquals(0, count("play"));
    assertEquals(0, spins.calls());
  }

  @Test
  void playOfUnknownMachineIsNotFound() throws Exception { // AC-29
    postJson(BASE + "/00000000-0000-0000-0000-000000000000" + PLAY, PAID)
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.type").value("machine-not-found"));
  }
}
