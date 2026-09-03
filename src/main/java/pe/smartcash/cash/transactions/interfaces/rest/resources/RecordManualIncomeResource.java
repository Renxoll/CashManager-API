package pe.smartcash.cash.transactions.interfaces.rest.resources;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

/** {@code workspaceId} opcional: null = módulo "General" del usuario. */
public record RecordManualIncomeResource(
    @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
    @NotBlank String currency,
    @NotBlank String source,
    UUID workspaceId) {}
