package pe.smartcash.cash.analytics.interfaces.rest.resources;

import java.math.BigDecimal;

public record CategoryPercentage(Long categoryId, String categoryName, BigDecimal amount, BigDecimal percentage) {}
