package com.thilaga.fruitmachine.api;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.thilaga.fruitmachine.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** API-03 read state: AC-28, AC-29, BR-09. */
@SpringBootTest
@Import({TestcontainersConfiguration.class, ScriptedSpinsConfig.class})
class MachineReadApiTest extends AbstractApiTest {

  @Test
  void readsConfiguredMachine() throws Exception { // AC-28
    String id = createMachine(FULL_MIN_RESERVE);

    getJson(BASE + "/" + id)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(id))
        .andExpect(jsonPath("$.slotCount").value(4))
        .andExpect(jsonPath("$.floatPence").value(1000))
        .andExpect(jsonPath("$.reservePence").value(500))
        .andExpect(jsonPath("$.freePlays").value(0))
        .andExpect(jsonPath("$.totalPlays").value(0))
        .andExpect(jsonPath("$.totalPaidPence").value(0))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }

  @Test
  void unknownMachineIsNotFound() throws Exception { // AC-29
    getJson(BASE + "/00000000-0000-0000-0000-000000000000")
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.type").value("machine-not-found"));
  }

  @Test
  void noneMachineAtZeroFloatIsAlwaysActive() throws Exception { // BR-09
    String id = createMachine(NONE_EMPTY);

    getJson(BASE + "/" + id)
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.floatPence").value(0))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
  }
}
