package pe.smartcash.cash.advisor.application.internal.outboundservices.acl;

import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import pe.smartcash.cash.advisor.domain.services.CategoryShare;
import pe.smartcash.cash.advisor.domain.services.FinancialContext;
import pe.smartcash.cash.advisor.domain.services.FinancialContextProvider;
import pe.smartcash.cash.analytics.domain.model.queries.FindMonthlySummaryQuery;
import pe.smartcash.cash.analytics.domain.services.CategoryBreakdownEntry;
import pe.smartcash.cash.analytics.domain.services.DashboardQueryService;
import pe.smartcash.cash.analytics.domain.services.MonthlySummary;

/**
 * Anti-Corruption Layer entre Advisor y Analytics: único punto donde este contexto habla con
 * Analytics, y solo a través de su API pública de dominio ({@link DashboardQueryService}) —
 * mismo patrón que {@code transactions...acl.UserDirectoryAdapter} hacia Profile. Vive en
 * application (no infrastructure): no hace I/O técnico propio, solo invoca otro Spring bean
 * en el mismo proceso.
 */
@Component
class FinancialContextAdapter implements FinancialContextProvider {

  private final DashboardQueryService dashboardQueryService;

  FinancialContextAdapter(DashboardQueryService dashboardQueryService) {
    this.dashboardQueryService = dashboardQueryService;
  }

  @Override
  public FinancialContext currentMonthContext(UUID userId) {
    MonthlySummary summary = dashboardQueryService.handle(new FindMonthlySummaryQuery(userId));
    return new FinancialContext(summary.totalSpent(), summary.previousMonthTotal(), toShares(summary.breakdown()));
  }

  private List<CategoryShare> toShares(List<CategoryBreakdownEntry> breakdown) {
    return breakdown.stream().map(entry -> new CategoryShare(entry.categoryName(), entry.amount(), entry.percentage())).toList();
  }
}
