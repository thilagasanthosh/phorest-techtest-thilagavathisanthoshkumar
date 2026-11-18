package com.thilaga.fruitmachine.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.thilaga.fruitmachine.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** API-04 refill: AC-19 to AC-21, AC-30 (refill record). */
@SpringBootTest
@Import({TestcontainersConfiguration.class, ScriptedSpinsConfig.class})
class MachineRefillApiTest extends AbstractApiTest {

  @Test
  void refillRestoresFloatAndIsRecorded() throws Exception { // AC-19
    String id = createMachine(NONE_EMPTY);

    postJson(BASE + "/" + id + "/refills", "{\"amountPence\":5000}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.floatPence").value(5000))
        .andExpect(jsonPath("$.status").value("ACTIVE"));
    assertEquals(1, count("refill"));
  }

  @Test
  void refusesNonPositiveRefillAndChangesNothing() throws Exception { // AC-20, NFR-13
    String id = createMachine(NONE_EMPTY);

    postJson(BASE + "/" + id + "/refills", "{\"amountPence\":0}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.type").value("validation-error"));
    postJson(BASE + "/" + id + "/refills", "{\"amountPence\":-100}")
        .andExpect(status().isBadRequest());

    getJson(BASE + "/" + id).andExpect(jsonPath("$.floatPence").value(0));
    assertEquals(0, count("refill"));
  }

  @Test
  void refillOfUnknownMachineIsNotFound() throws Exception { // AC-21
    postJson(BASE + "/00000000-0000-0000-0000-000000000000/refills", "{\"amountPence\":100}")
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.type").value("machine-not-found"));
  }
}
