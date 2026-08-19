package pe.smartcash.cash.transactions.domain.model.events;

import java.time.Instant;
import pe.smartcash.cash.transactions.domain.model.valueobjects.TransactionId;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;

/**
 * Hecho de negocio: una notificación bancaria cruda quedó aceptada y persistida como PENDING.
 * Es el punto de desacople entre el borde HTTP (que responde 202 apenas esto ocurre) y la
 * categorización real, que depende del LLM y puede tardar o fallar — ver el listener
 * asíncrono en {@code TransactionCommandServiceImpl}.
 */
public record TransactionReceived(TransactionId transactionId, UserId userId, String rawText, Instant occurredAt) {}
