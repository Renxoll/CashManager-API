package pe.smartcash.cash.transactions.domain.services;

import java.util.List;
import pe.smartcash.cash.transactions.domain.model.queries.FindPendingSendersByUserQuery;

public interface PendingSenderQueryService {

  List<PendingSenderDetail> handle(FindPendingSendersByUserQuery query);
}
