package pe.smartcash.cash.transactions.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

public record CreateTransactionResource(@NotBlank String userId, @NotBlank String rawText) {}
