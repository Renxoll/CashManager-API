package pe.smartcash.cash.transactions.application.internal.queryservices;

import java.util.List;
import org.springframework.stereotype.Service;
import pe.smartcash.cash.transactions.domain.model.aggregates.PendingSender;
import pe.smartcash.cash.transactions.domain.model.aggregates.PendingSenderRepository;
import pe.smartcash.cash.transactions.domain.model.queries.FindPendingSendersByUserQuery;
import pe.smartcash.cash.transactions.domain.services.PendingSenderDetail;
import pe.smartcash.cash.transactions.domain.services.PendingSenderQueryService;

@Service
class PendingSenderQueryServiceImpl implements PendingSenderQueryService {

  private final PendingSenderRepository pendingSenderRepository;

  PendingSenderQueryServiceImpl(PendingSenderRepository pendingSenderRepository) {
    this.pendingSenderRepository = pendingSenderRepository;
  }

  @Override
  public List<PendingSenderDetail> handle(FindPendingSendersByUserQuery query) {
    return pendingSenderRepository.findAllPendingByUserId(query.userId()).stream().map(this::toDetail).toList();
  }

  private PendingSenderDetail toDetail(PendingSender pendingSender) {
    return new PendingSenderDetail(
        pendingSender.id(),
        pendingSender.fromAddress(),
        pendingSender.domain(),
        pendingSender.sampleSnippet(),
        pendingSender.occurrenceCount(),
        pendingSender.firstSeenAt(),
        pendingSender.lastSeenAt());
  }
}
