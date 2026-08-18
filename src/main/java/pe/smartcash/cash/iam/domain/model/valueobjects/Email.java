package pe.smartcash.cash.iam.domain.model.valueobjects;

import java.util.Locale;
import java.util.regex.Pattern;

public record Email(String value) {

  private static final Pattern SIMPLE_EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

  public Email {
    if (value == null || !SIMPLE_EMAIL.matcher(value).matches()) {
      throw new IllegalArgumentException("Email inválido: " + value);
    }
    value = value.trim().toLowerCase(Locale.ROOT);
  }
}
