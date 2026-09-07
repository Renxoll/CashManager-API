package pe.smartcash.cash.transactions.interfaces.rest.resources;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/** {@code workspaceId} opcional: null = módulo "General" del usuario. {@code categoryCode}
 * obligatorio (a diferencia del ingreso): un gasto siempre lleva categoría. */
public record RecordManualExpenseResource(
    @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
    @NotBlank String currency,
    @NotBlank String merchant,
    @NotBlank String categoryCode,
    UUID workspaceId) {}
