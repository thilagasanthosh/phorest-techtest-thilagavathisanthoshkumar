package com.thilaga.fruitmachine.api;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.thilaga.fruitmachine.TestTables;
import com.thilaga.fruitmachine.TestcontainersConfiguration;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * System tests: the whole app on a random port, real HTTP, real MySQL, scripted spins (Issue 7).
 * ScriptedSpinsConfig is test-only and is imported here, never in main code.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import({TestcontainersConfiguration.class, ScriptedSpinsConfig.class})
abstract class SystemTestBase {

  static final String BASE = "/api/v1/machines";
  static final String PAID = "{\"useFreePlay\":false}";
  static final String FREE = "{\"useFreePlay\":true}";

  static final int[] NO_WIN = {0, 1, 0, 1};
  static final int[] SMALL = {0, 0, 1, 2};
  static final int[] FULL_HOUSE = {0, 1, 2, 3};
  static final int[] JACKPOT = {2, 2, 2, 2};

  private static final HttpClient HTTP = HttpClient.newHttpClient();
  private static final ObjectMapper JSON = new ObjectMapper();

  @LocalServerPort private int port;
  @Autowired protected ScriptedSpins spins;
  @Autowired protected JdbcTemplate jdbc;

  @BeforeEach
  void resetState() {
    TestTables.clearAll(jdbc);
    spins.reset();
  }

  /** 4 slots, k = 2, cost 100p, the given float and policy. */
  static String machine(int floatPence, String policy) {
    return "{\"slotCount\":4,\"colours\":[\"black\",\"white\",\"green\",\"yellow\"],\"k\":2,"
        + "\"playCostPence\":100,\"startingFloatPence\":"
        + floatPence
        + ",\"floatPolicy\":\""
        + policy
        + "\"}";
  }

  protected HttpResponse<String> post(String path, String body) throws Exception {
    return HTTP.send(
        HttpRequest.newBuilder(uri(path))
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(body))
            .build(),
        BodyHandlers.ofString());
  }

  protected HttpResponse<String> get(String path) throws Exception {
    return HTTP.send(HttpRequest.newBuilder(uri(path)).GET().build(), BodyHandlers.ofString());
  }

  protected HttpResponse<String> play(String id, String body) throws Exception {
    return post(BASE + "/" + id + "/plays", body);
  }

  protected String createMachine(String body) throws Exception {
    HttpResponse<String> r = post(BASE, body);
    assertEquals(201, r.statusCode(), r.body());
    return json(r).get("id").asText();
  }

  protected long count(String table) {
    return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
  }

  static JsonNode json(HttpResponse<String> r) {
    return JSON.readTree(r.body());
  }

  private URI uri(String path) {
    return URI.create("http://localhost:" + port + path);
  }
}
