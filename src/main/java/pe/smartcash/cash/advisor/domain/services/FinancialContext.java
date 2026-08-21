package pe.smartcash.cash.advisor.domain.services;

import java.math.BigDecimal;
import java.util.List;

/**
 * Forma propia de este contexto para "los datos financieros de un usuario", separada a
 * propósito de {@code analytics.domain.services.MonthlySummary}: el ACL (ver
 * {@code FinancialContextAdapter}) traduce uno en el otro en el borde, así que advisor nunca
 * importa tipos de dominio de analytics más allá de su API pública de lectura.
 */
public record FinancialContext(BigDecimal totalSpent, BigDecimal previousMonthTotal, List<CategoryShare> breakdown) {}
