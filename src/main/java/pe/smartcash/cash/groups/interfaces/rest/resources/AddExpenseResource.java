package pe.smartcash.cash.groups.interfaces.rest.resources;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record AddExpenseResource(
    @NotBlank String description,
    @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
    @NotBlank String currency,
    @NotNull UUID paidByUserId,
    @NotEmpty List<UUID> participantUserIds) {}
