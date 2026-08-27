package pe.smartcash.cash.gmailsync.domain.services;

/**
 * Puerto hacia el bounded context Transactions -- este contexto no importa nada de
 * {@code transactions.domain.*} directamente, solo su API pública a través de este puerto
 * (implementado por un ACL, ver application.internal.outboundservices.acl). Mismo criterio
 * que {@code transactions.domain.services.UserDirectory} hacia Profile.
 */
public interface BankNotificationIngestionPort {

  void ingest(String userId, String rawText);

  boolean isTrustedSender(String fromAddress);
}
