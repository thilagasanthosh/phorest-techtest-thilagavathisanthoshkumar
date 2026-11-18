package com.thilaga.fruitmachine.api;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.thilaga.fruitmachine.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/** Design 5.1: Swagger UI and OpenAPI document the endpoints. */
@SpringBootTest
@Import({TestcontainersConfiguration.class, ScriptedSpinsConfig.class})
class OpenApiApiTest extends AbstractApiTest {

  @Test
  void openApiDocumentListsTheFourEndpoints() throws Exception {
    getJson("/v3/api-docs")
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("/api/v1/machines")))
        .andExpect(content().string(containsString("/api/v1/machines/{id}")))
        .andExpect(content().string(containsString("/api/v1/machines/{id}/plays")))
        .andExpect(content().string(containsString("/api/v1/machines/{id}/refills")));
  }

  @Test
  void swaggerUiIsServed() throws Exception {
    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(
                "/swagger-ui/index.html"))
        .andExpect(status().isOk());
  }
}
