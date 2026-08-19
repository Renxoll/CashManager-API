package pe.smartcash.cash.transactions.domain.policy;

import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Solo compone datos de configuración (el allowlist), nada de Spring/HTTP/DNS acá: por eso
 * puede vivir en domain igual que su contrato, aunque el allowlist en sí lo arme la capa de
 * infraestructura a partir de {@code application.properties} (ver TransactionDomainConfig).
 */
public class AllowlistedBankSenderPolicy implements TrustedBankSenderPolicy {

  private final Set<String> trustedDomains;

  public AllowlistedBankSenderPolicy(Set<String> trustedDomains) {
    // Normaliza acá (no confía en que la property venga sin espacios/mayúsculas): así el
    // valor de app.inbound-email.trusted-sender-domains puede escribirse con el estilo que
    // sea más legible en el .properties.
    this.trustedDomains =
        trustedDomains.stream().map(domain -> domain.trim().toLowerCase(Locale.ROOT)).collect(Collectors.toUnmodifiableSet());
  }

  @Override
  public boolean isSatisfiedBy(String fromAddress) {
    if (fromAddress == null || fromAddress.isBlank()) {
      return false;
    }
    String domain = extractDomain(fromAddress);
    return domain != null && trustedDomains.contains(domain);
  }

  private String extractDomain(String address) {
    int at = address.lastIndexOf('@');
    if (at < 0 || at == address.length() - 1) {
      return null;
    }
    // El "From" de un correo puede venir como "Notificaciones BCP <alertas@bcp.com.pe>";
    // el dominio real está entre el último '@' y el primer delimitador de cierre que siga.
    String afterAt = address.substring(at + 1);
    int cut = afterAt.indexOf('>');
    if (cut >= 0) {
      afterAt = afterAt.substring(0, cut);
    }
    return afterAt.trim().toLowerCase(Locale.ROOT);
  }
}
