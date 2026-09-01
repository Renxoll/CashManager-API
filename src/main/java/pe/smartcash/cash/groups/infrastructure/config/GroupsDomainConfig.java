package pe.smartcash.cash.groups.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.smartcash.cash.groups.domain.services.DebtSimplifier;

@Configuration
class GroupsDomainConfig {

  @Bean
  DebtSimplifier debtSimplifier() {
    return new DebtSimplifier();
  }
}
