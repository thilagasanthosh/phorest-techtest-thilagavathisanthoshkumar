package com.thilaga.fruitmachine.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.thilaga.fruitmachine.TestTables;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** Shared HTTP helpers. Subclasses import TestcontainersConfiguration and ScriptedSpinsConfig. */
abstract class AbstractApiTest {

  static final String BASE = "/api/v1/machines";

  static final String FULL_MIN_RESERVE =
      """
      {"slotCount":4,"colours":["black","white","green","yellow"],"k":2,
       "playCostPence":100,"startingFloatPence":1000,"floatPolicy":"MIN_RESERVE"}""";

  static final String ONE_SLOT_JACKPOT =
      """
      {"slotCount":1,"colours":["black"],"k":1,
       "playCostPence":100,"startingFloatPence":1000,"floatPolicy":"MIN_RESERVE"}""";

  static final String NONE_EMPTY =
      """
      {"slotCount":4,"colours":["black","white","green","yellow"],"k":2,
       "playCostPence":100,"startingFloatPence":0,"floatPolicy":"NONE"}""";

  static final int[] SMALL_PRIZE = {0, 0, 1, 2};
  static final int[] JACKPOT = {0, 0, 0, 0};

  @Autowired private WebApplicationContext context;
  @Autowired protected JdbcTemplate jdbc;
  @Autowired protected ScriptedSpins spins;

  protected MockMvc mvc;

  @BeforeEach
  void resetState() {
    mvc = MockMvcBuilders.webAppContextSetup(context).build();
    TestTables.clearAll(jdbc);
    spins.reset();
  }

  protected ResultActions postJson(String url, String json) throws Exception {
    return mvc.perform(post(url).contentType(MediaType.APPLICATION_JSON).content(json));
  }

  protected ResultActions getJson(String url) throws Exception {
    return mvc.perform(get(url).accept(MediaType.APPLICATION_JSON));
  }

  /** Creates a machine and returns its id from the Location header. */
  protected String createMachine(String json) throws Exception {
    String location =
        postJson(BASE, json)
            .andExpect(
                org.springframework.test.web.servlet.result.MockMvcResultMatchers.status()
                    .isCreated())
            .andReturn()
            .getResponse()
            .getHeader("Location");
    return location.substring(location.lastIndexOf('/') + 1);
  }

  protected long count(String table) {
    return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
  }
}
