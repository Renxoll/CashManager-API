package pe.smartcash.cash.advisor.application.internal.outboundservices.acl;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;
import pe.smartcash.cash.advisor.domain.services.CategoryShare;
import pe.smartcash.cash.advisor.domain.services.FinancialContext;
import pe.smartcash.cash.advisor.domain.services.FinancialContextProvider;
import pe.smartcash.cash.analytics.domain.model.queries.FindMonthlySummaryQuery;
import pe.smartcash.cash.analytics.domain.services.CategoryBreakdownEntry;
import pe.smartcash.cash.analytics.domain.services.CurrencySummary;
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
    // El asesor todavía no distingue monedas en el prompt (mezclarlas en el mismo texto
    // confundiría más de lo que ayuda) -- se queda con la moneda de mayor actividad
    // (gasto+ingreso) del usuario ese mes, no necesariamente PEN. Sin ninguna transacción,
    // MonthlySummary siempre trae al menos un CurrencySummary vacío en PEN (ver
    // DashboardQueryServiceImpl), así que esto nunca lanza sobre lista vacía.
    CurrencySummary primary =
        summary.currencies().stream().max(Comparator.comparing(c -> c.totalSpent().add(c.totalIncome()), Comparator.naturalOrder())).orElseThrow();
    return new FinancialContext(primary.totalSpent(), primary.previousMonthTotal(), primary.totalIncome(), toShares(primary.breakdown()));
  }

  private List<CategoryShare> toShares(List<CategoryBreakdownEntry> breakdown) {
    return breakdown.stream().map(entry -> new CategoryShare(entry.categoryName(), entry.amount(), entry.percentage())).toList();
  }
}
