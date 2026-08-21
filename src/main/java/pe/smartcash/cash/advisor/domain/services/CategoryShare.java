package pe.smartcash.cash.advisor.domain.services;

import java.math.BigDecimal;

/** Una categoría de gasto del mes en curso, ya con su porcentaje resuelto. */
public record CategoryShare(String categoryName, BigDecimal amount, BigDecimal percentage) {}
