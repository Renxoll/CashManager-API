package pe.smartcash.cash.analytics.application.internal.queryservices;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import pe.smartcash.cash.analytics.domain.model.queries.FindMonthlySummaryQuery;
import pe.smartcash.cash.analytics.domain.services.DashboardQueryService;
import pe.smartcash.cash.analytics.domain.services.MonthlySummary;
import pe.smartcash.cash.analytics.infrastructure.persistence.TransactionReadRepository;

/**
 * "Mes en curso"/"mes anterior" se resuelven en UTC (mismo criterio que el resto de la app,
 * que persiste todo con {@code Clock.systemUTC()} -- ver {@code TransactionDomainConfig}): no
 * hay todavía un concepto de zona horaria por usuario, así que ambos lados quedan
 * consistentes aunque no reflejen exactamente la medianoche local del usuario en Perú.
 */
@Service
class DashboardQueryServiceImpl implements DashboardQueryService {

  private final TransactionReadRepository transactionReadRepository;
  private final Clock clock;

  DashboardQueryServiceImpl(TransactionReadRepository transactionReadRepository, Clock clock) {
    this.transactionReadRepository = transactionReadRepository;
    this.clock = clock;
  }

  @Override
  public MonthlySummary handle(FindMonthlySummaryQuery query) {
    YearMonth currentMonth = YearMonth.from(clock.instant().atZone(ZoneOffset.UTC));
    YearMonth previousMonth = currentMonth.minusMonths(1);

    Instant previousMonthStart = startOf(previousMonth);
    Instant currentMonthStart = startOf(currentMonth);
    Instant nextMonthStart = startOf(currentMonth.plusMonths(1));

    var totalSpent = transactionReadRepository.sumProcessedAmount(query.userId(), currentMonthStart, nextMonthStart);
    var previousMonthTotal = transactionReadRepository.sumProcessedAmount(query.userId(), previousMonthStart, currentMonthStart);
    var breakdown = transactionReadRepository.findCategoryBreakdown(query.userId(), currentMonthStart, nextMonthStart);

    return new MonthlySummary(totalSpent, previousMonthTotal, breakdown);
  }

  private static Instant startOf(YearMonth yearMonth) {
    return yearMonth.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
  }
}
