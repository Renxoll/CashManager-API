package pe.smartcash.cash.groups.interfaces.rest.resources;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record RecordSettlementResource(@NotNull UUID toUserId, @NotNull @DecimalMin(value = "0.01") BigDecimal amount, @NotBlank String currency) {}
