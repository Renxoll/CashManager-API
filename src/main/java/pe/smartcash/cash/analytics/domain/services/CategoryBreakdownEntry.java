package pe.smartcash.cash.analytics.domain.services;

import java.math.BigDecimal;

/** Una fila del desglose por categoría; {@code percentage} ya viene calculado en SQL.
 * {@code categoryId} es texto (id bigint del catálogo cerrado o UUID de una categoría de
 * módulo custom) -- el cliente solo lo usa como clave de lista. */
public record CategoryBreakdownEntry(String categoryId, String categoryName, BigDecimal amount, BigDecimal percentage) {}
