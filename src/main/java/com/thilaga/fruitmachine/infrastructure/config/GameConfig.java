package com.thilaga.fruitmachine.infrastructure.config;

import com.thilaga.fruitmachine.domain.SpinGenerator;
import com.thilaga.fruitmachine.infrastructure.RandomSpinGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
class GameConfig {

  @Bean
  SpinGenerator spinGenerator() {
    return new RandomSpinGenerator();
  }
}
