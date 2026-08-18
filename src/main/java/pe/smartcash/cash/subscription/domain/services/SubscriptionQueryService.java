package pe.smartcash.cash.subscription.domain.services;

import java.util.Optional;
import pe.smartcash.cash.subscription.domain.model.queries.FindActiveSubscriptionByUserIdQuery;

public interface SubscriptionQueryService {

  Optional<SubscriptionDetail> handle(FindActiveSubscriptionByUserIdQuery query);
}
