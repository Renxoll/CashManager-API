package pe.smartcash.cash.transactions.interfaces.rest.resources;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** {@code currency} opcional (por defecto PEN). {@code categoryCode} es obligatorio solo al
 * completar un gasto FAILED; en uno ya leído se ignora. */
public record CorrectTransactionDetailsResource(
    @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal amount,
    String currency,
    @NotBlank @Size(max = 100) String merchant,
    String categoryCode) {}
