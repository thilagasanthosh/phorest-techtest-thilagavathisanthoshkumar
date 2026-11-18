package com.thilaga.fruitmachine.api;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.thilaga.fruitmachine.domain.MachineBusyException;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Error mapping for paths no real endpoint reaches: busy (503) and internal errors (500). */
class ApiExceptionHandlerTest {

  @RestController
  @RequestMapping("/stub")
  static class Stub {
    @GetMapping("/busy")
    String busy() {
      throw new MachineBusyException(new RuntimeException("lock wait timeout"));
    }

    @GetMapping("/boom")
    String boom() {
      throw new IllegalStateException("secret internal detail");
    }
  }

  private final MockMvc mvc =
      MockMvcBuilders.standaloneSetup(new Stub())
          .setControllerAdvice(new ApiExceptionHandler())
          .build();

  @Test
  void busyMachineIs503WithRetryAfterAndProblemType() throws Exception { // UC-12, NFR-12
    mvc.perform(get("/stub/busy"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(header().string("Retry-After", "1"))
        .andExpect(header().string("Content-Type", containsString("application/problem+json")))
        .andExpect(jsonPath("$.type").value("machine-busy"));
  }

  @Test
  void unexpectedErrorIs500WithoutStackTrace() throws Exception { // design 5.4
    mvc.perform(get("/stub/boom"))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.type").value("internal-error"))
        .andExpect(jsonPath("$.detail", not(containsString("secret"))))
        .andExpect(jsonPath("$.detail", not(containsString("IllegalStateException"))));
  }
}
