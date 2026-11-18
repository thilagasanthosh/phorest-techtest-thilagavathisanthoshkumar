package com.thilaga.fruitmachine.api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Test-only: replaces the random spin source so every prize can be forced (NFR-09). */
@TestConfiguration(proxyBeanMethods = false)
class ScriptedSpinsConfig {

  @Bean
  @Primary
  ScriptedSpins scriptedSpins() {
    return new ScriptedSpins();
  }
}
