package pe.smartcash.cash.analytics.interfaces.rest.resources;

import java.math.BigDecimal;
import java.util.List;

public record CurrencySummaryResponse(
    String currency, BigDecimal totalSpent, BigDecimal previousMonthTotal, BigDecimal totalIncome, List<CategoryPercentage> breakdown) {}
