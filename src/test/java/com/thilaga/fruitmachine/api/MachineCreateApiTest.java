package com.thilaga.fruitmachine.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.thilaga.fruitmachine.TestcontainersConfiguration;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** API-01 configure: AC-01 to AC-05, NFR-02. */
@SpringBootTest
@Import({TestcontainersConfiguration.class, ScriptedSpinsConfig.class})
class MachineCreateApiTest extends AbstractApiTest {

  @Test
  void createsMachineWithExactSettingsAndLocation() throws Exception { // AC-01
    postJson(BASE, FULL_MIN_RESERVE)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.slotCount").value(4))
        .andExpect(jsonPath("$.colours[0]").value("black"))
        .andExpect(jsonPath("$.k").value(2))
        .andExpect(jsonPath("$.floatPence").value(1000))
        .andExpect(jsonPath("$.reservePence").value(500))
        .andExpect(jsonPath("$.status").value("ACTIVE"))
        .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith(BASE + "/")));
  }

  @Test
  void defaultsApplyWhenOnlyCostAndFloatGiven() throws Exception { // AC-02
    postJson(BASE, "{\"playCostPence\":100,\"startingFloatPence\":1000}")
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.slotCount").value(4))
        .andExpect(jsonPath("$.colours.length()").value(4))
        .andExpect(jsonPath("$.k").value(2))
        .andExpect(jsonPath("$.floatPolicy").value("MIN_RESERVE"));
  }

  @Test
  void refusesInvalidSettingsAndCreatesNothing() throws Exception { // AC-03, NFR-13
    String[] invalid = {
      "{\"slotCount\":4,\"colours\":[\"black\",\"white\"],\"k\":0,\"playCostPence\":100,\"startingFloatPence\":1000}",
      "{\"slotCount\":4,\"colours\":[\"black\",\"white\"],\"k\":5,\"playCostPence\":100,\"startingFloatPence\":1000}",
      "{\"slotCount\":4,\"colours\":[\"black\",\"black\"],\"k\":2,\"playCostPence\":100,\"startingFloatPence\":1000}",
      "{\"playCostPence\":0,\"startingFloatPence\":1000}",
      "{\"playCostPence\":100,\"startingFloatPence\":-1}",
    };
    for (String body : invalid) {
      postJson(BASE, body)
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.type").value("validation-error"))
          .andExpect(jsonPath("$.errors.length()").value(org.hamcrest.Matchers.greaterThan(0)));
    }
    assertEquals(0, count("machine"));
  }

  @Test
  void refusesStartingFloatBelowReserveAndAcceptsExactlyReserve() throws Exception { // AC-04
    postJson(BASE, FULL_MIN_RESERVE.replace("1000", "499"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.type").value("validation-error"));
    postJson(BASE, FULL_MIN_RESERVE.replace("1000", "500"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.floatPence").value(500));
  }

  @Test
  void acceptsLargeVariableConfiguration() throws Exception { // AC-05
    String colours =
        IntStream.range(0, 300)
            .mapToObj(i -> "\"C" + i + "\"")
            .collect(Collectors.joining(",", "[", "]"));
    String body =
        "{\"slotCount\":10,\"colours\":"
            + colours
            + ",\"k\":3,\"playCostPence\":50,\"startingFloatPence\":1000}";
    postJson(BASE, body)
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.slotCount").value(10))
        .andExpect(jsonPath("$.colours.length()").value(300))
        .andExpect(jsonPath("$.k").value(3));
  }

  @Test
  void refusesSlotCountAboveNfr02Limit() throws Exception { // NFR-02
    postJson(BASE, "{\"slotCount\":10001,\"playCostPence\":100,\"startingFloatPence\":1000}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.type").value("validation-error"));
  }
}
