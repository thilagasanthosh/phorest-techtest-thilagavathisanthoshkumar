package com.thilaga.fruitmachine.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.http.HttpResponse;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/** SC-01, SC-02, SC-12 over real HTTP: configure, variable configuration, invalid input. */
class ConfigureSystemIT extends SystemTestBase {

  @Test
  void sc01_fourSlotMachineIsCreatedAndActive() throws Exception {
    HttpResponse<String> r = post(BASE, machine(1000, "MIN_RESERVE"));

    assertEquals(201, r.statusCode(), r.body());
    assertEquals("ACTIVE", json(r).get("status").asText());
    assertEquals(500, json(r).get("reservePence").asInt());
    assertEquals(0, json(r).get("totalPlays").asInt());
    String id = json(r).get("id").asText();
    assertEquals(1000, json(get(BASE + "/" + id)).get("floatPence").asInt());
  }

  @Test
  void sc02_tenSlotsThreeHundredColoursRunOfThreeWinsRunOfTwoDoesNot() throws Exception {
    String colours =
        IntStream.range(0, 300)
            .mapToObj(i -> "\"c" + i + "\"")
            .collect(Collectors.joining(",", "[", "]"));
    String body =
        "{\"slotCount\":10,\"colours\":"
            + colours
            + ",\"k\":3,\"playCostPence\":50,\"startingFloatPence\":1000,\"floatPolicy\":\"MIN_RESERVE\"}";

    HttpResponse<String> created = post(BASE, body);
    assertEquals(201, created.statusCode(), created.body());
    assertEquals(300, json(created).get("colours").size());
    String id = json(created).get("id").asText();

    spins.enqueue(new int[] {0, 0, 0, 1, 2, 3, 4, 5, 6, 7});
    assertEquals("SMALL_PRIZE", json(play(id, PAID)).get("prize").asText());

    spins.enqueue(new int[] {0, 0, 1, 1, 2, 3, 4, 5, 6, 7});
    assertEquals("NONE", json(play(id, PAID)).get("prize").asText());
  }

  @Test
  void sc12_kAboveSlotCountRefusedWithFaultsAndNothingCreated() throws Exception {
    String k5 =
        "{\"slotCount\":4,\"colours\":[\"black\",\"white\",\"green\",\"yellow\"],\"k\":5,"
            + "\"playCostPence\":100,\"startingFloatPence\":1000,\"floatPolicy\":\"MIN_RESERVE\"}";

    HttpResponse<String> r = post(BASE, k5);

    assertEquals(400, r.statusCode(), r.body());
    assertEquals("validation-error", json(r).get("type").asText());
    assertTrue(json(r).get("errors").size() > 0);
    assertEquals(0, count("machine"));
  }

  @Test
  void sc12_playUnknownMachineRefusedAsNotFound() throws Exception {
    HttpResponse<String> r = play(UUID.randomUUID().toString(), PAID);

    assertEquals(404, r.statusCode(), r.body());
    assertEquals("machine-not-found", json(r).get("type").asText());
  }
}
