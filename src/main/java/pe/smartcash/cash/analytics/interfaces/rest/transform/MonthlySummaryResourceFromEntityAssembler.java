package pe.smartcash.cash.analytics.interfaces.rest.transform;

import pe.smartcash.cash.analytics.domain.services.CurrencySummary;
import pe.smartcash.cash.analytics.domain.services.MonthlySummary;
import pe.smartcash.cash.analytics.interfaces.rest.resources.CategoryPercentage;
import pe.smartcash.cash.analytics.interfaces.rest.resources.CurrencySummaryResponse;
import pe.smartcash.cash.analytics.interfaces.rest.resources.MonthlySummaryResponse;

public final class MonthlySummaryResourceFromEntityAssembler {

  private MonthlySummaryResourceFromEntityAssembler() {}

  public static MonthlySummaryResponse toResourceFromEntity(MonthlySummary summary) {
    return new MonthlySummaryResponse(summary.currencies().stream().map(MonthlySummaryResourceFromEntityAssembler::toResource).toList());
  }

  private static CurrencySummaryResponse toResource(CurrencySummary summary) {
    var breakdown =
        summary.breakdown().stream()
            .map(entry -> new CategoryPercentage(entry.categoryId(), entry.categoryName(), entry.amount(), entry.percentage()))
            .toList();
    return new CurrencySummaryResponse(summary.currency(), summary.totalSpent(), summary.previousMonthTotal(), summary.totalIncome(), breakdown);
  }
}
