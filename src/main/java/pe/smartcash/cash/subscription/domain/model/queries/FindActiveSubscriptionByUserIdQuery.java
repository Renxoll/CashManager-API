package pe.smartcash.cash.subscription.domain.model.queries;

import pe.smartcash.cash.subscription.domain.model.valueobjects.UserId;

public record FindActiveSubscriptionByUserIdQuery(UserId userId) {}
