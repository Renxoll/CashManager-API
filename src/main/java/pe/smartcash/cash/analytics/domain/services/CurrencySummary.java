package pe.smartcash.cash.analytics.domain.services;

import java.math.BigDecimal;
import java.util.List;

/** El resumen del mes, acotado a una sola moneda -- nunca se suman ni se convierten montos
 * entre monedas distintas, ver {@code MonthlySummary}. */
public record CurrencySummary(
    String currency, BigDecimal totalSpent, BigDecimal previousMonthTotal, BigDecimal totalIncome, List<CategoryBreakdownEntry> breakdown) {}
