package pe.smartcash.cash.transactions.domain.policy;

/**
 * Regla de negocio: solo se procesan correos reenviados por remitentes de banca reconocidos.
 * Se nombra por la regla que encierra (qué remitentes son de confianza), no por su mecánica
 * de validación — el "cómo" (allowlist, DNS, lo que sea) vive en la implementación.
 */
public interface TrustedBankSenderPolicy {

  boolean isSatisfiedBy(String fromAddress);
}
