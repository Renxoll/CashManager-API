package pe.smartcash.cash.transactions.domain.model.commands;

import java.math.BigDecimal;
import java.util.UUID;
import pe.smartcash.cash.transactions.domain.model.valueobjects.UserId;

/**
 * Gasto cargado a mano por el usuario -- para lo que no llega por correo (p. ej. Yape solo
 * manda constancia por transferencias sobre cierto monto). {@code merchant}: en qué / a
 * quién se gastó, se guarda tal cual. {@code categoryCode}: código del catálogo cerrado si
 * el módulo destino es el "General", o código de una categoría del módulo custom si no.
 * {@code workspaceId} null = módulo "General" del usuario.
 */
public record RecordManualExpenseCommand(
    UserId userId, BigDecimal amount, String currency, String merchant, String categoryCode, UUID workspaceId) {}
