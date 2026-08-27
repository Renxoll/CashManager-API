package pe.smartcash.cash.transactions.domain.policy;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;
import pe.smartcash.cash.transactions.domain.model.aggregates.UserTrustedSenderRepository;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;

/**
 * Solo compone datos de configuración (el allowlist global) y delega al puerto de confianza
 * por usuario: nada de Spring/HTTP/DNS acá, por eso puede vivir en domain igual que su
 * contrato, aunque el allowlist global en sí lo arme la capa de infraestructura a partir de
 * {@code application.properties} (ver TransactionDomainConfig).
 */
public class AllowlistedBankSenderPolicy implements TrustedBankSenderPolicy {

  private final Set<String> globalTrustedDomains;
  private final UserTrustedSenderRepository userTrustedSenderRepository;

  public AllowlistedBankSenderPolicy(Set<String> globalTrustedDomains, UserTrustedSenderRepository userTrustedSenderRepository) {
    // Normaliza acá (no confía en que la property venga sin espacios/mayúsculas): así el
    // valor de app.inbound-email.trusted-sender-domains puede escribirse con el estilo que
    // sea más legible en el .properties.
    this.globalTrustedDomains =
        globalTrustedDomains.stream().map(domain -> domain.trim().toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
    this.userTrustedSenderRepository = userTrustedSenderRepository;
  }

  @Override
  public boolean isSatisfiedBy(UserId userId, String fromAddress) {
    String domain = EmailDomainExtractor.extract(fromAddress);
    if (domain == null) {
      return false;
    }
    return globalTrustedDomains.contains(domain) || userTrustedSenderRepository.isTrusted(userId, domain);
  }
}
