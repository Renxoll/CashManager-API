package pe.smartcash.cash.transactions.domain.policy;

import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;

/**
 * Regla de negocio: solo se procesan correos reenviados por remitentes de banca reconocidos.
 * Se nombra por la regla que encierra (qué remitentes son de confianza), no por su mecánica
 * de validación — el "cómo" (allowlist global, dominios que el propio usuario aprobó, DNS, lo
 * que sea) vive en la implementación. Lleva {@code userId} porque la confianza ya no es
 * puramente global: un dominio puede ser confiable solo para el usuario que lo aprobó (ver
 * {@code PendingSender} / {@code UserTrustedSenderRepository}).
 */
public interface TrustedBankSenderPolicy {

  boolean isSatisfiedBy(UserId userId, String fromAddress);
}
