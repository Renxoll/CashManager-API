package pe.smartcash.cash.transactions.infrastructure.config;

import java.time.Clock;
import java.util.Set;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pe.smartcash.cash.transactions.domain.model.aggregates.UserTrustedSenderRepository;
import pe.smartcash.cash.transactions.domain.policy.AllowlistedBankSenderPolicy;
import pe.smartcash.cash.transactions.domain.policy.CategorizedExpenseNotificationPolicy;
import pe.smartcash.cash.transactions.domain.policy.DefaultCategorizedExpenseNotificationPolicy;
import pe.smartcash.cash.transactions.domain.policy.TrustedBankSenderPolicy;
import pe.smartcash.cash.transactions.domain.services.CategoryCatalog;
import pe.smartcash.cash.transactions.domain.services.TransactionNotifier;
import pe.smartcash.cash.transactions.domain.services.UserDirectory;
import pe.smartcash.cash.transactions.domain.service.BankNotificationHeuristicParser;

/**
 * El dominio no tiene ni una sola anotación de Spring; esta clase es la única que sabe que
 * Spring existe y arma los objetos de dominio como beans a partir de sus puertos.
 */
@Configuration
class TransactionDomainConfig {

  @Bean
  BankNotificationHeuristicParser bankNotificationHeuristicParser() {
    return new BankNotificationHeuristicParser();
  }

  @Bean
  CategorizedExpenseNotificationPolicy categorizedExpenseNotificationPolicy(
      UserDirectory userDirectory, TransactionNotifier notifier, CategoryCatalog categoryCatalog) {
    return new DefaultCategorizedExpenseNotificationPolicy(userDirectory, notifier, categoryCatalog);
  }

  @Bean
  TrustedBankSenderPolicy trustedBankSenderPolicy(
      @Value("#{'${app.inbound-email.trusted-sender-domains}'.split(',')}") Set<String> trustedDomains,
      UserTrustedSenderRepository userTrustedSenderRepository) {
    return new AllowlistedBankSenderPolicy(trustedDomains, userTrustedSenderRepository);
  }

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }
}
