package pe.smartcash.cash.analytics.interfaces.rest.transform;

import pe.smartcash.cash.analytics.domain.services.MonthlySummary;
import pe.smartcash.cash.analytics.interfaces.rest.resources.CategoryPercentage;
import pe.smartcash.cash.analytics.interfaces.rest.resources.MonthlySummaryResponse;

public final class MonthlySummaryResourceFromEntityAssembler {

  private MonthlySummaryResourceFromEntityAssembler() {}

  public static MonthlySummaryResponse toResourceFromEntity(MonthlySummary summary) {
    var breakdown =
        summary.breakdown().stream()
            .map(entry -> new CategoryPercentage(entry.categoryId(), entry.categoryName(), entry.amount(), entry.percentage()))
            .toList();
    return new MonthlySummaryResponse(summary.totalSpent(), summary.previousMonthTotal(), summary.totalIncome(), breakdown);
  }
}
