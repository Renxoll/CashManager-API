package pe.smartcash.cash.transactions.domain.service;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Merchant;
import pe.smartcash.cash.transactions.domain.model.valueobjects.Money;

/**
 * Servicio de dominio puro (sin I/O, sin anotaciones de framework): interpreta el caso feliz
 * "SÍMBOLO+monto en Comercio" (p. ej. "Consumo de S/24.50 en Starbucks") para poder
 * aprovechar el cache de comercios sin pagar una llamada al LLM. Cualquier formato que no
 * calce con precisión (miles, comas decimales, redacciones distintas) devuelve
 * {@code Optional.empty()} y el flujo sigue con el LLM, que es la fuente de verdad real.
 */
public final class BankNotificationHeuristicParser {

  private static final Pattern AMOUNT_PATTERN =
      Pattern.compile("(S/|\\$|USD|PEN|EUR)\\s?(\\d+\\.\\d{2})", Pattern.CASE_INSENSITIVE);

  private static final Pattern MERCHANT_PATTERN = Pattern.compile("\\ben\\s+([\\p{L}0-9.,'&\\- ]{2,60})$");

  public Optional<ParsedHint> parse(String rawText) {
    Matcher amountMatcher = AMOUNT_PATTERN.matcher(rawText);
    if (!amountMatcher.find()) {
      return Optional.empty();
    }
    Matcher merchantMatcher = MERCHANT_PATTERN.matcher(rawText.trim());
    if (!merchantMatcher.find()) {
      return Optional.empty();
    }

    BigDecimal amount = new BigDecimal(amountMatcher.group(2));
    String currency = mapSymbolToCurrency(amountMatcher.group(1));
    String merchantName = normalizeMerchant(merchantMatcher.group(1).trim());

    return Optional.of(new ParsedHint(new Money(amount, currency), new Merchant(merchantName)));
  }

  private String mapSymbolToCurrency(String symbol) {
    return switch (symbol.toUpperCase(Locale.ROOT)) {
      case "S/", "PEN" -> "PEN";
      case "USD" -> "USD";
      case "EUR" -> "EUR";
      default -> "USD"; // "$"
    };
  }

  private String normalizeMerchant(String raw) {
    StringBuilder result = new StringBuilder();
    for (String word : raw.toLowerCase(Locale.ROOT).split("\\s+")) {
      if (!word.isEmpty()) {
        result.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(' ');
      }
    }
    return result.toString().trim();
  }
}
