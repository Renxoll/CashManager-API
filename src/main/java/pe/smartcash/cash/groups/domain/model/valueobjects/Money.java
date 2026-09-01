package pe.smartcash.cash.groups.domain.model.valueobjects;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.regex.Pattern;

/** Copia deliberada de {@code transactions.domain.model.valueobjects.Money} -- cada
 * bounded context define sus propios value objects, ver {@link UserId}. */
public record Money(BigDecimal amount, String currency) {

  private static final Pattern ISO_4217 = Pattern.compile("^[A-Z]{3}$");

  public Money {
    Objects.requireNonNull(amount, "amount");
    Objects.requireNonNull(currency, "currency");
    if (amount.signum() < 0) {
      throw new IllegalArgumentException("El monto no puede ser negativo: " + amount);
    }
    if (amount.scale() > 2) {
      throw new IllegalArgumentException("El monto no puede tener más de 2 decimales: " + amount);
    }
    if (!ISO_4217.matcher(currency).matches()) {
      throw new IllegalArgumentException("Moneda inválida, se espera código ISO 4217: " + currency);
    }
  }
}
