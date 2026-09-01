package pe.smartcash.cash.analytics.interfaces.rest.resources;

import java.util.List;

public record MonthlySummaryResponse(List<CurrencySummaryResponse> currencies) {}
