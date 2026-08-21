package pe.smartcash.cash.analytics.interfaces.rest.resources;

import java.math.BigDecimal;
import java.util.List;

public record MonthlySummaryResponse(BigDecimal totalSpent, BigDecimal previousMonthTotal, List<CategoryPercentage> breakdown) {}
