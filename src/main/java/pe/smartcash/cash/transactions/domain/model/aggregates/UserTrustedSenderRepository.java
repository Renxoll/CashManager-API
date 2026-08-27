package pe.smartcash.cash.transactions.domain.model.aggregates;

import java.time.Instant;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;

/**
 * Dominios de remitente que un usuario aprobó explícitamente (ver {@link PendingSender#approve}),
 * encima del allowlist global curado en {@code app.inbound-email.trusted-sender-domains}. No es
 * un aggregate con invariantes propias -- es una tabla de hechos simple (¿este usuario confía en
 * este dominio, sí o no?) -- por eso no hay una clase de dominio {@code UserTrustedSender}, solo
 * este puerto.
 */
public interface UserTrustedSenderRepository {

  boolean isTrusted(UserId userId, String domain);

  void trust(UserId userId, String domain, Instant trustedAt);
}
