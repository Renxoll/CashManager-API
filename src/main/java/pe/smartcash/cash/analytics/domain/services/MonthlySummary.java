package pe.smartcash.cash.analytics.domain.services;

import java.math.BigDecimal;
import java.util.List;

public record MonthlySummary(
    BigDecimal totalSpent, BigDecimal previousMonthTotal, BigDecimal totalIncome, List<CategoryBreakdownEntry> breakdown) {}
