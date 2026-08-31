package pe.smartcash.cash.transactions.interfaces.rest.resources;

import jakarta.validation.constraints.NotNull;

/** Body de {@code PATCH /api/v1/transactions/{id}/internal-transfer}. */
public record SetInternalTransferResource(@NotNull Boolean internalTransfer) {}
