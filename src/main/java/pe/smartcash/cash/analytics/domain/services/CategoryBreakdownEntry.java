package pe.smartcash.cash.analytics.domain.services;

import java.math.BigDecimal;

/** Una fila del desglose por categoría; {@code percentage} ya viene calculado en SQL. */
public record CategoryBreakdownEntry(Long categoryId, String categoryName, BigDecimal amount, BigDecimal percentage) {}
