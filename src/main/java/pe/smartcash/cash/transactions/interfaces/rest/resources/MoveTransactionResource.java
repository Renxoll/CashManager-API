package pe.smartcash.cash.transactions.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/** {@code categoryCode}: obligatorio si la transacción es un gasto (una categoría válida en
 * el módulo destino), ignorado si es un ingreso. */
public record MoveTransactionResource(@NotNull UUID workspaceId, String categoryCode) {}
