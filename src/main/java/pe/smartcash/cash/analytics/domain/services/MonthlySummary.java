package pe.smartcash.cash.analytics.domain.services;

import java.util.List;

/**
 * Un {@link CurrencySummary} por cada moneda con al menos una transacción PROCESSED en el
 * mes actual o el anterior -- nunca se mezclan ni se convierten S/ y $ en un solo total, ver
 * el javadoc de {@code TransactionReadRepository}. Con datos solo en PEN (el caso normal),
 * la lista trae un único elemento.
 */
public record MonthlySummary(List<CurrencySummary> currencies) {}
