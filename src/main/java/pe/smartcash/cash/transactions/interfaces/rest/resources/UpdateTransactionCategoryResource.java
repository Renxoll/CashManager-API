package pe.smartcash.cash.transactions.interfaces.rest.resources;

import jakarta.validation.constraints.NotBlank;

public record UpdateTransactionCategoryResource(@NotBlank String categoryCode) {}
