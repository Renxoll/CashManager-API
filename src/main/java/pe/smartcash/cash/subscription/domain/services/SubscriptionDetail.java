package pe.smartcash.cash.subscription.domain.services;

import java.time.Instant;
import pe.smartcash.cash.subscription.domain.model.valueobjects.PlanCode;
import pe.smartcash.cash.subscription.domain.model.valueobjects.SubscriptionId;
import pe.smartcash.cash.subscription.domain.model.valueobjects.SubscriptionStatus;
import pe.smartcash.cash.subscription.domain.model.valueobjects.UserId;

public record SubscriptionDetail(
    SubscriptionId id,
    UserId userId,
    PlanCode planCode,
    SubscriptionStatus status,
    Instant startedAt,
    Instant renewsAt,
    Instant canceledAt) {}
